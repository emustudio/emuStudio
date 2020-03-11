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
import net.emustudio.plugins.compilers.asZ80.exceptions.NeedMorePassException;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.Statement;
import net.emustudio.plugins.compilers.asZ80.Namespace;

import java.util.List;

public class Row {

    protected Label label;
    protected Statement statement;
    private int current_address; // its computed in pass2

    public Row(Label label, Statement statement) {
        this.label = label;
        this.statement = statement;
    }

    public Row(Statement statement) {
        this.label = null;
        this.statement = statement;
    }

    public boolean getIncludeLoops(String filename) {
        if (statement == null) {
            return false;
        }
        if (statement instanceof PseudoINCLUDE) {
            PseudoINCLUDE i = (PseudoINCLUDE) statement;
            return i.isEqualName(filename);
        }
        return false;
    }

    public int getSize() {
        if (statement != null) {
            return statement.getSize();
        } else {
            return 0;
        }
    }

    // do pass1 for all elements
    public void pass1()
        throws Exception {
        if (statement != null) {
            statement.pass1();
        }
    }

    public void pass1(List<String> inclfiles, Namespace parent) throws Exception {
        ((PseudoINCLUDE) statement).pass1(inclfiles, parent);
    }

    public int pass2(Namespace prev_env, int addr_start) throws Exception {
        this.current_address = addr_start;
        if (label != null) {
            label.setAddress(addr_start);
        }
        // pass2 pre definiciu makra nemozem volat. ide totiz o samotnu expanziu
        // makra. preto pass2 mozem volat az pri samotnom volani makra (pass2 triedy
        // MacroCallPseudo)
        if (statement != null) {
            if (!(statement instanceof PseudoMACRO)) {
                addr_start = statement.pass2(prev_env, addr_start);
            }
        }
        return addr_start;
    }

    public boolean pass3(Namespace env) throws Exception {
        try {
            if (statement != null) {
                statement.pass2(env, this.current_address);
            }
        } catch (NeedMorePassException e) {
            return false;
        }
        return true;
    }

    // code generation
    public void pass4(IntelHEX hex) throws Exception {
        if (statement != null) {
            if (!(statement instanceof PseudoMACRO)) {
                statement.generateCode(hex);
            }
        }
    }
}
