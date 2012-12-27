/*
 * CompilerImpl.java
 *
 * Created on Piatok, 2007, august 10, 8:22
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
package net.sf.emustudio.zilogZ80.assembler.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.HEXFileHandler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import java.io.Reader;
import net.sf.emustudio.zilogZ80.assembler.tree.Program;

/**
 * Implementation class of the Zilog Z80 assembler.
 * @author Peter Jakubčo
 */
@PluginType(type=PLUGIN_TYPE.COMPILER,
        title="Zilog Z80 Assembler",
        copyright="\u00A9 Copyright 2007-2012, Peter Jakubčo",
        description="Custom version of the assembler. For syntax look at users manual.")
public class CompilerImpl extends AbstractCompiler {
    private LexerZ80 lex;
    private ParserZ80 par;
    private SourceFileExtension[] suffixes;

    /** Creates a new instance of assemblerZ80 */
    public CompilerImpl(Long pluginID) {
        super(pluginID);
        lex = new LexerZ80((Reader) null);
        par = new ParserZ80(lex);
        suffixes = new SourceFileExtension[1];
        suffixes[0] = new SourceFileExtension("asm", "Z80 assembler source");
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
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

        Object parsedProgram;
        HEXFileHandler hex = new HEXFileHandler();

        printInfo(CompilerImpl.class.getAnnotation(PluginType.class).title() + ", version " + getVersion());
        lex.reset(in, 0, 0, 0);
        parsedProgram = par.parse().value;

        if (parsedProgram == null) {
            printError("Unexpected end of file");
            return null;
        }
        if (ParserZ80.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Program stat = (Program) parsedProgram;
        Namespace env = new Namespace();
        stat.pass1(env); // create symbol table
        stat.pass2(0); // try to evaluate all expressions + compute relative addresses
        while (stat.pass3(env) == true) {
            // don't worry about deadlock
        }
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
            // Remove ".*" suffix and add ".hex" suffix to the filename
            int i = fileName.lastIndexOf(".");
            if (i >= 0) {
                fileName = fileName.substring(0, i);
            }
            fileName += ".hex"; // the output suffix
            
            hex.generateFile(fileName);
            printInfo("Compile was sucessfull. Output: " + fileName);

            MemoryContext memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);
            if (memory != null) {
                if (hex.loadIntoMemory(memory)) {
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
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerZ80(in);
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
