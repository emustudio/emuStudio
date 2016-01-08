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
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.exceptions.NeedMorePassException;
import net.sf.emustudio.intel8080.assembler.treeAbstract.CodePseudoNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.PseudoBlock;

import java.util.List;

public class InstructionNode {
    protected LabelNode label;
    protected CodePseudoNode codePseudo;
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
        if (codePseudo !=null) {
            return codePseudo.getSize();
        }
        return 0;
    }

    public void pass1(List<String> inclfiles, CompileEnv parentEnv) throws Exception {
        ((IncludePseudoNode)codePseudo).pass1(inclfiles, parentEnv);        
    }

    public void pass1() throws Exception {
        if (codePseudo instanceof PseudoBlock) {
            ((PseudoBlock)codePseudo).pass1();
        }
    }
    
    public int pass2(CompileEnv prev_env, int addr_start) throws Exception {
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
    
    public boolean pass3(CompileEnv env) throws Exception {
        try {
            if (codePseudo != null) {
                codePseudo.pass2(env,this.current_address);
            }
        } catch (NeedMorePassException e) {
            return false;
        }
        return true;
    }
    
    // code generation
    public void pass4(HEXFileManager hex) throws Exception {
        if (codePseudo != null) { 
            if (!(codePseudo instanceof MacroPseudoNode)) {
                codePseudo.pass4(hex);
            }
        }
    }

    public boolean getIncludeLoops(String filename) {
        if (codePseudo != null && codePseudo instanceof IncludePseudoNode) {
            IncludePseudoNode i = (IncludePseudoNode)codePseudo;
            if (i.isEqualName(filename)) {
                return true;
            }
        }
        return false;
    }

}
