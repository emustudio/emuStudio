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
package net.emustudio.plugins.compiler.brainc;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceFileExtension;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.brainc.tree.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "BrainDuck Compiler"
)
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private final static List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("b", "brainfuck language source (*.b)")
    );

    private final LexerImpl lexer;
    private final ParserImpl parser;

    private MemoryContext<Short> memory;
    private int programLocation;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        // lexer has to be reset WITH a reader object before compile
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, MemoryContext.class);
                if (memory.getDataType() != Short.class) {
                    throw new InvalidContextException(
                        "Unexpected memory cell type. Expected Short but was: " + memory.getDataType()
                    );
                }
            } catch (ContextNotFoundException | InvalidContextException e) {
                LOGGER.warn("Memory is not available", e);
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
        return "Compiler for esoteric architecture based on brainfuck.";
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            notifyCompileStart();
            IntelHEX hex = compileToHex(inputFileName);

            hex.generate(outputFileName);
            notifyInfo("Compile was successful. Output: " + outputFileName);
            programLocation = hex.getProgramLocation();

            if (memory != null) {
                hex.loadIntoMemory(memory);
                notifyInfo("Compiled file was loaded into operating memory.");
            } else {
                notifyWarning("Memory is not available.");
            }
            return true;
        } catch (Exception e) {
            LOGGER.trace("Compilation error", e);
            notifyError("Compilation error: " + e.getMessage());
            return false;
        } finally {
            notifyCompileFinish();
        }
    }

    @Override
    public boolean compile(String inputFileName) {
        String outputFileName = Objects.requireNonNull(inputFileName);
        for (SourceFileExtension extension : SOURCE_FILE_EXTENSIONS) {
            int i = inputFileName.lastIndexOf("." + extension.getExtension());

            if (i >= 0) {
                outputFileName = outputFileName.substring(0, i);
                break;
            }
        }
        outputFileName += ".hex";
        return compile(inputFileName, outputFileName);
    }

    @Override
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerImpl(in);
    }

    @Override
    public List<SourceFileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private IntelHEX compileToHex(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);

        notifyInfo(getTitle() + ", version " + getVersion());

        Object parsedProgram;
        IntelHEX hex = new IntelHEX();

        try (Reader reader = new FileReader(inputFileName)) {
            lexer.reset(reader, 0, 0, 0);
            parser.reset();
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
            program.firstPass(0);
            program.secondPass(hex);
            return hex;
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.brainc.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
