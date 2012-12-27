/*
 * Row.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.brainduck.brainc.tree;

import emulib.plugins.compiler.HEXFileHandler;

public class Row {

    private Statement stat;

    public Row(Statement stat) {
        this.stat = stat;
    }

    public int pass1(int addr_start) throws Exception {
        if (stat != null) {
            addr_start = stat.pass1(addr_start);
        }
        return addr_start;
    }

    public void pass2(HEXFileHandler hex) throws Exception {
        if (stat != null) {
            stat.pass2(hex);
        }
    }
}
