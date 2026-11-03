/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Per-computer debug state. UI mutates rules/watches only while paused. */
public final class DebugSession implements AutoCloseable, DebugCPU.Listener {
    public final CPU cpu;
    public final DebugCPU debug;
    public final MemoryContext<?> memory;
    public final List<String> watches = new ArrayList<>();
    public final List<Rule> rules = new ArrayList<>();
    private final Path settings;
    private Properties lastSaved;
    private MemoryAnalysis image;
    private AnnotationIndex annotations;
    private volatile ProgramIndex programIndex;
    // Never acquire the session monitor while holding this lock in a memory notification.
    private final Object indexPublicationLock = new Object();
    private SourceAddressMap sourceMappings = SourceAddressMap.empty();
    private final IdentityHashMap<Rule, int[]> sourceBindings = new IdentityHashMap<>();
    private final IdentityHashMap<Rule, Long> executionRuleIds = new IdentityHashMap<>();
    private long nextExecutionRuleId;
    private long[] executionRuleKeys = new long[0];
    private final Set<Long> indexedBreakpointIds = new HashSet<>();
    private Rule[] evaluationRules = new Rule[0];
    private int[] executionRuleOrder = new int[0], sourceRuleOrder = new int[0], candidateOrder = new int[0];
    private int candidateCount;
    private final Consumer<ProgramIndex.Breakpoint> collectExecutionCandidate = breakpoint -> {
        int slot = Arrays.binarySearch(executionRuleKeys, breakpoint.id());
        if (slot >= 0) candidateOrder[candidateCount++] = executionRuleOrder[slot];
    };
    private long generation;
    private boolean closed;
    private int activeMutations;
    private SourceCaptureGuard activeSourceCapture;
    private final Set<Integer> disabledBreakpoints = new HashSet<>();
    private BreakpointHit pendingHit;

    /** Application-owned index; no memory cells are read to construct it. */
    public synchronized ProgramIndex programIndex() {
        if (closed) throw new IllegalStateException("Debugger session closed");
        ProgramIndex.Layout layout = programIndex == null ? null : programIndex.snapshot().layout();
        int size = memory == null ? cpu.getAddressSpaceSize() : memory.getAddressSpaceSize();
        int banks = memory == null ? 1 : memory.getBanksCount();
        int common = memory == null ? 0 : memory.getCommonBoundary();
        if (layout == null || layout.addressSpaceSize() != size
                || layout.bankCount() != banks || layout.commonBoundary() != common) {
            ProgramIndex replacement = ProgramIndex.create("session:" + UUID.randomUUID(), size, banks, common);
            synchronized (indexPublicationLock) {
                programIndex = replacement;
            }
            image = null;
            annotations = null;
            sourceMappings = SourceAddressMap.empty();
            sourceBindings.clear();
            indexedBreakpointIds.clear();
            generation++;
        }
        return programIndex;
    }

    public synchronized void setSourceMappings(SourceAddressMap mappings) {
        requirePaused();
        if (mappings.programIndex() != programIndex())
            throw new IllegalStateException("Source mappings belong to another memory image");
        sourceMappings = mappings;
        bindSourceRules();
    }

    public MemoryAnalysis image() throws IOException {
        requirePaused();
        programIndex();
        if (image == null) installImage(imageCapture());
        return image;
    }

    public AnnotationIndex annotations() throws IOException {
        image();
        return annotations;
    }

    public void replaceImage() throws IOException {
        installImage(captureImage());
    }

    ImageCapture imageCapture() throws IOException {
        programIndex();
        long current = pausedGeneration();
        synchronized (this) {
            if (image != null) return new ImageCapture(image, annotations, current, programIndex, programIndex.snapshot());
        }
        return captureImage(programIndex());
    }

    ImageCapture captureImage() throws IOException {
        ProgramIndex.Layout layout = programIndex().snapshot().layout();
        return captureImage(ProgramIndex.create("image:" + UUID.randomUUID(), layout.addressSpaceSize(),
                layout.bankCount(), layout.commonBoundary()));
    }

    private ImageCapture captureImage(ProgramIndex targetIndex) throws IOException {
        long current = pausedGeneration();
        ProgramIndex.Snapshot base = programIndex().snapshot();
        MemoryAnalysis captured = captureMemory();
        // Read persistence into detached storage. A rejected capture must not mutate this session.
        AnnotationIndex loaded = new AnnotationIndex(captured);
        Path path = annotationPath(captured);
        if (path != null && Files.isRegularFile(path)) loaded.load(path);
        applyPaused(current, () -> {});
        return new ImageCapture(captured, loaded, current, targetIndex, base);
    }

    synchronized AnnotationIndex installImage(ImageCapture captured) {
        applyPaused(captured.generation, () -> {});
        programIndex();
        requireImageBase(captured);
        if (captured.image == image && captured.annotations == annotations && captured.targetIndex == programIndex)
            return annotations;

        boolean replacing = programIndex != captured.targetIndex;
        AnnotationIndex installed = new AnnotationIndex(captured.image, captured.targetIndex);
        ProgramIndex.Snapshot targetBase = replacing ? captured.targetBase : captured.base;
        if (installed.installCaptured(captured.annotations, targetBase) != ProgramIndex.CommitResult.PUBLISHED)
            throw new IllegalStateException("Analysis discarded: source, metadata or memory image changed");
        // The same-index content CAS is its linearization point. New-image content is still
        // detached; recheck the live image and adopt it atomically with observed write invalidation.
        if (replacing) {
            synchronized (indexPublicationLock) {
                requireImageBase(captured);
                programIndex = captured.targetIndex;
                image = captured.image;
                annotations = installed;
            }
            sourceMappings = SourceAddressMap.empty();
            sourceBindings.clear();
            indexedBreakpointIds.clear();
            synchronizeBreakpoints();
            generation++;
        } else {
            image = captured.image;
            annotations = installed;
        }
        return annotations;
    }

    private void requireImageBase(ImageCapture captured) {
        ProgramIndex.Snapshot current = programIndex.snapshot();
        if (current.contentRevision() != captured.base.contentRevision()
                || current.memoryRevision() != captured.base.memoryRevision()
                || current.layout() != captured.base.layout())
            throw new IllegalStateException("Analysis discarded: source, metadata or memory image changed");
    }

    MemoryAnalysis captureMemory() {
        if (memory == null) throw new IllegalStateException("Memory unavailable");
        long current = pausedGeneration();
        MemoryAnalysis captured = MemoryAnalysis.capture(memory);
        applyPaused(current, () -> {});
        return captured;
    }

    static final class ImageCapture {
        final MemoryAnalysis image;
        final AnnotationIndex annotations;
        final long generation;
        final ProgramIndex targetIndex;
        final ProgramIndex.Snapshot base;
        final ProgramIndex.Snapshot targetBase;

        ImageCapture(MemoryAnalysis image, AnnotationIndex annotations, long generation,
                     ProgramIndex targetIndex, ProgramIndex.Snapshot base) {
            this.image = image;
            this.annotations = annotations;
            this.generation = generation;
            this.targetIndex = targetIndex;
            this.base = base;
            this.targetBase = targetIndex.snapshot();
        }
    }

    private Path annotationPath(MemoryAnalysis image) {
        return settings == null
                ? null
                : settings.resolveSibling(
                        settings.getFileName() + "." + image.identity() + ".annotations");
    }

    public void saveAnnotations() throws IOException {
        requirePaused();
        Path path = annotationPath(image);
        if (path != null) annotations.save(path);
    }

    public void clearAnnotations() throws IOException {
        requirePaused();
        annotations().clear();
        saveAnnotations();
    }

    private final Deque<DebugCPU.Instruction> history = new ArrayDeque<>();
    private int historyLimit;
    private InstructionRecording.Writer recording;
    private Path recordingPath;
    private String recordingError = "";
    private boolean attached;
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
    private final CPU.CPUListener stateListener =
            new CPU.CPUListener() {
                @Override
                public void runStateChanged(CPU.RunState value) {
                    synchronized (DebugSession.this) {
                        state = value;
                        generation++;
                        if (value == CPU.RunState.STATE_RUNNING) {
                            pendingHit = null;
                            synchronizeBreakpoints();
                            invalidateIndex();
                        }
                        else if (value == CPU.RunState.STATE_STOPPED_BREAK && pendingHit == null) {
                            // Native CPUs stop before invoking the portable probe. A step or pause
                            // landing on a configured breakpoint must inspect that address too.
                            int at = cpu.getInstructionLocation();
                            if (cpu.isBreakpointSupported() && cpu.isBreakpointSet(at))
                                pendingHit = new BreakpointHit(at, memory == null ? 0 : memory.getBank(at), null);
                        }
                    }
                }

                @Override
                public void internalStateChanged() {
                    synchronized (DebugSession.this) {
                        generation++;
                    }
                }
            };

    public DebugSession(CPU cpu, MemoryContext<?> memory, Path settings) throws IOException {
        this.cpu = Objects.requireNonNull(cpu);
        this.debug = cpu.getDebugger().orElse(null);
        this.memory = debug == null ? memory : debug.memory();
        this.settings = settings;
        if (this.memory != null || cpu.getAddressSpaceSize() > 0) programIndex();
        load();
        cpu.addCPUListener(stateListener);
        if (this.memory != null) this.memory.addMemoryListener(memoryListener);
        updateProbe();
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
        runMachineOperation(mutation, false);
    }

    /** Source rules bind while paused; execution thereafter uses their resolved address targets. */
    public void runExecution(Runnable execution) {
        runMachineOperation(execution, true);
    }

    private void runMachineOperation(Runnable mutation, boolean execution) {
        Objects.requireNonNull(mutation);
        SourceCaptureGuard operation = new SourceCaptureGuard();
        synchronized (this) {
            if (closed) throw new IllegalStateException("Debugger session closed");
            pendingHit = null;
            if (execution) bindSourceRules();
            else sourceBindings.clear();
            generation++;
            invalidateIndex();
            activeMutations++;
            activeSourceCapture = operation;
        }
        try {
            mutation.run();
        } finally {
            synchronized (this) {
                activeMutations--;
                generation++;
                invalidateIndex();
                if (memory != null && !closed) programIndex();
                operation.stamp = new SourceStamp(this);
                if (activeMutations == 0) activeSourceCapture = null;
            }
        }
    }

    synchronized void applyPaused(long expectedGeneration, Runnable apply) {
        requirePaused();
        requireNoMutation();
        if (generation != expectedGeneration)
            throw new IllegalStateException("Analysis discarded: machine changed during capture");
        apply.run();
    }

    /** Capture only under the paused lease; expensive graph discovery uses this decoder outside it. */
    synchronized AnalysisCapture captureAnalysis(long expectedGeneration, ProgramIndex expectedIndex, int bank) {
        applyPaused(expectedGeneration, () -> {});
        if (programIndex() != expectedIndex) throw new IllegalStateException("Analysis discarded: memory image changed");
        ProgramIndex.Snapshot base = expectedIndex.snapshot();
        Disassembler decoder = cpu.getDisassembler().capture(bank)
                .orElseThrow(() -> new IllegalStateException("Stable flow analysis unsupported by this CPU"));
        applyPaused(expectedGeneration, () -> {});
        ProgramIndex.Snapshot current = expectedIndex.snapshot();
        if (programIndex() != expectedIndex || current.memoryRevision() != base.memoryRevision()
                || current.contentRevision() != base.contentRevision())
            throw new IllegalStateException("Analysis discarded: memory or metadata changed during capture");
        return new AnalysisCapture(decoder, base);
    }

    static final class AnalysisCapture {
        final Disassembler decoder;
        final ProgramIndex.Snapshot base;
        AnalysisCapture(Disassembler decoder, ProgramIndex.Snapshot base) { this.decoder = decoder; this.base = base; }
    }

    /** Associates a compiler callback with its enclosing machine mutation, including its reset. */
    public synchronized SourceCaptureGuard captureSourceGuard() {
        requirePaused();
        if (activeMutations > 1) throw new IllegalStateException("Concurrent machine mutations prevent source capture");
        if (activeMutations == 1) return activeSourceCapture;
        SourceCaptureGuard guard = new SourceCaptureGuard();
        guard.stamp = new SourceStamp(this);
        return guard;
    }

    public synchronized void applySourceCapture(SourceCaptureGuard guard, Runnable apply) {
        Objects.requireNonNull(guard);
        SourceStamp stamp = guard.stamp;
        if (stamp == null) throw new IllegalStateException("Compilation mutation has not finished");
        applyPaused(stamp.generation, () -> {});
        ProgramIndex.Snapshot current = programIndex == null ? null : programIndex.snapshot();
        if (programIndex != stamp.index || (current != null && (current.memoryRevision() != stamp.memory
                || current.contentRevision() != stamp.content)))
            throw new IllegalStateException("Source mappings discarded: machine or metadata changed after compilation");
        apply.run();
    }

    public static final class SourceCaptureGuard {
        private volatile SourceStamp stamp;
        private SourceCaptureGuard() { }
    }

    private static final class SourceStamp {
        final ProgramIndex index;
        final long generation, content, memory;
        SourceStamp(DebugSession session) {
            index = session.programIndex;
            generation = session.generation;
            ProgramIndex.Snapshot snapshot = index == null ? null : index.snapshot();
            content = snapshot == null ? 0 : snapshot.contentRevision();
            memory = snapshot == null ? 0 : snapshot.memoryRevision();
        }
    }

    private void invalidateAnalysis() {
        // Writers notify after changing cells. Advance the index before waiting for a paused
        // analysis lease, so its publication cannot race a blocked memory-listener callback.
        invalidateIndex();
        synchronized (this) {
            generation++;
            sourceBindings.clear();
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

    public synchronized void record(int limit) {
        requirePaused();
        if (limit > 0 && debug == null)
            throw new IllegalStateException("Instruction recording unsupported by this CPU");
        if (limit < 0 || limit > 1000000)
            throw new IllegalArgumentException("History limit must be 0..1000000");
        historyLimit = limit;
        if (limit > 0) while (history.size() > limit) history.removeFirst();
        updateProbe();
    }

    public synchronized boolean isRecording() {
        return historyLimit > 0;
    }

    public synchronized List<DebugCPU.Instruction> history() {
        return List.copyOf(history);
    }

    public synchronized InstructionRecording.Header recordingHeader() {
        if (debug == null) throw new IllegalStateException("Instruction recording unsupported by this CPU");
        return new InstructionRecording.Header(debug.processor(), debug.registers());
    }

    public synchronized void recordTo(Path path) throws IOException {
        requirePaused();
        if (recording != null) throw new IllegalStateException("Stop the current file recording first");
        InstructionRecording.Writer writer = new InstructionRecording.Writer(path, recordingHeader());
        recording = writer;
        recordingPath = path.toAbsolutePath().normalize();
        recordingError = "";
        updateProbe();
    }

    public synchronized void stopRecordingFile() throws IOException {
        requirePaused();
        InstructionRecording.Writer writer = recording;
        recording = null;
        recordingPath = null;
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            recordingError = "Incomplete recording: " + e.getMessage();
            status = recordingError;
            throw e;
        } finally { updateProbe(); }
        if (!recordingError.isBlank()) throw new IOException(recordingError);
    }

    public synchronized Path recordingPath() { return recordingPath; }
    public synchronized String recordingError() { return recordingError; }

    public void saveHistory(Path path, boolean text) throws IOException {
        requirePaused();
        InstructionRecording.Header header = recordingHeader();
        List<DebugCPU.Instruction> events = history();
        if (text) InstructionRecording.exportText(path, header, events);
        else InstructionRecording.write(path, header, events);
    }

    public synchronized void updateProbe() {
        sourceBindings.clear();
        bindSourceRules();
        synchronizeBreakpoints();
        boolean needed = recording != null || historyLimit > 0 || rules.stream().anyMatch(r -> r.enabled);
        if (debug != null && needed != attached) {
            if (needed) debug.addListener(this);
            else debug.removeListener(this);
            attached = needed;
        }
    }

    /** Replays logical editor state only at a supported paused edit/start boundary. */
    private void synchronizeBreakpoints() {
        if (programIndex == null && memory == null && cpu.getAddressSpaceSize() <= 0) return;
        ProgramIndex index = programIndex();
        for (;;) {
            ProgramIndex.Snapshot base = index.snapshot();
            try (ProgramIndex.BreakpointUpdate update = index.updateBreakpoints(base)) {
                for (long id : indexedBreakpointIds) update.remove(id);
                Set<Long> ids = new HashSet<>();
                for (Rule rule : rules) {
                    if (!rule.kind.equals("EXEC")) continue;
                    if (rule.address >= base.layout().addressSpaceSize()) continue;
                    Long id = executionRuleIds.get(rule);
                    if (id == null) {
                        id = nextExecutionRuleId++;
                        executionRuleIds.put(rule, id);
                    }
                    update.put(id, rule.address, rule.enabled, rule.condition);
                    ids.add(id);
                }
                if (cpu.isBreakpointSupported()) {
                    Set<Integer> nativeRules = new TreeSet<>(cpu.getBreakpoints());
                    nativeRules.addAll(disabledBreakpoints);
                    for (int address : nativeRules) {
                        if (address < 0 || address >= base.layout().addressSpaceSize()) continue;
                        long id = Long.MIN_VALUE + address;
                        update.put(id, address, cpu.isBreakpointSet(address), "");
                        ids.add(id);
                    }
                }
                if (update.commit() == ProgramIndex.CommitResult.PUBLISHED) {
                    indexedBreakpointIds.clear();
                    indexedBreakpointIds.addAll(ids);
                    evaluationRules = rules.toArray(new Rule[0]);
                    executionRuleIds.entrySet().removeIf(entry -> !ids.contains(entry.getValue()));
                    TreeMap<Long, Integer> orderById = new TreeMap<>();
                    int[] sources = new int[evaluationRules.length];
                    int sourceCount = 0;
                    for (int i = 0; i < evaluationRules.length; i++) {
                        Rule rule = evaluationRules[i];
                        Long id = executionRuleIds.get(rule);
                        if (rule.kind.equals("EXEC") && id != null) orderById.put(id, i);
                        if (rule.kind.equals("SOURCE") && rule.enabled) sources[sourceCount++] = i;
                    }
                    sourceRuleOrder = Arrays.copyOf(sources, sourceCount);
                    executionRuleKeys = new long[orderById.size()];
                    executionRuleOrder = new int[orderById.size()];
                    int slot = 0;
                    for (Map.Entry<Long, Integer> entry : orderById.entrySet()) {
                        executionRuleKeys[slot] = entry.getKey();
                        executionRuleOrder[slot++] = entry.getValue();
                    }
                    candidateOrder = new int[evaluationRules.length];
                    return;
                }
            }
        }
    }

    public long evaluate(String expression, int bank) {
        return WatchExpression.evaluate(expression, debug, memory, bank);
    }

    @Override
    public synchronized boolean beforeInstruction(int address, int bank) {
        if (stoppedAddress == address) {
            stoppedAddress = -1;
            return false;
        }
        stoppedAddress = -1;
        try {
            // UI editing is paused-only. Reacquire this immutable view at every instruction boundary.
            if (state != CPU.RunState.STATE_RUNNING) synchronizeBreakpoints();
            ProgramIndex.Snapshot snapshot = programIndex == null ? null : programIndex.snapshot();
            boolean execution = snapshot != null && snapshot.hasExecutionBreakpoint(address);
            candidateCount = 0;
            if (execution) snapshot.forEachExecutionBreakpoint(address, collectExecutionCandidate);
            for (int order : sourceRuleOrder) candidateOrder[candidateCount++] = order;
            Arrays.sort(candidateOrder, 0, candidateCount);
            for (int i = 0; i < candidateCount; i++) {
                Rule rule = evaluationRules[candidateOrder[i]];
                if (rule.enabled
                        && ((execution && rule.kind.equals("EXEC") && rule.address == address)
                                || (rule.kind.equals("SOURCE") && sourceMatches(rule, address)))
                        && condition(rule, memory == null ? bank : memory.getSelectedBank())
                        && ++rule.hits >= rule.afterHits) {
                    stoppedAddress = address;
                    status = "Breakpoint: " + rule.name;
                    pendingHit = new BreakpointHit(address, bank, rule);
                    return true;
                }
            }
        } catch (RuntimeException e) {
            status = "Breakpoint evaluation failed: " + e.getMessage();
            stoppedAddress = address;
            return true;
        }
        return false;
    }

    private boolean sourceMatches(Rule rule, int address) {
        int[] bindings = sourceBindings.get(rule);
        return bindings != null && Arrays.binarySearch(bindings, address) >= 0;
    }

    private void bindSourceRules() {
        for (Rule rule : rules) {
            if (!rule.kind.equals("SOURCE") || !rule.enabled || sourceBindings.containsKey(rule)) continue;
            List<Integer> addresses = new ArrayList<>();
            sourceMappings.forEachCurrentLocation(rule.source, rule.address, location -> addresses.add(location.address));
            if (!addresses.isEmpty())
                sourceBindings.put(rule, addresses.stream().mapToInt(Integer::intValue).sorted().distinct().toArray());
        }
    }

    private boolean condition(Rule rule, int bank) {
        return rule.condition.isBlank() || evaluate(rule.condition, bank) != 0;
    }

    @Override
    public synchronized boolean afterInstruction(DebugCPU.Instruction event) {
        if (historyLimit > 0) {
            history.addLast(event);
            while (history.size() > historyLimit) history.removeFirst();
        }
        if (recording != null) {
            try { recording.append(event); }
            catch (IOException e) {
                recordingError = "Incomplete recording: " + e.getMessage();
                status = recordingError;
                // Return through the CPU probe to pause safely; listener changes wait for a paused boundary.
                return true;
            }
        }
        try {
            for (Rule candidate : rules)
                if (candidate.enabled && candidate instanceof AccessRule) {
                    AccessRule rule = (AccessRule) candidate;
                    DebugCPU.Access match =
                            event.accesses.stream()
                                    .filter(
                                            a ->
                                                    a.address == rule.address
                                                            && (rule.bank < 0
                                                                    || rule.bank == a.bank)
                                                            && a.write
                                                                    == rule.kind.equals("WRITE"))
                                    .findFirst().orElse(null);
                    if (match != null
                            && condition(
                                    rule, memory == null ? event.bank : memory.getSelectedBank())
                            && ++rule.hits >= rule.afterHits) {
                        status = "Watchpoint: " + rule.name;
                        pendingHit = new BreakpointHit(match.address, match.bank, rule);
                        return true;
                    }
                }
        } catch (RuntimeException e) {
            status = "Watchpoint evaluation failed: " + e.getMessage();
            return true;
        }
        return false;
    }

    public void save() throws IOException {
        requirePaused();
        writeSettings();
    }

    public boolean isBreakpointSet(int address) {
        if (cpu.isBreakpointSupported()) return cpu.isBreakpointSet(address);
        return rules.stream().anyMatch(r -> isSimpleBreakpoint(r, address) && r.enabled);
    }

    public void setBreakpoint(int address, boolean enabled) {
        requirePaused();
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        if (cpu.isBreakpointSupported()) {
            if (!enabled) cpu.unsetBreakpoint(address);
            else cpu.setBreakpoint(address);
            disabledBreakpoints.remove(address);
            synchronizeBreakpoints();
        } else if (debug != null) {
            if (!enabled) rules.removeIf(r -> isSimpleBreakpoint(r, address));
            else {
                Rule rule = rules.stream().filter(r -> isSimpleBreakpoint(r, address)).findFirst()
                        .orElseGet(() -> {
                            Rule added = new Rule("Breakpoint", "EXEC", address, "", 1);
                            rules.add(added);
                            return added;
                        });
                rule.enabled = true;
            }
            updateProbe();
        } else throw new IllegalStateException("Breakpoints unavailable for this CPU");
    }

    List<Integer> ordinaryBreakpoints() {
        Set<Integer> addresses = new TreeSet<>(cpu.getBreakpoints());
        addresses.addAll(disabledBreakpoints);
        return new ArrayList<>(addresses);
    }

    void toggleOrdinaryBreakpoint(int address) {
        requirePaused();
        if (cpu.isBreakpointSet(address)) {
            cpu.unsetBreakpoint(address);
            disabledBreakpoints.add(address);
        } else {
            cpu.setBreakpoint(address);
            disabledBreakpoints.remove(address);
        }
        synchronizeBreakpoints();
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
        final int address, bank;
        final Rule rule;

        BreakpointHit(int address, int bank, Rule rule) {
            this.address = address;
            this.bank = bank;
            this.rule = rule;
        }
    }

    private void writeSettings() throws IOException {
        if (settings == null) throw new IOException("Computer configuration path unavailable");
        Properties p = new Properties();
        p.setProperty("version", "1");
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
            if (r instanceof AccessRule) p.setProperty(k + "bank", Integer.toString(((AccessRule) r).bank));
            p.setProperty(k + "condition", r.condition);
            p.setProperty(k + "enabled", Boolean.toString(r.enabled));
            p.setProperty(k + "after", Long.toString(r.afterHits));
            p.setProperty(k + "source", r.source);
        }
        List<Integer> ordinary = ordinaryBreakpoints();
        p.setProperty("ordinary", Integer.toString(ordinary.size()));
        for (int i = 0; i < ordinary.size(); i++) {
            p.setProperty("ordinary." + i, Integer.toString(ordinary.get(i)));
            p.setProperty("ordinary." + i + ".enabled", Boolean.toString(cpu.isBreakpointSet(ordinary.get(i))));
        }
        if (!p.equals(lastSaved)) {
            AtomicProperties.save(settings, p);
            lastSaved = p;
        }
    }

    private void load() throws IOException {
        if (settings == null || !Files.isRegularFile(settings)) return;
        Properties p = AtomicProperties.load(settings);
        if (!"1".equals(p.getProperty("version")) || !cpu.getTitle().equals(p.getProperty("cpu")))
            throw new IOException("Debugger settings belong to another CPU/version");
        try {
            int ordinary = count(p, "ordinary");
            List<Integer> addresses = new ArrayList<>();
            for (int i = 0; i < ordinary; i++) {
                int address = Integer.parseInt(p.getProperty("ordinary." + i));
                if (address < 0 || address >= cpu.getAddressSpaceSize())
                    throw new IllegalArgumentException("Breakpoint address outside CPU");
                if (Boolean.parseBoolean(p.getProperty("ordinary." + i + ".enabled", "true"))) addresses.add(address);
                else disabledBreakpoints.add(address);
            }
            int count = count(p, "watches");
            for (int i = 0; i < count; i++)
                watches.add(Objects.requireNonNull(p.getProperty("watch." + i)));
            count = count(p, "rules");
            for (int i = 0; i < count; i++) {
                String k = "rule." + i + ".";
                String kind = p.getProperty(k + "kind");
                Rule r;
                if ("READ".equals(kind) || "WRITE".equals(kind))
                    r = new AccessRule(p.getProperty(k + "name"), kind,
                            Integer.parseInt(p.getProperty(k + "address")), Integer.parseInt(p.getProperty(k + "bank")),
                            p.getProperty(k + "condition"), Long.parseLong(p.getProperty(k + "after")));
                else
                    r = new Rule(p.getProperty(k + "name"), kind, Integer.parseInt(p.getProperty(k + "address")),
                            p.getProperty(k + "condition"), Long.parseLong(p.getProperty(k + "after")));
                r.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled"));
                r.source = p.getProperty(k + "source", "");
                if (memory != null && r instanceof AccessRule && ((AccessRule) r).bank >= memory.getBanksCount())
                    throw new IllegalArgumentException("Breakpoint bank outside memory");
                if (r.kind.equals("SOURCE")) {
                    if (r.address < 1 || r.source.isBlank())
                        throw new IllegalArgumentException("Invalid source breakpoint");
                } else if (r.address >= cpu.getAddressSpaceSize())
                    throw new IllegalArgumentException("Breakpoint address outside CPU");
                rules.add(r);
            }
            addresses.forEach(cpu::setBreakpoint);
            disabledBreakpoints.forEach(cpu::unsetBreakpoint);
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
        if (state == CPU.RunState.STATE_RUNNING) cpu.pause();
        try { stopRecordingFile(); }
        catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(DebugSession.class).error("Could not finish instruction recording", e);
        }
        if (settings != null)
            try {
                writeSettings();
            } catch (IOException e) {
                org.slf4j.LoggerFactory.getLogger(DebugSession.class)
                        .error("Could not save debugger settings", e);
            }
        cpu.removeCPUListener(stateListener);
        if (memory != null) memory.removeMemoryListener(memoryListener);
        if (debug != null) debug.removeListener(this);
        attached = false;
        synchronized (this) {
            closed = true;
            pendingHit = null;
            generation++;
        }
    }

    public static class Rule {
        public final String name, kind, condition;
        public final int address;
        public final long afterHits;
        public String source = "";
        public boolean enabled = true;
        public long hits;

        public Rule(
                String name, String kind, int address, String condition, long afterHits) {
            this(name, kind, address, condition, afterHits, false);
        }

        private Rule(String name, String kind, int address, String condition, long afterHits, boolean access) {
            boolean validKind = access ? "READ".equals(kind) || "WRITE".equals(kind)
                    : "EXEC".equals(kind) || "SOURCE".equals(kind);
            if (name == null
                    || name.isBlank()
                    || !validKind
                    || address < 0
                    || afterHits < 1) throw new IllegalArgumentException("Invalid breakpoint");
            this.name = name;
            this.kind = kind;
            this.address = address;
            this.condition = Objects.requireNonNull(condition);
            this.afterHits = afterHits;
        }
    }

    public static final class AccessRule extends Rule {
        public final int bank;

        public AccessRule(String name, String kind, int address, int bank, String condition, long afterHits) {
            super(name, kind, address, condition, afterHits, true);
            if (bank < -1) throw new IllegalArgumentException("Invalid watchpoint bank");
            this.bank = bank;
        }
    }
}
