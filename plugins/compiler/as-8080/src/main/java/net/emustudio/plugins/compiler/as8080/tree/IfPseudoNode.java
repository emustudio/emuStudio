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
import net.emustudio.plugins.compiler.as8080.exceptions.AmbiguousException;
import net.emustudio.plugins.compiler.as8080.exceptions.NeedMorePassException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.PseudoBlock;

public class IfPseudoNode extends PseudoBlock {
    private ExprNode expr;
    private Statement stat;
    private boolean condTrue; // for pass4

    public IfPseudoNode(ExprNode expr, Statement stat, int line, int column) {
        super(line, column);
        this.expr = expr;
        this.stat = stat;
        this.condTrue = false;
    }

    // if doesnt have and id
    @Override
    public String getName() {
        return "";
    }

    @Override
    public int getSize() {
        if (expr.getValue() != 0) {
            return stat.getSize();
        } else {
            return 0;
        }
    }

    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        stat.pass1(env);

        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return stat.pass2(env, addr_start);
            } else {
                return addr_start;
            }
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "IF expression");
        }
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
        if (condTrue) {
            stat.pass4(hex);
        }
    }

    @Override
    public String toString() {
        return "IfPseudoNode{" +
            "expr=" + expr +
            ", stat=" + stat +
            ", condTrue=" + condTrue +
            '}';
    }
}
