/*
 * MacroPseudoNode.java
 *
 * Created on Sobota, 2007, september 29, 13:44
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

import as_8080.impl.HEXFileHandler;
import as_8080.impl.CompileEnv;
import as_8080.tree8080Abstract.ExprNode;
import as_8080.tree8080Abstract.PseudoBlock;

import java.util.Vector;

/**
 *
 * @author vbmacher
 */
public class MacroPseudoNode extends PseudoBlock {

    private Vector<String> params; // macro parameters
    private Vector<ExprNode> call_params; // concrete parameters, they can change
    private Statement stat;
    private String mnemo;

    /** Creates a new instance of MacroPseudoNode */
    public MacroPseudoNode(String name, Vector<String> params, Statement s, int line,
            int column) {
        super(line, column);
        this.mnemo = name;
        if (params == null) {
            this.params = new Vector<String>();
        } else {
            this.params = params;
        }
        this.stat = s;
    }

    @Override
    public String getName() {
        return mnemo;
    }

    public void setCallParams(Vector<ExprNode> params) {
        this.call_params = params;
    }

    /// compile time /// 
    @Override
    public int getSize() {
        return 0;
    }

    public int getStatSize() {
        return stat.getSize();
    }

    @Override
    public void pass1() throws Exception {
        stat.pass1(); // pass1 creates block symbol table (local for block)
    }
    // for pass4
    private CompileEnv newEnv;
    // this is macro expansion ! can be called only in MacroCallPseudo class
    // call parameters have to be set

    @Override
    public int pass2(CompileEnv env, int addr_start) throws Exception {
        newEnv = new CompileEnv();
        // add local statement env to newEnv
        stat.getCompileEnv().copyTo(newEnv);
        env.copyTo(newEnv); // add parent statement env to newEnv
        // remove all existing definitions of params name (from level-up environment)
        for (int i = 0; i < params.size(); i++) {
            newEnv.removeAllDefinitions(params.get(i));
        }
        // check of call_params
        if (call_params == null) {
            throw new Exception("[" + line + "," + column
                    + "] Unknown macro parameters");
        }
        if (call_params.size() != params.size()) {
            throw new Exception("[" + line + "," + column
                    + "] Incorrect macro paramers count");
        }
        // create/rewrite symbols => parameters as equ pseudo instructions
        for (int i = 0; i < params.size(); i++) {
            newEnv.addEquDef(new EquPseudoNode(params.get(i), call_params.get(i),
                    line, column));
        }
        return stat.pass2(newEnv, addr_start);
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        stat.pass4(hex, newEnv);
    }
}
