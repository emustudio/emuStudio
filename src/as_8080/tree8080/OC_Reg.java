/*
 * OC_Reg.java
 *
 * Created on Sobota, 2007, september 29, 15:07
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
public class OC_Reg extends OpCodeNode {
    private byte reg;
    
    /** Creates a new instance of OC_RegNode */
    public OC_Reg(String mnemo, byte reg, int line, int column) {
        super(mnemo, line, column);
        this.reg = reg;
    }
 
    /// compile time ///
    
    public int getSize() { return 1; }
    public void pass1(IMessageReporter r) {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        return addr_start + 1;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 0;
        
        if (mnemo.equals("inr")) { opCode = 4 | (reg << 3); }
        else if (mnemo.equals("dcr")) { opCode = 5 | (reg << 3); }
        else if (mnemo.equals("add")) { opCode = 128 | reg; }
        else if (mnemo.equals("adc")) { opCode = 136 | reg; }
        else if (mnemo.equals("sub")) { opCode = 144 | reg; }
        else if (mnemo.equals("sbb")) { opCode = 152 | reg; }
        else if (mnemo.equals("ana")) { opCode = 160 | reg; }
        else if (mnemo.equals("xra")) { opCode = 168 | reg; }
        else if (mnemo.equals("ora")) { opCode = 176 | reg; }
        else if (mnemo.equals("cmp")) { opCode = 184 | reg; }
        
        hex.putCode(String.format("%1$02X",opCode));
    }
    
}
