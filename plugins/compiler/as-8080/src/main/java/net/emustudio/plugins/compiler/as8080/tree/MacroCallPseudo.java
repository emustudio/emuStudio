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
import net.emustudio.plugins.compiler.as8080.exceptions.UndefinedMacroException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.ExprNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.PseudoNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MacroCallPseudo extends PseudoNode {

    private List<ExprNode> params; // vector of expressions
    private MacroPseudoNode macro; // only pointer...
    private IntelHEX statHex; // hex file for concrete macro
    private String mnemo;

    public MacroCallPseudo(String name, List<ExprNode> params, int line, int column) {
        super(line, column);
        this.mnemo = name;
        this.params = Objects.requireNonNullElseGet(params, ArrayList::new);
        statHex = new IntelHEX();
    }

    @Override
    public int getSize() {
        return macro.getStatSize();
    }

    // this is a call for expanding a macro
    // also generate code for pass4
    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        // first find a macro
        this.macro = env.getMacro(this.mnemo);
        if (macro == null) {
            throw new UndefinedMacroException(line, column, this.mnemo);
        }
        // do pass2 for expressions (real macro parameters)
        try {
            for (ExprNode param : params) {
                param.eval(env, addr_start);
            }
            macro.setCallParams(params);
            int a = macro.pass2(env, addr_start);
            statHex.setNextAddress(addr_start);
            macro.pass4(statHex); // generate code for concrete macro
            return a;
        } catch (NeedMorePassException e) {
            throw new AmbiguousException(line, column, "MACRO expression");
        }
    }

    @Override
    public String getName() {
        return this.mnemo;
    }

    @Override
    public void pass4(IntelHEX hex) {
        hex.addTable(statHex.getTable());
    }
}
