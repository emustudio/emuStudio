/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.ssem.cpu;

import emulib.plugins.cpu.AbstractDebugColumn;

public class LineColumn  extends AbstractDebugColumn {
    private final static String LINE_FORMAT = "%04X";

    public LineColumn() {
        super("line", String.class, false);
    }

    @Override
    public void setDebugValue(int location, Object value) {
    }

    @Override
    public Object getDebugValue(int location) {
        return String.format(LINE_FORMAT, location / 4);
    }

    @Override
    public int getDefaultWidth() {
        return -1;
    }
}
