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

import net.sf.emustudio.zilogZ80.assembler.exceptions.NeedMorePassException;
import net.sf.emustudio.zilogZ80.assembler.exceptions.UnknownIdentifierException;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Expression;

public class Identifier extends Expression {
    private String name;
    private int line;
    private int col;

    public Identifier(String name, int line, int col) {
        this.name = name;
        this.line = line;
        this.col = col;
    }

    public int getSize() {
        return 0;
    }

    @Override
    public int eval(Namespace env, int curr_addr) throws Exception {
        // identifier in expression can be only label, equ, or set statement. macro NOT
        // search in env for labels
        Label lab = env.getLabel(this.name);
        if ((lab != null) && (lab.getAddress() == null)) {
            throw new NeedMorePassException(lab.getLine(), lab.getColumn());
        } else if (lab != null) {
            this.value = lab.getAddress();
            return this.value;
        }

        PseudoEQU equ = env.getConstant(this.name);
        if (equ != null) {
            this.value = equ.getValue();
            return this.value;
        }

        PseudoVAR set = env.getVariable(this.name);
        if (set != null) {
            this.value = set.getValue();
            return this.value;
        } else {
            throw new UnknownIdentifierException(line, col, this.name);
        }
    }
}
