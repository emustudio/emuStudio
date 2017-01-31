/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.debugTable;

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.exceptions.InvalidInstructionException;

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
    public DisassembledInstruction disassemble(int i) throws InvalidInstructionException {
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
