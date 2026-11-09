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
    public final List<Breakpoint> breakpoints = new CopyOnWriteArrayList<>();
    public final List<Integer> memoryWatches = new CopyOnWriteArrayList<>();
    private final List<String> loadWarnings = new ArrayList<>();
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
    private Breakpoint[] evaluationBreakpoints = new Breakpoint[0];
    private final Map<Integer, MemoryWatchTracker> memoryWatchTrackers = new LinkedHashMap<>();
    private volatile List<MemoryWatchView> memoryWatchSnapshot = List.of();
    private int stepAddress = -1;
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
            ProgramIndex replacement = ProgramIndex.create(size, annotationEngine);
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
    private long historyCaptured, nextSequence;
    private final Map<String, Debugging.Expression> expressions = new HashMap<>();
    private volatile Map<String, ValueView> watchSnapshot = Map.of();
    private volatile Map<String, String> watchFailures = Map.of();
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

    public DebugSession(CPU cpu, net.emustudio.emulib.plugins.cpu.CPUContext cpuContext, MemoryContext<?> memory, Path settings) throws IOException {
        this(cpu, cpuContext, memory, settings, new MemoryAnnotations());
    }

    public DebugSession(CPU cpu, net.emustudio.emulib.plugins.cpu.CPUContext cpuContext, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine) throws IOException {
        this(cpu, cpuContext, memory, settings, annotationEngine, null, List.of(), null);
    }

    public DebugSession(CPU cpu, net.emustudio.emulib.plugins.cpu.CPUContext cpuContext, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine,
                        net.emustudio.emulib.plugins.memory.Memory memoryPlugin,
                        List<net.emustudio.emulib.plugins.device.Device> devices,
                        net.emustudio.emulib.plugins.compiler.Compiler compiler) throws IOException {
        this.cpu = Objects.requireNonNull(cpu);
        this.annotationEngine = Objects.requireNonNull(annotationEngine);
        this.debug = cpu instanceof Debugging ? (Debugging) cpu : null;
        this.memory = memory;
        this.settings = settings;
        if (this.memory != null || cpu.getAddressSpaceSize() > 0) annotations();
        load();
        annotationEngine.addListener(annotationListener);
        if (this.memory != null) this.memory.addMemoryListener(memoryListener);
        execution = new net.emustudio.application.emulation.EmulationController(cpu, cpuContext, memoryPlugin, devices, this, compiler);
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
            if (!execution.isBusy()) operation.ready.complete(null);
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

    /** Applies debugger settings immediately without pausing execution. */
    public CompletableFuture<Void> update(Runnable change) {
        try { applyUpdate(change); return CompletableFuture.completedFuture(null); }
        catch (RuntimeException failure) { return CompletableFuture.failedFuture(failure); }
    }

    private synchronized void applyUpdate(Runnable change) {
        if (closed) throw new IllegalStateException("Debugger session closed");
        try {
            change.run();
        } finally {
            // An I/O failure after editing settings must not leave hidden old runtime behavior.
            updateProbe();
            // Watches are evaluated only at execution boundaries, never while the CPU runs.
            if (state != CPU.RunState.STATE_RUNNING) refreshWatchesNow();
            expressions.keySet().retainAll(new HashSet<>(watches));
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
        historyCaptured = 0;
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
        syncMemoryWatches();
        publishBreakpoints();
        boolean needed = recording != null || historyLimit > 0 || !memoryWatchTrackers.isEmpty()
                || breakpoints.stream().anyMatch(breakpoint -> breakpoint.enabled);
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

    private void syncMemoryWatches() {
        Map<Integer, MemoryWatchTracker> current = new LinkedHashMap<>();
        for (int address : memoryWatches) {
            MemoryWatchTracker tracker = memoryWatchTrackers.get(address);
            current.putIfAbsent(address, tracker != null ? tracker : new MemoryWatchTracker(address, debug));
        }
        memoryWatchTrackers.clear();
        memoryWatchTrackers.putAll(current);
    }

    private void trimHistory() {
        while (historyLimit > 0 && history.size() > historyLimit) history.removeFirst();
    }

    /** Replays logical editor state only at a supported paused edit/start boundary. */
    private void publishBreakpoints() {
        if (programIndex == null && memory == null && cpu.getAddressSpaceSize() <= 0) return;
        ProgramIndex index = programIndex();
        index.editBreakpoints((base, update) -> {
            update.clear();
            for (Breakpoint breakpoint : breakpoints) {
                if (breakpoint.enabled && breakpoint.address < base.layout().addressSpaceSize())
                    update.add(breakpoint.address);
            }
            return true;
        });
        evaluationBreakpoints = breakpoints.toArray(new Breakpoint[0]);
    }

    private synchronized Debugging.Expression expression(String source) {
        if (debug == null) throw new IllegalStateException("Expressions unsupported by this CPU");
        return expressions.computeIfAbsent(source, debug::compile);
    }

    public Map<String, ValueView> watchValues() { return watchSnapshot; }
    public Map<String, String> watchErrors() { return watchFailures; }
    public List<MemoryWatchView> memoryWatchValues() { return memoryWatchSnapshot; }
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
        List<MemoryWatchView> memoryValues = new ArrayList<>();
        for (MemoryWatchTracker tracker : memoryWatchTrackers.values()) memoryValues.add(tracker.view());
        memoryWatchSnapshot = List.copyOf(memoryValues);
    }

    private void clearWatchChanges() {
        Map<String, ValueView> cleared = new LinkedHashMap<>();
        watchSnapshot.forEach((source, value) -> cleared.put(source, new ValueView(value.text, value.formats, false)));
        watchSnapshot = Map.copyOf(cleared);
    }

    @Override
    public synchronized boolean beforeStep() {
        int address = execution.cpuContext().getInstructionLocation();
        stepAddress = address;
        boolean stop = false;
        if (skipBeforeStep || stoppedAddress == address) skipBeforeStep = false;
        else stop = checkBreakpoints(address);
        stoppedAddress = -1;
        if (stop) stoppedAddress = address;
        if (observationDirty) updateProbe();
        return stop;
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
                        InstructionRecording.sizeOf(event); // rejects steps the recording format cannot encode
                        historyCaptured++;
                        history.addLast(event);
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
                ? captured.get().programLocation.getAsInt() : stepAddress;
        for (MemoryWatchTracker tracker : memoryWatchTrackers.values()) tracker.afterStep(address);
        if (observationDirty) updateProbe();
        return false;
    }

    private boolean checkBreakpoints(int address) {
        for (Breakpoint breakpoint : evaluationBreakpoints) {
            if (!breakpoint.enabled || breakpoint.address != address) continue;
            pendingHit = new BreakpointHit(address);
            return true;
        }
        return false;
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
        return breakpoints.stream().anyMatch(breakpoint -> breakpoint.address == address && breakpoint.enabled);
    }

    public CompletableFuture<Void> setBreakpoint(int address, boolean enabled) {
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        if (debug == null) throw new IllegalStateException("Breakpoints unavailable for this CPU");
        return update(() -> {
            if (!enabled) breakpoints.removeIf(breakpoint -> breakpoint.address == address);
            else {
                Breakpoint breakpoint = breakpoints.stream().filter(candidate -> candidate.address == address).findFirst()
                        .orElseGet(() -> {
                            Breakpoint added = new Breakpoint(address);
                            breakpoints.add(added);
                            return added;
                        });
                breakpoint.enabled = true;
            }
        });
    }

    List<Integer> ordinaryBreakpoints() {
        Set<Integer> addresses = new TreeSet<>();
        for (Breakpoint breakpoint : breakpoints) addresses.add(breakpoint.address);
        return new ArrayList<>(addresses);
    }

    void toggleOrdinaryBreakpoint(int address) {
        update(() -> breakpoints.stream().filter(breakpoint -> breakpoint.address == address)
                .findFirst().ifPresent(breakpoint -> breakpoint.enabled = !breakpoint.enabled));
    }

    /** Passive monitors; they record the last access but never stop execution. */
    public CompletableFuture<Void> addMemoryWatch(int address) {
        if (address < 0) throw new IllegalArgumentException("Invalid memory watch address");
        if (debug == null) throw new IllegalStateException("Memory watches unavailable for this CPU");
        return update(() -> { if (!memoryWatches.contains(address)) memoryWatches.add(address); });
    }

    public CompletableFuture<Void> removeMemoryWatch(int address) {
        return update(() -> memoryWatches.remove(Integer.valueOf(address)));
    }

    synchronized BreakpointHit takeBreakpointHit() {
        if (closed || state == CPU.RunState.STATE_RUNNING) return null;
        BreakpointHit hit = pendingHit;
        pendingHit = null;
        return hit;
    }

    static final class BreakpointHit {
        final int address;

        BreakpointHit(int address) {
            this.address = address;
        }
    }

    /** Problems found while loading settings; each call returns and clears them. */
    public synchronized List<String> takeLoadWarnings() {
        List<String> result = List.copyOf(loadWarnings);
        loadWarnings.clear();
        return result;
    }

    public synchronized void save() throws IOException {
        if (settings == null) return;
        Properties p = programIndex == null ? new Properties() : annotations().properties();
        for (int i = 0; i < watches.size(); i++) p.setProperty("watch." + i, watches.get(i));
        for (int i = 0; i < breakpoints.size(); i++) {
            Breakpoint breakpoint = breakpoints.get(i);
            p.setProperty("breakpoint." + i + ".address", Integer.toString(breakpoint.address));
            p.setProperty("breakpoint." + i + ".enabled", Boolean.toString(breakpoint.enabled));
        }
        for (int i = 0; i < memoryWatches.size(); i++) p.setProperty("memoryWatch." + i, Integer.toString(memoryWatches.get(i)));
        if (!p.equals(lastSaved)) {
            AtomicProperties.save(settings, p);
            lastSaved = p;
        }
    }

    private void load() throws IOException {
        if (settings == null || !Files.isRegularFile(settings)) return;
        Properties p;
        try {
            p = AtomicProperties.load(settings);
        } catch (IllegalArgumentException | java.nio.charset.CharacterCodingException unreadable) {
            Files.delete(settings);
            loadWarning("Debugger settings file " + settings + " was unreadable and has been deleted.");
            return;
        }
        int skipped = 0, droppedConditions = 0;
        for (int i : AtomicProperties.indices(p, "watch.", "")) {
            String watch = p.getProperty("watch." + i);
            if (watch.isBlank()) skipped++;
            else watches.add(watch);
        }
        for (int i : AtomicProperties.indices(p, "breakpoint.", ".address")) {
            String k = "breakpoint." + i + ".";
            if (!addLoadedBreakpoint(p.getProperty(k + "address"), p.getProperty(k + "enabled", "true"))) skipped++;
        }
        for (int i : AtomicProperties.indices(p, "memoryWatch.", "")) {
            if (!addLoadedMemoryWatch(p.getProperty("memoryWatch." + i))) skipped++;
        }
        // Earlier versions stored breakpoints and memory watches as generic rules.
        for (int i : AtomicProperties.indices(p, "rule.", ".kind")) {
            String k = "rule." + i + ".";
            String kind = p.getProperty(k + "kind");
            String condition = p.getProperty(k + "condition", "");
            boolean valid;
            if ("CONDITION".equals(kind)) {
                droppedConditions++;
                continue;
            } else if ("EXEC".equals(kind)) {
                valid = addLoadedBreakpoint(p.getProperty(k + "address"), p.getProperty(k + "enabled", "true"));
            } else if ("READ".equals(kind) || "WRITE".equals(kind)) {
                valid = addLoadedMemoryWatch(p.getProperty(k + "address"));
            } else valid = false;
            if (!valid) skipped++;
            else if (!condition.isBlank()) droppedConditions++;
        }
        if (programIndex != null) {
            skipped += annotations().importProperties(p);
            programIndex.annotationsChanged();
        }
        if (skipped > 0) loadWarning("Skipped " + skipped + " invalid debugger setting(s) in " + settings + ".");
        if (droppedConditions > 0)
            loadWarning("Removed " + droppedConditions + " unsupported breakpoint condition(s) from " + settings + ".");
    }

    private boolean addLoadedBreakpoint(String address, String enabled) {
        try {
            int at = Integer.parseInt(address);
            if (at < 0 || at >= cpu.getAddressSpaceSize()) return false;
            if (!"true".equals(enabled) && !"false".equals(enabled)) return false;
            if (breakpoints.stream().anyMatch(breakpoint -> breakpoint.address == at)) return true;
            Breakpoint breakpoint = new Breakpoint(at);
            breakpoint.enabled = Boolean.parseBoolean(enabled);
            breakpoints.add(breakpoint);
            return true;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private boolean addLoadedMemoryWatch(String address) {
        try {
            int at = Integer.parseInt(address);
            if (at < 0 || at >= cpu.getAddressSpaceSize()) return false;
            if (!memoryWatches.contains(at)) memoryWatches.add(at);
            return true;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private void loadWarning(String message) {
        loadWarnings.add(message);
        org.slf4j.LoggerFactory.getLogger(DebugSession.class).warn(message);
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

    public static final class Breakpoint {
        public final int address;
        public volatile boolean enabled = true;

        public Breakpoint(int address) {
            if (address < 0) throw new IllegalArgumentException("Invalid breakpoint");
            this.address = address;
        }
    }

    /** Memory watch state published at execution boundaries; PCs are -1 until an access is seen. */
    public static final class MemoryWatchView {
        public final int address;
        public final String value, lastReadValue, lastWriteValue, error;
        public final int lastReadPc, lastWritePc;

        MemoryWatchView(int address, String value, int lastReadPc, String lastReadValue,
                        int lastWritePc, String lastWriteValue, String error) {
            this.address = address;
            this.value = value;
            this.lastReadPc = lastReadPc;
            this.lastReadValue = lastReadValue;
            this.lastWritePc = lastWritePc;
            this.lastWriteValue = lastWriteValue;
            this.error = error;
        }
    }

    /** Uses private expressions so watch display state and the shared expression cache stay untouched. */
    private static final class MemoryWatchTracker {
        final int address;
        Debugging.Expression read, write, value;
        String error = "";
        int lastReadPc = -1, lastWritePc = -1;
        String lastReadValue = "", lastWriteValue = "";

        MemoryWatchTracker(int address, Debugging debug) {
            this.address = address;
            try {
                if (debug == null) throw new IllegalStateException("Memory watches unavailable for this CPU");
                read = debug.compile("read(" + address + ")");
                write = debug.compile("write(" + address + ")");
                value = debug.compile("mem(" + address + ")");
            } catch (RuntimeException failure) {
                fail(failure);
            }
        }

        void afterStep(int pc) {
            if (!error.isEmpty()) return;
            try {
                boolean wasRead = read.test(), wasWritten = write.test();
                if (!wasRead && !wasWritten) return;
                String current = value.display().text;
                if (wasRead) { lastReadPc = pc; lastReadValue = current; }
                if (wasWritten) { lastWritePc = pc; lastWriteValue = current; }
            } catch (RuntimeException failure) {
                fail(failure);
            }
        }

        MemoryWatchView view() {
            String current = "";
            if (error.isEmpty()) {
                try { current = value.display().text; }
                catch (RuntimeException failure) { fail(failure); }
            }
            return new MemoryWatchView(address, current, lastReadPc, lastReadValue, lastWritePc, lastWriteValue, error);
        }

        private void fail(RuntimeException failure) {
            error = Objects.toString(failure.getMessage(), "Memory watch evaluation failed");
        }
    }

}
