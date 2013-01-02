/*
 * Statement.java
 *
 * Created on Piatok, 2007, september 21, 8:08
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
package net.sf.emustudio.intel8080.assembler.tree;

import emulib.runtime.HEXFileManager;
import java.util.ArrayList;
import java.util.List;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.impl.NeedMorePassException;
import net.sf.emustudio.intel8080.assembler.treeAbstract.PseudoBlock;

public class Statement {
    /* PASS1 = symbol table
     * 1. get all label definitions
     * 2. get all macro definitions
     */
    private CompileEnv env;
    private List<InstructionNode> list; // all instructions
    private List<String> includefiles; // list of files that
    // were checked for include-loops
    // in short: list of included files

    public Statement() {
        list = new ArrayList<InstructionNode>();
        this.env = new CompileEnv();
        includefiles = new ArrayList<String>();
    }

    public void addElement(InstructionNode node) {
        list.add(node);
    }

    public void addVector(List<InstructionNode> vec) {
        list.addAll(vec);
    }

    public int getSize() {
        InstructionNode in;
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            in = (InstructionNode) list.get(i);
            size += in.getSize();
        }
        return size;
    }

    public CompileEnv getCompileEnv() {
        return env;
    }

    public void pass1(CompileEnv env) throws Exception {
        this.env = env;
        pass1();
    }
    // creates symbol table
    // return next current address

    public void pass1() throws Exception {
        int i;
        InstructionNode in;
        // only labels and macros have right to be all added to symbol table at once
        for (i = 0; i < list.size(); i++) {
            in = (InstructionNode) list.get(i);
            if (in.label != null) {
                if (env.addLabelDef(in.label) == false) {
                    throw new Exception("Label already defined: " + in.label.getName());
                }
            }
            if ((in.codePseudo != null) && (in.codePseudo instanceof MacroPseudoNode)) {
                if (env.addMacroDef((MacroPseudoNode) in.codePseudo) == false) {
                    throw new Exception("Macro already defined: "
                            + ((MacroPseudoNode) in.codePseudo).getName());
                }
            }
            if ((in.codePseudo != null) && (in.codePseudo instanceof IncludePseudoNode)) {
                in.pass1(includefiles, env);
            } else if (in.codePseudo instanceof PseudoBlock) {
                in.pass1();
            }
        }
    }

    // pass2 tries to evaulate all expressions and compute relative addresses
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        int curr_addr;
        for (int i = 0; i < list.size(); i++) {
            InstructionNode in = (InstructionNode) list.get(i);
            try {
                curr_addr = in.pass2(parentEnv, addr_start);
                addr_start = curr_addr;
            } catch (NeedMorePassException e) {
                parentEnv.addPassNeed(in);
                addr_start += in.getSize();
            }
        }
        return addr_start;
    }

    public int pass2(int addr_start) throws Exception {
        return this.pass2(env, addr_start);
    }

    public boolean pass3(CompileEnv parentEnv) throws Exception {
        int pnCount = parentEnv.getPassNeedCount();
        for (int i = parentEnv.getPassNeedCount() - 1; i >= 0; i--) {
            if (parentEnv.getPassNeed(i).pass3(parentEnv) == true) {
                pnCount--;
                parentEnv.removePassNeed(i);
            }
        }
        if (pnCount < parentEnv.getPassNeedCount()) {
            return true;
        } else {
            return false;
        }
    }

    public void pass4(HEXFileManager hex) throws Exception {
        for (int i = 0; i < list.size(); i++) {
            InstructionNode in = (InstructionNode) list.get(i);
            in.pass4(hex);
        }
    }

    public void pass4(HEXFileManager hex, CompileEnv env) throws Exception {
        this.env = env;
        pass4(hex);
    }

    /**
     * Method check whether this "subprogram" contains include
     * pseudocode(s) and if yes, whether the statement calls for
     * filename given by parameter.
     * @param filename name of the file that "include" pseudocode should contain
     * @return true if subprogram contains "include filename" pseudocode
     */
    public boolean getIncludeLoops(String filename) {
        int i;
        for (i = 0; i < includefiles.size(); i++) {
            String s = includefiles.get(i);
            if (s.equals(filename)) {
                return true;
            }
        }
        includefiles.add(filename);
        InstructionNode in;
        for (i = 0; i < list.size(); i++) {
            in = (InstructionNode) list.get(i);
            if (in.getIncludeLoops(filename) == true) {
                return true;
            }
        }
        return false;
    }

    public void addIncludeFiles(List<String> inclfiles) {
        includefiles.addAll(inclfiles);
    }
}
