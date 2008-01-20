/*
 * DecimalValueNode.java
 *
 * Created on Sobota, 2007, september 29, 9:56
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
public class DecimalValueNode extends ExprNode {
    
    /** Creates a new instance of DecimalValueNode */
    public DecimalValueNode(int value) {
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
