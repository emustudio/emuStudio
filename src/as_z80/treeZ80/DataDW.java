/*
 * DataDW.java
 *
 * Created on Streda, 2008, august 13, 11:48
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_z80.treeZ80;

import as_z80.impl.HEXFileHandler;
import as_z80.impl.Namespace;
import as_z80.treeZ80Abstract.DataValue;
import as_z80.treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class DataDW extends DataValue{
    private Expression expression = null;
    
    /** Creates a new instance of DataDW */
    public DataDW(Expression expr, int line, int column) {
        super(line,column);
        this.expression = expr;
    }
    
    /// compile time ///
    public int getSize() { return 2; }
    
    public void pass1() {}

    public int pass2(Namespace env, int addr_start) throws Exception {
        expression.eval(env, addr_start);
        return addr_start + 2;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        hex.putCode(expression.encodeValue(2));
    }

}
