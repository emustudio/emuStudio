/**
 * RAMCompiler.java
 * 
 *  KISS, YAGNI
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
package ramc_ram.impl;

import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;
import interfaces.CA93D6D53B2CCE716745DD211F110C6E387C12431;
import java.io.Reader;

import ramc_ram.compiled.CompiledFileHandler;
import ramc_ram.tree.Program;
import runtime.StaticDialogs;
import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.compiler.ILexer;
import plugins.compiler.SimpleCompiler;
import ramc_ram.tree.RAMInstruction;
import runtime.Context;

public class RAMCompiler extends SimpleCompiler {

    private RAMLexer lex = null;
    private RAMParser par;
    private CA93D6D53B2CCE716745DD211F110C6E387C12431 mem;
    private RAMInstruction context; // not needed context for anything, but
                                    // necessary for the registration

    public RAMCompiler(Long pluginID) {
        super(pluginID);
        // lex has to be reset WITH a reader object before compile
        lex = new RAMLexer((Reader) null);
        par = new RAMParser(lex, this);
        context = new RAMInstruction(0,null);
        if (!Context.getInstance().register(pluginID, context,
                C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.class))
            StaticDialogs.showErrorMessage("Error: Could not register "
                    + "the ramc compiler");
    }

    private void print_text(String mes, int type) {
        fireMessage(-1,-1,mes,0,type);
    }

    public void print_text(int row, int col, String mes, int type) {
        fireMessage(row,col,mes,0,type);
    }

    @Override
    public String getTitle() {
        return "RAM compiler";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2010, P. Jakubčo";
    }

    @Override
    public String getVersion() {
        return "0.12-rc1";
    }

    @Override
    public String getDescription() {
        return "This is a compiler of Random Access Machine. It uses syntax"
                + "and semantics of instructions that is used in the book:\n\n"
                + "\"HUDÁK, Š.: Strojovo orientované jazyky, ISBN 80-969071-3-1\".";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);
        mem = (CA93D6D53B2CCE716745DD211F110C6E387C12431)Context
                .getInstance().getMemoryContext(pluginID,
                CA93D6D53B2CCE716745DD211F110C6E387C12431.class);
        
        if (mem == null) {
            StaticDialogs.showErrorMessage("Error: Could not connect to memory");
            return false;
        }
        return true;
    }
    
    /**
     * Compile the source code into HEXFileHadler
     * 
     * @param in  Reader object of the source code
     * @return HEXFileHandler object
     */
    private CompiledFileHandler compile(Reader in) throws Exception {
        if (par == null) {
            return null;
        }
        if (in == null) {
            return null;
        }

        Object s = null;
        CompiledFileHandler hex = new CompiledFileHandler();

        print_text(getTitle() + ", version " + getVersion(), ICompiler.TYPE_INFO);
        lex.reset(in, 0, 0, 0);
        s = par.parse().value;

        if (s == null) {
            print_text("Unexpected end of file", ICompiler.TYPE_ERROR);
            return null;
        }
        if (par.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Program program = (Program) s;
        program.pass1(0);
        program.pass2(hex);

        print_text("Compile was sucessfull.", ICompiler.TYPE_INFO);
        if (mem != null) {
            if (hex.loadIntoMemory(mem)) {
                print_text("Compiled file was loaded into operating memory.",
                        ICompiler.TYPE_INFO);
            } else {
                print_text("Compiled file couldn't be loaded into operating"
                        + " memory due to an error.", ICompiler.TYPE_ERROR);
            }
        }
        return hex;
    }

    @Override
    public boolean compile(String fileName, Reader reader) {
        print_text("This compiler doesn't support "
                + "compilation into a file.", ICompiler.TYPE_INFO);
        try {
            CompiledFileHandler c = compile(reader);
        } catch (Exception e) {
            print_text("Compile failed.", ICompiler.TYPE_ERROR);
            return false;
        }
        return true;
    }

    @Override
    public ILexer getLexer(Reader reader) {
        return new RAMLexer(reader);
    }

    @Override
    public int getProgramStartAddress() {
        return 0;
    }

    @Override
    public void destroy() {
        settings = null;
        par = null;
        lex = null;
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
