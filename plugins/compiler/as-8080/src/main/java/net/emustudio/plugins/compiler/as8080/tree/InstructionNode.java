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
import net.emustudio.plugins.compiler.as8080.exceptions.NeedMorePassException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.CodePseudoNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.PseudoBlock;

import java.util.List;

public class InstructionNode {
    protected LabelNode label;
    final CodePseudoNode codePseudo;
    private int current_address; // its computed in pass2

    public InstructionNode(LabelNode label, CodePseudoNode codePseudo) {
        this.label = label;
        this.codePseudo = codePseudo;
    }

    public InstructionNode(CodePseudoNode codePseudo) {
        this.label = null;
        this.codePseudo = codePseudo;
    }

    public int getSize() {
        if (codePseudo != null) {
            return codePseudo.getSize();
        }
        return 0;
    }

    void pass1(List<String> inclfiles, Namespace parentEnv) throws Exception {
        ((IncludePseudoNode) codePseudo).pass1(inclfiles, parentEnv);
    }

    void pass1() throws Exception {
        if (codePseudo instanceof PseudoBlock) {
            ((PseudoBlock) codePseudo).pass1();
        }
    }

    public int pass2(Namespace prev_env, int addr_start) throws Exception {
        this.current_address = addr_start;
        if (label != null) {
            label.setAddress(addr_start);
        }
        // pass2 pre definiciu makra nemozem volat. ide totiz o samotnu expanziu
        // makra. preto pass2 mozem volat az pri samotnom volani makra (pass2 triedy
        // MacroCallPseudo)
        if (codePseudo != null) {
            if (!(codePseudo instanceof MacroPseudoNode)) {
                addr_start = codePseudo.pass2(prev_env, addr_start);
            }
        }
        return addr_start;
    }

    boolean pass3(Namespace env) throws Exception {
        try {
            if (codePseudo != null) {
                codePseudo.pass2(env, this.current_address);
            }
        } catch (NeedMorePassException e) {
            return false;
        }
        return true;
    }

    // code generation
    public void pass4(IntelHEX hex) throws Exception {
        if (codePseudo != null) {
            if (!(codePseudo instanceof MacroPseudoNode)) {
                codePseudo.pass4(hex);
            }
        }
    }

    boolean getIncludeLoops(String filename) {
        if (codePseudo != null && codePseudo instanceof IncludePseudoNode) {
            IncludePseudoNode i = (IncludePseudoNode) codePseudo;
            return i.isEqualName(filename);
        }
        return false;
    }

}
