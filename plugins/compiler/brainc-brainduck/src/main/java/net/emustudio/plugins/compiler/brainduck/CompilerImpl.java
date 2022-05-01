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
package net.emustudio.plugins.compiler.brainduck;

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
import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.brainduck.tree.Program;
import net.emustudio.plugins.compiler.brainduck.tree.ProgramParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

@PluginRoot(type = PLUGIN_TYPE.COMPILER, title = "BrainDuck Compiler")
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private final static List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(new SourceFileExtension("b", "brainfuck language source (*.b)"));

    private MemoryContext<Byte> memory;
    private int programLocation;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, MemoryContext.class);
                if (memory.getDataType() != Byte.class) {
                    throw new InvalidContextException("Unexpected memory cell type. Expected Byte but was: " + memory.getDataType());
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
    public LexicalAnalyzer createLexer(String s) {
        BraincLexer lexer = createLexer(CharStreams.fromString(s));
        return new LexicalAnalyzerImpl(lexer);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            notifyCompileStart();
            IntelHEX hex = compileToHex(inputFileName);

            hex.generate(outputFileName);
            notifyInfo("Compile was successful. Output: " + outputFileName);
            programLocation = hex.findProgramLocation();

            if (memory != null) {
                hex.loadIntoMemory(memory, b -> b);
                notifyInfo("Compiled file was loaded into operating memory.");
            } else {
                notifyWarning("Memory is not available.");
            }
            return true;
        } catch (Exception e) {
            LOGGER.trace("Compilation error", e);
            notifyError("Compilation error: " + e);
            e.printStackTrace();
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
    public List<SourceFileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private IntelHEX compileToHex(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);
        notifyInfo(getTitle() + ", version " + getVersion());

        try (Reader reader = new FileReader(inputFileName)) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener());
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BraincParser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener());

            ProgramParser programParser = new ProgramParser();
            programParser.visit(parser.start());
            Program program = programParser.getProgram();

            IntelHEX hex = new IntelHEX();
            program.generateCode(hex);
            return hex;
        }
    }

    private BraincLexer createLexer(CharStream input) {
        BraincLexer lexer = new BraincLexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private BraincParser createParser(TokenStream tokenStream) {
        BraincParser parser = new BraincParser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.brainduck.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
