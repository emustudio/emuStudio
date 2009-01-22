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
    
    public void setAddress(int address) {
        this.value = address;
    }
    
    public int getAddress() { return value; }
    
    public String toString() {
        return String.valueOf(value);
    }
        
    /// compile time ///
    
    //??
    public int eval(Namespace env, int curr_addr) {
        this.setAddress(curr_addr);
        return curr_addr;
    }

}
