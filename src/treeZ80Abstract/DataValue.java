/*
 * DataValue.java
 *
 * Created on Sobota, 2007, september 29, 8:54
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80Abstract;

import impl.HEXFileHandler;
import impl.Namespace;

/**
 *
 * @author vbmacher
 */
public abstract class DataValue {
    
    protected int line;
    protected int column;
    
    public DataValue(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    /// compile time ///
    public abstract int getSize();
    public abstract void pass1() throws Exception;
    public abstract int pass2(Namespace env, int addr_start) throws Exception;
    public abstract void pass4(HEXFileHandler hex) throws Exception;
    
    /**
     * encode string into hex codes
     */ 
    protected String encodeValue(String literal) {
        byte[] byts = literal.getBytes();
        String enc = "";
        
        for (int i = 0; i < byts.length; i++)
            enc += Expression.encodeValue((int)byts[i],1);
        return enc;
    }

    
}
