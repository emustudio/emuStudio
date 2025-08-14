/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;

class DisassemblerStub implements Disassembler {
    private final int[] nextPositions;

    DisassemblerStub(int memorySize, int... nextPositions) {
        if (memorySize < nextPositions.length) {
            throw new IllegalArgumentException("Memory size < instruction.length");
        }

        this.nextPositions = new int[memorySize];
        System.arraycopy(nextPositions, 0, this.nextPositions, 0, nextPositions.length);
    }

    void set(int address, int value) {
        nextPositions[address] = value;
    }

    @Override
    public DisassembledInstruction disassemble(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNextInstructionPosition(int position) throws IndexOutOfBoundsException {
        if (nextPositions[position] == -1) {
            throw new IndexOutOfBoundsException();
        }
        return nextPositions[position];
    }
}
