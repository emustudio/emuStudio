/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.compiler;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java_cup.runtime.ComplexSymbolFactory;
import sk.tuke.emustudio.rasp.compiler.tree.Tree;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

/**
 * The implementation of the compiler of RASP abstract machine assembly
 * language.
 */
@PluginType(
        type = PLUGIN_TYPE.COMPILER,
        title = "RASP Assembler",
        copyright = "\u00A9 Copyright 2016, Michal Sipos",
        description = "Assembler of RASP machine language"
)
public class CompilerImpl extends AbstractCompiler {

    private final ContextPool contextPool;
    private static final SourceFileExtension[] SOURCE_FILE_EXTENSIONS = new SourceFileExtension[]{new SourceFileExtension("rasp", "RASP source file")};
    private static final String OUTPUT_FILE_EXTENSION = "bin";

    public CompilerImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    @Override
    public boolean compile(String inputFileName, String outputFileName) {
        notifyCompileStart();

        int errorCode = 0;
        try (Reader reader = new FileReader(inputFileName)) {

            RASPMemoryContext memory = (RASPMemoryContext) contextPool.getMemoryContext(pluginID, RASPMemoryContext.class);
            LexerImpl lexer = new LexerImpl(reader);
            ParserImpl parser = new ParserImpl(lexer, new ComplexSymbolFactory(), this);
            Tree tree = (Tree) parser.parse().value;
            if (tree == null) {
                throw new Exception("Unexpected end of file.");
            }
            if (parser.hasSyntaxErrors()) {
                throw new Exception("One ore more errors in the source code.");
            }

            
            tree.pass();
            CompilerOutput.getInstance().saveToFile(outputFileName);
            CompilerOutput.getInstance().loadIntoMemory(memory);
            CompilerOutput.getInstance().clear();

            notifyInfo("Compile was successfull.");
        } catch (Exception ex) {
            errorCode = 1;
            System.err.println("Compilation error " + ex.getMessage());
            ex.printStackTrace();
            notifyError("Compilation error");
            return false;
        } finally {
            notifyCompileFinish(errorCode);
        }
        return true;

    }

    @Override
    public boolean compile(String inputFileName) {
        String outputFileName = Objects.requireNonNull(inputFileName);
        for (SourceFileExtension srcExtension : SOURCE_FILE_EXTENSIONS) {
            int i = inputFileName.lastIndexOf("." + srcExtension.getExtension());
            if (i >= 0) {
                outputFileName = outputFileName.substring(0, i);
                break;
            }
        }
        return compile(inputFileName, outputFileName + "." + OUTPUT_FILE_EXTENSION);
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                int result = reader.read(cbuf, off, len);
                System.out.println("reading : " + new String(cbuf, off, len));
                return result;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        });
        
    }

    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        return SOURCE_FILE_EXTENSIONS;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void showSettings() {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

}
