/*
 * AddressValueNode.java
 *
 * Created on Sobota, 2007, september 29, 10:07
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.compileEnv;
import tree8080Abstract.ExprNode;

/**
 *
 * @author vbmacher
 */
public class AddressValueNode extends ExprNode {
    
    public void setAddress(int address) {
        this.value = address;
    }
    
    public int getAddress() { return value; }
    
    /// compile time ///
    
    //??
    public int eval(compileEnv env, int curr_addr) {
        this.setAddress(curr_addr);
        return curr_addr;
    }

}
