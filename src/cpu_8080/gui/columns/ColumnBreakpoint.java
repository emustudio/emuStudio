/*
 * ColumnBreakpoint.java
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

package cpu_8080.gui.columns;

import cpu_8080.gui.Disassembler;
import emuLib8.plugins.cpu.ICPU;
import emuLib8.plugins.cpu.SimpleDebugColumn;

/**
 *
 * @author vbmacher
 */
public class ColumnBreakpoint extends SimpleDebugColumn {
    private Disassembler dis;
    private ICPU cpu;

    public ColumnBreakpoint(Disassembler disasm, ICPU cpu) {
        super("breakpoint", java.lang.Boolean.class, true);
        this.dis = disasm;
        this.cpu = cpu;
    }
    
    @Override
    public void setDebugValue(int row, Object value) {
        try {
            boolean v = Boolean.valueOf(value.toString());
            cpu.setBreakpoint(dis.rowToLocation(row), v);
        } catch(IndexOutOfBoundsException e) {
        }
    }

    @Override
    public Object getDebugValue(int row) {
        try {
            int address = dis.rowToLocation(row);
            return cpu.getBreakpoint(address);
        } catch(IndexOutOfBoundsException e) {
            return false;
        }
    }

    @Override
    public boolean isCurrent(int row) {
        return dis.isRowCurrent(row);
    }

}
