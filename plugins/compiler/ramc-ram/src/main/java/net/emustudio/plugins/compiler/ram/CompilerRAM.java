/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.ram.ast.Program;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

@PluginRoot(type = PLUGIN_TYPE.COMPILER, title = "RAM Machine Assembler")
@SuppressWarnings("unused")
public class CompilerRAM extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerRAM.class);
    private static final List<FileExtension> SOURCE_FILE_EXTENSIONS = List.of(
            new FileExtension("ram", "Random Access Machine source")
    );

    private RamMemoryContext memory;

    public CompilerRAM(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
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
        return "RAM machine assembler";
    }

    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, RamMemoryContext.class);
            } catch (InvalidContextException | ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            this.notifyCompileStart();
            notifyInfo(getTitle() + ", version " + getVersion());

            try (Reader reader = new FileReader(inputFileName)) {
                org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
                lexer.addErrorListener(new ParserErrorListener());
                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RAMParser parser = createParser(tokens);
                parser.addErrorListener(new ParserErrorListener());

                Program program = new Program();
                new ProgramParser(program).visit(parser.rStart());

                program.assignLabels();
                program.saveToFile(outputFileName);

                notifyInfo(String.format("Compile was successful.\n\tOutput: %s", outputFileName));

                if (memory != null) {
                    memory.clear();
                    program.loadIntoMemory(memory);
                    notifyInfo("Compiled file was loaded into program memory.");
                } else {
                    notifyWarning("Memory is not available");
                }
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
        int i = inputFileName.toLowerCase(Locale.ENGLISH).lastIndexOf(".ram");

        String outputFileName = inputFileName;
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        outputFileName += ".bram";
        return compile(inputFileName, outputFileName);
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    public List<FileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.ram.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private RAMLexer createLexer(CharStream input) {
        RAMLexer lexer = new RAMLexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private RAMParser createParser(TokenStream tokenStream) {
        RAMParser parser = new RAMParser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }
}
