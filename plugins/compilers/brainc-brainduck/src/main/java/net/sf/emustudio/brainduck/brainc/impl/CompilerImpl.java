/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.brainduck.brainc.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.HEXFileManager;
import emulib.runtime.exceptions.ContextNotFoundException;
import net.sf.emustudio.brainduck.brainc.tree.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.COMPILER,
        title = "BrainDuck Compiler",
        copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description = "Compiler for esoteric architecture based on brainfuck."
)
@SuppressWarnings("unused")
public class CompilerImpl extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(CompilerImpl.class);

    private final LexerImpl lexer;
    private final ParserImpl parser;
    private final SourceFileExtension[] suffixes;
    private final ContextPool contextPool;

    public CompilerImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);

        // lexer has to be reset WITH a reader object before compile
        lexer = new LexerImpl((Reader) null);
        parser = new ParserImpl(lexer);
        suffixes = new SourceFileExtension[1];
        suffixes[0] = new SourceFileExtension("b", "Brainduck assembler source");
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.brainduck.brainc.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void destroy() {
    }

    private HEXFileManager compileToHex(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);

        notifyInfo(getTitle() + ", version " + getVersion());

        Object parsedProgram;
        HEXFileManager hex = new HEXFileManager();

        try (Reader reader = new FileReader(inputFileName)) {
            lexer.reset(reader, 0, 0, 0);
            parsedProgram = parser.parse().value;

            if (parsedProgram == null) {
                notifyError("Unexpected end of file");
                throw new Exception("Unexpected end of file");
            }
            if (parser.errorCount != 0) {
                throw new Exception("Program has errors");
            }

            // do several passes for compiling
            Program program = (Program) parsedProgram;
            program.firstPass(0);
            program.secondPass(hex);
            return hex;
        }
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        try {
            notifyCompileStart();
            parser.setCompiler(this);
            HEXFileManager hex = compileToHex(inputFileName);

            hex.generateFile(outputFileName);
            notifyInfo("Compile was successful. Output: " + outputFileName);
            programStart = hex.getProgramStart();

            // try to access the memory
            try {
                MemoryContext memory = contextPool.getMemoryContext(pluginID, MemoryContext.class);
                if (hex.loadIntoMemory(memory)) {
                    notifyInfo("Compiled file was loaded into operating memory.");
                } else {
                    notifyError("Compiled file couldn't be loaded into operating"
                        + "memory due to an error.");
                }
            } catch (ContextNotFoundException e) {
                notifyWarning("Memory is not found; only HEX file will be generated.");
            }
            notifyCompileFinish(0);
            return true;
        } catch (Exception e) {
            notifyError("Compilation error: " + e.getMessage());
            LOGGER.error("Compilation error", e);
            notifyCompileFinish(1);
            return false;
        }
    }

    @Override
    public boolean compile(String inputFileName) {
        String outputFileName = Objects.requireNonNull(inputFileName);
        for (SourceFileExtension extension : suffixes) {
            int i = inputFileName.lastIndexOf("." + extension.getExtension());

            if (i >= 0) {
                outputFileName = outputFileName.substring(0, i);
                break;
            }
        }
        outputFileName += ".hex";
        return compile(inputFileName, outputFileName);
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerImpl(in);
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        return suffixes;
    }

}
