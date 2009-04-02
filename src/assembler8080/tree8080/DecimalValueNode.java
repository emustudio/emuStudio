/*
 * DecimalValueNode.java
 *
 * Created on Sobota, 2007, september 29, 9:56
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package assembler8080.tree8080;

import assembler8080.impl.compileEnv;
import assembler8080.tree8080Abstract.ExprNode;

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
