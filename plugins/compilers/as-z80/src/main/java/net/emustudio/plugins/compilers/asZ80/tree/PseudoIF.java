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
import net.emustudio.plugins.compilers.asZ80.exceptions.AmbiguousException;
import net.emustudio.plugins.compilers.asZ80.exceptions.NeedMorePassException;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Expression;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Pseudo;

public class PseudoIF extends Pseudo {

    private Expression expr;
    private Program subprogram;
    private boolean condTrue; // => for generateCode; if this is true,
    // then generate code, otherwise not.

    public PseudoIF(Expression expr, Program stat, int line, int column) {
        super(line, column);
        this.expr = expr;
        this.subprogram = stat;
        this.condTrue = false;
    }

    @Override
    public int getSize() {
        if (condTrue) {
            return subprogram.getSize();
        } else {
            return 0;
        }
    }

    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        subprogram.pass1(env);
        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return subprogram.pass2(env, addr_start);
            } else {
                return addr_start;
            }
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "IF expression");
        }
    }

    @Override
    public void generateCode(IntelHEX hex) throws Exception {
        if (condTrue) {
            subprogram.pass4(hex);
        }
    }
}
