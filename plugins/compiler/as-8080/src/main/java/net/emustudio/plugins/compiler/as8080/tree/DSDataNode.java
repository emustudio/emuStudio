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

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.exceptions.AmbiguousException;
import net.emustudio.plugins.compiler.as8080.exceptions.NeedMorePassException;
import net.emustudio.plugins.compiler.as8080.exceptions.NegativeValueException;
import net.emustudio.plugins.compiler.as8080.exceptions.ValueTooBigException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.DataValueNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;

public class DSDataNode extends DataValueNode {

    private final ExprNode expression;

    public DSDataNode(ExprNode expr, int line, int column) {
        super(line, column);
        this.expression = expr;
    }

    @Override
    public int getSize() {
        return expression.getValue();
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        try {
            int val = expression.eval(env, addr_start);
            return addr_start + val;
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "DS expression");
        }
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
        StringBuilder str = new StringBuilder();

        if (expression.getValue() > 0xFF) {
            throw new ValueTooBigException(line, column, expression.getValue(), 0xFF);
        } else if (expression.getValue() < 0) {
            throw new NegativeValueException(line, column, expression.getValue());
        }

        str.append("00".repeat(Math.max(0, expression.getValue())));
        hex.add(str.toString());
    }

    @Override
    public String toString() {
        return "DSDataNode{" +
            "expression=" + expression +
            '}';
    }
}
