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
package net.emustudio.plugins.compiler.as8080.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.exceptions.ValueTooBigException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.DataValueNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;

public class DWDataNode extends DataValueNode {
    private final ExprNode expression;

    public DWDataNode(ExprNode expr, int line, int column) {
        super(line, column);
        this.expression = expr;
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        expression.eval(env, addr_start);
        return addr_start + 2;
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
        if (expression.getValue() > 0xFFFF) {
            throw new ValueTooBigException(line, column, expression.getValue(), 0xFFFF);
        }
        if (expression.getValue() < -32768) {
            throw new ValueTooBigException(line, column, expression.getValue(), -32768);
        }

        hex.putCode(expression.getEncValue(false));
    }

    @Override
    public String toString() {
        return "DWDataNode{" +
            "expression=" + expression +
            '}';
    }
}
