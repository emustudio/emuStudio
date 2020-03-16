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

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;

public abstract class DataValueNode {
    protected int line;
    protected int column;

    public DataValueNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    /// compile time ///
    public abstract int getSize();

    public abstract int pass2(Namespace env, int addr_start) throws Exception;

    public abstract void pass4(IntelHEX hex) throws Exception;

    // encode string to hex codes
    protected String getEncString(String literal) {
        byte[] byts = literal.getBytes();
        StringBuilder enc = new StringBuilder();

        for (byte byt : byts) {
            enc.append(ExprNode.getEncValue(byt, true));
        }
        return enc.toString();
    }
}
