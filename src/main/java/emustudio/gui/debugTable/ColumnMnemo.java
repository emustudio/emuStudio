/**
 * ColumnMnemo.java
 * 
 * KISS, YAGNI, DRY
 * 
 * Copyright (C) 2011-2012 Peter Jakubčo
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

import emulib.plugins.cpu.CPUInstruction;
import emulib.plugins.cpu.IDisassembler;
import emulib.plugins.cpu.SimpleDebugColumn;

/**
 * This class represents "mnemo" column in the debug table. This column displays
 * mnemonic representations of instructions.
 * 
 * @author vbmacher
 */
public class ColumnMnemo extends SimpleDebugColumn {
    private IDisassembler dis;

    /**
     * Creates an instance of the column.
     * 
     * @param disasm Dissassembler instance
     */
    public ColumnMnemo(IDisassembler disasm) {
        super("mnemonics", java.lang.String.class, false);
        this.dis = disasm;
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
     * Get mnemonic representation of an instruction on the specified location.
     * 
     * @param location address/location in memory
     * @return a String value representation of an instruction
     */
    @Override
    public Object getDebugValue(int location) {
        try {
            CPUInstruction instr = dis.disassemble(location);
            return instr.getMnemo();
        } catch(IndexOutOfBoundsException e) {
            return "incomplete instruction";
        } catch (NullPointerException x) {
            return "";
        }
    }

}
