/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.antlr.ParserErrorListener;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.io.FileType;
import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.brainduck.ast.Program;
import net.emustudio.plugins.compiler.brainduck.ast.ProgramParser;
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

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatHex;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "BrainDuck Compiler",
        description = "Compiler for esoteric architecture based on brainfuck."
)
@SuppressWarnings("unused")
public class CompilerBrainduck extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerBrainduck.class);
    private final static List<FileType> SOURCE_FILE_TYPES = List.of(
            new FileType("brainfuck language source", "b")
    );

    private MemoryContext<Byte> memory;

    public CompilerBrainduck(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemory(pluginID, Byte.class);
            } catch (ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    protected void compile(Path inputPath, Path outputPath) throws Exception {
        IntelHEX hex = compileToHex(inputPath);

        hex.generate(outputPath);
        int programLocation = hex.findProgramLocation();
        applicationApi.setProgramLocation(programLocation);

        info(String.format(
                "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                outputPath, formatHex(programLocation, 4)
        ));

        if (memory != null) {
            memory.clear();
            info("Memory has been cleared.");
            hex.loadIntoMemory(memory);
            info("Compiled file was loaded into operating memory.");
        } else {
            warning("Memory is not available.");
        }
    }

    @Override
    protected String getOutputExtension() {
        return "hex";
    }

    @Override
    public List<FileType> getSourceFileTypes() {
        return SOURCE_FILE_TYPES;
    }

    private IntelHEX compileToHex(Path inputPath) throws IOException {
        String fileName = inputPath.toString();
        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener(fileName));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            BraincParser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener(fileName));

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
}
