/*
 * OC_RegExpr.java
 *
 * Created on Pondelok, 2008, august 18, 8:55
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.Namespace;
import plugins.compiler.IMessageReporter;
import treeZ80Abstract.Expression;
import treeZ80Abstract.Instruction;

/**
 *
 * opcode = (first_byte+reg) expr
 * @author vbmacher
 */

public class OC_RegExpr extends Instruction {
    public static final int CALL = 0xC40000; // CALL cc,NN
    public static final int JP = 0xC20000; // JP cc,NN
    public static final int JR = 0x2000; // JR cc,N
    public static final int LD_IIX_NN = 0xDD7000; // LD (IX+N),r
    public static final int LD_IIY_NN = 0xFD7000; // LD (IY+N),r
    public static final int LD_RR = 0x010000; // LD rr,NN
    public static final int BIT = 0xCB40; // BIT b,r
    public static final int RES = 0xCB80; // RES b,r
    public static final int SET = 0xCBC0; // SET b,r
            
    private Expression expr;
    private boolean oneByte;
    private boolean bitInstr; // bit instruction? (BIT,SET,RES)
    private int old_opcode;
    
    /***
     * Creates a new instance of OC_RegExpr
     * 
     * @param pos index of byte where add register value;
     *        e.g. DD 70+reg XX XX => pos = 1;
     *             C4+reg 00 00    => pos = 0;
     */
    public OC_RegExpr(int opcode, int reg, int pos,Expression expr, 
            boolean oneByte, int line, int column) {
        super(opcode, line, column);
        this.opcode += (reg<<((getSize()-1-pos)*8));
        old_opcode = opcode; //this.opcode;
        this.oneByte = oneByte;
        this.expr = expr;
        bitInstr = false;
    }
    
    /**
     * Special constructor for BIT,RES and SET instructions
     */
    public OC_RegExpr(int opcode, Expression bit, int reg,
            int line, int column) {
        super(opcode,line,column);
        oneByte = true;
        this.expr = bit;
        this.opcode += reg;
        old_opcode = opcode; //this.opcode;
        bitInstr = true;
    }
    /// compile time ///

    public void pass1(IMessageReporter rep) {}

    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        int val = expr.getValue();
        if (oneByte && (Expression.getSize(val) > 1))
            throw new Exception("[" + line + "," + column + "] Error:" +
                    " value too large");
     //   opcode = old_opcode;
        if (old_opcode == JR) {
            val = (val-2)&0xff;
        }
        if (bitInstr) {
            if ((val > 7) || (val < 0))
                throw new Exception("[" + line + "," + column + "] Error:" +
                        " value can be only in range 0-7");
            opcode += (8*val);
        } else {
            if (oneByte) opcode += Expression.reverseBytes(val,1);
            else opcode += Expression.reverseBytes(val,2);
        }
        return addr_start + getSize();
    }

    // this can be only mvi instr
    public void pass4(HEXFileHandler hex) throws Exception {
        String s;
        if (getSize() == 1) s = "%1$02X";
        else if (getSize() == 2) s = "%1$04X";
        else if (getSize() == 3) s = "%1$06X";
        else s = "%1$08X";
        hex.putCode(String.format(s,opcode));
    }
    
}
