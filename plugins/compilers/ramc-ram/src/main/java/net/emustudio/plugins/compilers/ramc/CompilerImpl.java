/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compilers.ramc;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceFileExtension;
import net.emustudio.emulib.runtime.*;
import net.emustudio.plugins.compilers.ramc.tree.Program;
import net.emustudio.plugins.compilers.ramc.tree.RAMInstructionImpl;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "RAM Compiler"
)
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private static final List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("ram", "Random Access Machine source")
    );

    private final LexerImpl lexer;
    private final ParserImpl parser;
    private RAMMemoryContext memory;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        // lexer has to be reset WITH a reader object before compile
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer, this);
        RAMInstructionImpl context = new RAMInstructionImpl(0, null);

        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                pool.register(pluginID, context, RAMInstruction.class);
            } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
                applicationApi.getDialogs().showError("Could not register RAM instruction context", super.getTitle());
            }
        });
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Custom compiler for RAM abstract machine";
    }

    public void initialize() {
        try {
            this.memory = applicationApi.getContextPool().getMemoryContext(pluginID, RAMMemoryContext.class);
        } catch (InvalidContextException | ContextNotFoundException e) {
            LOGGER.warn("Memory is not available", e);
        }
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            this.notifyCompileStart();
            CompiledCode code = compileFrom(inputFileName);

            if (code.serialize(outputFileName)) {
                notifyInfo("Compilation was saved to the file: " + outputFileName);
            } else {
                notifyError("Could not save compiled file.");
            }
        } catch (Exception e) {
            LOGGER.trace("Compilation failed", e);
            notifyError("Compilation failed: " + e.getMessage());
            return false;
        } finally {
            notifyCompileFinish();
        }
        return true;
    }

    @Override
    public boolean compile(String inputFileName) {
        int i = inputFileName.lastIndexOf(".ram");

        String outputFileName = inputFileName;
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        outputFileName += ".ro";
        return compile(inputFileName, outputFileName);
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(reader);
    }

    @Override
    public int getProgramLocation() {
        return 0;
    }

    @Override
    public List<SourceFileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private CompiledCode compileFrom(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);

        notifyInfo(getTitle() + ", version " + getVersion());

        Object parsedProgram;
        CompiledCode compiledProgram = new CompiledCode();

        try (Reader reader = new FileReader(inputFileName)) {
            lexer.reset(reader, 0, 0, 0);
            parsedProgram = parser.parse().value;

            if (parsedProgram == null) {
                notifyError("Unexpected end of file");
                throw new Exception("Unexpected end of file");
            }
            if (parser.hasSyntaxErrors()) {
                throw new Exception("Program has errors");
            }

            // do several passes for compiling
            Program program = (Program) parsedProgram;
            program.pass1(0);
            program.pass2(compiledProgram);

            notifyInfo("Compile was successful.");
            if (memory != null) {
                //clear the memory before loading new image
                memory.clear();
                compiledProgram.loadIntoMemory(memory);
                notifyInfo("Compiled file was loaded into program memory.");
            } else {
                notifyWarning("Memory is not available");
            }
        }
        return compiledProgram;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compilers.ramc.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
