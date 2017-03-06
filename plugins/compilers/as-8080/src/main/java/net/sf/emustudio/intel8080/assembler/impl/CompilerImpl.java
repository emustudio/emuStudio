/*
 * Copyright (C) 2007-2015 Peter Jakubčo
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
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.HEXFileManager;
import emulib.runtime.exceptions.ContextNotFoundException;
import net.sf.emustudio.intel8080.assembler.tree.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.COMPILER,
        title = "Intel 8080 Assembler",
        copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description = "Light modified clone of original Intel's assembler. For syntax look at users manual."
)
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private final LexerImpl lexer;
    private final ParserImpl parser;
    private final SourceFileExtension[] suffixes = new SourceFileExtension[] {
            new SourceFileExtension("asm", "Assembler source file"),
            new SourceFileExtension("inc", "Include file")
    };
    private final ContextPool contextPool;

    public CompilerImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer);
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
        return new LexerImpl(in);
    }

    @Override
    public void destroy() {
    }

    private HEXFileManager compileToHex(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);

        notifyInfo(getTitle() + ", version " + getVersion());

        Object parsedAST;
        HEXFileManager hex = new HEXFileManager();

        try (Reader reader = new FileReader(inputFileName)) {
            lexer.reset(reader, 0, 0, 0);
            parsedAST = parser.parse().value;

            if (parsedAST == null) {
                throw new Exception("Unexpected end of file");
            }
            if (parser.errorCount != 0) {
                throw new Exception("One or more errors has been found, cannot continue.");
            }

            // do several passes for compiling
            Statement stat = (Statement) parsedAST;
            CompileEnv env = new CompileEnv(inputFileName);
            stat.pass1(env); // create symbol table
            stat.pass2(0); // try to evaluate all expressions + compute relative addresses
            while (stat.pass3(env)) {
                // don't worry about deadlock
            }
            if (env.getPassNeedCount() != 0) {
                throw new Exception("Error: could not evaulate all expressions");
            }
            stat.pass4(hex, env);
            return hex;
        }
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            notifyCompileStart();
            parser.setCompiler(this);
            HEXFileManager hex = compileToHex(inputFileName);

            hex.generateFile(outputFileName);
            notifyInfo("Compile was successful.\n Output: " + outputFileName);

            try {
                MemoryContext memory = contextPool.getMemoryContext(pluginID, MemoryContext.class);
                if (hex.loadIntoMemory(memory)) {
                    notifyInfo("Compiled file was loaded into operating memory.");
                } else {
                    notifyError("Compiled file couldn't be loaded into operating memory.");
                }
            } catch (ContextNotFoundException e) {
                notifyWarning("Memory is not found; only HEX file will be generated.");
            }
            programStart = hex.getProgramStart();
            notifyCompileFinish(0);
            return true;
        } catch (Exception e) {
            LOGGER.trace("Compiler exception", e);
            notifyError("Compilation error: " + e.getMessage());
            notifyCompileFinish(1);
            return false;
        }
    }

    @Override
    public boolean compile(String inputFileName) {
        int i = inputFileName.lastIndexOf(".asm");

        String outputFileName = inputFileName;
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        outputFileName += ".hex";
        return compile(inputFileName, outputFileName);
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
