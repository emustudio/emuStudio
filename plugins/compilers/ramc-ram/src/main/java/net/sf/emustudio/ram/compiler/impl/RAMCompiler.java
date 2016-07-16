/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ram.compiler.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.ram.compiler.tree.Program;
import net.sf.emustudio.ram.compiler.tree.RAMInstructionImpl;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.COMPILER,
        title = "RAM Compiler",
        copyright = "\u00A9 Copyright 2006-2016, Peter Jakubčo",
        description = "Custom compiler for RAM abstract machine"
)
@SuppressWarnings("unused")
public class RAMCompiler extends AbstractCompiler {
    private final static Logger LOGGER = LoggerFactory.getLogger(RAMCompiler.class);
    private static final SourceFileExtension[] SUFFIXES = new SourceFileExtension[] {
        new SourceFileExtension("ram", "Random Access Machine source")
    };

    private final ContextPool contextPool;
    private final LexerImpl lexer;
    private final ParserImpl parser;
    private RAMMemoryContext memory;

    public RAMCompiler(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);

        // lexer has to be reset WITH a reader object before compile
        lexer = new LexerImpl(null);
        parser = new ParserImpl(lexer);
        RAMInstructionImpl context = new RAMInstructionImpl(0, null);
        try {
            contextPool.register(pluginID, context, RAMInstruction.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register RAM instruction context", getTitle());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ram.compiler.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        memory = contextPool.getMemoryContext(pluginID, RAMMemoryContext.class);
    }

    private CompiledCode compileFrom(String inputFileName) throws Exception {
        Objects.requireNonNull(inputFileName);

        notifyInfo(getTitle() + ", version " + getVersion());
        parser.setCompiler(this);

        Object parsedProgram;
        CompiledCode compiledProgram = new CompiledCode();

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
            program.pass1(0);
            program.pass2(compiledProgram);

            notifyInfo("Compile was successful.");
            if (memory != null) {
                //clear the memory before loading new image
                memory.clear();
                compiledProgram.loadIntoMemory(memory);
                notifyInfo("Compiled file was loaded into operating memory.");
            }
        }
        return compiledProgram;
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        int errorCode = 0;
        try {
            this.notifyCompileStart();
            CompiledCode code = compileFrom(inputFileName);

            if (code.serialize(outputFileName)) {
                notifyInfo("Compilation was saved to the file: " + outputFileName);
            } else {
                notifyError("Could not save compiled file.");
            }
        } catch (Exception e) {
            errorCode = 1;
            LOGGER.trace("[errorCode={}] Compilation failed", errorCode, e);
            notifyError("Compilation failed: " + e.getMessage());
            return false;
        } finally {
            notifyCompileFinish(errorCode);
        }
        return true;
    }

    @Override
    public boolean compile(String inputFileName) {
        int i = inputFileName.lastIndexOf(".ram");

        String outputFileName = inputFileName;
        if (i >= 0) {
            outputFileName = outputFileName.substring(0, i);
        }
        outputFileName += ".ro";
        return compile(inputFileName, outputFileName);
    }
    
    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(reader);
    }

    @Override
    public int getProgramStartAddress() {
        return 0;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void showSettings() {
        // We do not support settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        return SUFFIXES;
    }
}
