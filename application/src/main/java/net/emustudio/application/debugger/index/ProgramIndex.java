/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Immutable, in-memory queries for one debugger session's memory image and layout.
 * Addresses count cells, not bytes. Create a new index for a different image/layout.
 * No query reads memory, performs I/O, decodes instructions, or evaluates expressions.
 * Visitors run synchronously; callback exceptions propagate. Returned values are deeply
 * immutable, ordering is deterministic, and snapshots require no reader locks.
 */
public final class ProgramIndex {
    private final IndexLayout layout;
    private final Object identity = new Object();
    private final AtomicReference<View> root;

    private ProgramIndex(String imageId, int size, int banks, int common) {
        layout = new IndexLayout(imageId, size, banks, common);
        Content empty = new Content(new Mappings(layout, new Mapping[0]), new Annotations(layout, new Annotation[0]), null);
        root = new AtomicReference<>(new View(identity, layout, empty, new BreakpointIndex(layout, new Breakpoint[0]),
                0, 0, 0, FlowState.NOT_BUILT));
    }

    /** Creates an empty index, copying and validating its immutable layout. */
    public static ProgramIndex create(String imageId, int addressSpaceSize, int bankCount, int commonBoundary) {
        return new ProgramIndex(imageId, addressSpaceSize, bankCount, commonBoundary);
    }

    /** Returns the existing coherent immutable root, without copying. */
    public Snapshot snapshot() { return root.get(); }
    public ContentUpdate updateContent(Snapshot base) { return new ContentEdit(own(base)); }
    public BreakpointUpdate updateBreakpoints(Snapshot base) { return new BreakpointEdit(own(base)); }

    /**
     * Advances memory revision, retains historical mappings, and makes current graph queries
     * unavailable. Old snapshots retain their meaning. Adapters must call this on observable
     * writes and before execution if notifications are unreliable; a snapshot does not freeze
     * live memory. Analysis requires a stable capture or a verified paused lifecycle.
     */
    public void memoryChanged() {
        while (true) {
            View old = root.get();
            View next = new View(identity, layout, old.content, old.breakpoints, old.contentRevision, old.breakpointRevision,
                    old.memoryRevision + 1, FlowState.STALE);
            if (root.compareAndSet(old, next)) return;
        }
    }

    public enum CommitResult { PUBLISHED, STALE }
    public enum FlowState { NOT_BUILT, CURRENT, TRUNCATED, STALE }
    public enum InstructionKind { LINEAR, JUMP, CALL, RETURN, STOP }
    public enum EdgeKind { FALL_THROUGH, BRANCH, CALL }

    public static final class Kinds {
        public static final String LABEL = "label";
        public static final String COMMENT = "comment";
        public static final String REGION = "region";
        public static final String CODE = "code";
        public static final String DATA = "data";
        private Kinds() { }
    }

    public interface Layout {
        String imageId();
        int addressSpaceSize();
        int bankCount();
        int commonBoundary();
        /** Validates bank/address; common cells always resolve to physical bank zero. */
        int physicalBank(int bank, int address);
    }

    public interface Snapshot {
        Layout layout();
        long contentRevision();
        long breakpointRevision();
        long memoryRevision();
        void forEachMapping(Consumer<? super Mapping> visitor);
        void forEachAnnotation(Consumer<? super Annotation> visitor);
        /** Includes disabled rules. */
        void forEachBreakpoint(Consumer<? super Breakpoint> visitor);
        void forEachMappingAt(int bank, int address, Consumer<? super Mapping> visitor);
        /** Exact document identity AND revision; explicit half-open source spans only. */
        void forEachMappingOverlappingSource(SourceSpan span, Consumer<? super Mapping> visitor);
        void forEachAnnotationStartingAt(int bank, int address, Consumer<? super Annotation> visitor);
        void forEachAnnotationCovering(int bank, int address, Consumer<? super Annotation> visitor);
        /** Enabled execution rules at the CPU-visible address, independent of memory banks. */
        boolean hasExecutionBreakpoint(int address);
        void forEachExecutionBreakpoint(int address, Consumer<? super Breakpoint> visitor);
        FlowState flowState();
        /** Exact instruction starts only; unavailable for NOT_BUILT/STALE. */
        Instruction instructionAt(int bank, int address);
        void forEachSuccessor(int bank, int address, EdgeVisitor visitor);
        /** All incoming edge kinds; callers wanting calls must filter CALL explicitly. */
        void forEachPredecessor(int bank, int address, EdgeVisitor visitor);
        /** Bank/start order; unavailable for NOT_BUILT/STALE. */
        void forEachInstruction(Consumer<? super Instruction> visitor);
    }

    /**
     * Thread-confined lazy copy-on-write edit. Upserts use (ownerId, entryId), independently
     * per category. The trusted application enforces ownership restrictions at plugin adapters.
     * Commit consumes the edit; a validation failure also consumes it. Close is
     * idempotent and publishes nothing. Failed edits cannot publish earlier staged entries.
     */
    public interface ContentUpdate extends AutoCloseable {
        ContentUpdate removeMappings(long ownerId);
        ContentUpdate putMapping(long ownerId, long entryId, Range range, SourceSpan source);
        ContentUpdate removeAnnotations(long ownerId);
        ContentUpdate putAnnotation(long ownerId, long entryId, Range range, String kind, String text);
        ContentUpdate removeAnnotation(long ownerId, long entryId);
        ContentUpdate replaceFlow(boolean truncated);
        ContentUpdate putInstruction(Range cells, InstructionKind kind, boolean unresolvedSuccessors);
        /** Known undecoded destinations are permitted. Unknown indirect targets are never edges. */
        ContentUpdate putEdge(int fromBank, int fromAddress, int toBank, int toAddress, EdgeKind kind);
        /**
         * Compares content AND memory revisions, preserving latest independent breakpoint state.
         * Source or CODE/DATA edits stale retained flow. replaceFlow supplies resulting flow.
         * Invalid edits throw IllegalArgumentException and publish nothing.
         */
        CommitResult commit();
        @Override void close();
    }

    public interface BreakpointUpdate extends AutoCloseable {
        /** CPU-visible address only; bank selection never changes execution-rule matching. */
        BreakpointUpdate put(long id, int address, boolean enabled, String condition);
        BreakpointUpdate remove(long id);
        /** Compares only breakpoint revision, preserving latest content/memory state. */
        CommitResult commit();
        @Override void close();
    }

    public interface Mapping {
        long ownerId();
        long entryId();
        Range range();
        SourceSpan source();
        long memoryRevision();
    }
    public interface Annotation {
        long ownerId();
        long entryId();
        Range range();
        String kind();
        String text();
    }
    public interface Breakpoint {
        long id();
        int address();
        boolean enabled();
        String condition();
    }
    public interface Instruction {
        Range cells();
        InstructionKind kind();
        boolean unresolvedSuccessors();
    }
    @FunctionalInterface
    public interface EdgeVisitor {
        void accept(int fromBank, int fromAddress, int toBank, int toAddress, EdgeKind kind);
    }

    /** Nonempty half-open cell range. Layout validates its upper bound and bank. */
    public static final class Range {
        public final int bank, start, end;
        public Range(int bank, int start, int end) {
            if (bank < 0 || start < 0 || end <= start)
                throw new IllegalArgumentException("Invalid bank or range");
            this.bank = bank;
            this.start = start;
            this.end = end;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Range)) return false;
            Range r = (Range) other;
            return bank == r.bank && start == r.start && end == r.end;
        }
        @Override public int hashCode() { return 31 * (31 * bank + start) + end; }
    }

    /** Exact document revision and nonempty half-open UTF-16 offset span. */
    public static final class SourceSpan {
        public final String fileId;
        public final long sourceRevision;
        public final int startOffset, endOffset;
        public SourceSpan(String fileId, long sourceRevision, int startOffset, int endOffset) {
            this.fileId = Objects.requireNonNull(fileId, "fileId");
            if (fileId.isEmpty() || sourceRevision < 0 || startOffset < 0 || endOffset <= startOffset)
                throw new IllegalArgumentException("Invalid source identity, revision, or span");
            this.sourceRevision = sourceRevision;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof SourceSpan)) return false;
            SourceSpan s = (SourceSpan) other;
            return sourceRevision == s.sourceRevision && startOffset == s.startOffset
                    && endOffset == s.endOffset && fileId.equals(s.fileId);
        }
        @Override public int hashCode() {
            int h = 31 * fileId.hashCode() + Long.hashCode(sourceRevision);
            return 31 * (31 * h + startOffset) + endOffset;
        }
    }

    private View own(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "base");
        if (!(snapshot instanceof View) || ((View) snapshot).identity != identity)
            throw new IllegalArgumentException("Snapshot belongs to another index");
        return (View) snapshot;
    }

    private static final class View implements Snapshot {
        final Object identity;
        final IndexLayout layout;
        final Content content;
        final BreakpointIndex breakpoints;
        final long contentRevision, breakpointRevision, memoryRevision;
        final FlowState flowState;

        View(Object identity, IndexLayout layout, Content content, BreakpointIndex breakpoints, long contentRevision, long breakpointRevision,
             long memoryRevision, FlowState flowState) {
            this.identity = identity;
            this.layout = layout;
            this.content = content;
            this.breakpoints = breakpoints;
            this.contentRevision = contentRevision;
            this.breakpointRevision = breakpointRevision;
            this.memoryRevision = memoryRevision;
            this.flowState = flowState;
        }

        @Override public Layout layout() { return layout; }
        @Override public long contentRevision() { return contentRevision; }
        @Override public long breakpointRevision() { return breakpointRevision; }
        @Override public long memoryRevision() { return memoryRevision; }
        @Override public FlowState flowState() { return flowState; }

        @Override public void forEachMapping(Consumer<? super Mapping> visitor) {
            Objects.requireNonNull(visitor, "visitor");
            for (Mapping value : content.mappings.values) visitor.accept(value);
        }
        @Override public void forEachAnnotation(Consumer<? super Annotation> visitor) {
            Objects.requireNonNull(visitor, "visitor");
            for (Annotation value : content.annotations.values) visitor.accept(value);
        }
        @Override public void forEachBreakpoint(Consumer<? super Breakpoint> visitor) {
            Objects.requireNonNull(visitor, "visitor");
            for (Breakpoint value : breakpoints.values) visitor.accept(value);
        }
        @Override public void forEachMappingAt(int bank, int address, Consumer<? super Mapping> visitor) {
            int physical = layout.physicalBank(bank, address);
            Objects.requireNonNull(visitor, "visitor");
            content.mappings.ranges.visit(physical, address, visitor);
        }
        @Override public void forEachMappingOverlappingSource(SourceSpan span, Consumer<? super Mapping> visitor) {
            Objects.requireNonNull(span, "span");
            Objects.requireNonNull(visitor, "visitor");
            content.mappings.sources.overlapping(span, visitor);
        }
        @Override public void forEachAnnotationStartingAt(int bank, int address, Consumer<? super Annotation> visitor) {
            int physical = layout.physicalBank(bank, address);
            Objects.requireNonNull(visitor, "visitor");
            content.annotations.ranges.startingAt(physical, address, visitor);
        }
        @Override public void forEachAnnotationCovering(int bank, int address, Consumer<? super Annotation> visitor) {
            int physical = layout.physicalBank(bank, address);
            Objects.requireNonNull(visitor, "visitor");
            content.annotations.ranges.covering(physical, address, visitor);
        }
        @Override public boolean hasExecutionBreakpoint(int address) {
            layout.checkAddress(address);
            return breakpoints.has(address);
        }
        @Override public void forEachExecutionBreakpoint(int address, Consumer<? super Breakpoint> visitor) {
            layout.checkAddress(address);
            Objects.requireNonNull(visitor, "visitor");
            breakpoints.visit(address, visitor);
        }
        private boolean flowAvailable() { return flowState == FlowState.CURRENT || flowState == FlowState.TRUNCATED; }
        @Override public Instruction instructionAt(int bank, int address) {
            int physical = layout.physicalBank(bank, address);
            return flowAvailable() ? content.flow.instructionAt(physical, address) : null;
        }
        @Override public void forEachSuccessor(int bank, int address, EdgeVisitor visitor) {
            int physical = layout.physicalBank(bank, address);
            Objects.requireNonNull(visitor, "visitor");
            if (flowAvailable()) content.flow.successors(physical, address, visitor);
        }
        @Override public void forEachPredecessor(int bank, int address, EdgeVisitor visitor) {
            int physical = layout.physicalBank(bank, address);
            Objects.requireNonNull(visitor, "visitor");
            if (flowAvailable()) content.flow.predecessors(physical, address, visitor);
        }
        @Override public void forEachInstruction(Consumer<? super Instruction> visitor) {
            Objects.requireNonNull(visitor, "visitor");
            if (flowAvailable()) content.flow.instructions(visitor);
        }
    }

    private abstract static class Edit {
        final Thread writer = Thread.currentThread();
        boolean open = true;
        void requireOpen() {
            if (writer != Thread.currentThread()) throw new IllegalStateException("Edit is thread-confined");
            if (!open) throw new IllegalStateException("Edit already consumed");
        }
        Range range(IndexLayout layout, Range range) {
            try { return layout.canonical(range); }
            catch (RuntimeException failure) { open = false; throw failure; }
        }
        int bank(IndexLayout layout, int bank, int address) {
            try { return layout.physicalBank(bank, address); }
            catch (RuntimeException failure) { open = false; throw failure; }
        }
        <T> T required(T value, String name) {
            if (value == null) {
                open = false;
                throw new NullPointerException(name);
            }
            return value;
        }
        void validate(boolean valid, String message) {
            if (!valid) {
                open = false;
                throw new IllegalArgumentException(message);
            }
        }
        public void close() {
            if (writer != Thread.currentThread()) throw new IllegalStateException("Edit is thread-confined");
            open = false;
        }
    }

    private final class ContentEdit extends Edit implements ContentUpdate {
        final View base;
        Map<Key, Mapping> mappings;
        Map<Key, Annotation> annotations;
        Map<Long, Instruction> instructions;
        Set<FlowGraph.Edge> edges;
        boolean truncated, metadataChangesFlow;

        ContentEdit(View base) { this.base = base; }
        private Map<Key, Mapping> mappings() {
            if (mappings == null) {
                mappings = new TreeMap<>();
                for (Mapping value : base.content.mappings.values)
                    mappings.put(new Key(value.ownerId(), value.entryId()), value);
            }
            return mappings;
        }
        private Map<Key, Annotation> annotations() {
            if (annotations == null) {
                annotations = new TreeMap<>();
                for (Annotation value : base.content.annotations.values)
                    annotations.put(new Key(value.ownerId(), value.entryId()), value);
            }
            return annotations;
        }
        @Override public ContentUpdate removeMappings(long ownerId) {
            requireOpen();
            mappings().entrySet().removeIf(entry -> entry.getKey().owner == ownerId);
            metadataChangesFlow = true;
            return this;
        }
        @Override public ContentUpdate putMapping(long ownerId, long entryId, Range range, SourceSpan source) {
            requireOpen();
            Range checked = range(layout, range);
            required(source, "source");
            mappings().put(new Key(ownerId, entryId), new MappingValue(ownerId, entryId, checked, source, base.memoryRevision));
            metadataChangesFlow = true;
            return this;
        }
        @Override public ContentUpdate removeAnnotations(long ownerId) {
            requireOpen();
            Iterator<Annotation> iterator = annotations().values().iterator();
            while (iterator.hasNext()) {
                Annotation value = iterator.next();
                if (value.ownerId() == ownerId) {
                    metadataChangesFlow |= affectsFlow(value.kind());
                    iterator.remove();
                }
            }
            return this;
        }
        @Override public ContentUpdate putAnnotation(long ownerId, long entryId, Range range, String kind, String text) {
            requireOpen();
            Range checked = range(layout, range);
            required(kind, "kind");
            required(text, "text");
            validate(!kind.isEmpty() && (!kind.equals(Kinds.LABEL) || !text.isEmpty()), "Empty annotation kind or label");
            Annotation old = annotations().put(new Key(ownerId, entryId),
                    new AnnotationValue(ownerId, entryId, checked, kind, text));
            metadataChangesFlow |= affectsFlow(kind) || (old != null && affectsFlow(old.kind()));
            return this;
        }
        @Override public ContentUpdate removeAnnotation(long ownerId, long entryId) {
            requireOpen();
            Annotation removed = annotations().remove(new Key(ownerId, entryId));
            if (removed != null) metadataChangesFlow |= affectsFlow(removed.kind());
            return this;
        }
        @Override public ContentUpdate replaceFlow(boolean truncated) {
            requireOpen();
            instructions = new TreeMap<>();
            edges = new TreeSet<>();
            this.truncated = truncated;
            return this;
        }
        private void requireFlow() {
            requireOpen();
            if (instructions == null) {
                open = false;
                throw new IllegalStateException("replaceFlow must precede instructions and edges");
            }
        }
        @Override public ContentUpdate putInstruction(Range cells, InstructionKind kind, boolean unresolvedSuccessors) {
            requireFlow();
            Range checked = range(layout, cells);
            required(kind, "kind");
            instructions.put(AddressTable.key(checked.bank, checked.start), new InstructionValue(checked, kind, unresolvedSuccessors));
            return this;
        }
        @Override public ContentUpdate putEdge(int fromBank, int fromAddress, int toBank, int toAddress, EdgeKind kind) {
            requireFlow();
            long from = AddressTable.key(bank(layout, fromBank, fromAddress), fromAddress);
            long to = AddressTable.key(bank(layout, toBank, toAddress), toAddress);
            edges.add(new FlowGraph.Edge(from, to, required(kind, "kind")));
            return this;
        }
        @Override public CommitResult commit() {
            requireOpen();
            open = false;
            Mappings nextMappings = mappings == null ? base.content.mappings : new Mappings(layout, mappings.values().toArray(new Mapping[0]));
            Annotations nextAnnotations = annotations == null ? base.content.annotations : new Annotations(layout, annotations.values().toArray(new Annotation[0]));
            FlowGraph flow = instructions == null ? base.content.flow
                    : new FlowGraph(layout, instructions, edges);
            Content content = new Content(nextMappings, nextAnnotations, flow);
            FlowState state = base.flowState;
            if (instructions != null) state = truncated ? FlowState.TRUNCATED : FlowState.CURRENT;
            else if (metadataChangesFlow && flow != null) state = FlowState.STALE;
            while (true) {
                View current = root.get();
                if (current.contentRevision != base.contentRevision || current.memoryRevision != base.memoryRevision)
                    return CommitResult.STALE;
                View next = new View(identity, layout, content, current.breakpoints, current.contentRevision + 1,
                        current.breakpointRevision, current.memoryRevision, state);
                if (root.compareAndSet(current, next)) return CommitResult.PUBLISHED;
            }
        }
    }

    private static boolean affectsFlow(String kind) { return kind.equals(Kinds.CODE) || kind.equals(Kinds.DATA); }

    private final class BreakpointEdit extends Edit implements BreakpointUpdate {
        final View base;
        Map<Long, Breakpoint> values;
        BreakpointEdit(View base) { this.base = base; }
        private Map<Long, Breakpoint> values() {
            if (values == null) {
                values = new TreeMap<>();
                for (Breakpoint value : base.breakpoints.values) values.put(value.id(), value);
            }
            return values;
        }
        @Override public BreakpointUpdate put(long id, int address, boolean enabled, String condition) {
            requireOpen();
            validate(address >= 0 && address < layout.size, "Address outside memory layout");
            values().put(id, new BreakpointValue(id, address, enabled, required(condition, "condition")));
            return this;
        }
        @Override public BreakpointUpdate remove(long id) {
            requireOpen();
            values().remove(id);
            return this;
        }
        @Override public CommitResult commit() {
            requireOpen();
            open = false;
            BreakpointIndex nextBreakpoints = values == null ? base.breakpoints
                    : new BreakpointIndex(layout, values.values().toArray(new Breakpoint[0]));
            while (true) {
                View current = root.get();
                if (current.breakpointRevision != base.breakpointRevision) return CommitResult.STALE;
                View next = new View(identity, layout, current.content, nextBreakpoints, current.contentRevision,
                        current.breakpointRevision + 1, current.memoryRevision, current.flowState);
                if (root.compareAndSet(current, next)) return CommitResult.PUBLISHED;
            }
        }
    }

    private static final class Content {
        final Mappings mappings;
        final Annotations annotations;
        final FlowGraph flow;
        Content(Mappings mappings, Annotations annotations, FlowGraph flow) {
            this.mappings = mappings;
            this.annotations = annotations;
            this.flow = flow;
        }
    }

    private static final class Mappings {
        final Mapping[] values;
        final RangePostings<Mapping> ranges;
        final SourceIndex sources;
        Mappings(Layout layout, Mapping[] values) {
            this.values = values;
            Range[] coverage = new Range[values.length];
            for (int i = 0; i < values.length; i++) coverage[i] = values[i].range();
            ranges = new RangePostings<>(layout, values, coverage);
            sources = new SourceIndex(values);
        }
    }

    private static final class Annotations {
        final Annotation[] values;
        final RangeIndex<Annotation> ranges;
        Annotations(Layout layout, Annotation[] values) {
            this.values = values;
            Range[] coverage = new Range[values.length];
            for (int i = 0; i < values.length; i++) coverage[i] = values[i].range();
            ranges = new RangeIndex<>(layout, values, coverage);
        }
    }

    private static final class IndexLayout implements Layout {
        final String imageId;
        final int size, banks, common;
        IndexLayout(String imageId, int size, int banks, int common) {
            this.imageId = Objects.requireNonNull(imageId, "imageId");
            if (imageId.isEmpty() || size < 1 || banks < 1 || common < 0 || common > size)
                throw new IllegalArgumentException("Invalid image identity or memory layout");
            this.size = size;
            this.banks = banks;
            this.common = common;
        }
        @Override public String imageId() { return imageId; }
        @Override public int addressSpaceSize() { return size; }
        @Override public int bankCount() { return banks; }
        @Override public int commonBoundary() { return common; }
        @Override public int physicalBank(int bank, int address) {
            if (bank < 0 || bank >= banks || address < 0 || address >= size)
                throw new IllegalArgumentException("Address/bank outside memory layout");
            return address < common ? bank : 0;
        }
        void checkAddress(int address) {
            if (address < 0 || address >= size) throw new IllegalArgumentException("Address outside memory layout");
        }
        void check(Range range) {
            Objects.requireNonNull(range, "range");
            physicalBank(range.bank, range.start);
            if (range.end > size) throw new IllegalArgumentException("Range outside memory layout");
        }
        Range canonical(Range range) {
            check(range);
            int physical = physicalBank(range.bank, range.start);
            return physical == range.bank ? range : new Range(physical, range.start, range.end);
        }
    }

    private static final class Key implements Comparable<Key> {
        final long owner, entry;
        Key(long owner, long entry) { this.owner = owner; this.entry = entry; }
        @Override public int compareTo(Key other) {
            int comparison = Long.compare(owner, other.owner);
            return comparison == 0 ? Long.compare(entry, other.entry) : comparison;
        }
    }
    private static final class MappingValue implements Mapping {
        final long owner, entry, memory;
        final Range range;
        final SourceSpan source;
        MappingValue(long owner, long entry, Range range, SourceSpan source, long memory) {
            this.owner = owner; this.entry = entry; this.range = range; this.source = source; this.memory = memory;
        }
        @Override public long ownerId() { return owner; }
        @Override public long entryId() { return entry; }
        @Override public Range range() { return range; }
        @Override public SourceSpan source() { return source; }
        @Override public long memoryRevision() { return memory; }
    }
    private static final class AnnotationValue implements Annotation {
        final long owner, entry;
        final Range range;
        final String kind, text;
        AnnotationValue(long owner, long entry, Range range, String kind, String text) {
            this.owner = owner; this.entry = entry; this.range = range; this.kind = kind; this.text = text;
        }
        @Override public long ownerId() { return owner; }
        @Override public long entryId() { return entry; }
        @Override public Range range() { return range; }
        @Override public String kind() { return kind; }
        @Override public String text() { return text; }
    }
    private static final class BreakpointValue implements Breakpoint {
        final long id;
        final int address;
        final boolean enabled;
        final String condition;
        BreakpointValue(long id, int address, boolean enabled, String condition) {
            this.id = id; this.address = address; this.enabled = enabled; this.condition = condition;
        }
        @Override public long id() { return id; }
        @Override public int address() { return address; }
        @Override public boolean enabled() { return enabled; }
        @Override public String condition() { return condition; }
    }
    private static final class InstructionValue implements Instruction {
        final Range cells;
        final InstructionKind kind;
        final boolean unresolved;
        InstructionValue(Range cells, InstructionKind kind, boolean unresolved) {
            this.cells = cells; this.kind = kind; this.unresolved = unresolved;
        }
        @Override public Range cells() { return cells; }
        @Override public InstructionKind kind() { return kind; }
        @Override public boolean unresolvedSuccessors() { return unresolved; }
    }
}
