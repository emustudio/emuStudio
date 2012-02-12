/*
 * ColumnAddress.java
 *
 *  Copyright (C) 2011 vbmacher
 *
 * KISS,YAGNI
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

import emulib.plugins.cpu.SimpleDebugColumn;

/**
 * This class represents "address" column in the debug table.
 * 
 * @author vbmacher
 */
public class ColumnAddress extends SimpleDebugColumn {

    /**
     * Creates new instance of the address column.
     */
    public ColumnAddress() {
        super("address", java.lang.String.class, false);
    }

    /**
     * Does nothing. Address cannot be changed.
     * @param location the address
     * @param value new value of the address
     */
    @Override
    public void setDebugValue(int location, Object value) {
    }

    /**
     * Gets formatted value of the address.
     * 
     * @param location The integer version of the address
     * @return formatted version of the address in hexadecimal format
     */
    @Override
    public Object getDebugValue(int location) {
        return String.format("%04Xh", location);
    }

}
