/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition.ColumnUnit;
import net.emustudio.emulib.plugins.compiler.antlr.ParserErrorListener;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.runtime.io.FileType;
import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.visitors.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "Intel 8080 Assembler",
        description = "Modified and extended clone of original Intel's 8080 assembler."
)
@SuppressWarnings("unused")
public class Assembler8080 extends AbstractCompiler {
    @Override
    public ColumnUnit getSourcePositionColumnUnit() {
        return ColumnUnit.CODE_POINT;
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(Assembler8080.class);
    private final static List<FileType> SOURCE_FILE_TYPES = List.of(
            new FileType("Assembler source file", "asm"),
            new FileType("Include file", "inc")
    );

    private MemoryContext<Byte> memory;

    public Assembler8080(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {
        super(pluginID, applicationApi, pluginSettings);
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
    public LexicalAnalyzer createLexer() {
        return new LexicalAnalyzerImpl(createLexer(null));
    }

    @Override
    protected void compile(Path inputPath, Path outputPath) throws IOException {
        String fileName = inputPath.toString();
        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener(fileName));
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            As8080Parser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener(fileName));

            Program program = new Program(fileName);
            new CreateProgramVisitor(program).visit(parser.rStart());

            IntelHEX hex = new IntelHEX();
            EvaluateExprVisitor evaluator = new EvaluateExprVisitor();
            BaseNodeVisitor[] visitors =
                    new BaseNodeVisitor[]{
                    new ExpandIncludesVisitor(),
                    new CheckDeclarationsVisitor(),
                    new ExpandMacrosVisitor(),
                    new SortMacroArgumentsVisitor(),
                    // macro expansion could bring re-definition of declarations, but we cannot check declarations again
                    // until the macro is properly integrated (b/c we could see multiple macro defs on multiple calls)
                    new CheckDeclarationsVisitor(),
                    evaluator,
                    new CheckExprSizesVisitor(),
                    new GenerateCodeVisitor(hex)
            };

            for (BaseNodeVisitor visitor : visitors) {
                visitor.visit(program);
            }

            if (!program.env().hasNoErrors()) {
                for (CompileError error : program.env().getErrors()) {
                    error(error.position, error.msg);
                }
                return;
            }

            hex.generate(outputPath);
            int programLocation = hex.findProgramLocation();
            applicationApi.setProgramLocation(programLocation);

            info(String.format(
                    "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                    outputPath, Numbers.formatHex(programLocation, 4)
            ));

            if (memory != null) {
                hex.loadIntoMemory(memory);
                Optional.ofNullable(applicationApi.getAnnotations()).ifPresent(annotations -> {
                    SourceCodeAnnotationsVisitor sourceMappings = new SourceCodeAnnotationsVisitor(pluginID);
                    sourceMappings.visit(program);
                    evaluator.labels().forEach(label -> sourceMappings.addLabel(label.getKey(), label.getValue()));
                    annotations.replaceOwner(pluginID, sourceMappings.annotations());
                });
                info("Compiled file was loaded into memory.");
            } else {
                warning("Memory is not available.");
            }
        }
    }

    @Override
    protected String getOutputExtension() {
        return "hex";
    }

    @Override
    public List<FileType> getSourceFileTypes() {
        return SOURCE_FILE_TYPES;
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
