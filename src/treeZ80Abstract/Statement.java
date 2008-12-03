/*
 * Statement.java
 *
 * Created on Piatok, 2007, september 21, 8:56
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80Abstract;

import impl.HEXFileHandler;
import impl.Namespace;
import plugins.compiler.IMessageReporter;


/**
 *
 * @author vbmacher
 */
public abstract class Statement {
   // protected String mnemo;
    protected int line;
    protected int column;
    
    public abstract boolean isPseudo();
    
    public Statement(int line, int column) {
       // this.mnemo = mnemo;
        this.line = line;
        this.column = column;
    }

    /// compile time ///
    
    // return size of compiled code
    public abstract int getSize();
    public abstract void pass1(IMessageReporter rep) throws Exception;
    public abstract int pass2(Namespace parentEnv, int addr_start) throws Exception;
    public abstract void pass4(HEXFileHandler hex) throws Exception;
}
