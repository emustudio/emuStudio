/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;

import java.util.ArrayList;
import java.util.List;

/** Bounded, instruction-only graph contributions; known facts are retained until an observer/mode/reset change. */
final class ExecutionBatch {
    private final long[] instructionEpoch, edgeEpoch, pendingEpoch;
    private final int[] pendingIndex;
    private final DecodedInstruction[] facts;
    private final int[] firstNext, secondNext;
    private final List<DecodedInstruction> instructions = new ArrayList<>();
    private final List<ExecutedBlock.Transition> transitions = new ArrayList<>();
    private long epoch = 1, batch;
    private DecodedInstruction last;
    private int nextAddress;

    ExecutionBatch(int addressSpace) {
        instructionEpoch = new long[addressSpace];
        edgeEpoch = new long[addressSpace];
        pendingEpoch = new long[addressSpace];
        pendingIndex = new int[addressSpace];
        facts = new DecodedInstruction[addressSpace];
        firstNext = new int[addressSpace];
        secondNext = new int[addressSpace];
    }

    void begin() {
        batch++;
        instructions.clear();
        transitions.clear();
        last = null;
    }

    void reset() { epoch++; }

    void executed(DecodedInstruction instruction, int next) {
        instruction(instruction);
        edge(instruction.address(), next);
        last = instruction;
        nextAddress = next;
    }

    void executed(OptimizedBlock operation, ExecutedBlock path) {
        if (operation.firstObservation(path, epoch)) {
            for (DecodedInstruction instruction : path.instructions()) instruction(instruction);
            for (ExecutedBlock.Transition transition : path.transitions()) edge(transition.from(), transition.to());
        }
        last = facts[path.lastAddress()];
        nextAddress = path.nextAddress();
    }

    ExecutedBlock finish() {
        if (last == null) return null;
        include(last);
        transitions.add(new ExecutedBlock.Transition(last.address(), nextAddress));
        return new ExecutedBlock(instructions, transitions, last.address(), nextAddress);
    }

    private void instruction(DecodedInstruction instruction) {
        int at = instruction.address();
        if (instructionEpoch[at] == epoch) {
            DecodedInstruction before = facts[at];
            if (before == instruction || before.length() == instruction.length()
                    && before.mnemonic().equals(instruction.mnemonic()) && before.encoding().equals(instruction.encoding())) return;
            // Code changed within this batch. Revisit previously compressed operation paths as needed.
            epoch++;
        }
        instructionEpoch[at] = epoch;
        facts[at] = instruction;
        include(instruction);
    }

    private void include(DecodedInstruction instruction) {
        int at = instruction.address();
        if (pendingEpoch[at] == batch) instructions.set(pendingIndex[at], instruction);
        else {
            pendingEpoch[at] = batch;
            pendingIndex[at] = instructions.size();
            instructions.add(instruction);
        }
    }

    private void edge(int from, int to) {
        if (edgeEpoch[from] != epoch) {
            edgeEpoch[from] = epoch;
            firstNext[from] = to;
            secondNext[from] = -1;
        } else if (firstNext[from] == to || secondNext[from] == to) return;
        else if (secondNext[from] == -1) secondNext[from] = to;
        // Changed code can produce additional destinations; retain them even beyond the usual two.
        include(facts[from]);
        transitions.add(new ExecutedBlock.Transition(from, to));
    }
}
