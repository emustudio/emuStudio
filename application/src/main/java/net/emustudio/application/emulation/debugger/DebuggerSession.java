/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.application.settings.DebuggerConfig;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.SourceCodePosition;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.StepView;
import net.emustudio.emulib.plugin.debugger.ValueView;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Host debugging policy; plugins interpret machine state at the execution seam.
 */
public final class DebuggerSession implements AutoCloseable, Debugging.Observer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggerSession.class);

    public final CPU cpu;
    public final CPUContext cpuContext;
    public final Debugging debug;
    public final MemoryContext<?> memory;
    public final MemoryAnnotations annotationEngine;
    public final DebuggerConfig config;
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
    // Inspection eligibility follows host notifications; command scheduling remains outside this module.
    private boolean running;
    private int activeMutations;
    private SourceCaptureGuard activeSourceCapture;
    private SourceCaptureGuard completedSourceCapture;
    private OptionalInt pendingHit = OptionalInt.empty();
    public final InstructionRecording recording;
    private final WatchExpressions watches;
    private AutoCloseable observation;
    private Debugging.Capture captureAttached;
    private boolean skipBeforeStep, observationDirty;
    private int stoppedAddress = -1;
    private final MemoryContext.MemoryListener memoryListener = new MemoryContext.MemoryListener() {
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

    /**
     * Machine provenance of a source capture; publication requires it to still match.
     */
    public static final class SourceCaptureGuard {
        private final DebuggerSession session;
        private final CompletableFuture<Void> ready = new CompletableFuture<>();
        private int size;
        private long generation, memoryRevision, annotationRevision;
        private volatile boolean captured;

        private SourceCaptureGuard(DebuggerSession session) {
            this.session = session;
        }

        // Read and refresh provenance only while holding the owning session's monitor.
        private void capture() {
            size = session.addressSpaceSize;
            generation = session.generation;
            memoryRevision = session.memoryRevision();
            annotationRevision = session.annotationEngine.revision();
            captured = true;
        }

        private boolean matches(DebuggerSession current) {
            return current == session && current.generation == generation
                    && current.cpu.getAddressSpaceSize() == size && current.memoryRevision() == memoryRevision
                    && current.annotationEngine.revision() == annotationRevision;
        }

        public CompletableFuture<Void> whenReady() {
            return ready.copy();
        }
    }

    public DebuggerSession(CPU cpu, CPUContext cpuContext, MemoryContext<?> memory, Path settings) throws IOException {
        this(cpu, cpuContext, memory, settings, new MemoryAnnotations());
    }

    public DebuggerSession(CPU cpu, CPUContext cpuContext, MemoryContext<?> memory, Path settings, MemoryAnnotations annotationEngine) throws IOException {
        this.cpu = Objects.requireNonNull(cpu);
        this.cpuContext = Objects.requireNonNull(cpuContext);
        this.annotationEngine = Objects.requireNonNull(annotationEngine);
        this.debug = cpu instanceof Debugging ? (Debugging) cpu : null;
        watches = new WatchExpressions(debug);
        recording = new InstructionRecording(debug != null, cpu::getTitle, cpu::getAddressSpaceSize);
        this.memory = memory;
        if (this.memory != null || cpu.getAddressSpaceSize() > 0) addressSpaceSize();
        config = new DebuggerConfig(settings, cpu::getAddressSpaceSize, annotationEngine);
        try {
            if (this.memory != null) this.memory.addMemoryListener(memoryListener);
            update(() -> {}).join();
        } catch (RuntimeException | Error failure) {
            if (this.memory != null) this.memory.removeMemoryListener(memoryListener);
            detachObservation();
            throw failure;
        }
    }

    public synchronized void record(int limit) {
        recording.record(limit);
        updateProbe();
    }

    /** Inspection eligibility follows host notifications, even before the CPU context updates. */
    public synchronized boolean isRunning() {
        return running;
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
        if (mappings.session() != this && !(memory == null && mappings.session() == null))
            throw new IllegalStateException("Source mappings belong to another memory image");
        sourceMappings = mappings;
        sourceMappingsCleared = false;
    }

    public synchronized Optional<SourceAddressMap> sourceMappings() {
        return Optional.ofNullable(sourceMappings);
    }

    /** Captures and installs the active mapping under the same provenance check. */
    public synchronized SourceAddressMap captureSources(SourceCaptureGuard guard, Compiler compiler,
                                                        SourceInputs inputs) throws IOException {
        SourceAddressMap[] captured = new SourceAddressMap[1];
        try {
            applySourceCapture(guard, () -> {
                try {
                    captured[0] = SourceAddressMap.capture(compiler, memory, inputs,
                            memory == null ? null : this, annotationEngine);
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
                setSourceMappings(captured[0]);
            });
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        }
        return captured[0];
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
        return annotationEngine.at(address).stream().filter(entry -> entry.source != null).min(Comparator.comparingLong(entry -> entry.ownerId)).map(entry -> entry.source);
    }

    public synchronized void clearSourceMappings() {
        sourceMappings = null;
        sourceMappingsCleared = true;
    }

    /**
     * Host reports execution transitions and completed machine work.
     */
    public synchronized void executionChanged(CPU.RunState previous, CPU.RunState current) {
        boolean starting = previous != CPU.RunState.STATE_RUNNING && current == CPU.RunState.STATE_RUNNING;
        advanceHostGeneration();
        running = current == CPU.RunState.STATE_RUNNING;
        if (starting) {
            watches.clearChanges();
            pendingHit = OptionalInt.empty();
            if (sourceMappings == null) sourceMappingsCleared = true;
            invalidateProgram();
        }
    }

    public synchronized void requirePaused() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        if (running) throw new IllegalStateException("Pause CPU first");
    }

    /**
     * Releases only the capture associated with the completed lifecycle publication.
     */
    private void completeSourceCapture(SourceCaptureGuard guard) {
        synchronized (this) {
            if (completedSourceCapture == guard) completedSourceCapture = null;
        }
        guard.ready.complete(null);
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
        SourceCaptureGuard guard = new SourceCaptureGuard(this);
        guard.capture();
        guard.ready.complete(null);
        return guard;
    }

    public synchronized void applySourceCapture(SourceCaptureGuard guard, Runnable apply) {
        Objects.requireNonNull(guard);
        if (!guard.captured) throw new IllegalStateException("Compilation mutation has not finished");
        // An execution boundary or memory write after compilation makes the mappings stale.
        synchronized (programLock) {
            if (closed || running || activeMutations != 0 || !guard.matches(this))
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

    public synchronized void startNewRecording(int limit, InstructionRecording.Mode mode, int from, int through) {
        recording.start(limit, mode, from, through);
        updateProbe();
    }

    public synchronized void recordTo(Path path) throws IOException {
        try {
            recording.recordTo(path);
        } finally {
            updateProbe();
        }
    }

    public synchronized void stopRecordingFile() throws IOException {
        try {
            recording.stopFile();
        } finally {
            updateProbe();
        }
    }

    public synchronized void updateProbe() {
        observationDirty = false;
        if (closed) return;
        Debugging.Capture capture = recording.requiresFullCapture() ? Debugging.Capture.FULL : Debugging.Capture.INSTRUCTION;
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
        int address = cpuContext.getInstructionLocation();
        boolean stop = false;
        if (skipBeforeStep || stoppedAddress == address) skipBeforeStep = false;
        else stop = checkBreakpoints(address);
        stoppedAddress = -1;
        if (stop) stoppedAddress = address;
        if (observationDirty) updateProbe();
        return stop;
    }

    @Override
    public synchronized boolean afterStep(boolean completed, Optional<DecodedInstruction> instruction, OptionalInt nextAddress, Optional<StepView> captured) {
        if (completed && instruction.isPresent()) {
            observedFlow.executed(instruction.get(), nextAddress);
            flow = null;
        }
        boolean fullCapture = recording.requiresFullCapture();
        recording.capture(completed, captured);
        if (fullCapture != recording.requiresFullCapture()) observationDirty = true;
        if (observationDirty) updateProbe();
        return false;
    }

    @Override
    public synchronized void failed(String message) {
        recording.fail(message);
        captureAttached = null;
        observationDirty = true;
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
        if (closed || running) return OptionalInt.empty();
        OptionalInt hit = pendingHit;
        pendingHit = OptionalInt.empty();
        return hit;
    }

    @Override
    public void close() {
        // The machine owner stops execution before releasing debugger resources.
        synchronized (this) {
            if (closed) return;
            if (running || activeMutations != 0 || completedSourceCapture != null)
                throw new IllegalStateException("Machine work must finish before closing debugger resources");
            closed = true;
        }
        try {
            recording.close();
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
            memoryImageRevision.incrementAndGet();
            invalidateProgram();
            pendingHit = OptionalInt.empty();
            generation++;
        }
    }

    /**
     * Invalidates analysis before and after a machine edit, even if the CPU emits no event.
     */
    public void runMutation(Runnable mutation, CompletionStage<Void> published) {
        memoryImageRevision.incrementAndGet();
        synchronized (this) {
            stoppedAddress = -1;
            watches.clearChanges();
        }
        try {
            runExecution(mutation, published);
        } finally {
            memoryImageRevision.incrementAndGet();
        }
    }

    /**
     * Manual steps skip a breakpoint at the current PC without changing subsequent breakpoint policy.
     */
    public void runStep(Runnable step, CompletionStage<Void> published) {
        synchronized (this) {
            skipBeforeStep = true;
        }
        try {
            runExecution(step, published);
        } finally {
            synchronized (this) {
                skipBeforeStep = false;
            }
        }
    }

    public void runExecution(Runnable execution, CompletionStage<Void> published) {
        try {
            runMachineOperation(execution, true, published);
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
    public void runCompilation(Runnable compilation, CompletionStage<Void> published) {
        synchronized (this) {
            stoppedAddress = -1;
            skipBeforeStep = false;
        }
        boolean restoreNotifications = memory != null && !memory.areMemoryNotificationsEnabled();
        if (restoreNotifications) memory.setMemoryNotificationsEnabled(true);
        try {
            runMachineOperation(compilation, false, published);
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

    private void runMachineOperation(Runnable mutation, boolean invalidateMemory, CompletionStage<Void> published) {
        Objects.requireNonNull(mutation);
        Objects.requireNonNull(published);
        SourceCaptureGuard operation = new SourceCaptureGuard(this);
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
                operation.capture();
                completedSourceCapture = operation;
                if (activeMutations == 0) activeSourceCapture = null;
            }
            published.thenRun(() -> completeSourceCapture(operation));
        }
    }

    /**
     * Carries a just-finished operation across host notifications, never across unrelated edits.
     */
    private void advanceHostGeneration() {
        SourceCaptureGuard guard = completedSourceCapture;
        boolean current = guard != null && !guard.ready.isDone() && guard.captured && guard.matches(this);
        generation++;
        if (current) guard.capture();
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
            if (!running) watches.refresh(config.watches);
            watches.retain(config.watches);
        }
    }

    private void detachObservation() {
        AutoCloseable current = observation;
        observation = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception failure) {
                LOGGER.warn("Debugger detach failed: " + failure.getMessage(), failure);
            }
        }
    }

    private boolean checkBreakpoints(int address) {
        if (!config.breakpoints.contains(address)) return false;
        pendingHit = OptionalInt.of(address);
        return true;
    }

}
