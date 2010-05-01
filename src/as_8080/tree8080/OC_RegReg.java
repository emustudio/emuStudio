/*
 * OC_RegReg.java
 *
 * Created on Sobota, 2007, september 29, 20:08
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_8080.tree8080;

import as_8080.impl.HEXFileHandler;
import as_8080.impl.compileEnv;
import as_8080.tree8080Abstract.OpCodeNode;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
// only for mov instruction
public class OC_RegReg extends OpCodeNode {
    private byte reg_src;
    private byte reg_dst;
    
    /** Creates a new instance of OC_RegReg */
    public OC_RegReg(String mnemo, byte reg_dst, byte reg_src, int line,
            int column) {
        super(mnemo, line, column);
        this.reg_dst = reg_dst;
        this.reg_src = reg_src;
    }
    
    /// compile time ///
    
    public int getSize() { return 1; }
    public void pass1(IMessageReporter r) {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        if ((reg_src == reg_dst) && (reg_src == 6))
            throw new Exception("["+line+","+column+"] Can't use M register on both src and dst");
        return addr_start + 1;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 64;
        opCode |= (reg_dst << 3);
        opCode |= reg_src;
        hex.putCode(String.format("%1$02X",opCode));
    }
    
}
