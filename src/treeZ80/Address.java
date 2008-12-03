/*
 * Address.java
 *
 * Created on Sobota, 2007, september 29, 10:07
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.Namespace;
import treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class Address extends Expression {
    private boolean wasSet;
    
    public void setAddress(int address) {
        this.value = address;
        this.wasSet = true;
    }
    
    public int getAddress() { return value; }
    
    public String toString() {
        return String.valueOf(value);
    }
    
    /** Creates a new instance of Address */
    public Address() {
        this.wasSet = false;
    }
    
    /// compile time ///
    
    //??
    public int eval(Namespace env, int curr_addr) {
        this.setAddress(curr_addr);
        return curr_addr;
    }

}
