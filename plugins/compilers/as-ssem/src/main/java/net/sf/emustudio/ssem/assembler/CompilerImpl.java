/*
 * Copyright (C) 2016 Peter Jakubčo
 * KISS, YAGNI, DRY
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
 *
 */
package net.sf.emustudio.ssem.assembler;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.runtime.ContextPool;
import java_cup.runtime.ComplexSymbolFactory;
import net.sf.emustudio.ssem.assembler.tree.Program;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.Objects;

@PluginType(
    type = PLUGIN_TYPE.COMPILER,
    title = "SSEM Assembler",
    copyright = "\u00A9 Copyright 2016, Peter Jakubčo",
    description = "Assembler of SSEM processor language"
)
public class CompilerImpl extends AbstractCompiler {
    private static final String OUTPUT_FILE_EXTENSION = ".bin";
    private static final SourceFileExtension[] SOURCE_FILE_EXTENSIONS = new SourceFileExtension[]{
        new SourceFileExtension("ssem", "SSEM source file")
    };

    public CompilerImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();

        int errorCode = 0;
        try (Reader reader = new FileReader(inputFileName)) {
            try (CodeGenerator codeGenerator = new CodeGenerator(new FileOutputStream(outputFileName))) {
                LexerImpl lexer = new LexerImpl(reader);
                ParserImpl parser = new ParserImpl(lexer, new ComplexSymbolFactory(), this);

                Program program = (Program) parser.parse().value;
                if (program == null) {
                    throw new Exception("Unexpected end of file");
                }
                if (parser.hasSyntaxErrors()) {
                    throw new Exception("One or more errors has been found, cannot continue.");
                }

                program.accept(codeGenerator);
                notifyInfo("Compile was successful. Output: " + outputFileName);
            }
        } catch (Exception e) {
            errorCode = 1;
            return false;
        } finally {
            notifyCompileFinish(errorCode);
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
        return compile(inputFileName, outputFileName + OUTPUT_FILE_EXTENSION);
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(reader);
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
        return "1.0";
    }
}
