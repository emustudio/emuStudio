/*
 * Copyright (C) 2007-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.assembler.tree;

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.intel8080.assembler.exceptions.AmbiguousException;
import net.sf.emustudio.intel8080.assembler.exceptions.UndefinedMacroException;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.exceptions.NeedMorePassException;
import net.sf.emustudio.intel8080.assembler.treeAbstract.ExprNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.PseudoNode;

import java.util.ArrayList;
import java.util.List;

public class MacroCallPseudo extends PseudoNode {

    private List<ExprNode> params; // vector of expressions
    private MacroPseudoNode macro; // only pointer...
    private HEXFileManager statHex; // hex file for concrete macro
    private String mnemo;

    public MacroCallPseudo(String name, List<ExprNode> params, int line, int column) {
        super(line, column);
        this.mnemo = name;
        if (params == null) {
            this.params = new ArrayList<>();
        } else {
            this.params = params;
        }
        statHex = new HEXFileManager();
    }

    @Override
    public int getSize() {
        return macro.getStatSize();
    }

    // this is a call for expanding a macro
    // also generate code for pass4
    @Override
    public int pass2(CompileEnv env, int addr_start) throws Exception {
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
    public void pass4(HEXFileManager hex) {
        hex.addTable(statHex.getTable());
    }
}
