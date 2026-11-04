/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.InstructionFlow;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.application.debugger.index.ProgramIndex;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final String metadataNamespace;
    private AnnotationIndex annotations;
    private final List<Runnable> metadataListeners = new CopyOnWriteArrayList<>();

    public void addMetadataListener(Runnable listener) { metadataListeners.add(listener); }
    public void removeMetadataListener(Runnable listener) { metadataListeners.remove(listener); }
    private void metadataChanged() { metadataListeners.forEach(Runnable::run); }
    private volatile ProgramIndex programIndex;
    // Never acquire the session monitor while holding this lock in a memory notification.
    private final Object indexPublicationLock = new Object();
    private final IdentityHashMap<Rule, Long> executionRuleIds = new IdentityHashMap<>();
    private long nextExecutionRuleId;
    private long[] executionRuleKeys = new long[0];
    private final Set<Long> indexedBreakpointIds = new HashSet<>();
    private Rule[] evaluationRules = new Rule[0];
    private int[] executionRuleOrder = new int[0], candidateOrder = new int[0];
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
        int size = cpu.getAddressSpaceSize();
        if (layout == null || layout.addressSpaceSize() != size) {
            ProgramIndex replacement = ProgramIndex.create(metadataIdentity(size), size);
            synchronized (indexPublicationLock) {
                programIndex = replacement;
            }
            annotations = null;
            indexedBreakpointIds.clear();
            generation++;
        }
        return programIndex;
    }

    public synchronized void setSourceMappings(SourceAddressMap mappings) {
        requirePaused();
        if (mappings.programIndex() != programIndex())
            throw new IllegalStateException("Source mappings belong to another memory image");
        metadataChanged();
    }

    public synchronized void clearSourceMappings() {
        ProgramIndex index = programIndex();
        // Memory writes advance the index revision under this lock; holding it keeps the clear from starving.
        synchronized (indexPublicationLock) {
            SourceAddressMap.clearPublished(index);
        }
        metadataChanged();
    }

    /** Live reads never retain a copy of memory cells. Reacquire after a machine or metadata edit. */
    public LiveMemory liveMemory() {
        if (memory == null) throw new IllegalStateException("Memory unavailable");
        Stamp stamp = liveStamp();
        return new LiveMemory(memory, stamp.base.layout(), () -> requireCurrent(stamp));
    }

    /** User metadata loads without reading or storing memory cells. */
    public AnnotationIndex annotations() throws IOException {
        Stamp stamp = liveStamp();
        synchronized (this) {
            if (annotations != null) return annotations;
        }
        Path path = annotationPath(stamp.base.layout().imageId());
        Properties loaded = path != null && Files.isRegularFile(path) ? AtomicProperties.load(path) : null;
        synchronized (this) {
            requireCurrent(stamp);
            if (annotations == null) {
                AnnotationIndex installed = new AnnotationIndex(stamp.index, this::metadataChanged);
                if (loaded != null && installed.installProperties(loaded, stamp.base) != ProgramIndex.CommitResult.PUBLISHED)
                    throw new IllegalStateException("Metadata discarded: machine or metadata changed while loading");
                annotations = installed;
            }
            return annotations;
        }
    }

    private String metadataIdentity(int size) {
        try {
            String identity = cpu.getTitle() + ":" + metadataNamespace + ":" + size;
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            for (byte value : digest) text.append(String.format("%02x", value & 255));
            return text.toString();
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }

    private Path annotationPath(String identity) {
        return settings == null ? null : settings.resolveSibling(settings.getFileName() + "." + identity + ".annotations");
    }

    public void saveAnnotations() throws IOException {
        AnnotationIndex current = annotations();
        Path path = annotationPath(current.programIndex().snapshot().layout().imageId());
        if (path != null) current.save(path);
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
                    metadataChanged();
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
                                pendingHit = new BreakpointHit(at, null);
                        }
                    }
                    metadataChanged();
                }

                @Override
                public void internalStateChanged() {
                    synchronized (DebugSession.this) {
                        generation++;
                    }
                    metadataChanged();
                }
            };

    public DebugSession(CPU cpu, MemoryContext<?> memory, Path settings) throws IOException {
        this.cpu = Objects.requireNonNull(cpu);
        this.debug = cpu.getDebugger().orElse(null);
        this.memory = debug == null ? memory : debug.memory();
        this.settings = settings;
        metadataNamespace = settings == null ? "session:" + UUID.randomUUID() : settings.toAbsolutePath().normalize().toUri().toString();
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
        runMachineOperation(mutation);
    }

    public void runExecution(Runnable execution) {
        runMachineOperation(execution);
    }

    private void runMachineOperation(Runnable mutation) {
        Objects.requireNonNull(mutation);
        SourceCaptureGuard operation = new SourceCaptureGuard();
        synchronized (this) {
            if (closed) throw new IllegalStateException("Debugger session closed");
            pendingHit = null;
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
                operation.stamp = new Stamp(programIndex, generation);
                if (activeMutations == 0) activeSourceCapture = null;
            }
        }
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
        return guard;
    }

    public synchronized void applySourceCapture(SourceCaptureGuard guard, Runnable apply) {
        Objects.requireNonNull(guard);
        Stamp stamp = guard.stamp;
        if (stamp == null) throw new IllegalStateException("Compilation mutation has not finished");
        applyCurrent(stamp, true, "Source mappings discarded: machine or metadata changed after compilation", apply);
    }

    public static final class SourceCaptureGuard {
        private volatile Stamp stamp;
        private SourceCaptureGuard() { }
    }

    private void invalidateAnalysis() {
        // Writers notify after changing cells. Advance the index before waiting for a paused
        // analysis lease, so its publication cannot race a blocked memory-listener callback.
        invalidateIndex();
        synchronized (this) {
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
        Set<Long> ids = new HashSet<>();
        index.editBreakpoints((base, update) -> {
            ids.clear();
            for (long id : indexedBreakpointIds) update.remove(id);
            for (Rule rule : rules) {
                if (!rule.kind.equals("EXEC")) continue;
                if (rule.address >= base.layout().addressSpaceSize()) continue;
                Long id = executionRuleIds.get(rule);
                if (id == null) {
                    id = nextExecutionRuleId++;
                    executionRuleIds.put(rule, id);
                }
                update.put(id, rule.address, rule.enabled);
                ids.add(id);
            }
            if (cpu.isBreakpointSupported()) {
                Set<Integer> nativeRules = new TreeSet<>(cpu.getBreakpoints());
                nativeRules.addAll(disabledBreakpoints);
                for (int address : nativeRules) {
                    if (address < 0 || address >= base.layout().addressSpaceSize()) continue;
                    long id = Long.MIN_VALUE + address;
                    update.put(id, address, cpu.isBreakpointSet(address));
                    ids.add(id);
                }
            }
            return true;
        });
        indexedBreakpointIds.clear();
        indexedBreakpointIds.addAll(ids);
        evaluationRules = rules.toArray(new Rule[0]);
        executionRuleIds.entrySet().removeIf(entry -> !ids.contains(entry.getValue()));
        TreeMap<Long, Integer> orderById = new TreeMap<>();
        for (int i = 0; i < evaluationRules.length; i++) {
            Rule rule = evaluationRules[i];
            Long id = executionRuleIds.get(rule);
            if (rule.kind.equals("EXEC") && id != null) orderById.put(id, i);
        }
        executionRuleKeys = new long[orderById.size()];
        executionRuleOrder = new int[orderById.size()];
        int slot = 0;
        for (Map.Entry<Long, Integer> entry : orderById.entrySet()) {
            executionRuleKeys[slot] = entry.getKey();
            executionRuleOrder[slot++] = entry.getValue();
        }
        candidateOrder = new int[evaluationRules.length];
    }

    public long evaluate(String expression) {
        return WatchExpression.evaluate(expression, debug, memory);
    }

    @Override
    public synchronized boolean beforeInstruction(int address) {
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
            Arrays.sort(candidateOrder, 0, candidateCount);
            for (int i = 0; i < candidateCount; i++) {
                Rule rule = evaluationRules[candidateOrder[i]];
                if (rule.enabled
                        && execution && rule.kind.equals("EXEC") && rule.address == address) {
                    stoppedAddress = address;
                    status = "Breakpoint: " + rule.name;
                    pendingHit = new BreakpointHit(address, rule);
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

    private boolean condition(Rule rule) {
        return rule.condition.isBlank() || evaluate(rule.condition) != 0;
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
                                                            && a.write
                                                                    == rule.kind.equals("WRITE"))
                                    .findFirst().orElse(null);
                    if (match != null
                            && condition(rule)
                            && ++rule.hits >= rule.afterHits) {
                        status = "Watchpoint: " + rule.name;
                        pendingHit = new BreakpointHit(match.address, rule);
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
        for (Rule rule : rules) if (rule.kind.equals("EXEC")) addresses.add(rule.address);
        return new ArrayList<>(addresses);
    }

    void toggleOrdinaryBreakpoint(int address) {
        requirePaused();
        if (!cpu.isBreakpointSupported()) {
            rules.stream().filter(rule -> isSimpleBreakpoint(rule, address)).findFirst().ifPresent(rule -> rule.enabled = !rule.enabled);
            updateProbe();
            return;
        }
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
        final int address;
        final Rule rule;

        BreakpointHit(int address, Rule rule) {
            this.address = address;
            this.rule = rule;
        }
    }

    private void writeSettings() throws IOException {
        if (settings == null) throw new IOException("Computer configuration path unavailable");
        Properties p = new Properties();
        p.setProperty("version", "2");
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
        List<Integer> ordinary = cpu.isBreakpointSupported() ? ordinaryBreakpoints() : List.of();
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
        if (!"2".equals(p.getProperty("version")) || !cpu.getTitle().equals(p.getProperty("cpu")))
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
                if ("SOURCE".equals(kind) || ("EXEC".equals(kind)
                        && (!p.getProperty(k + "condition", "").isBlank() || !"1".equals(p.getProperty(k + "after"))))) continue;
                Rule r;
                if ("READ".equals(kind) || "WRITE".equals(kind))
                    r = new AccessRule(p.getProperty(k + "name"), kind,
                            Integer.parseInt(p.getProperty(k + "address")),
                            p.getProperty(k + "condition"), Long.parseLong(p.getProperty(k + "after")));
                else
                    r = new Rule(p.getProperty(k + "name"), kind, Integer.parseInt(p.getProperty(k + "address")),
                            p.getProperty(k + "condition"), Long.parseLong(p.getProperty(k + "after")));
                r.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled"));
                if (r.address >= cpu.getAddressSpaceSize())
                    throw new IllegalArgumentException("Breakpoint address outside CPU");
                if ("EXEC".equals(kind) && cpu.isBreakpointSupported()) {
                    if (r.enabled) addresses.add(r.address);
                    else disabledBreakpoints.add(r.address);
                    continue;
                }
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
        public boolean enabled = true;
        public long hits;

        public Rule(
                String name, String kind, int address, String condition, long afterHits) {
            this(name, kind, address, condition, afterHits, false);
        }

        private Rule(String name, String kind, int address, String condition, long afterHits, boolean access) {
            boolean validKind = access ? "READ".equals(kind) || "WRITE".equals(kind)
                    : "EXEC".equals(kind) && condition != null && condition.isBlank() && afterHits == 1;
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
        public AccessRule(String name, String kind, int address, String condition, long afterHits) {
            super(name, kind, address, condition, afterHits, true);
        }
    }
}
