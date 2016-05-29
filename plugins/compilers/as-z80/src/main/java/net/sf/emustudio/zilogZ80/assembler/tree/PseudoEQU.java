/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
import net.sf.emustudio.zilogZ80.assembler.exceptions.AlreadyDefinedException;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Pseudo;

public class PseudoEQU extends Pseudo {

    private Expression expr;
    private String mnemo;

    public PseudoEQU(String id, Expression expr, int line, int column) {
        super(line, column);
        this.mnemo = id;
        this.expr = expr;
    }

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
    public void pass1() {
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        if (env.addConstant(this) == false) {
            throw new AlreadyDefinedException(line, column, "constant(" + mnemo + ")");
        }
        expr.eval(env, addr_start);
        return addr_start;
    }

    @Override
    public void generateCode(HEXFileManager hex) {
    }
}
