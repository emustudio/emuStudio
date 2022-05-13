/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ssem;


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
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
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
import java.nio.channels.FileChannel;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "SSEM Assembler"
)
@SuppressWarnings("unused")
public class SSEMCompiler extends AbstractCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEMCompiler.class);
    private static final List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("ssem", "SSEM source file")
    );
    private MemoryContext<Byte> memory;
    private int programLocation;

    public SSEMCompiler(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, MemoryContext.class);
                if (memory.getDataType() != Byte.class) {
                    throw new InvalidContextException(
                        "Unexpected memory cell type. Expected Byte but was: " + memory.getDataType()
                    );
                }
            } catch (InvalidContextException | ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();
        notifyInfo(getTitle() + ", version " + getVersion());

        try (Reader reader = new FileReader(inputFileName)) {
            Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener());
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            SSEMParser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener());

            ProgramParser programParser = new ProgramParser();
            programParser.visit(parser.start());

            Program program = programParser.getProgram();
            CodeGenerator codeGenerator = new CodeGenerator();
            ByteBuffer code = codeGenerator.generateCode(program);

            if (code.hasRemaining()) {
                writeToFile(code, outputFileName);
                writeToMemory(code);
            }

            programLocation = program.getStartLine() * 4;

            notifyInfo(String.format(
                "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                outputFileName, RadixUtils.formatWordHexString(programLocation)
            ));
        } catch (CompileException e) {
            notifyError(e.line, e.column, e.getMessage());
            return false;
        } catch (IOException e) {
            notifyError("Compilation error: " + e);
            LOGGER.error("Compilation error", e);
            return false;
        } finally {
            notifyCompileFinish();
        }

        return true;
    }

    private void writeToFile(ByteBuffer code, String outputFileName) throws IOException {
        code.rewind();
        try (FileChannel channel = new FileOutputStream(outputFileName, false).getChannel()) {
            channel.write(code);
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

    @Override
    public boolean compile(String inputFileName) {
        String outputFileName = Objects.requireNonNull(inputFileName);
        SourceFileExtension srcExtension = SOURCE_FILE_EXTENSIONS.get(0);

        int i = inputFileName.lastIndexOf("." + srcExtension.getExtension());
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        return compile(inputFileName, outputFileName + ".bin");
    }

    @Override
    public LexicalAnalyzer createLexer(String s) {
        SSEMLexer lexer = createLexer(CharStreams.fromString(s));
        return new LexicalAnalyzerImpl(lexer);
    }

    @Override
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public List<SourceFileExtension> getSourceFileExtensions() {
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
}
