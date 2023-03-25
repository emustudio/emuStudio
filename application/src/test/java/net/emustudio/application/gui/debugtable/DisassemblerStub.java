/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
