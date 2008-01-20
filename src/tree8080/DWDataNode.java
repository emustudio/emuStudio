/*
 * DWDataNode.java
 *
 * Created on Sobota, 2007, september 29, 9:30
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
public class DWDataNode extends DataValueNode{
    private ExprNode expression = null;
    
    public String getDataType() { return "dw"; }
    
    /** Creates a new instance of DWDataNode */
    public DWDataNode(ExprNode expr, int line, int column) {
        super(line,column);
        this.expression = expr;
    }
    
    /// compile time ///
    public int getSize() { return 2; }
    
    public void pass1() {}

    public int pass2(compileEnv env, int addr_start) throws Exception {
        expression.eval(env, addr_start);
        return addr_start + 2;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        hex.putCode(expression.getEncValue(false));
    }

}
