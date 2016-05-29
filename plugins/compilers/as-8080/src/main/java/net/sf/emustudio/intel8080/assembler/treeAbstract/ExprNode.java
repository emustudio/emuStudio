/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.intel8080.assembler.treeAbstract;

import emulib.runtime.RadixUtils;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;

public abstract class ExprNode {
    protected int value;

    public int getValue() {
        return value;
    }

    public abstract int eval(CompileEnv env, int curr_addr) throws Exception;

    public static String getEncValue(int val, boolean oneByte) {
        if (oneByte) {
            return RadixUtils.getByteHexString(val & 0xFF);
        } else {
            return String.format("%02X%02X", (val & 0xFF), ((val >> 8) & 0xFF));
        }
    }

    public String getEncValue(boolean oneByte) {
        if (oneByte) {
            return RadixUtils.getByteHexString(value & 0xFF);
        } else {
            return String.format("%02X%02X", (value & 0xFF), ((value >> 8) & 0xFF));
        }
    }
}
