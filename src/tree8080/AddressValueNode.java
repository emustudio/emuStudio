/*
 * AddressValueNode.java
 *
 * Created on Sobota, 2007, september 29, 10:07
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
public class AddressValueNode extends ExprNode {
    private boolean wasSet;
    
    public void setAddress(int address) {
        this.value = address;
        this.wasSet = true;
    }
    
    public int getAddress() { return value; }
    
    public String toString() {
        return String.valueOf(value);
    }
    
    /** Creates a new instance of AddressValueNode */
    public AddressValueNode() {
        this.wasSet = false;
    }
    
    /// compile time ///
    
    //??
    public int eval(compileEnv env, int curr_addr) {
        this.setAddress(curr_addr);
        return curr_addr;
    }

}
