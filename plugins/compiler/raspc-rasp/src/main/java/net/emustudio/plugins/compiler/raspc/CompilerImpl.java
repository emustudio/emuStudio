/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.raspc;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceFileExtension;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.compiler.raspc.tree.SourceCode;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * The implementation of the compiler of RASP abstract machine assembly language.
 */
@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "RASP Assembler"
)
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private static final List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("rasp", "RASP source file")
    );
    private static final String OUTPUT_FILE_EXTENSION = "brasp";

    private final ParserImpl parser;
    private final LexerImpl lexer;
    private RASPMemoryContext memory;
    private int programLocation;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        //compiler will be reset before compilation
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer, this);
    }

    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, RASPMemoryContext.class);
            } catch (ContextNotFoundException | InvalidContextException e) {
                LOGGER.warn("Memory context is not available", e);
            }
        });
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();

        try (Reader reader = new FileReader(inputFileName)) {
            lexer.reset(reader, 0, 0, 0);
            //ensure that file ends with a new line
            appendNewLine(inputFileName);
            SourceCode sourceCode = (SourceCode) parser.parse().value;
            if (sourceCode == null) {
                throw new Exception("Unexpected end of file.");
            }
            if (parser.hasSyntaxErrors()) {
                throw new Exception("One ore more errors in the source code.");
            }

            if (sourceCode.isProgramStartZero()) {
                notifyWarning("Program start must not be 0. It was automatically set to 20.");
            } else if (sourceCode.isProgramStartUndefined()) {
                notifyWarning("Program start was not defined. It was automatically set to 20.");
            }

            sourceCode.pass();
            CompilerOutput.getInstance().saveToFile(outputFileName);
            notifyInfo("Compilation was successful.");

            if (memory != null) {
                CompilerOutput.getInstance().loadIntoMemory(memory);
                notifyInfo("Program was loaded into program memory");
            } else {
                notifyWarning("Program memory is not available");
            }
            programLocation = CompilerOutput.getInstance().getProgramStart();
        } catch (Exception ex) {
            if (ex.getMessage() == null) {
                notifyError("Compilation error.");
            } else {
                notifyError("Compilation error: " + ex.getMessage());
            }
            return false;
        } finally {
            CompilerOutput.getInstance().clear();
            notifyCompileFinish();
        }

        return true;
    }

    @Override
    public boolean compile(String inputFileName) {
        String outputFileName = Objects.requireNonNull(inputFileName);
        for (SourceFileExtension srcExtension : SOURCE_FILE_EXTENSIONS) {
            int i = inputFileName.lastIndexOf("." + srcExtension.getExtension());
            if (i >= 0) {
                outputFileName = outputFileName.substring(0, i);
                break;
            }
        }
        return compile(inputFileName, outputFileName + "." + OUTPUT_FILE_EXTENSION);
    }

    @Override
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return reader.read(cbuf, off, len);
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        });

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
        return "Assembler of RASP machine language";
    }

    private boolean fileEndsWithNewLine(String fileName) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(new File(fileName), "r")) {
            long lastCharPosition = file.length() - 1;
            file.seek(lastCharPosition);
            byte b = file.readByte();
            return b == 0xA || b == 0xD;
        }
    }

    private void appendNewLine(String fileName) throws IOException {
        if (!fileEndsWithNewLine(fileName)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                writer.newLine();
            }
            notifyInfo("New line character automatically appended.");
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.raspc.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
