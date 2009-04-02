/*
 * OC_RegpairExpr.java
 *
 * Created on Sobota, 2007, september 29, 20:47
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package assembler8080.tree8080;

import assembler8080.impl.HEXFileHandler;
import assembler8080.impl.compileEnv;
import assembler8080.tree8080Abstract.ExprNode;
import assembler8080.tree8080Abstract.OpCodeNode;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
// this class uses only lxi instruction
public class OC_RegpairExpr extends OpCodeNode {
    private byte regpair;
    private ExprNode expr;
    
    /** Creates a new instance of OC_RegpairExpr */
    public OC_RegpairExpr(String mnemo, byte regpair, ExprNode expr, int line,
            int column) {
        super(mnemo, line, column);
        this.regpair = regpair;
        this.expr = expr;
    }
    
    /// compile time ///
    public int getSize() { return 3; }
    public void pass1(IMessageReporter r) {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return addr_start + 3;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        int opCode = 1 | (regpair << 4);
        hex.putCode(String.format("%1$02X",opCode));
        hex.putCode(expr.getEncValue(false));
    }
    
}
