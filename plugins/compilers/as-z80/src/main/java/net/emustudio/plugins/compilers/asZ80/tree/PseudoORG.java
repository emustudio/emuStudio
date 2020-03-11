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
import net.emustudio.plugins.compilers.asZ80.exceptions.AmbiguousException;
import net.emustudio.plugins.compilers.asZ80.exceptions.NeedMorePassException;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Expression;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Pseudo;
import net.emustudio.plugins.compilers.asZ80.Namespace;

import java.util.Objects;

public class PseudoORG extends Pseudo {
    private Expression expr;

    public PseudoORG(Expression expr, int line, int column) {
        super(line, column);
        this.expr = Objects.requireNonNull(expr);
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public void pass1() {
    }

    // org only changes current address
    // if expr isnt valuable, then error exception is thrown
    // it cant help even more passes, because its recursive:
    // org label
    // mvi a,50
    // label: hlt
    // label address cant be evaluated
    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        try {
            return expr.eval(parentEnv, addr_start);
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "ORG expression");
        }
    }

    @Override
    public void generateCode(IntelHEX hex) {
        hex.setNextAddress(expr.getValue());
    }
}
