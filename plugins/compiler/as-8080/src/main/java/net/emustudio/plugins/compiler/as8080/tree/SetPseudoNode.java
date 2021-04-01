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
import net.emustudio.plugins.compiler.as8080.exceptions.AlreadyDefinedException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.PseudoNode;

public class SetPseudoNode extends PseudoNode {
    private final ExprNode expr;
    private final String mnemo;

    public SetPseudoNode(String id, ExprNode expr, int line, int column) {
        super(line, column);
        this.mnemo = id;
        this.expr = expr;
    }

    @Override
    public String getName() {
        return mnemo;
    }

    public int getValue() {
        return expr.getValue();
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        if (!env.setVariable(this)) {
            throw new AlreadyDefinedException(line, column, "identifier(" + mnemo + ")");
        }
        expr.eval(env, addr_start);
        return addr_start;
    }

    @Override
    public void pass4(IntelHEX hex) {
    }

    @Override
    public String toString() {
        return "SetPseudoNode{" +
            "expr=" + expr +
            ", mnemo='" + mnemo + '\'' +
            '}';
    }
}
