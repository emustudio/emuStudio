/*
 * Copyright (C) 2009-2014 Peter Jakubčo
 *
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
package net.sf.emustudio.ram.compiler.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.io.FileReader;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import net.sf.emustudio.ram.compiler.tree.Program;
import net.sf.emustudio.ram.compiler.tree.RAMInstructionImpl;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

@PluginType(type = PLUGIN_TYPE.COMPILER,
title = "RAM Compiler",
copyright = "\u00A9 Copyright 2009-2014, Peter Jakubčo",
description = "Custom compiler for RAM abstract machine")
public class RAMCompiler extends AbstractCompiler {
    private LexerImpl lex = null;
    private ParserImpl par;
    private RAMMemoryContext memory;
    private RAMInstructionImpl context; // not needed context for anything, but
    private SourceFileExtension[] suffixes; // necessary for the registration

    public RAMCompiler(Long pluginID) {
        super(pluginID);
        // lex has to be reset WITH a reader object before compile
        lex = new LexerImpl((Reader) null);
        par = new ParserImpl(lex, this);
        context = new RAMInstructionImpl(0, null);
        try {
            ContextPool.getInstance().register(pluginID, context, RAMInstruction.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register RAM instruction context",
                    RAMCompiler.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ram.compiler.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        try {
            memory = (RAMMemoryContext) ContextPool.getInstance().getMemoryContext(pluginID,
                    RAMMemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            throw new PluginInitializationException(
                    this, "Could not access RAM program memory", e
            );
        }
    }

    private CompiledCode compileFrom(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);
        Objects.requireNonNull(par);
        
        notifyInfo(getTitle() + ", version " + getVersion());

        Object parsedProgram;
        CompiledCode compiledProgram = new CompiledCode();

        try (Reader reader = new FileReader(inputFileName)) {
            lex.reset(reader, 0, 0, 0);
            parsedProgram = par.parse().value;

            if (parsedProgram == null) {
                notifyError("Unexpected end of file");
                return null;
            }
            if (par.errorCount != 0) {
                return null;
            }

            // do several passes for compiling
            Program program = (Program) parsedProgram;
            program.pass1(0);
            program.pass2(compiledProgram);

            notifyInfo("Compile was sucessfull.");
            if (memory != null) {
                compiledProgram.loadIntoMemory(memory);
                notifyInfo("Compiled file was loaded into operating memory.");
            }
        }
        return compiledProgram;
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            CompiledCode code = compileFrom(inputFileName);

            if (code.serialize(outputFileName)) {
                notifyInfo("Compilation was saved to the file: " + outputFileName);
            } else {
                notifyError("Could not save compiled file.");
            }
        } catch (Exception e) {
            notifyError("Compilation failed: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean compile(String inputFileName) {
        int i = inputFileName.lastIndexOf(".asm");

        String outputFileName = inputFileName;
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        outputFileName += ".ram";
        return compile(inputFileName, outputFileName);
    }
    
    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(reader);
    }

    @Override
    public int getProgramStartAddress() {
        return 0;
    }

    @Override
    public void destroy() {
        settings = null;
    }

    @Override
    public void showSettings() {
        // We do not support settings GUI
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
