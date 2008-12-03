/*
 * PseudoEQU.java
 *
 * Created on Sobota, 2007, september 29, 10:37
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.Namespace;
import plugins.compiler.IMessageReporter;
import treeZ80Abstract.Expression;
import treeZ80Abstract.Pseudo;

/**
 *
 * @author vbmacher
 */
public class PseudoEQU extends Pseudo {
    private Expression expr;
    private String mnemo;
    
    /** Creates a new instance of PseudoEQU */
    public PseudoEQU(String id, Expression expr, int line, int column) {
        super(line,column);
        this.mnemo = id;
        this.expr = expr;
    }
    
    public String getName() { return mnemo;}
    
    public int getValue() { return expr.getValue(); }

    /// compile time ///
    public int getSize() { return 0; }
    
    public void pass1(IMessageReporter rep) {}
    
    public int pass2(Namespace env, int addr_start) throws Exception { 
        if (env.addEquDef(this) == false)
            throw new Exception("[" + line + "," + column
                    + "] Error: constant already defined: " + mnemo);
        expr.eval(env, addr_start);
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) {}

}
