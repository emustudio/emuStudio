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
package net.emustudio.plugins.compiler.as8080;

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
import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.as8080.tree.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "Intel 8080 Assembler"
)
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);
    private final static List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("asm", "Assembler source file"),
        new SourceFileExtension("inc", "Include file")
    );

    private final LexerImpl lexer;
    private final ParserImpl parser;
    private MemoryContext<Short> memory;
    private int programLocation;

    public CompilerImpl(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {
        super(pluginID, applicationApi, pluginSettings);
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer, this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, MemoryContext.class);
                if (memory.getDataType() != Short.class) {
                    throw new InvalidContextException(
                        "Unexpected memory cell type. Expected Short but was: " + memory.getDataType()
                    );
                }
            } catch (ContextNotFoundException | InvalidContextException e) {
                LOGGER.warn("Memory context is not available", e);
            }
        });
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
        return "Light modified clone of original Intel's 8080 assembler.";
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerImpl(in);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            notifyCompileStart();
            IntelHEX hex = compileToHex(inputFileName);

            hex.generate(outputFileName);
            programLocation = hex.getProgramLocation();
            notifyInfo("Compilation was successful.\n Output file: " + outputFileName);

            if (memory != null) {
                hex.loadIntoMemory(memory);
                notifyInfo("Compiled file was loaded into memory.");
            } else {
                notifyWarning("Memory is not available.");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.trace("Compiler exception", e);
            notifyError("Compilation error: " + e.getMessage());
            return false;
        } finally {
            notifyCompileFinish();
        }
    }

    @Override
    public boolean compile(String inputFileName) {
        int i = inputFileName.lastIndexOf(".asm");

        String outputFileName = inputFileName;
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        outputFileName += ".hex";
        return compile(inputFileName, outputFileName);
    }

    @Override
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public List<SourceFileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private IntelHEX compileToHex(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);

        notifyInfo(getTitle() + ", version " + getVersion());

        Object parsedAST;
        IntelHEX hex = new IntelHEX();

        try (Reader reader = new FileReader(inputFileName)) {
            lexer.reset(reader, 0, 0, 0);
            parsedAST = parser.parse().value;

            if (parsedAST == null) {
                throw new Exception("Unexpected end of file");
            }
            if (parser.hasSyntaxErrors()) {
                throw new Exception("One or more errors has been found, cannot continue.");
            }

            // do several passes for compiling
            Statement stat = (Statement) parsedAST;
            Namespace env = new Namespace(inputFileName);
            stat.pass1(env); // create symbol table
            stat.pass2(0); // try to evaluate all expressions + compute relative addresses
            while (stat.pass3(env)) {
                // don't worry about deadlock
            }
            if (env.getPassNeedCount() != 0) {
                throw new Exception("Error: could not evaulate all expressions");
            }
            stat.pass4(hex, env);
            return hex;
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.as8080.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
