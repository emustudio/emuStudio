/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.debugger.Debugging;
import net.emustudio.emulib.plugins.debugger.StepView;
import net.emustudio.emulib.plugins.debugger.ValueView;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;

/** Host debugging policy; plugins interpret machine state at the execution seam. */
public final class DebugSession implements AutoCloseable, Debugging.Observer {
    private final net.emustudio.application.emulation.EmulationController execution;
    public net.emustudio.application.emulation.EmulationController execution() { return execution; }
    public final CPU cpu;
    public final Debugging debug;
    public final MemoryContext<?> memory;
    public final MemoryAnnotations annotationEngine;
    public final List<String> watches = new CopyOnWriteArrayList<>();
    public final List<Rule> rules = new CopyOnWriteArrayList<>();
    private final Path settings;
    private Properties lastSaved;
    private AnnotationIndex annotations;
    private SourceAddressMap sourceMappings;
    private boolean sourceMappingsCleared;
    private volatile ProgramIndex programIndex;
    // Never acquire the session monitor while holding this lock in a memory notification.
    private final Object indexPublicationLock = new Object();
    private final Runnable annotationListener = () -> {
        synchronized (indexPublicationLock) {
            if (programIndex != null) programIndex.annotationsChanged();
        }

    };
    private final IdentityHashMap<Rule, Long> executionRuleIds = new IdentityHashMap<>();
    private long nextExecutionRuleId;
    private Rule[] evaluationRules = new Rule[0];
    private long generation;
    private boolean closed;
    private int activeMutations;
    private SourceCaptureGuard activeSourceCapture;
    private SourceCaptureGuard completedSourceCapture;
    private BreakpointHit pendingHit;

    /** Application-owned index; no memory cells are read to construct it. */
    public synchronized ProgramIndex programIndex() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        ProgramIndex.Layout layout = programIndex == null ? null : programIndex.snapshot().layout();
        int size = cpu.getAddressSpaceSize();
        if (layout == null || layout.addressSpaceSize() != size) {
            ProgramIndex replacement = ProgramIndex.create(cpu.getTitle() + ":" + size, size, annotationEngine);
            synchronized (indexPublicationLock) {
                programIndex = replacement;
            }
            annotations = null;
            generation++;
        }
        return programIndex;
    }

    public synchronized void setSourceMappings(SourceAddressMap mappings) {
        requirePaused();
        if (mappings.programIndex() != programIndex())
            throw new IllegalStateException("Source mappings belong to another memory image");
        sourceMappings = mappings;
        sourceMappingsCleared = false;

    }

    /** Checks captured mappings for staleness; uncaptured compiler mappings remain directly accessible. */
    public synchronized Optional<SourceCodePosition> sourceAt(int address) {
        if (sourceMappings != null) {
            if (sourceMappings.programIndex() != programIndex()) return Optional.empty();
            return sourceMappings.sourceAt(address).map(location -> location.position);
        }
        if (sourceMappingsCleared) return Optional.empty();
        return annotationEngine.at(address).stream().filter(entry -> entry.source != null)
                .min(Comparator.comparingLong(entry -> entry.ownerId)).map(entry -> entry.source);
    }

    public synchronized void clearSourceMappings() {
        programIndex();
        sourceMappings = null;
        sourceMappingsCleared = true;

    }

    /** Live reads never retain a copy of memory cells. Reacquire after a machine or metadata edit. */
    public LiveMemory liveMemory() {
        if (memory == null) throw new IllegalStateException("Memory unavailable");
        Stamp stamp = liveStamp();
        return new LiveMemory(memory, stamp.base.layout(), () -> requireCurrent(stamp));
    }

    /** User metadata is restored once at startup; reads need no disk access or paused CPU. */
    public synchronized AnnotationIndex annotations() {
        ProgramIndex index = programIndex();
        if (annotations == null) annotations = new AnnotationIndex(index, annotationEngine);
        return annotations;
    }

    private final Deque<InstructionRecording.Event> history = new ArrayDeque<>();
    private static final long MAX_HISTORY_BYTES = 32L * 1024 * 1024;
    private long historyBytes, historyCaptured, nextSequence;
    private boolean historyLimited;
    private final Map<String, Debugging.Expression> expressions = new HashMap<>();
    private volatile Map<String, ValueView> watchSnapshot = Map.of();
    private volatile Map<String, String> watchFailures = Map.of();
    private long nextWatchRefresh;
    private AutoCloseable observation;
    private boolean captureAttached, skipBeforeStep, observationDirty;
    private int historyLimit;
    public enum RecordingMode { FIRST, LAST, ADDRESS_RANGE, UNLIMITED }
    private RecordingMode recordingMode = RecordingMode.LAST;
    private int recordingFrom, recordingThrough;
    private InstructionRecording.Writer recording;
    private Path recordingPath;
    private String recordingError = "";
    private int stoppedAddress = -1;
    public volatile String status = "";
    private volatile CPU.RunState state = CPU.RunState.STATE_STOPPED_BREAK;
    private final MemoryContext.MemoryListener memoryListener =
            new MemoryContext.MemoryListener() {
                @Override
                public void memoryContentChanged(int from, int to) {
                    invalidateAnalysis();
                }

                @Override
                public void memorySizeChanged() {
                    invalidateAnalysis();

                }
            };
    /** Host calls once per changed execution snapshot, before notifying views. */
    public synchronized void executionChanged(CPU.RunState value) {
        boolean starting = state != CPU.RunState.STATE_RUNNING && value == CPU.RunState.STATE_RUNNING;
        state = value;
        advanceHostGeneration();
        if (starting) {
            clearWatchChanges();
            pendingHit = null;
            if (sourceMappings == null) sourceMappingsCleared = true;
            publishBreakpoints();
            invalidateIndex();
        }
    }

    public DebugSession(CPU cpu, MemoryContext<?> memory, Path settings) throws IOException {
        this(cpu, memory, settings, new MemoryAnnotations());
    }

    public DebugSession(CPU cpu, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine) throws IOException {
        this(cpu, memory, settings, annotationEngine, null, List.of(), null, new net.emustudio.application.ExecutionStateImpl());
    }

    public DebugSession(CPU cpu, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine,
                        net.emustudio.emulib.plugins.memory.Memory memoryPlugin,
                        List<net.emustudio.emulib.plugins.device.Device> devices,
                        net.emustudio.emulib.plugins.compiler.Compiler compiler,
                        net.emustudio.application.ExecutionStateImpl executionState) throws IOException {
        this.cpu = Objects.requireNonNull(cpu);
        this.annotationEngine = Objects.requireNonNull(annotationEngine);
        this.debug = cpu instanceof Debugging ? (Debugging) cpu : null;
        this.memory = memory;
        this.settings = settings;
        if (this.memory != null || cpu.getAddressSpaceSize() > 0) annotations();
        load();
        annotationEngine.addListener(annotationListener);
        if (this.memory != null) this.memory.addMemoryListener(memoryListener);
        execution = new net.emustudio.application.emulation.EmulationController(cpu, memoryPlugin, devices, this, compiler, executionState);
        update(() -> {}).join();
    }

    public synchronized void requirePaused() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        if (state == CPU.RunState.STATE_RUNNING) throw new IllegalStateException("Pause CPU first");
    }

    synchronized long pausedGeneration() {
        requirePaused();
        requireNoMutation();
        return generation;
    }

    /** Invalidates analysis before and after a machine edit, even if the CPU emits no event. */
    public void runMutation(Runnable mutation) {
        synchronized (this) { stoppedAddress = -1; clearWatchChanges(); }
        runExecution(mutation);
    }

    public void runExecution(Runnable execution) {
        try {
            runMachineOperation(execution, true);
        } finally {
            // A paused view must show the final machine value, even after a very short step.
            synchronized (this) { if (!closed) refreshWatchesNow(); }
        }
    }

    /** Compilers report memory writes through the memory listener while execution is paused. */
    public void runCompilation(Runnable compilation) {
        synchronized (this) { stoppedAddress = -1; skipBeforeStep = false; }
        boolean restoreNotifications = memory != null && !memory.areMemoryNotificationsEnabled();
        if (restoreNotifications) memory.setMemoryNotificationsEnabled(true);
        try {
            runMachineOperation(compilation, false);
        } finally {
            if (restoreNotifications) memory.setMemoryNotificationsEnabled(false);
        }
    }

    private void runMachineOperation(Runnable mutation, boolean invalidateMemory) {
        Objects.requireNonNull(mutation);
        SourceCaptureGuard operation = new SourceCaptureGuard();
        synchronized (this) {
            if (closed) throw new IllegalStateException("Debugger session closed");
            pendingHit = null;
            generation++;
            if (invalidateMemory) invalidateIndex();
            activeMutations++;
            activeSourceCapture = operation;
        }
        try {
            mutation.run();
        } finally {
            synchronized (this) {
                activeMutations--;
                generation++;
                if (invalidateMemory) invalidateIndex();
                if (memory != null && !closed) programIndex();
                operation.stamp = new Stamp(programIndex, generation);
                completedSourceCapture = operation;
                if (activeMutations == 0) activeSourceCapture = null;
            }
            if (!execution.snapshot().isBusy()) operation.ready.complete(null);
        }
    }

    /** Carries a just-finished operation across host notifications, never across unrelated edits. */
    private void advanceHostGeneration() {
        SourceCaptureGuard guard = completedSourceCapture;
        boolean current = guard != null && !guard.ready.isDone() && guard.stamp != null
                && guard.stamp.generation == generation && guard.stamp.matches(programIndex, true);
        generation++;
        if (current) guard.stamp = new Stamp(programIndex, generation);
    }

    /** Called by the command scheduler after final lifecycle publication. */
    public void completeSourceCapture() {
        SourceCaptureGuard guard;
        synchronized (this) {
            guard = completedSourceCapture;
            completedSourceCapture = null;
        }
        if (guard != null) guard.ready.complete(null);
    }

    synchronized void applyPaused(long expectedGeneration, Runnable apply) {
        requirePaused();
        requireNoMutation();
        if (generation != expectedGeneration)
            throw new IllegalStateException("Analysis discarded: machine changed during analysis");
        apply.run();
    }

    private synchronized Stamp liveStamp() {
        requirePaused();
        requireNoMutation();
        programIndex();
        return new Stamp(programIndex, generation);
    }

    /** Paused stamp for analyses that may run without a program index. */
    synchronized Stamp pausedStamp() {
        return new Stamp(programIndex, pausedGeneration());
    }

    /** Machine generation and index revisions observed together; later work is valid only while both hold. */
    static final class Stamp {
        final ProgramIndex index;
        final ProgramIndex.Snapshot base;
        final long generation;

        private Stamp(ProgramIndex index, long generation) {
            this.index = index;
            this.base = index == null ? null : index.snapshot();
            this.generation = generation;
        }

        private boolean matches(ProgramIndex current, boolean content) {
            if (current != index) return false;
            if (index == null) return true;
            ProgramIndex.Snapshot now = index.snapshot();
            return content ? now.sameRevisions(base) : now.memoryRevision() == base.memoryRevision();
        }
    }

    /** Runs apply only while the stamped generation and index revisions (content optionally) are current. */
    synchronized void applyCurrent(Stamp stamp, boolean content, String message, Runnable apply) {
        applyPaused(stamp.generation, () -> {
            if (!stamp.matches(programIndex, content)) throw new IllegalStateException(message);
            apply.run();
        });
    }

    private void requireCurrent(Stamp stamp) {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        applyCurrent(stamp, true, "Analysis discarded: memory, source or metadata changed", () -> {});
    }

    /** Use the CPU's wired decoder off the UI thread; publish only while its paused root is current. */
    public FlowAnalysis analyzeFlow(int start, int limit) {
        Stamp stamp = liveStamp();
        Runnable validate = () -> requireCurrent(stamp);
        Disassembler decoder = guardedDecoder(cpu.getDisassembler(), validate);
        FlowAnalysis.Pending pending = FlowAnalysis.reachable(decoder, start, limit, stamp.index, stamp.base);
        synchronized (this) {
            validate.run();
            return pending.publish();
        }
    }

    private static Disassembler guardedDecoder(Disassembler decoder, Runnable validate) {
        return new Disassembler() {
            @Override public Optional<InstructionFlow> getFlow(int address) {
                validate.run();
                Optional<InstructionFlow> flow = decoder.getFlow(address);
                validate.run();
                return flow;
            }

            @Override public DisassembledInstruction disassemble(int address) throws InvalidInstructionException {
                validate.run();
                DisassembledInstruction instruction = decoder.disassemble(address);
                validate.run();
                return instruction;
            }

            @Override public int getNextInstructionPosition(int address) {
                validate.run();
                int next = decoder.getNextInstructionPosition(address);
                validate.run();
                return next;
            }
        };
    }

    /** Associates a compiler callback with its enclosing machine mutation, including its reset. */
    public synchronized SourceCaptureGuard captureSourceGuard() {
        requirePaused();
        if (activeMutations > 1) throw new IllegalStateException("Concurrent machine mutations prevent source capture");
        if (activeMutations == 1) return activeSourceCapture;
        SourceCaptureGuard guard = new SourceCaptureGuard();
        guard.stamp = new Stamp(programIndex, generation);
        guard.ready.complete(null);
        return guard;
    }

    public synchronized void applySourceCapture(SourceCaptureGuard guard, Runnable apply) {
        Objects.requireNonNull(guard);
        Stamp stamp = guard.stamp;
        if (stamp == null) throw new IllegalStateException("Compilation mutation has not finished");
        synchronized (indexPublicationLock) {
            applyCurrent(stamp, true, "Source mappings discarded: machine or metadata changed after compilation", apply);
        }
    }

    public static final class SourceCaptureGuard {
        private volatile Stamp stamp;
        private final java.util.concurrent.CompletableFuture<Void> ready = new java.util.concurrent.CompletableFuture<>();
        public java.util.concurrent.CompletableFuture<Void> whenReady() { return ready.copy(); }
        private SourceCaptureGuard() { }
    }

    private void invalidateAnalysis() {
        // Writers notify after changing cells. Advance the index before waiting for a paused
        // analysis lease, so its publication cannot race a blocked memory-listener callback.
        invalidateIndex();
        synchronized (this) {
            if (sourceMappings == null) sourceMappingsCleared = true;
            generation++;
        }
    }

    private void invalidateIndex() {
        synchronized (indexPublicationLock) {
            if (programIndex != null) programIndex.memoryChanged();
        }
    }

    private void requireNoMutation() {
        if (activeMutations != 0)
            throw new IllegalStateException("Analysis discarded: machine mutation in progress");
    }

    /** Serializes debugger edits without pausing or invalidating program provenance. */
    public CompletableFuture<Void> update(Runnable change) {
        if (execution.isWorkerThread()) {
            try { applyUpdate(change); return CompletableFuture.completedFuture(null); }
            catch (Throwable failure) { return CompletableFuture.failedFuture(failure); }
        }
        return execution.update(() -> applyUpdate(change), false);
    }

    private synchronized void applyUpdate(Runnable change) {
        if (closed) throw new IllegalStateException("Debugger session closed");
        try {
            change.run();
        } finally {
            // An I/O failure after editing settings must not leave hidden old runtime behavior.
            updateProbe();
            refreshWatchesNow();
            Set<String> retainedExpressions = new HashSet<>(watches);
            rules.forEach(rule -> retainedExpressions.add(ruleSource(rule)));
            expressions.keySet().retainAll(retainedExpressions);
            if (status.startsWith("Inactive rule ") && rules.stream().allMatch(rule -> rule.error.isBlank())) status = "";
        }
    }

    public synchronized void prepareManualStep() { skipBeforeStep = true; }
    public synchronized void completeManualStep() { skipBeforeStep = false; }

    public synchronized void record(int limit) {
        if (limit > 0 && debug == null) throw new IllegalStateException("Instruction recording unsupported by this CPU");
        if (limit < 0 || limit > 1_000_000) throw new IllegalArgumentException("History limit must be 0..1000000");
        historyLimit = limit;
        if (limit > 0) {
            if (status.equals(recordingError)) status = "";
            recordingError = "";
        }
        recordingMode = RecordingMode.LAST;
        trimHistory();
        updateProbe();
    }

    public synchronized void startNewRecording(int limit, RecordingMode mode, int from, int through) {
        Objects.requireNonNull(mode);
        boolean bounded = mode == RecordingMode.FIRST || mode == RecordingMode.LAST;
        if (bounded && (limit <= 0 || limit > 1_000_000)) throw new IllegalArgumentException("Recording limit must be 1..1000000");
        if (mode == RecordingMode.ADDRESS_RANGE && (from < 0 || through < from || through >= cpu.getAddressSpaceSize()))
            throw new IllegalArgumentException("Recording range must be within CPU addresses, with From ≤ To");
        record(bounded ? limit : 1);
        historyLimit = bounded ? limit : Integer.MAX_VALUE;
        history.clear();
        historyBytes = 0;
        historyCaptured = 0;
        historyLimited = false;
        recordingMode = mode;
        recordingFrom = from;
        recordingThrough = through;
    }

    public synchronized boolean isRecording() { return historyLimit > 0; }
    public synchronized RecordingMode recordingMode() { return recordingMode; }
    public synchronized int recordingLimit() { return historyLimit; }
    public synchronized int recordingFrom() { return recordingFrom; }
    public synchronized int recordingThrough() { return recordingThrough; }
    public synchronized List<InstructionRecording.Event> history() { return List.copyOf(history); }

    private synchronized InstructionRecording.Header recordingHeader() {
        if (debug == null) throw new IllegalStateException("Instruction recording unsupported by this CPU");
        return new InstructionRecording.Header(cpu.getTitle());
    }

    public synchronized void recordTo(Path path) throws IOException {
        if (recording != null) throw new IllegalStateException("Stop the current file recording first");
        try {
            recording = new InstructionRecording.Writer(path, recordingHeader());
        } catch (IOException failure) {
            stopFailedRecorder("Could not start recording: " + failure.getMessage());
            throw failure;
        }
        recordingPath = path.toAbsolutePath().normalize();
        if (status.equals(recordingError)) status = "";
        recordingError = "";
        updateProbe();
    }

    public synchronized void stopRecordingFile() throws IOException {
        InstructionRecording.Writer writer = recording;
        recording = null;
        recordingPath = null;
        try { if (writer != null) writer.close(); }
        catch (IOException failure) {
            recordingError = "Incomplete recording: " + failure.getMessage();
            status = recordingError;
            throw failure;
        } finally { updateProbe(); }
    }

    public synchronized Path recordingPath() { return recordingPath; }
    public synchronized String recordingError() { return recordingError; }

    public void saveHistory(Path path, boolean text) throws IOException {
        InstructionRecording.Header header = recordingHeader();
        List<InstructionRecording.Event> events = history();
        if (text) InstructionRecording.exportText(path, header, events);
        else InstructionRecording.write(path, header, events);
    }

    public synchronized void updateProbe() {
        observationDirty = false;
        compileRules();
        publishBreakpoints();
        boolean needed = recording != null || historyLimit > 0 || !watches.isEmpty() || rules.stream().anyMatch(Rule::isActive);
        boolean capture = recording != null || historyLimit > 0;
        if (debug == null) return;
        if (observation != null && (!needed || capture != captureAttached)) detachObservation();
        if (needed && observation == null) {
            observation = debug.observe(this, capture);
            captureAttached = capture;
        }
    }

    private void detachObservation() {
        AutoCloseable current = observation;
        observation = null;
        if (current != null) {
            try { current.close(); }
            catch (Exception failure) { status = "Debugger detach failed: " + failure.getMessage(); }
        }
    }

    private void compileRules() {
        for (Rule rule : rules) {
            rule.compiled = null;
            if (!rule.enabled) { rule.failed = false; rule.error = ""; continue; }
            if (rule.failed) continue;
            rule.error = "";
            String text = ruleSource(rule);
            if (text.isBlank() && rule.kind.equals("EXEC")) continue;
            try { rule.compiled = expression(text); }
            catch (RuntimeException failure) { rule.error = Objects.toString(failure.getMessage(), "Invalid condition"); }
        }
    }

    private void trimHistory() {
        while (!history.isEmpty() && ((historyLimit > 0 && history.size() > historyLimit) || historyBytes > MAX_HISTORY_BYTES)) {
            if (historyBytes > MAX_HISTORY_BYTES && !historyLimited) {
                historyLimited = true;
                recordingError = "History retention limit reached; earlier captured steps are no longer retained";
                status = recordingError;
            }
            InstructionRecording.Event removed = history.removeFirst();
            try { historyBytes -= InstructionRecording.sizeOf(removed); }
            catch (IOException impossible) { throw new IllegalStateException(impossible); }
        }
    }

    private static String ruleSource(Rule rule) {
        String text = rule.condition;
        return rule.kind.equals("READ") || rule.kind.equals("WRITE")
                ? rule.kind.toLowerCase(Locale.ROOT) + "(" + rule.address + ")"
                        + (text.isBlank() ? "" : " && (" + text + ")")
                : text;
    }

    /** Replays logical editor state only at a supported paused edit/start boundary. */
    private void publishBreakpoints() {
        if (programIndex == null && memory == null && cpu.getAddressSpaceSize() <= 0) return;
        ProgramIndex index = programIndex();
        index.editBreakpoints((base, update) -> {
            for (long id : executionRuleIds.values()) update.remove(id);
            for (Rule rule : rules) {
                if (!rule.kind.equals("EXEC")) continue;
                if (rule.address >= base.layout().addressSpaceSize()) continue;
                long id = executionRuleIds.computeIfAbsent(rule, ignored -> nextExecutionRuleId++);
                update.put(id, rule.address, rule.isActive());
            }
            return true;
        });
        evaluationRules = rules.toArray(new Rule[0]);
        executionRuleIds.keySet().removeIf(rule -> !rules.contains(rule)
                || rule.address >= index.snapshot().layout().addressSpaceSize());
    }

    private synchronized Debugging.Expression expression(String source) {
        if (debug == null) throw new IllegalStateException("Expressions unsupported by this CPU");
        return expressions.computeIfAbsent(source, debug::compile);
    }

    public boolean evaluateCondition(String source) {
        if (execution.isWorkerThread()) return expression(source).test();
        AtomicReference<Boolean> truth = new AtomicReference<>();
        update(() -> truth.set(expression(source).test())).join();
        return truth.get();
    }

    public Map<String, ValueView> watchValues() { return watchSnapshot; }
    public Map<String, String> watchErrors() { return watchFailures; }
    public CompletableFuture<Void> refreshWatches() { return update(() -> {}); }

    private void refreshWatchesNow() {
        Map<String, ValueView> values = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (String watch : watches) {
            String error = watchFailures.get(watch);
            if (error != null) {
                errors.put(watch, error);
                continue;
            }
            try {
                ValueView current = expression(watch).display();
                ValueView previous = watchSnapshot.get(watch);
                values.put(watch, new ValueView(current.text, current.formats,
                        current.changed || previous != null && previous.changed));
            }
            catch (RuntimeException failure) {
                errors.put(watch, Objects.toString(failure.getMessage(), "Watch evaluation failed"));
            }
        }
        watchSnapshot = Map.copyOf(values);
        watchFailures = Map.copyOf(errors);
        nextWatchRefresh = System.nanoTime() + 50_000_000L;
    }

    private void sampleWatches() {
        if (!watches.isEmpty() && System.nanoTime() >= nextWatchRefresh) refreshWatchesNow();
    }

    private void clearWatchChanges() {
        Map<String, ValueView> cleared = new LinkedHashMap<>();
        watchSnapshot.forEach((source, value) -> cleared.put(source, new ValueView(value.text, value.formats, false)));
        watchSnapshot = Map.copyOf(cleared);
    }

    @Override
    public synchronized boolean beforeStep() {
        int address = cpu.getInstructionLocation();
        if (skipBeforeStep || stoppedAddress == address) {
            skipBeforeStep = false;
            stoppedAddress = -1;
            sampleWatches();
            return false;
        }
        stoppedAddress = -1;
        return checkRules(address, true);
    }

    private void deactivate(Rule rule, RuntimeException failure) {
        rule.failed = true;
        rule.error = Objects.toString(failure.getMessage(), "Condition evaluation failed");
        status = "Inactive rule " + rule.name + ": " + rule.error;
        org.slf4j.LoggerFactory.getLogger(DebugSession.class).warn(status);
    }

    @Override
    public synchronized boolean afterStep(boolean completed, Optional<StepView> captured) {
        long sequence = ++nextSequence;
        if (captured.isPresent()) {
            InstructionRecording.Event event = new InstructionRecording.Event(sequence, completed, captured.get());
            int address = event.view.programLocation.orElse(-1);
            if (historyLimit > 0 && (recordingMode != RecordingMode.ADDRESS_RANGE
                    || (address >= recordingFrom && address <= recordingThrough))) {
                if (recordingMode != RecordingMode.FIRST || historyCaptured < historyLimit) {
                    try {
                        int bytes = InstructionRecording.sizeOf(event);
                        historyCaptured++;
                        history.addLast(event);
                        historyBytes += bytes;
                        trimHistory();
                    } catch (IOException failure) { failed("History capture failed: " + failure.getMessage()); }
                }
            }
            if (recording != null) {
                try { recording.append(event); }
                catch (IOException failure) { stopFailedRecorder("Incomplete recording: " + failure.getMessage()); }
            }
        }
        int address = captured.isPresent() && captured.get().programLocation.isPresent()
                ? captured.get().programLocation.getAsInt() : cpu.getInstructionLocation();
        return checkRules(address, false);
    }

    private boolean checkRules(int address, boolean before) {
        boolean stop = false;
        for (Rule rule : evaluationRules) {
            boolean execution = rule.kind.equals("EXEC");
            if (execution != before || (execution && rule.address != address) || !rule.isActive()) continue;
            try {
                boolean matches = rule.compiled == null ? ruleSource(rule).isBlank() : rule.compiled.test();
                if (matches && ++rule.hits >= rule.afterHits) {
                    if (before) stoppedAddress = address;
                    status = (before ? "Breakpoint: " : "Watchpoint: ") + rule.name;
                    pendingHit = new BreakpointHit(rule.address >= 0 ? rule.address : address, rule);
                    stop = true;
                }
            } catch (RuntimeException failure) { deactivate(rule, failure); }
        }
        sampleWatches();
        if (observationDirty) updateProbe();
        return stop;
    }

    @Override
    public synchronized void failed(String message) {
        historyLimit = 0;
        captureAttached = false;
        stopFailedRecorder(message);
    }

    private void stopFailedRecorder(String message) {
        observationDirty = true;
        recordingError = message;
        status = message;
        InstructionRecording.Writer current = recording;
        recording = null;
        recordingPath = null;
        if (current != null) {
            try { current.abort(); }
            catch (IOException failure) {
                org.slf4j.LoggerFactory.getLogger(DebugSession.class).warn("Could not close failed recording", failure);
            }
        }
        org.slf4j.LoggerFactory.getLogger(DebugSession.class).warn(message);
    }

    public boolean isBreakpointSet(int address) {
        return rules.stream().anyMatch(r -> isSimpleBreakpoint(r, address) && r.enabled);
    }

    public CompletableFuture<Void> setBreakpoint(int address, boolean enabled) {
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        if (debug == null) throw new IllegalStateException("Breakpoints unavailable for this CPU");
        return update(() -> {
            if (!enabled) rules.removeIf(rule -> isSimpleBreakpoint(rule, address));
            else {
                Rule rule = rules.stream().filter(candidate -> isSimpleBreakpoint(candidate, address)).findFirst()
                        .orElseGet(() -> {
                            Rule added = new Rule("Breakpoint", "EXEC", address, "", 1);
                            rules.add(added);
                            return added;
                        });
                rule.enabled = true;
                rule.failed = false;
            }
        });
    }

    List<Integer> ordinaryBreakpoints() {
        Set<Integer> addresses = new TreeSet<>();
        for (Rule rule : rules) if (rule.kind.equals("EXEC")) addresses.add(rule.address);
        return new ArrayList<>(addresses);
    }

    void toggleOrdinaryBreakpoint(int address) {
        update(() -> rules.stream().filter(rule -> isSimpleBreakpoint(rule, address))
                .findFirst().ifPresent(rule -> rule.enabled = !rule.enabled));
    }

    private boolean isSimpleBreakpoint(Rule rule, int address) {
        return rule.kind.equals("EXEC") && rule.address == address
                && rule.condition.isBlank() && rule.afterHits == 1;
    }

    synchronized BreakpointHit takeBreakpointHit() {
        if (closed || state == CPU.RunState.STATE_RUNNING) return null;
        BreakpointHit hit = pendingHit;
        pendingHit = null;
        return hit;
    }

    static final class BreakpointHit {
        final int address;
        final Rule rule;

        BreakpointHit(int address, Rule rule) {
            this.address = address;
            this.rule = rule;
        }
    }

    public synchronized void save() throws IOException {
        if (settings == null) return;
        Properties p = programIndex == null ? new Properties() : annotations().properties();
        p.setProperty("version", "4");
        p.setProperty("cpu", cpu.getTitle());
        p.setProperty("watches", Integer.toString(watches.size()));
        for (int i = 0; i < watches.size(); i++) p.setProperty("watch." + i, watches.get(i));
        p.setProperty("rules", Integer.toString(rules.size()));
        for (int i = 0; i < rules.size(); i++) {
            Rule r = rules.get(i);
            String k = "rule." + i + ".";
            p.setProperty(k + "name", r.name);
            p.setProperty(k + "kind", r.kind);
            p.setProperty(k + "address", Integer.toString(r.address));
            p.setProperty(k + "condition", r.condition);
            p.setProperty(k + "enabled", Boolean.toString(r.enabled));
            p.setProperty(k + "after", Long.toString(r.afterHits));
        }
        if (!p.equals(lastSaved)) {
            AtomicProperties.save(settings, p);
            lastSaved = p;
        }
    }

    private void load() throws IOException {
        if (settings == null || !Files.isRegularFile(settings)) return;
        Properties p = AtomicProperties.load(settings);
        if (!"4".equals(p.getProperty("version")) || !cpu.getTitle().equals(p.getProperty("cpu"))) {
            Path archived = settings.resolveSibling(settings.getFileName() + ".incompatible-" + UUID.randomUUID());
            Files.move(settings, archived);
            org.slf4j.LoggerFactory.getLogger(DebugSession.class)
                    .warn("Debugger settings belong to another CPU/version; preserved at {}. Starting with empty settings.", archived);
            return;
        }
        try {
            int count = count(p, "watches");
            for (int i = 0; i < count; i++)
                watches.add(Objects.requireNonNull(p.getProperty("watch." + i)));
            count = count(p, "rules");
            for (int i = 0; i < count; i++) {
                String k = "rule." + i + ".";
                String kind = p.getProperty(k + "kind");
                if ("SOURCE".equals(kind)) throw new IllegalArgumentException("Unsupported source rule");
                Rule r = new Rule(p.getProperty(k + "name"), kind, Integer.parseInt(p.getProperty(k + "address")),
                        p.getProperty(k + "condition"), Long.parseLong(p.getProperty(k + "after")));
                r.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled"));
                if (r.address >= cpu.getAddressSpaceSize())
                    throw new IllegalArgumentException("Breakpoint address outside CPU");
                rules.add(r);
            }
            if (programIndex != null && p.containsKey("entries")
                    && programIndex.snapshot().layout().imageId().equals(p.getProperty("image.identity"))) {
                annotations().restore(p);
                programIndex.annotationsChanged();
            }
            lastSaved = p;
        } catch (RuntimeException e) {
            throw new IOException("Invalid debugger settings", e);
        }
    }

    private static int count(Properties p, String key) {
        int n = Integer.parseInt(p.getProperty(key, "0"));
        if (n < 0 || n > 10000) throw new IllegalArgumentException("Too many entries");
        return n;
    }

    @Override
    public void close() {
        // Probe listeners and execution memory may only be changed at a paused boundary.
        execution.close();
        try { stopRecordingFile(); }
        catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(DebugSession.class).error("Could not finish instruction recording", e);
        }
        if (settings != null)
            try {
                save();
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(DebugSession.class)
                        .error("Could not save debugger settings", e);
            }

        annotationEngine.removeListener(annotationListener);
        if (memory != null) memory.removeMemoryListener(memoryListener);
        detachObservation();
        synchronized (this) {
            closed = true;
            pendingHit = null;
            generation++;
        }
    }

    public static final class Rule {
        public final String name, kind, condition;
        public final int address;
        public final long afterHits;
        public volatile boolean enabled = true;
        public volatile long hits;
        public volatile String error = "";
        private Debugging.Expression compiled;
        private boolean failed;
        public boolean isActive() { return enabled && !failed && error.isBlank(); }

        public Rule(String name, String kind, int address, String condition, long afterHits) {
            if (name == null
                    || name.isBlank()
                    || !("EXEC".equals(kind) || "CONDITION".equals(kind) || "READ".equals(kind) || "WRITE".equals(kind))
                    || (address < 0 && !"CONDITION".equals(kind))
                    || afterHits < 1) throw new IllegalArgumentException("Invalid breakpoint");
            this.name = name;
            this.kind = kind;
            this.address = address;
            this.condition = Objects.requireNonNull(condition);
            this.afterHits = afterHits;
        }
    }

}
