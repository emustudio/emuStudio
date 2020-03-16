/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.as8080.treeAbstract;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.plugins.compiler.as8080.Namespace;

public abstract class ExprNode {
    protected int value;

    public int getValue() {
        return value;
    }

    public abstract int eval(Namespace env, int curr_addr) throws Exception;

    public static String getEncValue(int val, boolean oneByte) {
        if (oneByte) {
            return RadixUtils.formatByteHexString(val & 0xFF);
        } else {
            return String.format("%02X%02X", (val & 0xFF), ((val >> 8) & 0xFF));
        }
    }

    public String getEncValue(boolean oneByte) {
        if (oneByte) {
            return RadixUtils.formatByteHexString(value & 0xFF);
        } else {
            return String.format("%02X%02X", (value & 0xFF), ((value >> 8) & 0xFF));
        }
    }
}
