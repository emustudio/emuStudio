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
import net.emustudio.plugins.compiler.as8080.treeAbstract.PseudoNode;

public class OrgPseudoNode extends PseudoNode {
    private final ExprNode expr;

    public OrgPseudoNode(ExprNode expr, int line, int column) {
        super(line, column);
        this.expr = expr;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String getName() {
        return "";
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
    public void pass4(IntelHEX hex) {
        hex.setNextAddress(expr.getValue());
    }
}
