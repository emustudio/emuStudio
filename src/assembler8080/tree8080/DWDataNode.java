/*
 * DWDataNode.java
 *
 * Created on Sobota, 2007, september 29, 9:30
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package assembler8080.tree8080;

import assembler8080.impl.HEXFileHandler;
import assembler8080.impl.compileEnv;
import assembler8080.tree8080Abstract.DataValueNode;
import assembler8080.tree8080Abstract.ExprNode;

/**
 *
 * @author vbmacher
 */
public class DWDataNode extends DataValueNode{
    private ExprNode expression = null;
    
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
