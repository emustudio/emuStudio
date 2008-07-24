/*
 * OC_Regpair.java
 *
 * Created on Sobota, 2007, september 29, 20:15
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.HEXFileHandler;
import compiler8080.compileEnv;
import tree8080Abstract.OpCodeNode;

/**
 *
 * @author vbmacher
 */
public class OC_Regpair extends OpCodeNode {
    private byte regpair;
    private boolean psw; // if regpair 11 is treated as sp or psw
    
    /** Creates a new instance of OC_RegRegpair */
    public OC_Regpair(String mnemo, byte regpair, boolean psw, int line,
            int column) {
        super(mnemo, line, column);
        this.regpair = regpair;
        this.psw = psw;
    }
    
    /// compile time ///

    public int getSize() { return 1; }

    public void pass1() {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        return addr_start + 1;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 0;
        
        if (mnemo.equals("stax")) { opCode = 2 | (regpair << 4); }
        else if (mnemo.equals("ldax")) { opCode = 10 | (regpair << 4); }
        else if (mnemo.equals("push")) { opCode = 197 | (regpair << 4); }
        else if (mnemo.equals("pop")) { opCode = 193 | (regpair << 4); }
        else if (mnemo.equals("dad")) { opCode = 9 | (regpair << 4); }
        else if (mnemo.equals("inx")) { opCode = 3 | (regpair << 4); }
        else if (mnemo.equals("dcx")) { opCode = 11 | (regpair << 4); }
        
        hex.putCode(String.format("%1$02X",opCode));
    }

    
}
