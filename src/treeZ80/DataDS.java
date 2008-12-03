/*
 * DataDS.java
 *
 * Created on Streda, 2008, august 13, 11:47
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.NeedMorePassException;
import impl.Namespace;
import treeZ80Abstract.DataValue;
import treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class DataDS extends DataValue {
    private Expression expression = null;
    
    /** Creates a new instance of DataDS */
    public DataDS(Expression expr, int line, int column) {
        super(line,column);
        this.expression = expr;
    }
    
    /// compile time ///
    
    public int getSize() { return expression.getValue(); }
    
    public void pass1() {}

    public int pass2(Namespace env, int addr_start) throws Exception{
        try { 
            int val = expression.eval(env, addr_start);
            return addr_start+val;
        }
        catch(NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                + "] Error: DS expression can't be ambiguous");
        }
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        String str = "";
        if (!expression.is8Bit())
            throw new Exception("[" + line + "," + column + "] Error:" +
                    " value too large");
        if (expression.getValue() < 0)
            throw new Exception("[" + line + "," + column + "] Error:" +
                    " value can't be negative");
        
        for (int i = 0; i < expression.getValue(); i++)
            str += "00";
        hex.putCode(str);
    }

}
