/*
 * DataValueNode.java
 *
 * Created on Sobota, 2007, september 29, 8:54
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080Abstract;

import compiler8080.*;

/**
 *
 * @author vbmacher
 */
public abstract class DataValueNode {
    public abstract String getDataType();
    
    protected int line;
    protected int column;
    
    public DataValueNode(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    /// compile time ///
    public abstract int getSize();
    public abstract void pass1() throws Exception;
    public abstract int pass2(compileEnv env, int addr_start) throws Exception;
    public abstract void pass4(HEXFileHandler hex) throws Exception;
    
        // encode string to hex codes
    protected String getEncString(String literal) {
        byte[] byts = literal.getBytes();
        String enc = "";
        
        for (int i = 0; i < byts.length; i++)
            enc += ExprNode.getEncValue((int)byts[i],true);
        return enc;
    }

    
}
