/*
 * Assembler8080.java
 *
 * Created on Piatok, 2007, august 10, 8:22
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
package as_8080.impl;

import as_8080.tree8080.Statement;

import java.io.Reader;
import emuLib8.plugins.compiler.HEXFileHandler;

import emuLib8.plugins.compiler.ILexer;
import emuLib8.plugins.compiler.SimpleCompiler;
import emuLib8.plugins.compiler.SourceFileExtension;
import emuLib8.plugins.memory.IMemoryContext;
import emuLib8.runtime.Context;

/**
 * Main implementation class of the plugin (assembler for 8080 processor).
 *
 * @author Peter Jakubčo <pjakubco at gmail.com>
 */
public class Assembler8080 extends SimpleCompiler {
    private Lexer8080 lex;
    private Parser8080 par;
    private SourceFileExtension[] suffixes;

    /** Creates a new instance of compiler8080 */
    public Assembler8080(Long pluginID) {
        super(pluginID);
        lex = new Lexer8080((Reader) null);
        par = new Parser8080(lex);
        suffixes = new SourceFileExtension[1];
        suffixes[0] = new SourceFileExtension("asm", "8080 assembler source");
    }

    @Override
    public String getTitle() {
        return "Intel 8080 Assembler";
    }

    @Override
    public String getVersion() {
        return "0.30b";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2007-2010, P.Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Light modified clone of original Intel's assembler. For syntax look"
                + " at users manual.";
    }

    @Override
    public ILexer getLexer(Reader in) {
        return (ILexer)new Lexer8080(in);
    }

    @Override
    public void destroy() {
        lex = null;
        par = null;
    }

    /**
     * Compile the source code into HEXFileHadler
     *
     * @return HEXFileHandler object
     */
    public HEXFileHandler compile(Reader in) throws Exception {
        if (in == null) {
            return null;
        }

        Object s = null;
        HEXFileHandler hex = new HEXFileHandler();

        printInfo(getTitle() + ", version " + getVersion());
        lex.reset(in, 0, 0, 0);
        s = par.parse().value;

        if (s == null) {
            printError("Unexpected end of file");
            return null;
        }
        if (Parser8080.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Statement stat = (Statement) s;
        CompileEnv env = new CompileEnv();
        stat.pass1(env); // create symbol table
        stat.pass2(0); // try to evaluate all expressions + compute relative addresses
        while (stat.pass3(env) == true)
            ;
        if (env.getPassNeedCount() != 0) {
            printError("Error: can't evaulate all expressions");
            return null;
        }
        stat.pass4(hex, env);
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
            printInfo("Compile was sucessfull. Output: " + fileName);

            IMemoryContext mem = Context.getInstance().getMemoryContext(pluginID,
                    IMemoryContext.class);
            if (mem != null) {
                if (hex.loadIntoMemory(mem)) {
                    printInfo("Compiled file was loaded into operating memory.");
                } else {
                    printError("Compiled file couldn't be loaded into operating"
                            + "memory due to an error.");
                }
            }
            programStart = hex.getProgramStart();
            return true;
        } catch (Exception e) {
            printError(e.getMessage());
            return false;
        }
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
