/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.cpu.DecodedInstruction;
import net.emustudio.emulib.plugin.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugin.debugger.ExecutedBlock;

import java.util.ArrayList;
import java.util.List;

import static net.emustudio.plugins.cpu.brainduck.EmulatorEngine.*;

/** Original instruction facts and paths for one cached operation without device I/O. */
final class OptimizedBlock {
    final Profiler.CachedOperation operation;
    final int start, end, minPointer, maxPointer;
    final ExecutedBlock once, repeated, skipped, continuing;
    private final byte[] encoding;

    private OptimizedBlock(int start, Profiler.CachedOperation operation, Byte[] memory, Decoder decoder)
            throws InvalidInstructionException {
        this.operation = operation;
        this.start = start;
        end = operation.nextIP;
        encoding = new byte[end - start];
        List<DecodedInstruction> instructions = new ArrayList<>();
        int pointer = 0, min = 0, max = 0;
        for (int at = start; at < end; at++) {
            byte opcode = memory[at];
            // Device I/O and stop/failure boundaries require individual instruction execution.
            if (opcode < I_INC || opcode > I_LOOP_END || opcode == I_READ || opcode == I_PRINT)
                throw new IllegalArgumentException("Operation requires exact boundaries");
            if (opcode == I_INC) pointer++;
            if (opcode == I_DEC) pointer--;
            min = Math.min(min, pointer);
            max = Math.max(max, pointer);
            encoding[at - start] = opcode;
            instructions.add(decoder.decode(at));
        }
        minPointer = min;
        maxPointer = max;
        List<ExecutedBlock.Transition> edges = new ArrayList<>();
        for (int at = start; at < end - 1; at++) edges.add(new ExecutedBlock.Transition(at, at + 1));
        once = path(instructions, edges, end);
        if (operation.type == Profiler.CachedOperation.TYPE.REPEAT) {
            repeated = skipped = continuing = null;
        } else {
            skipped = new ExecutedBlock(List.of(instructions.get(0)),
                    List.of(new ExecutedBlock.Transition(start, end)), start, end);
            List<ExecutedBlock.Transition> loopEdges = new ArrayList<>(edges);
            loopEdges.add(new ExecutedBlock.Transition(end - 1, start));
            continuing = new ExecutedBlock(instructions, loopEdges, end - 1, start);
            repeated = path(instructions, loopEdges, end);
        }
    }

    static OptimizedBlock capture(int start, Profiler.CachedOperation operation, Byte[] memory, Decoder decoder) {
        try {
            if (operation.nextIP <= start || operation.nextIP > memory.length) return null;
            if (operation.type == Profiler.CachedOperation.TYPE.SCANLOOP && operation.argument == 0) return null;
            return new OptimizedBlock(start, operation, memory, decoder);
        } catch (InvalidInstructionException | IllegalArgumentException failure) {
            return null;
        }
    }

    boolean isCurrent(Byte[] memory) {
        for (int offset = 0; offset < encoding.length; offset++)
            if (memory[start + offset] != encoding[offset]) return false;
        return true;
    }

    boolean pointerInBounds(int pointer, int size) {
        return (long) pointer + minPointer >= 0 && (long) pointer + maxPointer < size;
    }

    private ExecutedBlock path(List<DecodedInstruction> instructions, List<ExecutedBlock.Transition> prefix, int next) {
        List<ExecutedBlock.Transition> edges = new ArrayList<>(prefix);
        edges.add(new ExecutedBlock.Transition(end - 1, next));
        return new ExecutedBlock(instructions, edges, end - 1, next);
    }
}
