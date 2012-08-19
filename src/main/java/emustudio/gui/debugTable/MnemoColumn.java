/*
 * MnemoColumn.java
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
 * This class represents "mnemo" column in the debug table.
 * 
 * The column displays mnemonic (textual) representations of the instruction.
 * 
 */
public class MnemoColumn extends AbstractDebugColumn {
    private Disassembler disassembler;

    /**
     * Creates an instance of the column.
     * 
     * @param disassembler Dissassembler object
     */
    public MnemoColumn(Disassembler disassembler) {
        super("mnemonics", java.lang.String.class, false);
        this.disassembler = disassembler;
    }

    /**
     * Does nothing, user cannot change the mnemonic represetnation.
     * 
     * @param location
     * @param value 
     */
    @Override
    public void setDebugValue(int location, Object value) {
    }

    /**
     * Get mnemonic representation of the instruction.
     * 
     * @param location position in the memory
     * @return a String value representation of an instruction
     */
    @Override
    public Object getDebugValue(int location) {
        try {
            DisassembledInstruction instr = disassembler.disassemble(location);
            return instr.getMnemo();
        } catch (InvalidInstructionException e) {
            return "[invalid]";
        } catch(IndexOutOfBoundsException e) {
            return "[incomplete]";
        } catch (NullPointerException x) {
            return "[null]";
        }
    }

}
