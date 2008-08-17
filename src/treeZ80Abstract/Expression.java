/*
 * Expression.java
 *
 * Created on Sobota, 2007, september 22, 8:30
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80Abstract;

import impl.compileEnv;

/**
 *
 * @author vbmacher
 */
public abstract class Expression {
    protected int value;
    
    /// compile time ///
    public int getValue() { return value; }
    
    public boolean is8Bit() {
        if (value <= 255 && value >= -128) return true;
        else return false;
    }
    
    public static boolean is8Bit(int val) {
        if (val <= 255 && val >= -128) return true;
        else return false;
    }

    public static int getSize(int val) {
        if (val == (val&0xFF)) return 1;
        else if (val == (val&0xFFFF)) return 2;
        else if (val == (val&0xFFFFFF)) return 3;
        else return 4;
    }
    
    public abstract int eval(compileEnv env, int curr_addr) throws Exception;
    
    public static String encodeValue(int val, boolean oneByte) {
        if (oneByte) return String.format("%02X",(val & 0xFF));
        else return String.format("%02X%02X",(val & 0xFF),((val>>8)&0xFF));
    };
    
    public String encodeValue(boolean oneByte) {
        if (oneByte) return String.format("%02X",(value & 0xFF));
        else return String.format("%02X%02X",(value & 0xFF),((value>>8)&0xFF));
    }

}
