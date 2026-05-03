/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.rasp;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.compiler.AbstractCompiler;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.compiler.rasp.ast.Program;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
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
import java.util.*;

@PluginRoot(
        type = PLUGIN_TYPE.COMPILER,
        title = "RASP Machine Assembler"
)
public class CompilerRASP extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerRASP.class);
    private static final List<FileExtension> SOURCE_FILE_EXTENSIONS = List.of(
            new FileExtension("rasp", "RASP source file")
    );

    private RaspMemoryContext memory;

    public CompilerRASP(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() {
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                memory = pool.getMemoryContext(pluginID, RaspMemoryContext.class);
            } catch (InvalidContextException | ContextNotFoundException e) {
                LOGGER.warn("Memory is not available", e);
            }
        });
    }

    @Override
    public void compile(Path inputPath, Optional<Path> outputPathX) {
        try {
            this.notifyCompileStart();
            notifyInfo(getTitle() + ", version " + getVersion());

            Path finalOutputPath = outputPathX.orElse(convertInputToOutputPath(inputPath, ".brasp"));
            try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
                org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
                lexer.addErrorListener(new ParserErrorListener());
                CommonTokenStream tokens = new CommonTokenStream(lexer);

                RASPParser parser = createParser(tokens);
                parser.addErrorListener(new ParserErrorListener());

                Program program = new Program();
                new ProgramParser(program).visit(parser.rStart());

                Map<Integer, Integer> compiled = program.compile();
                program.saveToFile(finalOutputPath, compiled);

                int programLocation = program.getProgramLocation(compiled);
                applicationApi.setProgramLocation(programLocation);

                notifyInfo(String.format(
                        "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                        finalOutputPath, RadixUtils.formatWordHexString(programLocation)
                ));

                if (memory != null) {
                    memory.clear();
                    program.loadIntoMemory(memory, compiled);
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
        return "RASP machine assembler";
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.compiler.rasp.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private RASPLexer createLexer(CharStream input) {
        RASPLexer lexer = new RASPLexer(input);
        lexer.removeErrorListeners();
        return lexer;
    }

    private RASPParser createParser(TokenStream tokenStream) {
        RASPParser parser = new RASPParser(tokenStream);
        parser.removeErrorListeners();
        return parser;
    }
}
