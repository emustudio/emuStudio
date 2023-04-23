/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;
import net.emustudio.plugins.compiler.as8080.visitors.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "Intel 8080 Assembler"
)
@SuppressWarnings("unused")
public class Assembler8080 extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(Assembler8080.class);
    private final static List<FileExtension> SOURCE_FILE_EXTENSIONS = List.of(
            new FileExtension("asm", "Assembler source file"),
            new FileExtension("inc", "Include file")
    );

    private MemoryContext<Byte> memory;

    public Assembler8080(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {
        super(pluginID, applicationApi, pluginSettings);
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
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Modified and extended clone of original Intel's 8080 assembler.";
    }

    @Override
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    public void compile(Path inputPath, Optional<Path> outputPath) {
        notifyCompileStart();
        notifyInfo(getTitle() + ", version " + getVersion());

        Path finalOutputPath = outputPath.orElse(convertInputToOutputPath(inputPath, ".hex"));
        try (Reader reader = new FileReader(inputPath.toFile())) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener(inputPath.toString()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            As8080Parser parser = createParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new ParserErrorListener(inputPath.toString()));

            Program program = new Program(inputPath.toString());
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
                    new GenerateCodeVisitor(hex)
            };

            for (NodeVisitor visitor : visitors) {
                visitor.visit(program);
            }

            if (program.env().hasNoErrors()) {
                hex.generate(finalOutputPath);
                int programLocation = hex.findProgramLocation();
                applicationApi.setProgramLocation(programLocation);

                notifyInfo(String.format(
                        "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                        finalOutputPath, RadixUtils.formatWordHexString(programLocation)
                ));

                if (memory != null) {
                    hex.loadIntoMemory(memory, b -> b);
                    notifyInfo("Compiled file was loaded into memory.");
                } else {
                    notifyWarning("Memory is not available.");
                }
            } else {
                for (CompileError error : program.env().getErrors()) {
                    notifyError(error.position, error.msg);
                }
            }
        } catch (CompileException e) {
            notifyError(e.position, e.getMessage());
        } catch (IOException e) {
            notifyError("Compilation error: " + e);
        } finally {
            notifyCompileFinish();
        }
    }

    @Override
    public List<FileExtension> getSourceFileExtensions() {
        return SOURCE_FILE_EXTENSIONS;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.as8080.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private As8080Lexer createLexer(CharStream input) {
        As8080Lexer lexer = new As8080Lexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private As8080Parser createParser(TokenStream tokenStream) {
        As8080Parser parser = new As8080Parser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }
}
