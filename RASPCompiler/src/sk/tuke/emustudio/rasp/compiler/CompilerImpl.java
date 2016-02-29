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
import java.io.Reader;
import java.util.Objects;
        
/**
 * The implementation of the compiler of RASP abstract machine assembly language.
 */

@PluginType(
        type = PLUGIN_TYPE.COMPILER,
        title = "RASP Assembler",
        copyright = "\u00A9 Copyright 2016, Michal Sipos",
        description = "Assembler of RASP machine language"
)
public class CompilerImpl extends AbstractCompiler{
    private final ContextPool contextPool;

    public CompilerImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    @Override
    public boolean compile(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean compile(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LexicalAnalyzer getLexer(Reader reader) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void showSettings() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isShowSettingsSupported() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
