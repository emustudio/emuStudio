/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.Disassembler;
import net.emustudio.emulib.plugin.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugin.cpu.InstructionFlow;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ValueView;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/** Host debugging policy; plugins interpret machine state at the execution seam. */
public final class DebuggerSession implements AutoCloseable, Debugging.Observer {
    private final net.emustudio.application.emulation.EmulationController execution;
    public net.emustudio.application.emulation.EmulationController execution() { return execution; }
    public final CPU cpu;
    public final Debugging debug;
    public final MemoryContext<?> memory;
    public final MemoryAnnotations annotationEngine;
    public final List<String> watches = new CopyOnWriteArrayList<>();
    public final Set<Integer> breakpoints = new java.util.concurrent.ConcurrentSkipListSet<>();
    public final List<Integer> memoryWatches = new CopyOnWriteArrayList<>();
    private final List<String> loadWarnings = new ArrayList<>();
    private final Path settings;
    private Properties lastSaved;
    private AnnotationIndex annotations;
    private SourceAddressMap sourceMappings;
    private boolean sourceMappingsCleared;
    private int addressSpaceSize;
    // Memory notifications advance the revision before waiting for the session monitor.
    private final Object programLock = new Object();
    private final java.util.concurrent.atomic.AtomicLong memoryRevision = new java.util.concurrent.atomic.AtomicLong();
    private volatile FlowAnalysis flow;
    private long flowRevision;
    private final Runnable annotationListener = () -> { synchronized (programLock) { flow = null; } };
    private final Map<Integer, MemoryWatchTracker> memoryWatchTrackers = new LinkedHashMap<>();
    private volatile List<MemoryWatchView> memoryWatchSnapshot = List.of();
    private int stepAddress = -1;
    private long generation;
    private boolean closed;
    private int activeMutations;
    private SourceCaptureGuard activeSourceCapture;
    private SourceCaptureGuard completedSourceCapture;
    private BreakpointHit pendingHit;

    /** CPU-visible cells; querying the layout never reads memory. */
    public synchronized int addressSpaceSize() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        int size = cpu.getAddressSpaceSize();
        if (size < 1) throw new IllegalStateException("Memory unavailable");
        if (addressSpaceSize != size) {
            addressSpaceSize = size;
            annotations = null;
            invalidateProgram();
            generation++;
        }
        return size;
    }

    public long memoryRevision() { return memoryRevision.get(); }
    synchronized boolean isCurrentMemory(int size, long revision) {
        return !closed && cpu.getAddressSpaceSize() == size && memoryRevision() == revision;
    }
    public long programRevision() { return memoryRevision.get() + annotationEngine.revision(); }

    /** Current immutable analysis, or null when its program has changed. */
    public synchronized FlowAnalysis flowAnalysis() {
        if (closed) return null;
        addressSpaceSize();
        synchronized (programLock) { return flowRevision == programRevision() ? flow : null; }
    }

    public synchronized void setSourceMappings(SourceAddressMap mappings) {
        requirePaused();
        if (mappings.session() != this)
            throw new IllegalStateException("Source mappings belong to another memory image");
        sourceMappings = mappings;
        sourceMappingsCleared = false;

    }

    /** Checks captured mappings for staleness; uncaptured compiler mappings remain directly accessible. */
    public synchronized Optional<SourceCodePosition> sourceAt(int address) {
        if (sourceMappings != null) {
            if (sourceMappings.session() != this) return Optional.empty();
            return sourceMappings.sourceAt(address).map(location -> location.position);
        }
        if (sourceMappingsCleared) return Optional.empty();
        return annotationEngine.at(address).stream().filter(entry -> entry.source != null)
                .min(Comparator.comparingLong(entry -> entry.ownerId)).map(entry -> entry.source);
    }

    public synchronized void clearSourceMappings() {
        addressSpaceSize();
        sourceMappings = null;
        sourceMappingsCleared = true;

    }

    /** Live reads never retain a copy of memory cells. Reacquire after a machine or metadata edit. */
    public LiveMemory liveMemory() {
        if (memory == null) throw new IllegalStateException("Memory unavailable");
        Stamp stamp = liveStamp();
        return new LiveMemory(memory, stamp.size, () -> requireCurrent(stamp));
    }

    /** User metadata is restored once at startup; reads need no disk access or paused CPU. */
    public synchronized AnnotationIndex annotations() {
        int size = addressSpaceSize();
        if (annotations == null) annotations = new AnnotationIndex(size, annotationEngine);
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
    private int executionAddress = -1;
    private long executionRevision;
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
    public synchronized void executionChanged(CPU.RunState value, long machineRevision) {
        boolean starting = state != CPU.RunState.STATE_RUNNING && value == CPU.RunState.STATE_RUNNING;
        int address = cpu.getInstructionLocation();
        if (state != value || executionAddress != address || executionRevision != machineRevision) advanceHostGeneration();
        executionAddress = address;
        executionRevision = machineRevision;
        state = value;
        if (starting) {
            clearWatchChanges();
            pendingHit = null;
            if (sourceMappings == null) sourceMappingsCleared = true;
            invalidateProgram();
        }
    }

    public DebuggerSession(CPU cpu, MemoryContext<?> memory, Path settings) throws IOException {
        this(cpu, memory, settings, new MemoryAnnotations());
    }

    public DebuggerSession(CPU cpu, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine) throws IOException {
        this(cpu, memory, settings, annotationEngine, null, List.of(), null, new net.emustudio.application.ExecutionStateImpl());
    }

    public DebuggerSession(CPU cpu, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine,
                        net.emustudio.emulib.plugin.memory.Memory memoryPlugin,
                        List<net.emustudio.emulib.plugin.device.Device> devices,
                        net.emustudio.emulib.plugin.compiler.Compiler compiler,
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
            if (invalidateMemory) invalidateProgram();
            activeMutations++;
            activeSourceCapture = operation;
        }
        try {
            mutation.run();
        } finally {
            synchronized (this) {
                activeMutations--;
                generation++;
                if (invalidateMemory) invalidateProgram();
                if (memory != null && !closed) addressSpaceSize();
                operation.stamp = new Stamp(this);
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
                && guard.stamp.generation == generation && guard.stamp.matches(this);
        generation++;
        if (current) guard.stamp = new Stamp(this);
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
        addressSpaceSize();
        return new Stamp(this);
    }

    /** Captures operation provenance, excluding independent breakpoint edits. */
    synchronized Stamp pausedStamp() {
        pausedGeneration();
        if (cpu.getAddressSpaceSize() > 0) addressSpaceSize();
        return new Stamp(this);
    }

    static final class Stamp {
        final DebuggerSession session;
        final int size;
        final long generation, memoryRevision, annotationRevision;

        private Stamp(DebuggerSession session) {
            this.session = session;
            size = session.addressSpaceSize;
            generation = session.generation;
            memoryRevision = session.memoryRevision();
            annotationRevision = session.annotationEngine.revision();
        }

        private boolean matches(DebuggerSession current) {
            return current == session && current.cpu.getAddressSpaceSize() == size
                    && current.memoryRevision() == memoryRevision
                    && current.annotationEngine.revision() == annotationRevision;
        }
    }

    synchronized void applyCurrent(Stamp stamp, String message, Runnable apply) {
        applyPaused(stamp.generation, () -> {
            synchronized (programLock) {
                if (!stamp.matches(this)) throw new IllegalStateException(message);
                apply.run();
            }
        });
    }

    private void requireCurrent(Stamp stamp) {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        applyCurrent(stamp, "Analysis discarded: memory, source or metadata changed", () -> {});
    }

    /** Use the CPU's wired decoder off the UI thread; publish only while its paused program revision is current. */
    public FlowAnalysis analyzeFlow(int start, int limit) {
        Stamp stamp = liveStamp();
        Runnable validate = () -> requireCurrent(stamp);
        Disassembler decoder = guardedDecoder(cpu.getDisassembler(), validate);
        FlowAnalysis result = FlowAnalysis.reachable(decoder, start, limit, stamp.size, annotationEngine);
        synchronized (this) {
            synchronized (programLock) {
                boolean published = annotationEngine.ifRevision(stamp.annotationRevision, () ->
                        applyCurrent(stamp, "Flow analysis discarded: program changed", () -> {
                            flow = result;
                            flowRevision = programRevision();
                        }));
                if (!published) throw new IllegalStateException("Flow analysis discarded: annotations changed");
                return result;
            }
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
        guard.stamp = new Stamp(this);
        guard.ready.complete(null);
        return guard;
    }

    public synchronized void applySourceCapture(SourceCaptureGuard guard, Runnable apply) {
        Objects.requireNonNull(guard);
        Stamp stamp = guard.stamp;
        if (stamp == null) throw new IllegalStateException("Compilation mutation has not finished");
        synchronized (programLock) {
            applyCurrent(stamp, "Source mappings discarded: machine or metadata changed after compilation", apply);
        }
    }

    public static final class SourceCaptureGuard {
        private volatile Stamp stamp;
        private final java.util.concurrent.CompletableFuture<Void> ready = new java.util.concurrent.CompletableFuture<>();
        public java.util.concurrent.CompletableFuture<Void> whenReady() { return ready.copy(); }
        private SourceCaptureGuard() { }
    }

    private void invalidateAnalysis() {
        // Writers notify after changing cells. Advance the revision before waiting for a paused
        // analysis lease, so its publication cannot race a blocked memory-listener callback.
        invalidateProgram();
        synchronized (this) {
            if (sourceMappings == null) sourceMappingsCleared = true;
            generation++;
        }
    }

    private void invalidateProgram() {
        synchronized (programLock) {
            memoryRevision.incrementAndGet();
            flow = null;
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
        boolean needed = recording != null || historyLimit > 0 || !memoryWatchTrackers.isEmpty()
                || !breakpoints.isEmpty();
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
        int address = cpu.getInstructionLocation();
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
        if (!breakpoints.contains(address)) return false;
        pendingHit = new BreakpointHit(address);
        return true;
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
                org.slf4j.LoggerFactory.getLogger(DebuggerSession.class).warn("Could not close failed recording", failure);
            }
        }
        org.slf4j.LoggerFactory.getLogger(DebuggerSession.class).warn(message);
    }

    public boolean isBreakpointSet(int address) { return breakpoints.contains(address); }

    public CompletableFuture<Void> setBreakpoint(int address, boolean enabled) {
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        if (debug == null) throw new IllegalStateException("Breakpoints unavailable for this CPU");
        return update(() -> {
            if (enabled) breakpoints.add(address);
            else breakpoints.remove(address);
        });
    }

    List<Integer> ordinaryBreakpoints() { return List.copyOf(breakpoints); }

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
        Properties p = addressSpaceSize == 0 ? new Properties() : annotations().properties();
        for (int i = 0; i < watches.size(); i++) p.setProperty("watch." + i, watches.get(i));
        int breakpointIndex = 0;
        for (int address : breakpoints)
            p.setProperty("breakpoint." + breakpointIndex++ + ".address", Integer.toString(address));
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
        int skipped = 0;
        for (int i : AtomicProperties.indices(p, "watch.", "")) {
            String watch = p.getProperty("watch." + i);
            if (watch.isBlank()) skipped++;
            else watches.add(watch);
        }
        for (int i : AtomicProperties.indices(p, "breakpoint.", ".address")) {
            String k = "breakpoint." + i + ".";
            if (!addLoadedBreakpoint(p.getProperty(k + "address"))) skipped++;
        }
        for (int i : AtomicProperties.indices(p, "memoryWatch.", "")) {
            if (!addLoadedMemoryWatch(p.getProperty("memoryWatch." + i))) skipped++;
        }
        if (addressSpaceSize != 0) {
            skipped += annotations().importProperties(p);
        }
        if (skipped > 0) loadWarning("Skipped " + skipped + " invalid debugger setting(s) in " + settings + ".");
    }

    private boolean addLoadedBreakpoint(String address) {
        try {
            int at = Integer.parseInt(address);
            if (at < 0 || at >= cpu.getAddressSpaceSize()) return false;
            breakpoints.add(at);
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
        org.slf4j.LoggerFactory.getLogger(DebuggerSession.class).warn(message);
    }

    @Override
    public void close() {
        // Probe listeners and execution memory may only be changed at a paused boundary.
        execution.close();
        try { stopRecordingFile(); }
        catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(DebuggerSession.class).error("Could not finish instruction recording", e);
        }
        if (settings != null)
            try {
                save();
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(DebuggerSession.class)
                        .error("Could not save debugger settings", e);
            }

        annotationEngine.removeListener(annotationListener);
        if (memory != null) memory.removeMemoryListener(memoryListener);
        detachObservation();
        synchronized (this) {
            closed = true;
            invalidateProgram();
            pendingHit = null;
            generation++;
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
