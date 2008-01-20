/*
 * EquPseudoNode.java
 *
 * Created on Sobota, 2007, september 29, 10:37
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.*;
import tree8080Abstract.*;
/**
 *
 * @author vbmacher
 */
public class EquPseudoNode extends PseudoNode {
    private ExprNode expr;
    private String mnemo;
    
    /** Creates a new instance of EquPseudoNode */
    public EquPseudoNode(String id, ExprNode expr, int line, int column) {
        super(line,column);
        this.mnemo = id;
        this.expr = expr;
    }
    
    public String getName() { return mnemo;}

    // this is used in macro expansion for defining macro params
  //  public void setExpr(ExprNode expr) {
    //    this.expr = expr;
    //}
    
    public int getValue() { return expr.getValue(); }

    /// compile time ///
    public int getSize() { return 0; }
    
    public void pass1() {}
    
    public int pass2(compileEnv env, int addr_start) throws Exception { 
        if (env.addEquDef(this) == false)
            throw new Exception("[" + line + "," + column
                    + "] Constant already defined: " + mnemo);
        expr.eval(env, addr_start);
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) {}

}
