/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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
    private long generation;
    private boolean closed;
    private int activeMutations;
    private final Set<Integer> keptBreakpoints = new HashSet<>();
    private BreakpointHit pendingHit;

    public MemoryAnalysis image() throws IOException {
        requirePaused();
        if (image == null) replaceImage();
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
        long current = pausedGeneration();
        synchronized (this) {
            if (image != null) return new ImageCapture(image, annotations, current);
        }
        return captureImage();
    }

    ImageCapture captureImage() throws IOException {
        long current = pausedGeneration();
        MemoryAnalysis captured = captureMemory();
        AnnotationIndex loaded = new AnnotationIndex(captured);
        Path path = annotationPath(captured);
        if (path != null && Files.isRegularFile(path)) loaded.load(path);
        applyPaused(current, () -> {});
        return new ImageCapture(captured, loaded, current);
    }

    synchronized void installImage(ImageCapture captured) {
        applyPaused(
                captured.generation,
                () -> {
                    image = captured.image;
                    annotations = captured.annotations;
                });
    }

    MemoryAnalysis captureMemory() {
        long current = pausedGeneration();
        MemoryAnalysis captured = MemoryAnalysis.capture(memory);
        applyPaused(current, () -> {});
        return captured;
    }

    static final class ImageCapture {
        final MemoryAnalysis image;
        final AnnotationIndex annotations;
        final long generation;

        ImageCapture(MemoryAnalysis image, AnnotationIndex annotations, long generation) {
            this.image = image;
            this.annotations = annotations;
            this.generation = generation;
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
        annotations = new AnnotationIndex(image);
        saveAnnotations();
    }

    private final Deque<DebugCPU.Instruction> history = new ArrayDeque<>();
    private int historyLimit;
    private boolean attached;
    private int stoppedAddress = -1, stoppedBank = -1;
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
                        if (value == CPU.RunState.STATE_RUNNING) pendingHit = null;
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
        Objects.requireNonNull(mutation);
        synchronized (this) {
            if (closed) throw new IllegalStateException("Debugger session closed");
            pendingHit = null;
            generation++;
            activeMutations++;
        }
        try {
            mutation.run();
        } finally {
            synchronized (this) {
                activeMutations--;
                generation++;
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

    private synchronized void invalidateAnalysis() {
        generation++;
    }

    private void requireNoMutation() {
        if (activeMutations != 0)
            throw new IllegalStateException("Analysis discarded: machine mutation in progress");
    }

    public synchronized void record(int limit) {
        requirePaused();
        if (limit > 0 && debug == null)
            throw new IllegalStateException("Instruction recording unsupported by this CPU");
        if (limit < 0 || limit > 10000)
            throw new IllegalArgumentException("History limit must be 0..10000");
        historyLimit = limit;
        while (history.size() > limit) history.removeFirst();
        updateProbe();
    }

    public synchronized boolean isRecording() {
        return historyLimit > 0;
    }

    public synchronized List<DebugCPU.Instruction> history() {
        return List.copyOf(history);
    }

    public void updateProbe() {
        boolean needed = historyLimit > 0 || rules.stream().anyMatch(r -> r.enabled);
        if (debug != null && needed != attached) {
            if (needed) debug.addListener(this);
            else debug.removeListener(this);
            attached = needed;
        }
    }

    public long evaluate(String expression, int bank) {
        return WatchExpression.evaluate(expression, debug, memory, bank);
    }

    @Override
    public synchronized boolean beforeInstruction(int address, int bank) {
        if (stoppedAddress == address && stoppedBank == bank) {
            stoppedAddress = -1;
            return false;
        }
        stoppedAddress = -1;
        try {
            for (Rule rule : rules)
                if (rule.enabled
                        && (rule.bank < 0 || rule.bank == bank)
                        && ((rule.kind.equals("EXEC") && rule.address == address)
                                || (rule.kind.equals("SOURCE") && sourceMatches(rule, address)))
                        && condition(rule, memory == null ? bank : memory.getSelectedBank())
                        && ++rule.hits >= rule.afterHits) {
                    stoppedAddress = address;
                    stoppedBank = bank;
                    status = "Breakpoint: " + rule.name;
                    pendingHit = new BreakpointHit(address, bank, rule);
                    return true;
                }
        } catch (RuntimeException e) {
            status = "Breakpoint evaluation failed: " + e.getMessage();
            stoppedAddress = address;
            stoppedBank = bank;
            return true;
        }
        return false;
    }

    private boolean sourceMatches(Rule r, int address) {
        if (memory == null || memory.annotations() == null) return false;
        for (SourceCodeAnnotation a : memory.annotations().get(address, SourceCodeAnnotation.class))
            if (a.getPosition().line == r.address
                    && Path.of(a.getPosition().fileName)
                            .toAbsolutePath()
                            .normalize()
                            .toString()
                            .equals(r.source)) return true;
        return false;
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
        try {
            for (Rule rule : rules)
                if (rule.enabled && (rule.kind.equals("READ") || rule.kind.equals("WRITE"))) {
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

    public BreakpointState breakpointState(int address) {
        if (cpu.isBreakpointSupported())
            return !cpu.isBreakpointSet(address) ? BreakpointState.OFF
                    : keptBreakpoints.contains(address) ? BreakpointState.KEEP_INSPECTION : BreakpointState.ON;
        return rules.stream().filter(r -> isSimpleBreakpoint(r, address) && r.enabled)
                .map(r -> r.keepInspection ? BreakpointState.KEEP_INSPECTION : BreakpointState.ON)
                .findFirst().orElse(BreakpointState.OFF);
    }

    public void setBreakpointState(int address, BreakpointState value) {
        requirePaused();
        if (address < 0 || address >= cpu.getAddressSpaceSize())
            throw new IllegalArgumentException("Address outside CPU");
        if (cpu.isBreakpointSupported()) {
            if (value == BreakpointState.OFF) cpu.unsetBreakpoint(address);
            else cpu.setBreakpoint(address);
            if (value == BreakpointState.KEEP_INSPECTION) keptBreakpoints.add(address);
            else keptBreakpoints.remove(address);
        } else if (debug != null) {
            if (value == BreakpointState.OFF) rules.removeIf(r -> isSimpleBreakpoint(r, address));
            else {
                Rule rule = rules.stream().filter(r -> isSimpleBreakpoint(r, address)).findFirst()
                        .orElseGet(() -> {
                            Rule added = new Rule("Breakpoint", "EXEC", address, -1, "", 1);
                            rules.add(added);
                            return added;
                        });
                rule.enabled = true;
                rule.keepInspection = value == BreakpointState.KEEP_INSPECTION;
            }
            updateProbe();
        } else throw new IllegalStateException("Breakpoints unavailable for this CPU");
    }

    private boolean isSimpleBreakpoint(Rule rule, int address) {
        return rule.kind.equals("EXEC") && rule.address == address && rule.bank == -1
                && rule.condition.isBlank() && rule.afterHits == 1;
    }

    BreakpointHit inspectionAtBreakpoint(int address, int bank) {
        Rule rule = cpu.isBreakpointSupported() ? null
                : rules.stream().filter(r -> isSimpleBreakpoint(r, address) && r.enabled).findFirst().orElse(null);
        return new BreakpointHit(address, bank, rule);
    }

    synchronized BreakpointHit takeBreakpointHit() {
        if (closed || state == CPU.RunState.STATE_RUNNING) return null;
        BreakpointHit hit = pendingHit;
        pendingHit = null;
        return hit;
    }

    boolean keepsInspection(BreakpointHit hit) {
        if (hit == null) return false;
        return hit.rule == null ? cpu.isBreakpointSet(hit.address) && keptBreakpoints.contains(hit.address)
                : rules.contains(hit.rule) && hit.rule.enabled && hit.rule.keepInspection;
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
            p.setProperty(k + "bank", Integer.toString(r.bank));
            p.setProperty(k + "condition", r.condition);
            p.setProperty(k + "enabled", Boolean.toString(r.enabled));
            p.setProperty(k + "after", Long.toString(r.afterHits));
            p.setProperty(k + "source", r.source);
            p.setProperty(k + "keepInspection", Boolean.toString(r.keepInspection));
        }
        List<Integer> ordinary = new ArrayList<>(cpu.getBreakpoints());
        Collections.sort(ordinary);
        p.setProperty("ordinary", Integer.toString(ordinary.size()));
        for (int i = 0; i < ordinary.size(); i++) {
            p.setProperty("ordinary." + i, Integer.toString(ordinary.get(i)));
            p.setProperty("ordinary." + i + ".keepInspection", Boolean.toString(keptBreakpoints.contains(ordinary.get(i))));
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
                addresses.add(address);
                if (Boolean.parseBoolean(p.getProperty("ordinary." + i + ".keepInspection"))) keptBreakpoints.add(address);
            }
            int count = count(p, "watches");
            for (int i = 0; i < count; i++)
                watches.add(Objects.requireNonNull(p.getProperty("watch." + i)));
            count = count(p, "rules");
            for (int i = 0; i < count; i++) {
                String k = "rule." + i + ".";
                Rule r =
                        new Rule(
                                p.getProperty(k + "name"),
                                p.getProperty(k + "kind"),
                                Integer.parseInt(p.getProperty(k + "address")),
                                Integer.parseInt(p.getProperty(k + "bank")),
                                p.getProperty(k + "condition"),
                                Long.parseLong(p.getProperty(k + "after")));
                r.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled"));
                r.source = p.getProperty(k + "source", "");
                r.keepInspection = Boolean.parseBoolean(p.getProperty(k + "keepInspection"));
                if (memory != null && r.bank >= memory.getBanksCount())
                    throw new IllegalArgumentException("Breakpoint bank outside memory");
                if (r.kind.equals("SOURCE")) {
                    if (r.address < 1 || r.source.isBlank())
                        throw new IllegalArgumentException("Invalid source breakpoint");
                } else if (r.address >= cpu.getAddressSpaceSize())
                    throw new IllegalArgumentException("Breakpoint address outside CPU");
                rules.add(r);
            }
            addresses.forEach(cpu::setBreakpoint);
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

    public static final class Rule {
        public final String name, kind, condition;
        public final int address, bank;
        public final long afterHits;
        public String source = "";
        public boolean enabled = true;
        public boolean keepInspection;
        public long hits;

        public Rule(
                String name, String kind, int address, int bank, String condition, long afterHits) {
            if (name == null
                    || name.isBlank()
                    || !List.of("EXEC", "READ", "WRITE", "SOURCE").contains(kind)
                    || address < 0
                    || bank < -1
                    || afterHits < 1) throw new IllegalArgumentException("Invalid breakpoint");
            this.name = name;
            this.kind = kind;
            this.address = address;
            this.bank = bank;
            this.condition = Objects.requireNonNull(condition);
            this.afterHits = afterHits;
        }
    }
}
