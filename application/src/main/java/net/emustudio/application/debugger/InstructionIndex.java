/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;

import java.util.Objects;
import java.util.function.Supplier;

/** Adapts indexed instruction coverage to the CPU-visible address space. */
public final class InstructionIndex {
    private final Supplier<ProgramIndex.Snapshot> snapshots;
    private volatile View current;

    public InstructionIndex(Supplier<ProgramIndex.Snapshot> snapshots) {
        this.snapshots = Objects.requireNonNull(snapshots);
    }

    /**
     * Capture one metadata root for pagination. An unchanged view is returned by identity;
     * image, content or memory changes produce a new view and invalidate cached boundaries.
     * Breakpoint edits do not affect instruction coverage. This does not freeze live decoder memory.
     */
    public View snapshot() {
        ProgramIndex.Snapshot snapshot = Objects.requireNonNull(snapshots.get());
        View previous = current;
        if (previous != null && previous.matches(snapshot)) return previous;
        View next = new View(snapshot);
        current = next;
        return next;
    }

    /** Immutable instruction boundaries addressed only by CPU-visible logical locations. */
    public static final class View {
        private final ProgramIndex.Snapshot snapshot;

        private View(ProgramIndex.Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        private boolean matches(ProgramIndex.Snapshot other) {
            return snapshot.layout() == other.layout() && snapshot.sameRevisions(other);
        }

        /**
         * Return the exclusive end of a known instruction starting here, or -1 when unavailable.
         * Addresses outside the indexed memory are unknown, allowing a larger CPU space to use its decoder.
         */
        public int nextAddress(int logicalAddress) {
            if (logicalAddress < 0 || logicalAddress >= snapshot.layout().addressSpaceSize()) return -1;
            ProgramIndex.Instruction instruction = snapshot.instructionAt(logicalAddress);
            return instruction == null ? -1 : instruction.cells().end;
        }
    }
}
