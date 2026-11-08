/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.antlr.ParserErrorListener;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.io.FileType;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.rasp.ast.Program;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
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
import java.util.Map;
import java.util.Optional;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "RASP Machine Assembler",
        description = "RASP machine assembler"
)
public class CompilerRASP extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerRASP.class);
    private static final List<FileType> SOURCE_FILE_TYPES = List.of(
            new FileType("RASP source file", "rasp")
    );

    private RaspMemoryContext memory;

    public CompilerRASP(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.get(pluginID, RaspMemoryContext.class);
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

            RASPParser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener(fileName));

            Program program = new Program(fileName);
            new ProgramParser(program).visit(parser.rStart());

            Map<Integer, Integer> compiled = program.compile();
            program.saveToFile(outputPath, compiled);

            int programLocation = program.getProgramLocation(compiled);
            applicationApi.setProgramLocation(programLocation);

            info(String.format(
                    "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                    outputPath, RadixUtils.formatHex(programLocation, 4)
            ));

            if (memory != null) {
                memory.clear();
                program.loadIntoMemory(memory, compiled);
                info("Compiled file was loaded into program memory.");
            } else {
                warning("Memory is not available");
            }
        }
    }

    @Override
    protected String getOutputExtension() {
        return "brasp";
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    public List<FileType> getSourceFileTypes() {
        return SOURCE_FILE_TYPES;
    }

    private RASPLexer createLexer(CharStream input) {
        RASPLexer lexer = new RASPLexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private RASPParser createParser(TokenStream tokenStream) {
        RASPParser parser = new RASPParser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }
}
