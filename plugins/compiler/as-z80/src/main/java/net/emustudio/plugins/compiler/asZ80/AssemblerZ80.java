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
package net.emustudio.plugins.compiler.asZ80;

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
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.exceptions.CompileException;
import net.emustudio.plugins.compiler.asZ80.visitors.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

@PluginRoot(
    type = PLUGIN_TYPE.COMPILER,
    title = "Zilog Z80 Assembler"
)
@SuppressWarnings("unused")
public class AssemblerZ80 extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(AssemblerZ80.class);
    private final static List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("asm", "Assembler source file"),
        new SourceFileExtension("inc", "Include file")
    );

    private MemoryContext<Byte> memory;
    private int programLocation;

    public AssemblerZ80(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {
        super(pluginID, applicationApi, pluginSettings);
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
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Assembler targetting Z80 microprocessor";
    }

    @Override
    public LexicalAnalyzer createLexer(String s) {
        AsZ80Lexer lexer = createLexer(CharStreams.fromString(s));
        return new LexicalAnalyzerImpl(lexer);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();
        notifyInfo(getTitle() + ", version " + getVersion());

        try (Reader reader = new FileReader(inputFileName)) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener());
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            AsZ80Parser parser = createParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ParserErrorListener());

            Program program = new Program();
            new CreateProgramVisitor(program).visit(parser.rStart());

            IntelHEX hex = new IntelHEX();
            NodeVisitor[] visitors = new NodeVisitor[]{
                new ExpandIncludesVisitor(),
                new CheckDeclarationsVisitor(),
                new ExpandMacrosVisitor(),
                new SortMacroArgumentsVisitor(),
                // macro expansion could bring re-definition of declarations, but we cannot check declarations again
                // until the macro is properly integrated (b/c we could see multiple macro defs on multiple calls)
                new CheckDeclarationsVisitor(),
                new EvaluateExprVisitor(),
                new CheckExprSizesVisitor(),
                new CollectExprsInOpcodeVisitor(),
                new GenerateCodeVisitor(hex)
            };

            for (NodeVisitor visitor : visitors) {
                visitor.visit(program);
            }

            programLocation = 0;
            if (program.env().hasNoErrors()) {
                hex.generate(outputFileName);
                programLocation = hex.findProgramLocation();

                notifyInfo(String.format(
                    "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                    outputFileName, RadixUtils.formatWordHexString(programLocation)
                ));

                if (memory != null) {
                    hex.loadIntoMemory(memory, b -> b);
                    notifyInfo("Compiled file was loaded into memory.");
                } else {
                    notifyWarning("Memory is not available.");
                }
                return true;
            } else {
                for (CompileError error : program.env().getErrors()) {
                    notifyError(error.line, error.column, error.msg);
                }
                return false;
            }
        } catch (CompileException e) {
            notifyError(e.line, e.column, e.getMessage());
            return false;
        } catch (IOException e) {
            notifyError("Compilation error: " + e);
            return false;
        } finally {
            notifyCompileFinish();
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
        return compile(inputFileName, outputFileName + ".hex");
    }

    @Override
    public int getProgramLocation() {
        return programLocation;
    }

    @Override
    public List<SourceFileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.asZ80.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private AsZ80Lexer createLexer(CharStream input) {
        AsZ80Lexer lexer = new AsZ80Lexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private AsZ80Parser createParser(TokenStream tokenStream) {
        AsZ80Parser parser = new AsZ80Parser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }
}