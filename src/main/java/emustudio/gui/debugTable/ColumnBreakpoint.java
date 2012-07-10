/*
 * ColumnBreakpoint.java
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

import emulib.plugins.cpu.ICPU;
import emulib.plugins.cpu.SimpleDebugColumn;

/**
 * This class represents "breakpoint" column in the debug table.
 *
 * @author vbmacher
 */
public class ColumnBreakpoint extends SimpleDebugColumn {
    private ICPU cpu;

    /**
     * Creates new instance of the address column.
     *
     * @param cpu CPU plug-in
     */
    public ColumnBreakpoint(ICPU cpu) {
        super("bp", java.lang.Boolean.class, true);
        this.cpu = cpu;
    }
    
    /**
     * Set/unset a breakpoint on specified location.
     * 
     * @param location the address/location where the breakpoint should be set/unset
     * @param value the value of the breakpoint (Boolean instance)
     */
    @Override
    public void setDebugValue(int location, Object value) {
        try {
            boolean v = Boolean.valueOf(value.toString());
            cpu.setBreakpoint(location, v);
        } catch(IndexOutOfBoundsException e) {
        }
    }

    /**
     * Detemine if breakpoint on specified locaion is set.
     * 
     * @param location the address/location in memory
     * @return Boolean instance set to true if a breakpoint is set,
     * false otherwise
     */
    @Override
    public Object getDebugValue(int location) {
        try {
            return cpu.getBreakpoint(location);
        } catch(IndexOutOfBoundsException e) {
            return false;
        }
    }

}
