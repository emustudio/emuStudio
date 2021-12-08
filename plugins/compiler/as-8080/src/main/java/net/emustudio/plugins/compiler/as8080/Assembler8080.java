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
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.exceptions.CompileException;
import net.emustudio.plugins.compiler.as8080.visitors.CreateProgramVisitor;
import net.emustudio.plugins.compiler.as8080.visitors.ExpandIncludesVisitor;
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
    title = "Intel 8080 Assembler"
)
@SuppressWarnings("unused")
public class Assembler8080 extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(Assembler8080.class);
    private final static List<SourceFileExtension> SOURCE_FILE_EXTENSIONS = List.of(
        new SourceFileExtension("asm", "Assembler source file"),
        new SourceFileExtension("inc", "Include file")
    );

    private MemoryContext<Byte> memory;
    private int programLocation;

    public Assembler8080(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {
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
        return "Lightly modified/extended clone of original Intel's 8080 assembler.";
    }

    @Override
    public LexicalAnalyzer createLexer(String s) {
        As8080Lexer lexer = createLexer(CharStreams.fromString(s));
        return new Lexer(lexer);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();
        notifyInfo(getTitle() + ", version " + getVersion());

        try (Reader reader = new FileReader(inputFileName)) {
            org.antlr.v4.runtime.Lexer lexer = createLexer(CharStreams.fromReader(reader));
            lexer.addErrorListener(new ParserErrorListener());
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            As8080Parser parser = createParser(tokens);
            parser.addErrorListener(new ParserErrorListener());

            Program program = new Program();
            new CreateProgramVisitor(program).visit(parser.rStart());

            NodeVisitor[] visitors = new NodeVisitor[] {
                new ExpandIncludesVisitor()
            };

            for (NodeVisitor visitor : visitors) {
                visitor.visit(program);
            }


//            ProgramParser programParser = new ProgramParser();
//            programParser.visit(parser.start());
//
//            Program program = programParser.getProgram();
//            CodeGenerator codeGenerator = new CodeGenerator();
//            ByteBuffer code = codeGenerator.generateCode(program);
//
//            if (code.hasRemaining()) {
//                writeToFile(code, outputFileName);
//                writeToMemory(code);
//            }

         //   programLocation = program.getStartLine() * 4;

            notifyInfo(String.format(
                "Compile was successful.\n\tOutput: %s\n\tProgram starts at 0x%s",
                outputFileName, RadixUtils.formatWordHexString(programLocation)
            ));
        } catch (CompileException e) {
            notifyError(e.line, e.column, e.getMessage());
            return false;
        } catch (IOException e) {
            notifyError("Compilation error: " + e);
            LOGGER.error("Compilation error", e);
            return false;
        } finally {
            notifyCompileFinish();
        }

        return true;

//        try {
//            IntelHEX hex = compileToHex(inputFileName);
//
//            hex.generate(outputFileName);
//            programLocation = hex.getProgramLocation();
//            notifyInfo("Compilation was successful.\n Output file: " + outputFileName);
//
//            if (memory != null) {
//                hex.loadIntoMemory(memory);
//                notifyInfo("Compiled file was loaded into memory.");
//            } else {
//                notifyWarning("Memory is not available.");
//            }
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOGGER.trace("Compiler exception", e);
//            notifyError("Compilation error: " + e.getMessage());
//            return false;
//        } finally {
//            notifyCompileFinish();
//        }
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

//    private IntelHEX compileToHex(String inputFileName) throws Exception {
//        Objects.requireNonNull(inputFileName);
//
//        notifyInfo(getTitle() + ", version " + getVersion());
//
//        Object parsedAST;
//        IntelHEX hex = new IntelHEX();
//
//        try (Reader reader = new FileReader(inputFileName)) {
//            lexer.reset(reader, 0, 0, 0);
//            parser.reset();
//            parsedAST = parser.parse().value;
//
//            if (parsedAST == null) {
//                throw new Exception("Unexpected end of file");
//            }
//            if (parser.hasSyntaxErrors()) {
//                throw new Exception("One or more errors has been found, cannot continue.");
//            }
//
//            // do several passes for compiling
//            Statement stat = (Statement) parsedAST;
//            Namespace env = new Namespace(inputFileName);
//            stat.pass1(env); // create symbol table
//            stat.pass2(0); // try to evaluate all expressions + compute relative addresses
//            while (stat.pass3(env)) {
//                // don't worry about deadlock
//            }
//            if (env.getPassNeedCount() != 0) {
//                throw new Exception("Could not evaluate: " + env.getPassNeedView());
//            }
//            stat.pass4(hex, env);
//            return hex;
//        }
//    }

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
