/*
 * OC_RegExpr.java
 *
 * Created on Sobota, 2007, september 29, 20:51
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package tree8080;

import compiler8080.*;
import tree8080Abstract.*;
/**
 *
 * @author vbmacher
 */
// this class uses only mvi instruction
public class OC_RegExpr extends OpCodeNode {
    private byte reg;
    private ExprNode expr;
    
    /** Creates a new instance of OC_RegExpr */
    public OC_RegExpr(String mnemo, byte reg, ExprNode expr, 
            int line, int column) {
        super(mnemo, line, column);
        this.reg = reg;
        this.expr = expr;
    }
    
    /// compile time ///

    public int getSize() { return 2; }
    public void pass1() {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return addr_start + 2;
    }

    // this can be only mvi instr
    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 6;
        
        if (expr.getEncValue(true).length() > 2)
            throw new Exception("["+line+","+column+"] Value of expression is too big");
        opCode |= (reg << 3);
        hex.putCode(String.format("%1$02X",opCode));
        hex.putCode(expr.getEncValue(true));
    }
    
}
