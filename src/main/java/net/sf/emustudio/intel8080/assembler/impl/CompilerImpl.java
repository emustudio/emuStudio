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
package net.sf.emustudio.intel8080.assembler.impl;

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
import net.sf.emustudio.intel8080.assembler.tree.Statement;

/**
 * Main implementation class of the plugin (assembler for 8080 processor).
 *
 * @author Peter Jakubčo
 */
@PluginType(type=PLUGIN_TYPE.COMPILER,
        title="Intel 8080 Assembler",
        copyright="\u00A9 Copyright 2007-2012, Peter Jakubčo",
        description="Light modified clone of original Intel's assembler. For syntax look at users manual.")
public class CompilerImpl extends AbstractCompiler {
    private Lexer8080 lexer;
    private Parser8080 parser;
    private SourceFileExtension[] suffixes;

    /** Creates a new instance */
    public CompilerImpl(Long pluginID) {
        super(pluginID);
        lexer = new Lexer8080((Reader) null);
        parser = new Parser8080(lexer);
        suffixes = new SourceFileExtension[1];
        suffixes[0] = new SourceFileExtension("asm", "8080 assembler source");
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.intel8080.assembler.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new Lexer8080(in);
    }

    @Override
    public void destroy() {
        lexer = null;
        parser = null;
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

        Object parsedAST;
        HEXFileHandler hex = new HEXFileHandler();

        printInfo(CompilerImpl.class.getAnnotation(PluginType.class).title() + ", version " + getVersion());
        lexer.reset(in, 0, 0, 0);
        parsedAST = parser.parse().value;

        if (parsedAST == null) {
            printError("Unexpected end of file");
            return null;
        }
        if (Parser8080.errorCount != 0) {
            return null;
        }

        // do several passes for compiling
        Statement stat = (Statement) parsedAST;
        CompileEnv env = new CompileEnv();
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
                    printError("Compiled file couldn't be loaded into operating memory due to an error.");
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
