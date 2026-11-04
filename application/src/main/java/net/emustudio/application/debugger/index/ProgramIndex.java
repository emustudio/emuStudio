/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import net.emustudio.emulib.runtime.annotations.AnnotationEngine;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
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
    private final AnnotationEngine annotationEngine;
    private final AtomicReference<View> root;

    private ProgramIndex(String imageId, int size, AnnotationEngine annotationEngine) {
        this.annotationEngine = Objects.requireNonNull(annotationEngine);
        layout = new IndexLayout(imageId, size);
        Content empty = new Content(null);
        root = new AtomicReference<>(new View(identity, layout, empty, new BreakpointIndex(layout, new Breakpoint[0]),
                0, 0, 0, FlowState.NOT_BUILT));
    }

    /** Creates an empty index, copying and validating its immutable layout. */
    public static ProgramIndex create(String imageId, int addressSpaceSize) {
        return new ProgramIndex(imageId, addressSpaceSize, new AnnotationEngine());
    }

    public static ProgramIndex create(String imageId, int addressSpaceSize, AnnotationEngine annotationEngine) {
        return new ProgramIndex(imageId, addressSpaceSize, annotationEngine);
    }

    public AnnotationEngine annotationEngine() { return annotationEngine; }

    /** Invalidates graph results after live annotation changes. */
    public void annotationsChanged() {
        while (true) {
            View old = root.get();
            View next = new View(identity, layout, old.content, old.breakpoints, old.contentRevision + 1,
                    old.breakpointRevision, old.memoryRevision,
                    old.flowState == FlowState.NOT_BUILT ? FlowState.NOT_BUILT : FlowState.STALE);
            if (root.compareAndSet(old, next)) return;
        }
    }

    /** Returns the existing coherent immutable root, without copying. */
    public Snapshot snapshot() { return root.get(); }
    public ContentUpdate updateContent(Snapshot base) { return new ContentEdit(own(base)); }
    public BreakpointUpdate updateBreakpoints(Snapshot base) { return new BreakpointEdit(own(base)); }

    /**
     * Replays a logical edit against fresh roots until it publishes. The edit returns false to
     * abandon without committing. Unbounded: callers must not race a continuous writer.
     */
    public boolean editContent(BiPredicate<Snapshot, ContentUpdate> edit) {
        while (true) {
            Snapshot base = snapshot();
            try (ContentUpdate update = updateContent(base)) {
                if (!edit.test(base, update)) return false;
                if (update.commit() == CommitResult.PUBLISHED) return true;
            }
        }
    }

    /** Breakpoint counterpart of {@link #editContent}. */
    public void editBreakpoints(BiPredicate<Snapshot, BreakpointUpdate> edit) {
        while (true) {
            Snapshot base = snapshot();
            try (BreakpointUpdate update = updateBreakpoints(base)) {
                if (!edit.test(base, update) || update.commit() == CommitResult.PUBLISHED) return;
            }
        }
    }

    /**
     * Advances memory revision and makes current graph queries
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
        public static final String CODE = "code";
        public static final String DATA = "data";
        private Kinds() { }
    }

    public interface Layout {
        String imageId();
        int addressSpaceSize();
    }

    public interface Snapshot {
        Layout layout();
        long contentRevision();
        long memoryRevision();
        /** Same content and memory revisions; meaningful only for snapshots of one index. */
        default boolean sameRevisions(Snapshot other) {
            return contentRevision() == other.contentRevision() && memoryRevision() == other.memoryRevision();
        }
        /** Enabled execution rules at the CPU-visible address. */
        boolean hasExecutionBreakpoint(int address);
        void forEachExecutionBreakpoint(int address, Consumer<? super Breakpoint> visitor);
        FlowState flowState();
        /** Exact instruction starts only; unavailable for NOT_BUILT/STALE. */
        Instruction instructionAt(int address);
        void forEachSuccessor(int address, EdgeVisitor visitor);
        /** Address order; unavailable for NOT_BUILT/STALE. */
        void forEachInstruction(Consumer<? super Instruction> visitor);
    }

    /**
     * Thread-confined lazy copy-on-write edit of the control-flow graph.
     * Commit consumes the edit; a validation failure also consumes it. Close is
     * idempotent and publishes nothing. Failed edits cannot publish earlier staged entries.
     */
    public interface ContentUpdate extends AutoCloseable {
        ContentUpdate replaceFlow(boolean truncated);
        ContentUpdate putInstruction(Range cells, InstructionKind kind);
        /** Known undecoded destinations are permitted. Unknown indirect targets are never edges. */
        ContentUpdate putEdge(int fromAddress, int toAddress, EdgeKind kind);
        /**
         * Compares content AND memory revisions, preserving latest independent breakpoint state.
         * replaceFlow supplies resulting flow.
         * Invalid edits throw IllegalArgumentException and publish nothing.
         */
        CommitResult commit();
        @Override void close();
    }

    public interface BreakpointUpdate extends AutoCloseable {
        /** CPU-visible address only. */
        BreakpointUpdate put(long id, int address, boolean enabled);
        BreakpointUpdate remove(long id);
        /** Compares only breakpoint revision, preserving latest content/memory state. */
        CommitResult commit();
        @Override void close();
    }

    public interface Breakpoint {
        long id();
        int address();
        boolean enabled();
    }
    public interface Instruction {
        Range cells();
        InstructionKind kind();
    }
    @FunctionalInterface
    public interface EdgeVisitor {
        void accept(int fromAddress, int toAddress, EdgeKind kind);
    }

    /** Nonempty half-open range of CPU-visible cells. Layout validates its upper bound. */
    public static final class Range {
        public final int start, end;
        public Range(int start, int end) {
            if (start < 0 || end <= start)
                throw new IllegalArgumentException("Invalid range");
            this.start = start;
            this.end = end;
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Range)) return false;
            Range r = (Range) other;
            return start == r.start && end == r.end;
        }
        @Override public int hashCode() { return 31 * start + end; }
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
        @Override public long memoryRevision() { return memoryRevision; }
        @Override public FlowState flowState() { return flowState; }

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
        @Override public Instruction instructionAt(int address) {
            layout.checkAddress(address);
            return flowAvailable() ? content.flow.instructionAt(address) : null;
        }
        @Override public void forEachSuccessor(int address, EdgeVisitor visitor) {
            layout.checkAddress(address);
            Objects.requireNonNull(visitor, "visitor");
            if (flowAvailable()) content.flow.successors(address, visitor);
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
            try { layout.check(range); return range; }
            catch (RuntimeException failure) { open = false; throw failure; }
        }
        int address(IndexLayout layout, int address) {
            try { layout.checkAddress(address); return address; }
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
        Map<Integer, Instruction> instructions;
        Set<FlowGraph.Edge> edges;
        boolean truncated;

        ContentEdit(View base) { this.base = base; }
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
        @Override public ContentUpdate putInstruction(Range cells, InstructionKind kind) {
            requireFlow();
            Range checked = range(layout, cells);
            required(kind, "kind");
            instructions.put(checked.start, new InstructionValue(checked, kind));
            return this;
        }
        @Override public ContentUpdate putEdge(int fromAddress, int toAddress, EdgeKind kind) {
            requireFlow();
            int from = address(layout, fromAddress);
            int to = address(layout, toAddress);
            edges.add(new FlowGraph.Edge(from, to, required(kind, "kind")));
            return this;
        }
        @Override public CommitResult commit() {
            requireOpen();
            open = false;
            FlowGraph flow = instructions == null ? base.content.flow
                    : new FlowGraph(layout, instructions, edges);
            Content content = new Content(flow);
            FlowState state = base.flowState;
            if (instructions != null) state = truncated ? FlowState.TRUNCATED : FlowState.CURRENT;
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
        @Override public BreakpointUpdate put(long id, int address, boolean enabled) {
            requireOpen();
            validate(address >= 0 && address < layout.size, "Address outside memory layout");
            values().put(id, new BreakpointValue(id, address, enabled));
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
        final FlowGraph flow;
        Content(FlowGraph flow) {
            this.flow = flow;
        }
    }

    private static final class IndexLayout implements Layout {
        final String imageId;
        final int size;
        IndexLayout(String imageId, int size) {
            this.imageId = Objects.requireNonNull(imageId, "imageId");
            if (imageId.isEmpty() || size < 1)
                throw new IllegalArgumentException("Invalid image identity or memory layout");
            this.size = size;
        }
        @Override public String imageId() { return imageId; }
        @Override public int addressSpaceSize() { return size; }
        void checkAddress(int address) {
            if (address < 0 || address >= size) throw new IllegalArgumentException("Address outside memory layout");
        }
        void check(Range range) {
            Objects.requireNonNull(range, "range");
            checkAddress(range.start);
            if (range.end > size) throw new IllegalArgumentException("Range outside memory layout");
        }
    }

    private static final class BreakpointValue implements Breakpoint {
        final long id;
        final int address;
        final boolean enabled;
        BreakpointValue(long id, int address, boolean enabled) {
            this.id = id; this.address = address; this.enabled = enabled;
        }
        @Override public long id() { return id; }
        @Override public int address() { return address; }
        @Override public boolean enabled() { return enabled; }
    }
    private static final class InstructionValue implements Instruction {
        final Range cells;
        final InstructionKind kind;
        InstructionValue(Range cells, InstructionKind kind) {
            this.cells = cells; this.kind = kind;
        }
        @Override public Range cells() { return cells; }
        @Override public InstructionKind kind() { return kind; }
    }
}
