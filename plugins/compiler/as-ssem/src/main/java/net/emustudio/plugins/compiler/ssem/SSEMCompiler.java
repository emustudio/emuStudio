/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ssem;


import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.ssem.ast.Program;
import net.emustudio.plugins.compiler.ssem.ast.ProgramParser;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "SSEM Assembler"
)
@SuppressWarnings("unused")
public class SSEMCompiler extends AbstractCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEMCompiler.class);
    private static final List<FileExtension> SOURCE_FILE_EXTENSIONS = List.of(
            new FileExtension("ssem", "SSEM source file")
    );
    private MemoryContext<Byte> memory;

    public SSEMCompiler(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, MemoryContext.class);
                Class<?> cellTypeClass = memory.getCellTypeClass();
                if (cellTypeClass != Byte.class) {
                    throw new InvalidContextException(
                            "Unexpected memory cell type. Expected Byte but was: " + cellTypeClass
                    );
                }
            } catch (InvalidContextException | ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    public void compile(Path inputPath, Optional<Path> outputPath) {
        notifyCompileStart();
        notifyInfo(getTitle() + ", version " + getVersion());

        Path finalOutputPath = outputPath.orElse(convertInputToOutputPath(inputPath, ".bssem"));
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
                writeToFile(code, finalOutputPath);
                writeToMemory(code);
            }

            int programLocation = program.getStartLine() * 4;
            applicationApi.setProgramLocation(programLocation);

            notifyInfo(String.format(
                    "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                    finalOutputPath, RadixUtils.formatWordHexString(programLocation)
            ));
        } catch (CompileException e) {
            notifyError(e.position, e.getMessage());
        } catch (IOException e) {
            notifyError("Compilation error: " + e);
            LOGGER.error("Compilation error", e);
        } finally {
            notifyCompileFinish();
        }
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    public List<FileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
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
        return "Assembler of SSEM computer language";
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

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.ssem.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private void writeToFile(ByteBuffer code, Path outputPath) throws IOException {
        code.rewind();
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile(), false)) {
            fos.getChannel().write(code);
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
            notifyWarning("Memory is not available.");
        }
    }
}
