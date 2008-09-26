/*
 * DSDataNode.java
 *
 * Created on Sobota, 2007, september 29, 9:36
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.HEXFileHandler;
import compiler8080.NeedMorePassException;
import compiler8080.compileEnv;
import tree8080Abstract.DataValueNode;
import tree8080Abstract.ExprNode;

/**
 *
 * @author vbmacher
 */
public class DSDataNode extends DataValueNode {
    private ExprNode expression = null;
    
    /** Creates a new instance of DSDataNode */
    public DSDataNode(ExprNode expr, int line, int column) {
        super(line,column);
        this.expression = expr;
    }
    
    /// compile time ///
    
    public int getSize() { return expression.getValue(); }
    
    public void pass1() {}

    public int pass2(compileEnv env, int addr_start) throws Exception{
        try { 
            int val = expression.eval(env, addr_start);
            return addr_start+val;
        }
        catch(NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                + "] DS expression can't be ambiguous");
        }
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        String str = "";
        
        if (expression.getEncValue(true).length() > 2)
            throw new Exception("[" + line + "," + column + "] value too large");
        if (expression.getValue() < 0)
            throw new Exception("[" + line + "," + column + "] value can't be negative");
        
        for (int i = 0; i < expression.getValue(); i++)
            str += "00";
        hex.putCode(str);
    }

}
