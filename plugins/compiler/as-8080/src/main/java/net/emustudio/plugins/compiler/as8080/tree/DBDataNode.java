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
import net.emustudio.plugins.compiler.as8080.exceptions.ValueTooBigException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.DataValueNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.OpCodeNode;

public class DBDataNode extends DataValueNode {

    private ExprNode expression = null;
    private String literalString = null;
    private OpCodeNode opcode = null;

    public DBDataNode(ExprNode expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }

    public DBDataNode(String literalString, int line, int column) {
        super(line, column);
        this.literalString = literalString;
    }

    public DBDataNode(OpCodeNode opcode, int line, int column) {
        super(line, column);
        this.opcode = opcode;
    }

    /// compile time ///
    @Override
    public int getSize() {
        if (expression != null) {
            return 1;
        } else if (literalString != null) {
            return literalString.length();
        } else if (opcode != null) {
            return opcode.getSize();
        }
        return 0;
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        if (expression != null) {
            expression.eval(env, addr_start);
            return addr_start + 1;
        } else if (literalString != null) {
            return addr_start + literalString.length();
        } else if (opcode != null) {
            return opcode.pass2(env, addr_start);
        }
        return addr_start;
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
        if (expression != null) {
            if (expression.getValue() > 0xFF) {
                throw new ValueTooBigException(line, column, expression.getValue(), 0xFF);
            }
            if (expression.getValue() < -128) {
                throw new ValueTooBigException(line, column, expression.getValue(), -128);
            }
            hex.add(expression.getEncValue(true));
        } else if (literalString != null) {
            hex.add(this.getEncString(literalString));
        } else if (opcode != null) {
            opcode.pass4(hex);
        }
    }

    @Override
    public String toString() {
        return "DBDataNode{" +
            "expression=" + expression +
            ", literalString='" + literalString + '\'' +
            ", opcode=" + opcode +
            '}';
    }
}
