/*
 * OpcodeColumn.java
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2011-2012, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.gui.debugTable;

import emulib.plugins.cpu.AbstractDebugColumn;
import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.cpu.InvalidInstructionException;

/**
 * This class represents "opcode" column in the debug table.
 *
 * The opcode means operating code - the formatted binary representation of the instruction.
 *
 */
public class OpcodeColumn extends AbstractDebugColumn {
    private final Disassembler disassembler;

    /**
     * Creates an instance of the column.
     *
     * @param disassembler Dissassembler object
     */
    public OpcodeColumn(Disassembler disassembler) {
        super("opcode", java.lang.String.class, false);
        this.disassembler = disassembler;
    }

    /**
     * Does nothing, user cannot change the opcode.
     *
     * @param location
     * @param value
     */
    @Override
    public void setDebugValue(int location, Object value) {
    }

    /**
     * Get formatted opcode of an instruction on the specified location.
     *
     * @param location address/location in memory
     * @return a String value - formatted representation of an instruction opcode
     */
    @Override
    public Object getDebugValue(int location) {
        try {
            DisassembledInstruction instr = disassembler.disassemble(location);
            return instr.getOpCode();
        } catch (InvalidInstructionException | IndexOutOfBoundsException | NullPointerException e) {
            return "";
        }
    }

}
