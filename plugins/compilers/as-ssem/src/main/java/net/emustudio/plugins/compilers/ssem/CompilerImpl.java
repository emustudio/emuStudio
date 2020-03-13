/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.ssem;


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
import net.emustudio.plugins.compilers.ssem.tree.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "SSEM Assembler"
)
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private static final List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("ssem", "SSEM source file")
    );
    private MemoryContext<Byte> memory;
    private int programLocation;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
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
                        "Unexpected memory cell type. Expected Short but was: " + memory.getDataType()
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

        if (memory == null) {
            notifyWarning("Memory is not available.");
        }

        try (Reader reader = new FileReader(inputFileName)) {
            try (CodeGenerator codeGenerator = new CodeGenerator(new MemoryAndFileOutput(outputFileName, memory))) {
                LexerImpl lexer = new LexerImpl(reader);
                ParserImpl parser = new ParserImpl(lexer, this);

                Program program = (Program) parser.parse().value;
                if (program == null) {
                    throw new Exception("Unexpected end of file");
                }
                if (parser.hasSyntaxErrors()) {
                    throw new Exception("One or more errors has been found, cannot continue.");
                }

                program.accept(codeGenerator);
                programLocation = program.getStartLine() * 4;
                notifyInfo("Compile was successful. Output: " + outputFileName);
            }
        } catch (Exception e) {
            LOGGER.trace("Compilation error.", e);
            notifyError("Compilation error: " + e.getMessage());

            return false;
        } finally {
            notifyCompileFinish();
        }

        return true;
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
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(reader);
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

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compilers.ssem.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
