/*
 * DecimalExpr.java
 *
 * Created on Sobota, 2007, september 29, 9:56
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.compileEnv;
import treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class DecimalExpr extends Expression {
    
    /** Creates a new instance of DecimalExpr */
    public DecimalExpr(int value) {
        this.value = value;
    }
    
    /// compile time ///

    public int getSize() {
        if ((value & 0xFF) == value) return 1;
        else return 2;
    }

    public int eval(compileEnv env, int curr_addr) throws Exception {
        return value;
    }

}
