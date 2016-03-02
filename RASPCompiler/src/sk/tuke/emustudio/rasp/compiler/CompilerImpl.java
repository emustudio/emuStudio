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
import emulib.runtime.ContextPool;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java_cup.runtime.ComplexSymbolFactory;
import sk.tuke.emustudio.rasp.compiler.tree.Tree;

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
            LexerImpl lexer = new LexerImpl(reader);
            ParserImpl parser = new ParserImpl(lexer, new ComplexSymbolFactory(), this);
            Tree tree = (Tree) parser.parse().value;
            if (tree == null) {
                throw new Exception("Unexpected end of file.");
            }
            if(parser.hasSyntaxErrors()){
                throw  new Exception("One ore more errors in the source code.");
            }
            tree.pass();
            notifyInfo("Compile was successfull.");
        } catch (Exception ex) {
            errorCode = 1;
            return false;
        } finally {
            notifyCompileFinish(errorCode);
        }
        return true;

    }

    @Override
    public boolean compile(String inputFileName) {
        return compile(inputFileName, "out.bin");
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        return new LexerImpl(reader);
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
