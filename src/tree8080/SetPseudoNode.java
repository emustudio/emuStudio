/*
 * SetPseudoNode.java
 *
 * Created on Sobota, 2007, september 29, 10:40
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.HEXFileHandler;
import compiler8080.compileEnv;
import plugins.compiler.IMessageReporter;
import tree8080Abstract.ExprNode;
import tree8080Abstract.PseudoNode;

/**
 *
 * @author vbmacher
 */
public class SetPseudoNode extends PseudoNode {
    private ExprNode expr;
    private String mnemo;
    
    /** Creates a new instance of SetPseudoNode */
    public SetPseudoNode(String id, ExprNode expr, int line, int column) {
        super(line, column);
        this.mnemo = id;
        this.expr = expr;
    }
    
    public String getName() { return mnemo;}
    public int getValue() { return expr.getValue(); }
    /// compile time ///

    public int getSize() { return 0; }
    public void pass1(IMessageReporter r) {}

    public int pass2(compileEnv env, int addr_start) throws Exception { 
        if (env.addSetDef(this) == false)
            throw new Exception("[" + line + "," + column
                    + "] Variable can't be set (already defined): " + mnemo);
        expr.eval(env, addr_start);
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) {}
    
}
