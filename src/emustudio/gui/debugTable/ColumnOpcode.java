/*
 * ColumnOpcode.java
 *
 *  Copyright (C) 2011 vbmacher
 *
 * KISS, YAGNI
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

import emuLib8.plugins.cpu.CPUInstruction;
import emuLib8.plugins.cpu.IDisassembler;
import emuLib8.plugins.cpu.SimpleDebugColumn;

/**
 *
 * @author vbmacher
 */
public class ColumnOpcode extends SimpleDebugColumn {
    private IDisassembler dis;

    public ColumnOpcode(IDisassembler disasm) {
        super("mnemonics", java.lang.String.class, false);
        this.dis = disasm;
    }

    @Override
    public void setDebugValue(int row, Object value) {
    }

    @Override
    public Object getDebugValue(int location) {
        try {
            CPUInstruction instr = dis.disassemble(location);
            return instr.getOpCode();
        } catch(IndexOutOfBoundsException e) {
            return "";
        }
    }

}
