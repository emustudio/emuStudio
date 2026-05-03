/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.ram;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.ram.ast.Program;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(type = PLUGIN_TYPE.COMPILER, title = "RAM Machine Assembler")
@SuppressWarnings("unused")
public class CompilerRAM extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerRAM.class);
    private static final List<FileExtension> SOURCE_FILE_EXTENSIONS = List.of(
            new FileExtension("ram", "Random Access Machine source")
    );

    private RamMemoryContext memory;

    public CompilerRAM(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
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
        return "RAM machine assembler";
    }

    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, RamMemoryContext.class);
            } catch (InvalidContextException | ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    public void compile(Path inputPath, Optional<Path> outputPath) {
        try {
            this.notifyCompileStart();
            notifyInfo(getTitle() + ", version " + getVersion());

            Path finalOutputPath = outputPath.orElse(convertInputToOutputPath(inputPath, ".bram"));
            try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
                org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
                lexer.addErrorListener(new ParserErrorListener());
                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RAMParser parser = createParser(tokens);
                parser.addErrorListener(new ParserErrorListener());

                Program program = new Program();
                new ProgramParser(program).visit(parser.rStart());

                program.assignLabels();
                program.check();
                program.saveToFile(finalOutputPath);

                notifyInfo(String.format("Compile was successful.\n\tOutput: %s", finalOutputPath));

                if (memory != null) {
                    memory.clear();
                    program.loadIntoMemory(memory);
                    notifyInfo("Compiled file was loaded into program memory.");
                } else {
                    notifyWarning("Memory is not available");
                }
            }

        } catch (Exception e) {
            LOGGER.trace("Compilation failed", e);
            notifyError("Compilation failed: " + e.getMessage());
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

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.ram.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private RAMLexer createLexer(CharStream input) {
        RAMLexer lexer = new RAMLexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private RAMParser createParser(TokenStream tokenStream) {
        RAMParser parser = new RAMParser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }
}
