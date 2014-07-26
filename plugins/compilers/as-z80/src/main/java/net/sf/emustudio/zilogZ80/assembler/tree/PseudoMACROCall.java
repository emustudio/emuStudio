/*
 * PseudoMACROCall.java
 *
 * Created on Pondelok, 2007, október 8, 17:03
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.runtime.HEXFileManager;
import java.util.ArrayList;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.impl.NeedMorePassException;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Pseudo;

public class PseudoMACROCall extends Pseudo {

    private ArrayList<Expression> params; // ArrayList of expressions
    private PseudoMACRO macro; // only pointer...
    private HEXFileManager statHex; // hex file for concrete macro
    private String mnemo;

    public PseudoMACROCall(String name, ArrayList<Expression> params, int line, int column) {
        super(line, column);
        this.mnemo = name;
        if (params == null) {
            this.params = new ArrayList<Expression>();
        } else {
            this.params = params;
        }
        statHex = new HEXFileManager();
    }

    @Override
    public int getSize() {
        return macro.getStatSize();
    }

    @Override
    public void pass1() {
    }

    // this is a call for expanding a macro
    // also generate code for pass4
    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        // first find a macro
        this.macro = env.getMacro(this.mnemo);
        if (macro == null) {
            throw new Exception("[" + line + "," + column
                    + "] Error: Undefined macro: " + this.mnemo);
        }
        // do pass2 for expressions (real macro parameters)
        try {
            for (int i = 0; i < params.size(); i++) {
                params.get(i).eval(env, addr_start);
            }
            macro.setCallParams(params);
            int a = macro.pass2(env, addr_start);
            statHex.setNextAddress(addr_start);
            macro.pass4(statHex); // generate code for concrete macro
            return a;
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] Error: MACRO expression can't be ambiguous");
        }
    }

    @Override
    public void pass4(HEXFileManager hex) {
        hex.addTable(statHex.getTable());
    }
}
