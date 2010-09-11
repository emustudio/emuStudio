/*
 * InstructionNode.java
 *
 * Created on Piatok, 2007, september 21, 8:12
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package as_8080.tree8080;

import as_8080.impl.NeedMorePassException;
import as_8080.impl.CompileEnv;
import as_8080.tree8080Abstract.PseudoBlock;
import as_8080.tree8080Abstract.CodePseudoNode;

import java.util.Vector;
import emuLib8.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
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
    

    /// compile time ///
    public int getSize() { 
        if (codePseudo !=null) return codePseudo.getSize();
        else return 0;
    }

    public void pass1(Vector<String> inclfiles, 
            CompileEnv parentEnv) throws Exception {
        ((IncludePseudoNode)codePseudo).pass1(inclfiles, parentEnv);        
    }

    public void pass1() throws Exception {
        if (codePseudo instanceof PseudoBlock)
            ((PseudoBlock)codePseudo).pass1();
    }
    
    public int pass2(CompileEnv prev_env, int addr_start) throws Exception {
        this.current_address = addr_start;
        if (label != null) label.setAddress(new Integer(addr_start));
        // pass2 pre definiciu makra nemozem volat. ide totiz o samotnu expanziu
        // makra. preto pass2 mozem volat az pri samotnom volani makra (pass2 triedy
        // MacroCallPseudo)
        if (codePseudo != null) 
            if ((codePseudo instanceof MacroPseudoNode) == false)
                addr_start = codePseudo.pass2(prev_env, addr_start);
        return addr_start;
    }
    
    public int getCurrentAddress() { return this.current_address; }
    
    public boolean pass3(CompileEnv env) throws Exception {
        try {
            if (codePseudo != null)
                codePseudo.pass2(env,this.current_address);
        } catch (NeedMorePassException e) {
            return false;
        }
        return true;
    }
    
    // code generation
    public void pass4(HEXFileHandler hex) throws Exception {
        if (codePseudo != null) 
            if ((codePseudo instanceof MacroPseudoNode) == false)
                codePseudo.pass4(hex);
    }

    public boolean getIncludeLoops(String filename) {
        if (codePseudo == null) return false;
        if (codePseudo instanceof IncludePseudoNode) {
            IncludePseudoNode i = (IncludePseudoNode)codePseudo;
            if (i.isEqualName(filename)) 
                return true;
        }
        return false;
    }

}
