/*
 * DataDW.java
 *
 * Created on Streda, 2008, august 13, 11:48
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.compileEnv;
import treeZ80Abstract.DataValue;
import treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class DataDW extends DataValue{
    private Expression expression = null;
    
    public String getDataType() { return "dw"; }
    
    /** Creates a new instance of DataDW */
    public DataDW(Expression expr, int line, int column) {
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
        hex.putCode(expression.encodeValue(false));
    }

}
