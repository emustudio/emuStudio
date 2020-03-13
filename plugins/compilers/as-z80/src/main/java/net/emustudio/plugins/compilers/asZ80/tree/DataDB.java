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
package net.emustudio.plugins.compilers.asZ80.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compilers.asZ80.Namespace;
import net.emustudio.plugins.compilers.asZ80.exceptions.ValueTooBigException;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.DataValue;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Expression;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Instruction;

public class DataDB extends DataValue {

    private Expression expression = null;
    private String literalString = null;
    private Instruction opcode = null;

    public DataDB(Expression expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }

    public DataDB(String literalString, int line, int column) {
        super(line, column);
        this.literalString = literalString;
    }

    public DataDB(Instruction opcode, int line, int column) {
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
    public void pass1() throws Exception {
        if (opcode != null) {
            opcode.pass1();
        }
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        int next;
        if (expression != null) {
            expression.eval(env, addr_start);
            next = addr_start + 1;
        } else if (literalString != null) {
            next = addr_start + literalString.length();
        } else if (opcode != null) {
            next = opcode.pass2(env, addr_start);
        } else {
            next = addr_start;
        }
        return next;
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
            hex.putCode(expression.encodeValue(1));
        } else if (literalString != null) {
            hex.putCode(this.encodeValue(literalString));
        } else if (opcode != null) {
            opcode.generateCode(hex);
        }
    }
}
