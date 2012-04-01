/**
 * RAMCompiler.java
 * 
 *  KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
import interfaces.C8E258161A30C508D5E8ED07CE943EEF7408CA508;
import java.io.Reader;

import ramc_ram.compiled.CompiledFileHandler;
import ramc_ram.tree.Program;
import emulib.runtime.StaticDialogs;
import emulib.plugins.ISettingsHandler;
import emulib.plugins.compiler.ILexer;
import emulib.plugins.compiler.SimpleCompiler;
import emulib.plugins.compiler.SourceFileExtension;
import ramc_ram.tree.RAMInstruction;
import emulib.runtime.Context;

public class RAMCompiler extends SimpleCompiler {

    private RAMLexer lex = null;
    private RAMParser par;
    private C8E258161A30C508D5E8ED07CE943EEF7408CA508 mem;
    private RAMInstruction context; // not needed context for anything, but
    private SourceFileExtension[] suffixes;
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

    @Override
    public String getTitle() {
        return "RAM compiler";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2011, P. Jakubčo";
    }

    @Override
    public String getVersion() {
        return "0.14b";
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
        mem = (C8E258161A30C508D5E8ED07CE943EEF7408CA508)Context
                .getInstance().getMemoryContext(pluginID,
                C8E258161A30C508D5E8ED07CE943EEF7408CA508.class);
        
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

        printInfo(getTitle() + ", version " + getVersion());
        lex.reset(in, 0, 0, 0);
        s = par.parse().value;

        if (s == null) {
            printError("Unexpected end of file");
            return null;
        }
        if (par.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Program program = (Program) s;
        program.pass1(0);
        program.pass2(hex);

        printInfo("Compile was sucessfull.");
        if (mem != null) {
            if (hex.loadIntoMemory(mem)) {
                printInfo("Compiled file was loaded into operating memory.");
            } else {
                printError("Compiled file couldn't be loaded into operating"
                        + " memory due to an error.");
            }
        }
        return hex;
    }

    @Override
    public boolean compile(String fileName, Reader reader) {
        printInfo("This compiler doesn't support "
                + "compilation into a file.");
        try {
            CompiledFileHandler c = compile(reader);
        } catch (Exception e) {
            printError("Compile failed.");
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
    
    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        return suffixes;
    }

}
