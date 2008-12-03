/*
 * Expression.java
 *
 * Created on Sobota, 2007, september 22, 8:30
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80Abstract;

import impl.Namespace;

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

    private static double log2(double val) {
        return Math.log(val)/Math.log(2.0);
    }
    
    public static int getSize(int val) {
        int y = (int)(Math.ceil(log2(val+1)/8.0));
        return (y == 0) ? 1 : y;
    }
    
    public abstract int eval(Namespace env, int curr_addr) throws Exception;
    
    public static String encodeValue(int val) {
        int size = getSize(val);
        if (size == 1) return String.format("%02X",(val & 0xFF));
        else if (size == 2) return String.format("%02X%02X",(val & 0xFF),((val>>8)&0xFF));
        else if (size == 3) return String.format("%02X%02X%02X",
                (val & 0xFF),((val>>8)&0xFF),((val>>16)&0xFF));
        else return String.format("%02X%02X%02X%02X",
                (val & 0xFF),((val>>8)&0xFF),((val>>16)&0xFF),
                ((val>>24)&0xFF));
    };

    public static int reverseBytes(int val) {
        int i = 0;
        int size = getSize(val);
        for (int j = 0; j < size; j++) {
//            System.out.println(Integer.toHexString(val) + " : " 
  //                  + Integer.toHexString(val&0xFF) + " : " 
    //                + Integer.toHexString(val>>8));
            i += (val&0xFF);
            val >>= 8;
            i <<= 8;
        }
        return i>>8;
    }
    
    public String encodeValue() {
        return encodeValue(value);
    }

}
