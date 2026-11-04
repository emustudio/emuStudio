/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.debugger.index.ProgramIndex;

import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Adapts physical instruction coverage to the CPU's currently mapped logical address space. */
public final class InstructionIndex {
    private final Supplier<ProgramIndex.Snapshot> snapshots;
    private final IntSupplier selectedBank;
    private volatile View current;

    public InstructionIndex(Supplier<ProgramIndex.Snapshot> snapshots, IntSupplier selectedBank) {
        this.snapshots = Objects.requireNonNull(snapshots);
        this.selectedBank = Objects.requireNonNull(selectedBank);
    }

    /**
     * Capture one mapping for a pagination operation. An unchanged view is returned by identity;
     * image, content, memory or mapping changes produce a new view and invalidate cached boundaries.
     * Breakpoint edits do not affect instruction coverage. This does not freeze live decoder memory.
     */
    public View snapshot() {
        ProgramIndex.Snapshot snapshot = Objects.requireNonNull(snapshots.get());
        int bank = snapshot.layout().physicalBank(selectedBank.getAsInt(), 0);
        View previous = current;
        if (previous != null && previous.matches(snapshot, bank)) return previous;
        View next = new View(snapshot, bank);
        current = next;
        return next;
    }

    /** Immutable instruction boundaries addressed only by CPU-visible logical locations. */
    public static final class View {
        private final ProgramIndex.Snapshot snapshot;
        private final int bank;

        private View(ProgramIndex.Snapshot snapshot, int bank) {
            this.snapshot = snapshot;
            this.bank = bank;
        }

        private boolean matches(ProgramIndex.Snapshot other, int otherBank) {
            return snapshot.layout() == other.layout() && bank == otherBank && snapshot.sameRevisions(other);
        }

        /**
         * Return the exclusive end of a known instruction starting here, or -1 when unavailable.
         * Addresses outside the indexed memory are unknown, allowing a larger CPU space to use its decoder.
         */
        public int nextAddress(int logicalAddress) {
            if (logicalAddress < 0 || logicalAddress >= snapshot.layout().addressSpaceSize()) return -1;
            ProgramIndex.Instruction instruction = snapshot.instructionAt(bank, logicalAddress);
            return instruction == null ? -1 : instruction.cells().end;
        }
    }
}
