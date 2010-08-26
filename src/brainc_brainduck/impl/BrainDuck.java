/**
 * BrainDuck.java
 *
 * KISS, YAGNI
 * 
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package brainc_brainduck.impl;

import java.io.Reader;

import plugins.compiler.ICompiler;
import plugins.compiler.ILexer;
import plugins.memory.IMemoryContext;
import brainc_brainduck.tree.Program;
import plugins.compiler.HEXFileHandler;
import plugins.compiler.SimpleCompiler;
import runtime.Context;

/**
 * Main class implementing main compiler interface.
 *
 * @author Peter Jakubčo <pjakubco at gmail.com>
 */
public class BrainDuck extends SimpleCompiler {
    private BDLexer lex;
    private BDParser par;

    /**
     * Public constructor.
     *
     * @param pluginID plug-in identification number
     */
    public BrainDuck(Long pluginID) {
        super(pluginID);
        // lex has to be reset WITH a reader object before compile
        lex = new BDLexer((Reader) null);
        par = new BDParser(lex);
    }

    @Override
    public String getTitle() {
        return "BrainDuck Compiler";
    }

    @Override
    public String getVersion() {
        return "0.14b";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2010, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Compiler for esoteric language called BrainDuck.";
    }

    @Override
    public void destroy() {
        this.par = null;
        this.lex = null;
    }

    /**
     * Compile the source code into HEXFileHadler
     * 
     * @param in  Reader object of the source code
     * @return HEXFileHandler object
     */
    private HEXFileHandler compile(Reader in) throws Exception {
        if (in == null)
            return null;

        Object s = null;
        HEXFileHandler hex = new HEXFileHandler();

        fireMessage(-1,-1,getTitle() + ", version " + getVersion(),
                0,ICompiler.TYPE_INFO);
        lex.reset(in, 0, 0, 0);
        s = par.parse().value;

        if (s == null) {
            fireMessage(-1,-1,"Unexpected end of file", 0,ICompiler.TYPE_ERROR);
            return null;
        }
        if (par.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Program program = (Program) s;
        program.pass1(0);
        program.pass2(hex);
        return hex;
    }

    @Override
    public boolean compile(String fileName, Reader in) {
        try {
            HEXFileHandler hex = compile(in);
            if (hex == null) {
                return false;
            }
            hex.generateFile(fileName);
            fireMessage(-1,-1,"Compile was sucessfull. Output: "
                    + fileName,0, ICompiler.TYPE_INFO);
            programStart = hex.getProgramStart();
            
            // try to access the memory
            IMemoryContext mem = Context.getInstance().getMemoryContext(pluginID,
                    IMemoryContext.class);
            if (mem != null) {
                if (hex.loadIntoMemory(mem)) {
                    fireMessage(-1,-1,
                            "Compiled file was loaded into operating memory.",0,
                            ICompiler.TYPE_INFO);
                } else {
                    fireMessage(-1,-1,
                            "Compiled file couldn't be loaded into operating"
                            + "memory due to an error.", 0,ICompiler.TYPE_ERROR);
                }
            }
            return true;
        } catch (Exception e) {
            fireMessage(-1,-1,e.getMessage(), 0, ICompiler.TYPE_ERROR);
            return false;
        }
    }

    @Override
    public ILexer getLexer(Reader in) {
        return new BDLexer(in);
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
