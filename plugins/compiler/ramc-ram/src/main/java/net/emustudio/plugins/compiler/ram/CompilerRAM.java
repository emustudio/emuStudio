/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.compiler.AbstractCompiler;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugin.compiler.antlr.ParserErrorListener;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextNotFoundException;
import net.emustudio.emulib.io.FileType;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.compiler.ram.ast.Program;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "RAM Machine Assembler",
        description = "RAM machine assembler"
)
@SuppressWarnings("unused")
public class CompilerRAM extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerRAM.class);
    private static final List<FileType> SOURCE_FILE_TYPES = List.of(
            new FileType("Random Access Machine source", "ram")
    );

    private RamMemoryContext memory;

    public CompilerRAM(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.get(pluginID, RamMemoryContext.class);
            } catch (ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    protected void compile(Path inputPath, Path outputPath) throws IOException {
        String fileName = inputPath.toString();
        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener(fileName));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            RAMParser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener(fileName));

            Program program = new Program(fileName);
            new ProgramParser(program).visit(parser.rStart());

            program.assignLabels();
            program.check();
            program.saveToFile(outputPath);

            info(String.format("Compile was successful.\n\tOutput: %s", outputPath));

            if (memory != null) {
                memory.clear();
                program.loadIntoMemory(memory);
                info("Compiled file was loaded into program memory.");
            } else {
                warning("Memory is not available");
            }
        }
    }

    @Override
    protected String getOutputExtension() {
        return "bram";
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    public List<FileType> getSourceFileTypes() {
        return SOURCE_FILE_TYPES;
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
