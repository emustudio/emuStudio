/*
 * CompilerImpl.java
 *
 * Copyright (C) 2009-2012 Peter Jakubčo
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
package net.sf.emustudio.brainduck.brainc.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.HEXFileHandler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.brainduck.brainc.tree.Program;

/**
 * Main class implementing the compiler.
 *
 * @author Peter Jakubčo
 */
@PluginType(type=PLUGIN_TYPE.COMPILER,
        title="BrainDuck Compiler",
        copyright="\u00A9 Copyright 2009-2012, Peter Jakubčo",
        description="Compiler for esoteric architecture called BrainDuck.")
public class CompilerImpl extends AbstractCompiler {
    private LexerBD lex;
    private ParserBD par;
    private SourceFileExtension[] suffixes;

    /**
     * Public constructor.
     *
     * @param pluginID plug-in identification number
     */
    public CompilerImpl(Long pluginID) {
        super(pluginID);
        // lex has to be reset WITH a reader object before compile
        lex = new LexerBD((Reader) null);
        par = new ParserBD(lex);
        suffixes = new SourceFileExtension[1];
        suffixes[0] = new SourceFileExtension("asm", "Brainduck assembler source");
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.brainduck.brainc.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
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
        if (par.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Program program = (Program) parsedProgram;
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
            
            // Remove ".*" suffix and add ".hex" suffix to the filename
            int i = fileName.lastIndexOf(".");
            if (i >= 0) {
                fileName = fileName.substring(0, i);
            }
            fileName += ".hex"; // the output suffix

            hex.generateFile(fileName);
            printInfo("Compile was sucessfull. Output: " + fileName);
            programStart = hex.getProgramStart();
            
            // try to access the memory
            MemoryContext memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);
            if (memory != null) {
                if (hex.loadIntoMemory(memory)) {
                    printInfo("Compiled file was loaded into operating memory.");
                } else {
                    printError("Compiled file couldn't be loaded into operating"
                            + "memory due to an error.");
                }
            }
            return true;
        } catch (Exception e) {
            printError(e.getMessage());
            return false;
        }
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerBD(in);
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
