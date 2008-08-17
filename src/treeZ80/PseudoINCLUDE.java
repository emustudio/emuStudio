/*
 * PseudoINCLUDE.java
 *
 * Created on 14.8.2008, 9:27:10
 * hold to: KISS, YAGNI
 *
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.compileEnv;
import treeZ80Abstract.Pseudo;

/**
 *
 * @author vbmacher
 */
public class PseudoINCLUDE extends Pseudo {
    private String filename;
    
    public PseudoINCLUDE(String filename, int line, int column) {
        super(line,column);
        this.filename = filename;
    }
    
    public String getName() { return ""; }

    public int getSize() { }

    public void pass1() throws Exception { }

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
    }

    public void pass4(HEXFileHandler hex) throws Exception { }

}
