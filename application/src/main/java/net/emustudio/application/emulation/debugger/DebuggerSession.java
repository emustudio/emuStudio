/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.application.settings.DebuggerConfig;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ValueView;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Host debugging policy; plugins interpret machine state at the execution seam.
 */
public final class DebuggerSession implements AutoCloseable, Debugging.Observer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggerSession.class);

    private final EmulationController execution;
    public final CPU cpu;
    private final CPUContext cpuContext;
    public final Debugging debug;
    public final MemoryContext<?> memory;
    public final MemoryAnnotations annotationEngine;
    public final DebuggerConfig config;
    private final List<Consumer<CPU.RunState>> executionListeners = new CopyOnWriteArrayList<>();
    private SourceAddressMap sourceMappings;
    private boolean sourceMappingsCleared;
    private int addressSpaceSize;
    // Memory notifications advance the revision before waiting for the session monitor.
    private final Object programLock = new Object();
    private final AtomicLong memoryRevision = new AtomicLong();
    // Source addresses survive execution and its data writes, but not explicit machine edits.
    private final AtomicLong memoryImageRevision = new AtomicLong();
    private volatile FlowAnalysis flow;
    private final FlowAnalysis.Observations observedFlow = new FlowAnalysis.Observations();
    private long generation;
    private boolean closed;
    private int activeMutations;
    private SourceCaptureGuard activeSourceCapture;
    private SourceCaptureGuard completedSourceCapture;
    private OptionalInt pendingHit = OptionalInt.empty();
    private final InstructionRecording history = new InstructionRecording();
    private long nextSequence;
    private final WatchExpressions watches;
    private AutoCloseable observation;
    private Debugging.Capture captureAttached;
    private boolean skipBeforeStep, observationDirty;
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
                    memoryImageRevision.incrementAndGet();
                    invalidateAnalysis();
                }
            };

    public enum RecordingMode {FIRST, LAST, ADDRESS_RANGE, UNLIMITED}

    /** Machine provenance of a source capture; publication requires it to still match. */
    private static final class Stamp {
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

    public static final class SourceCaptureGuard {
        private volatile Stamp stamp;
        private final CompletableFuture<Void> ready = new CompletableFuture<>();

        public CompletableFuture<Void> whenReady() {
            return ready.copy();
        }

        private SourceCaptureGuard() {
        }
    }

    public DebuggerSession(CPU cpu, CPUContext cpuContext, MemoryContext<?> memory, Path settings) throws IOException {
        this(cpu, cpuContext, memory, settings, new MemoryAnnotations());
    }

    public DebuggerSession(CPU cpu, CPUContext cpuContext, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine) throws IOException {
        this(cpu, cpuContext, memory, settings, annotationEngine, null, List.of(), null);
    }

    public DebuggerSession(CPU cpu, CPUContext cpuContext, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine,
                           Memory memoryPlugin,
                           List<Device> devices,
                           Compiler compiler) throws IOException {
        this.cpu = Objects.requireNonNull(cpu);
        this.cpuContext = Objects.requireNonNull(cpuContext);
        this.annotationEngine = Objects.requireNonNull(annotationEngine);
        this.debug = cpu instanceof Debugging ? (Debugging) cpu : null;
        watches = new WatchExpressions(debug);
        this.memory = memory;
        if (this.memory != null || cpu.getAddressSpaceSize() > 0) addressSpaceSize();
        config = new DebuggerConfig(settings, cpu::getAddressSpaceSize, annotationEngine);
        if (this.memory != null) this.memory.addMemoryListener(memoryListener);
        execution = new EmulationController(cpu, cpuContext, memoryPlugin, devices, this, compiler);
        update(() -> {
        }).join();
    }

    public synchronized void record(int limit) {
        if (limit > 0 && debug == null)
            throw new IllegalStateException("Instruction recording unsupported by this CPU");
        if (limit < 0 || limit > 1_000_000) throw new IllegalArgumentException("History limit must be 0..1000000");
        history.setLimit(limit);
        if (limit > 0) {
            clearRecordingError();
        }
        updateProbe();
    }

    public EmulationController execution() {
        return execution;
    }

    /**
     * CPU-visible cells; querying the layout never reads memory.
     */
    public synchronized int addressSpaceSize() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        int size = cpu.getAddressSpaceSize();
        if (size < 1) throw new IllegalStateException("Memory unavailable");
        if (addressSpaceSize != size) {
            addressSpaceSize = size;
            memoryImageRevision.incrementAndGet();
            invalidateProgram();
            generation++;
        }
        return size;
    }

    public long memoryRevision() {
        return memoryRevision.get();
    }

    long memoryImageRevision() {
        return memoryImageRevision.get();
    }

    public long programRevision() {
        return memoryRevision.get() + annotationEngine.revision();
    }

    /**
     * Last selected immutable runtime-flow view; independent of current memory contents.
     */
    public synchronized FlowAnalysis flowAnalysis() {
        return closed ? null : flow;
    }

    public synchronized int lastExecutedAddress() {
        return observedFlow.lastExecutedAddress();
    }

    /**
     * Address-ordered predecessors from the same observations used by Inspection.
     */
    public synchronized List<Integer> observedInstructionsBefore(int address, int count) {
        return observedFlow.instructionsBefore(address, count);
    }

    public synchronized int nextObservedInstruction(int address) {
        return observedFlow.nextInstruction(address);
    }

    /**
     * Called only after a successful explicit CPU reset; annotations are retained.
     */
    public synchronized void cpuReset() {
        observedFlow.reset();
        flow = null;
    }

    public synchronized void setSourceMappings(SourceAddressMap mappings) {
        requirePaused();
        if (mappings.session() != this)
            throw new IllegalStateException("Source mappings belong to another memory image");
        sourceMappings = mappings;
        sourceMappingsCleared = false;
    }

    /**
     * Checks captured mappings for staleness; uncaptured compiler mappings remain directly accessible.
     */
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

    public void addExecutionListener(Consumer<CPU.RunState> listener) {
        executionListeners.add(Objects.requireNonNull(listener));
    }

    public void removeExecutionListener(Consumer<CPU.RunState> listener) {
        executionListeners.remove(listener);
    }

    /**
     * Host reports execution transitions and completed machine work.
     */
    public synchronized void executionChanged(CPU.RunState value, long machineRevision) {
        boolean starting = state != CPU.RunState.STATE_RUNNING && value == CPU.RunState.STATE_RUNNING;
        int address = cpuContext.getInstructionLocation();
        if (state != value || executionAddress != address || executionRevision != machineRevision)
            advanceHostGeneration();
        executionAddress = address;
        executionRevision = machineRevision;
        state = value;
        if (starting) {
            watches.clearChanges();
            pendingHit = OptionalInt.empty();
            if (sourceMappings == null) sourceMappingsCleared = true;
            invalidateProgram();
        }
        executionListeners.forEach(listener -> listener.accept(value));
    }

    public synchronized void requirePaused() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        if (state == CPU.RunState.STATE_RUNNING) throw new IllegalStateException("Pause CPU first");
    }

    /**
     * Called by the command scheduler after final lifecycle publication.
     */
    public void completeSourceCapture() {
        SourceCaptureGuard guard;
        synchronized (this) {
            guard = completedSourceCapture;
            completedSourceCapture = null;
        }
        if (guard != null) guard.ready.complete(null);
    }

    /**
     * Select an immutable view of completed runtime observations; never decode or execute memory.
     */
    public synchronized FlowAnalysis analyzeFlow(int start) {
        requirePaused();
        flow = observedFlow.view(start, annotationEngine);
        return flow;
    }

    /**
     * Associates a compiler callback with its enclosing machine mutation, including its reset.
     */
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
        // An execution boundary or memory write after compilation makes the mappings stale.
        synchronized (programLock) {
            if (closed || state == CPU.RunState.STATE_RUNNING || activeMutations != 0
                    || generation != stamp.generation || !stamp.matches(this))
                throw new IllegalStateException("Source mappings discarded: machine or metadata changed after compilation");
            apply.run();
        }
    }

    /**
     * Applies debugger settings immediately without pausing execution.
     */
    public CompletableFuture<Void> update(Runnable change) {
        try {
            applyUpdate(change);
            return CompletableFuture.completedFuture(null);
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    public synchronized void prepareManualStep() {
        skipBeforeStep = true;
    }

    public synchronized void completeManualStep() {
        skipBeforeStep = false;
    }

    public synchronized void startNewRecording(int limit, RecordingMode mode, int from, int through) {
        Objects.requireNonNull(mode);
        boolean bounded = mode == RecordingMode.FIRST || mode == RecordingMode.LAST;
        if (bounded && (limit <= 0 || limit > 1_000_000))
            throw new IllegalArgumentException("Recording limit must be 1..1000000");
        if (mode == RecordingMode.ADDRESS_RANGE && (from < 0 || through < from || through >= cpu.getAddressSpaceSize()))
            throw new IllegalArgumentException("Recording range must be within CPU addresses, with From ≤ To");
        record(bounded ? limit : 1);
        history.start(bounded ? limit : Integer.MAX_VALUE, mode, from, through);
    }

    public synchronized boolean isRecording() {
        return history.isRecording();
    }

    public synchronized RecordingMode recordingMode() {
        return history.mode();
    }

    public synchronized int recordingLimit() {
        return history.limit();
    }

    public synchronized int recordingFrom() {
        return history.from();
    }

    public synchronized int recordingThrough() {
        return history.through();
    }

    public synchronized List<InstructionRecording.Event> history() {
        return history.events();
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
        clearRecordingError();
        updateProbe();
    }

    public synchronized void stopRecordingFile() throws IOException {
        InstructionRecording.Writer writer = recording;
        recording = null;
        recordingPath = null;
        try {
            if (writer != null) writer.close();
        } catch (IOException failure) {
            recordingError = "Incomplete recording: " + failure.getMessage();
            status = recordingError;
            throw failure;
        } finally {
            updateProbe();
        }
    }

    public synchronized Path recordingPath() {
        return recordingPath;
    }

    public synchronized String recordingError() {
        return recordingError;
    }

    public void saveHistory(Path path, boolean text) throws IOException {
        InstructionRecording.Header header = recordingHeader();
        List<InstructionRecording.Event> events = history();
        if (text) InstructionRecording.exportText(path, header, events);
        else InstructionRecording.write(path, header, events);
    }

    public synchronized void updateProbe() {
        observationDirty = false;
        Debugging.Capture capture = recording != null || history.isRecording() ? Debugging.Capture.FULL
                : Debugging.Capture.INSTRUCTION;
        if (debug == null) return;
        if (observation != null && capture != captureAttached) detachObservation();
        if (observation == null) {
            observation = debug.observe(this, capture);
            captureAttached = capture;
        }
    }

    public Map<String, ValueView> watchValues() {
        return watches.values();
    }

    public Map<String, String> watchErrors() {
        return watches.errors();
    }

    public CompletableFuture<Void> refreshWatches() {
        return update(() -> {
        });
    }

    @Override
    public synchronized boolean beforeStep() {
        int address = execution.cpuContext().getInstructionLocation();
        boolean stop = false;
        if (skipBeforeStep || stoppedAddress == address) skipBeforeStep = false;
        else stop = checkBreakpoints(address);
        stoppedAddress = -1;
        if (stop) stoppedAddress = address;
        if (observationDirty) updateProbe();
        return stop;
    }

    @Override
    public synchronized boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction,
                                          OptionalInt nextAddress, Optional<StepView> captured) {
        if (completed && instruction.isPresent()) {
            observedFlow.executed(instruction.get(), nextAddress);
            flow = null;
        }
        long sequence = ++nextSequence;
        if (captured.isPresent()) {
            InstructionRecording.Event event = new InstructionRecording.Event(sequence, completed, captured.get());
            retainHistory(event);
            appendRecording(event);
        }
        if (observationDirty) updateProbe();
        return false;
    }

    @Override
    public synchronized void failed(String message) {
        history.stop();
        captureAttached = null;
        stopFailedRecorder(message);
    }

    public boolean isBreakpointSet(int address) {
        return config.breakpoints.contains(address);
    }

    public CompletableFuture<Void> setBreakpoint(int address, boolean enabled) {
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        if (debug == null) throw new IllegalStateException("Breakpoints unavailable for this CPU");
        return update(() -> {
            if (enabled) config.breakpoints.add(address);
            else config.breakpoints.remove(address);
        });
    }

    public synchronized OptionalInt takeBreakpointHit() {
        if (closed || state == CPU.RunState.STATE_RUNNING) return OptionalInt.empty();
        OptionalInt hit = pendingHit;
        pendingHit = OptionalInt.empty();
        return hit;
    }

    @Override
    public void close() {
        // Probe listeners and execution memory may only be changed at a paused boundary.
        execution.close();
        try {
            stopRecordingFile();
        } catch (IOException e) {
            LOGGER.error("Could not finish instruction recording", e);
        }
        try {
            config.save();
        } catch (IOException e) {
            LOGGER.error("Could not save debugger settings", e);
        }

        if (memory != null) memory.removeMemoryListener(memoryListener);
        detachObservation();
        synchronized (this) {
            closed = true;
            memoryImageRevision.incrementAndGet();
            invalidateProgram();
            pendingHit = OptionalInt.empty();
            generation++;
        }
    }

    /**
     * Invalidates analysis before and after a machine edit, even if the CPU emits no event.
     */
    public void runMutation(Runnable mutation) {
        memoryImageRevision.incrementAndGet();
        synchronized (this) {
            stoppedAddress = -1;
            watches.clearChanges();
        }
        try {
            runExecution(mutation);
        } finally {
            memoryImageRevision.incrementAndGet();
        }
    }

    public void runExecution(Runnable execution) {
        try {
            runMachineOperation(execution, true);
        } finally {
            // A paused view must show the final machine value, even after a very short step.
            synchronized (this) {
                if (!closed) watches.refresh(config.watches);
            }
        }
    }

    /**
     * Compilers report memory writes through the memory listener while execution is paused.
     */
    public void runCompilation(Runnable compilation) {
        synchronized (this) {
            stoppedAddress = -1;
            skipBeforeStep = false;
        }
        boolean restoreNotifications = memory != null && !memory.areMemoryNotificationsEnabled();
        if (restoreNotifications) memory.setMemoryNotificationsEnabled(true);
        try {
            runMachineOperation(compilation, false);
        } finally {
            if (restoreNotifications) memory.setMemoryNotificationsEnabled(false);
        }
    }

    synchronized boolean isCurrentMemoryImage(int size, long revision) {
        return !closed && cpu.getAddressSpaceSize() == size && memoryImageRevision() == revision;
    }

    List<Integer> ordinaryBreakpoints() {
        return List.copyOf(config.breakpoints);
    }

    private void runMachineOperation(Runnable mutation, boolean invalidateMemory) {
        Objects.requireNonNull(mutation);
        SourceCaptureGuard operation = new SourceCaptureGuard();
        synchronized (this) {
            if (closed) throw new IllegalStateException("Debugger session closed");
            pendingHit = OptionalInt.empty();
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
            if (!execution.isBusy()) operation.ready.complete(null);
        }
    }

    /**
     * Carries a just-finished operation across host notifications, never across unrelated edits.
     */
    private void advanceHostGeneration() {
        SourceCaptureGuard guard = completedSourceCapture;
        boolean current = guard != null && !guard.ready.isDone() && guard.stamp != null
                && guard.stamp.generation == generation && guard.stamp.matches(this);
        generation++;
        if (current) guard.stamp = new Stamp(this);
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
        }
    }

    private synchronized void applyUpdate(Runnable change) {
        if (closed) throw new IllegalStateException("Debugger session closed");
        try {
            change.run();
        } finally {
            // An I/O failure after editing settings must not leave hidden old runtime behavior.
            updateProbe();
            // Watches are evaluated only at execution boundaries, never while the CPU runs.
            if (state != CPU.RunState.STATE_RUNNING) watches.refresh(config.watches);
            watches.retain(config.watches);
        }
    }

    private synchronized InstructionRecording.Header recordingHeader() {
        if (debug == null) throw new IllegalStateException("Instruction recording unsupported by this CPU");
        return new InstructionRecording.Header(cpu.getTitle());
    }

    private void clearRecordingError() {
        if (status.equals(recordingError)) status = "";
        recordingError = "";
    }

    private void detachObservation() {
        AutoCloseable current = observation;
        observation = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception failure) {
                status = "Debugger detach failed: " + failure.getMessage();
                LOGGER.warn(status, failure);
            }
        }
    }

    private void retainHistory(InstructionRecording.Event event) {
        try {
            history.retain(event);
        } catch (IOException failure) {
            failed("History capture failed: " + failure.getMessage());
        }
    }

    private void appendRecording(InstructionRecording.Event event) {
        if (recording == null) return;
        try {
            recording.append(event);
        } catch (IOException failure) {
            stopFailedRecorder("Incomplete recording: " + failure.getMessage());
        }
    }

    private boolean checkBreakpoints(int address) {
        if (!config.breakpoints.contains(address)) return false;
        pendingHit = OptionalInt.of(address);
        return true;
    }

    private void stopFailedRecorder(String message) {
        observationDirty = true;
        recordingError = message;
        status = message;
        InstructionRecording.Writer current = recording;
        recording = null;
        recordingPath = null;
        if (current != null) {
            try {
                current.abort();
            } catch (IOException failure) {
                LOGGER.warn("Could not close failed recording", failure);
            }
        }
        LOGGER.warn(message);
    }

}
