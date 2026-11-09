/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.compiler.AbstractCompiler;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugin.compiler.antlr.ParserErrorListener;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextNotFoundException;
import net.emustudio.emulib.NumberUtils;
import net.emustudio.emulib.io.FileType;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.compiler.ssem.ast.Program;
import net.emustudio.plugins.compiler.ssem.ast.ProgramParser;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static net.emustudio.emulib.RadixUtils.formatHex;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "SSEM Assembler",
        description = "Assembler of SSEM computer language"
)
@SuppressWarnings("unused")
public class SSEMCompiler extends AbstractCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEMCompiler.class);
    private static final List<FileType> SOURCE_FILE_TYPES = List.of(
            new FileType("SSEM source file", "ssem")
    );
    private MemoryContext<Byte> memory;

    public SSEMCompiler(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
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
    protected void compile(Path inputPath, Path outputPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener(inputPath.toString()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            SSEMParser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener(inputPath.toString()));

            ProgramParser programParser = new ProgramParser(inputPath.toString());
            programParser.visit(parser.start());

            Program program = programParser.getProgram();
            CodeGenerator codeGenerator = new CodeGenerator();
            ByteBuffer code = codeGenerator.generateCode(program);

            if (code.hasRemaining()) {
                writeToFile(code, outputPath);
                writeToMemory(code);
            }

            int programLocation = program.getStartLine() * 4;
            applicationApi.setProgramLocation(programLocation);

            info(String.format(
                    "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                    outputPath, formatHex(programLocation, 4)
            ));
        }
    }

    @Override
    protected String getOutputExtension() {
        return "bssem";
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    public List<FileType> getSourceFileTypes() {
        return SOURCE_FILE_TYPES;
    }

    private SSEMLexer createLexer(CharStream input) {
        SSEMLexer lexer = new SSEMLexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private SSEMParser createParser(TokenStream tokenStream) {
        SSEMParser parser = new SSEMParser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }

    private void writeToFile(ByteBuffer code, Path outputPath) throws IOException {
        code.rewind();
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile(), false)) {
            while (code.hasRemaining()) {
                fos.getChannel().write(code);
            }
        }
    }

    private void writeToMemory(ByteBuffer code) {
        if (memory != null) {
            code.rewind();
            code.position(4); // First 4 bytes is start line
            byte[] data = new byte[code.remaining()];
            code.get(data);
            memory.clear();
            memory.write(0, NumberUtils.nativeBytesToBytes(data));
        } else {
            warning("Memory is not available.");
        }
    }
}
