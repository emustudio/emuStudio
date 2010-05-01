/*
 * ExprNode.java
 *
 * Created on Sobota, 2007, september 22, 8:30
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_8080.tree8080Abstract;

import as_8080.impl.compileEnv;

/**
 *
 * @author vbmacher
 */
public abstract class ExprNode {
    protected int value;
    
    /// compile time ///
    public int getValue() { return value; }
    
    public boolean is8Bit() {
        if (value <= 255 && value >= -128) return true;
        else return false;
    }
    
    public abstract int eval(compileEnv env, int curr_addr) throws Exception;
    
    public static String getEncValue(int val, boolean oneByte) {
        if (oneByte) return String.format("%02X",(val & 0xFF));
        else return String.format("%02X%02X",(val & 0xFF),((val>>8)&0xFF));
    };
    
    public String getEncValue(boolean oneByte) {
        if (oneByte) return String.format("%02X",(value & 0xFF));
        else return String.format("%02X%02X",(value & 0xFF),((value>>8)&0xFF));
    }

}
