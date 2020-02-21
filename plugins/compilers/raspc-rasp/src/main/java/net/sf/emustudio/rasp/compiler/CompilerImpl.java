/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016-2017, Michal Šipoš
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.rasp.compiler;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.ContextNotFoundException;
import java_cup.runtime.ComplexSymbolFactory;
import net.sf.emustudio.rasp.compiler.tree.SourceCode;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;

import java.io.*;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The implementation of the compiler of RASP abstract machine assembly
 * language.
 */
@PluginType(
    type = PLUGIN_TYPE.COMPILER,
    title = "RASP Assembler",
    copyright = "\u00A9 Copyright 2016-2017, Michal Šipoš",
    description = "Assembler of RASP machine language"
)
public class CompilerImpl extends AbstractCompiler {

    private final ContextPool contextPool;
    private static final SourceFileExtension[] SOURCE_FILE_EXTENSIONS = new SourceFileExtension[]{new SourceFileExtension("rasp", "RASP source file")};
    private static final String OUTPUT_FILE_EXTENSION = "bin";

    private final ParserImpl parser;
    private final LexerImpl lexer;

    public CompilerImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);

        this.contextPool = Objects.requireNonNull(contextPool);

        //compiler will be reset before compilation
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer, new ComplexSymbolFactory(), this);
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
            notifyInfo("New line character automatically appended, OK.");
        }

    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();

        int errorCode = 0;
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

            try {
                RASPMemoryContext memory = contextPool.getMemoryContext(pluginID, RASPMemoryContext.class);
                CompilerOutput.getInstance().loadIntoMemory(memory);
            } catch (ContextNotFoundException e) {
                notifyWarning("Program memory is not available");
            }
            CompilerOutput.getInstance().clear();

            notifyInfo("Compile was successfull.");
        } catch (Exception ex) {
            errorCode = 1;
            if (ex.getMessage() == null) {
                notifyError("Compilation error.");
            } else {
                notifyError("Compilation error: " + ex.getMessage());
            }
            return false;
        } finally {
            notifyCompileFinish(errorCode);
        }
        programStart = CompilerOutput.getInstance().getProgramStart();
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
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int result = reader.read(cbuf, off, len);
                //System.out.println("reading : " + new String(cbuf, off, len));
                return result;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        });

    }

    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        return SOURCE_FILE_EXTENSIONS;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void showSettings() {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.rasp.compiler.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

}
