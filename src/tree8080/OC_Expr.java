/*
 * OC_Expr.java
 *
 * Created on Sobota, 2007, september 29, 20:54
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
public class OC_Expr extends OpCodeNode {
    private ExprNode expr;
    
    /** Creates a new instance of OC_Expr */
    public OC_Expr(String mnemo, ExprNode expr, int line, int column) {
        super(mnemo,line, column);
        this.mnemo = mnemo;
        this.expr = expr;
    }

    /// compile time ///
    public int getSize() {
        int rval =0;
        if (mnemo.equals("sta")) rval = 3;
        else if (mnemo.equals("lda")) rval = 3;
        else if (mnemo.equals("shld")) rval = 3;
        else if (mnemo.equals("lhld")) rval = 3;
        else if (mnemo.equals("jmp")) rval = 3;
        else if (mnemo.equals("jc")) rval = 3;
        else if (mnemo.equals("jnc")) rval = 3;
        else if (mnemo.equals("jz")) rval = 3;
        else if (mnemo.equals("jnz")) rval = 3;
        else if (mnemo.equals("jm")) rval = 3;
        else if (mnemo.equals("jp")) rval =3;
        else if (mnemo.equals("jpe")) rval = 3;
        else if (mnemo.equals("jpo")) rval = 3;
        else if (mnemo.equals("call")) rval = 3;
        else if (mnemo.equals("cc")) rval = 3;
        else if (mnemo.equals("cnc")) rval = 3;
        else if (mnemo.equals("cz")) rval = 3;
        else if (mnemo.equals("cnz")) rval = 3;
        else if (mnemo.equals("cm")) rval = 3;
        else if (mnemo.equals("cp")) rval = 3;
        else if (mnemo.equals("cpe")) rval = 3;
        else if (mnemo.equals("cpo")) rval = 3;
        else if (mnemo.equals("cpe")) rval = 3;
        else if (mnemo.equals("rst")) rval = 1;
        else rval = 2;
        return rval;
    }
    
    public void pass1() {}

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        return (addr_start + this.getSize());
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        short opCode = 198; // opcode for adi: 11 (000adi) 110
        boolean oneDataByte = true; // how many data bytes
        boolean insertAfter = true; // if expression have to be written after opcode
        boolean found = false;
        String code = "";
        
        if (mnemo.equals("adi")) found = true;
        else if (mnemo.equals("aci")) { opCode |= 8; found = true; }
        else if (mnemo.equals("sui")) { opCode |= 16; found = true; }
        else if (mnemo.equals("sbi")) { opCode |= 24; found = true; }
        else if (mnemo.equals("ani")) { opCode |= 32; found = true; }
        else if (mnemo.equals("xri")) { opCode |= 40; found = true; }
        else if (mnemo.equals("ori")) { opCode |= 48; found = true; }
        else if (mnemo.equals("cpi")) { opCode |= 56; found = true; }
        else { opCode = 34; oneDataByte = false; }

        if (found == false) {
            if (mnemo.equals("shld")) found = true;
            else if (mnemo.equals("lhld")) { opCode |= 8; found = true; }
            else if (mnemo.equals("sta")) { opCode |= 16; found = true; }
            else if (mnemo.equals("lda")) { opCode |= 24; found = true; }
            else opCode = 194;
        }
        
        if (found == false) {
            if (mnemo.equals("jmp")) { opCode |= 1; found = true; }
            else if (mnemo.equals("jnz")) found = true;
            else if (mnemo.equals("jz")) { opCode |= 8; found = true; }
            else if (mnemo.equals("jnc")) { opCode |= 16; found = true; }
            else if (mnemo.equals("jc")) { opCode |= 24; found = true; }
            else if (mnemo.equals("jpo")) { opCode |= 32; found = true; }
            else if (mnemo.equals("jpe")) { opCode |= 40; found = true; }
            else if (mnemo.equals("jp")) { opCode |= 48; found = true; }
            else if (mnemo.equals("jm")) { opCode |= 56; found = true; }
            else opCode = 196;
        }
 
        if (found == false) {
            if (mnemo.equals("call")) { opCode |= 9; found = true; }
            else if (mnemo.equals("cnz")) found = true;
            else if (mnemo.equals("cz")) { opCode |= 8; found = true; }
            else if (mnemo.equals("cnc")) { opCode |= 16; found = true; }
            else if (mnemo.equals("cc")) { opCode |= 24; found = true; }
            else if (mnemo.equals("cpo")) { opCode |= 32; found = true; }
            else if (mnemo.equals("cpe")) { opCode |= 40; found = true; }
            else if (mnemo.equals("cp")) { opCode |= 48; found = true; }
            else if (mnemo.equals("cm")) { opCode |= 56; found = true; }
            else opCode = 199;
        }

        if ((found == false) && mnemo.equals("rst")) {
            int v = expr.getValue();
            if (v > 7) throw new Exception("[" + line + "," + column + "] value too large");
            opCode |= (expr.getValue() << 3);
            insertAfter = false; found = true;
        }
        if ((found == false) && mnemo.equals("in")) { opCode = 219; oneDataByte = true; found = true; }
        if ((found == false) && mnemo.equals("out")) { opCode = 211; oneDataByte = true; found = true;}
    
        code = String.format("%02X",opCode);
        if (insertAfter) {
            if (oneDataByte) {
                if (expr.getEncValue(true).length() > 2)
                    throw new Exception("[" + line + "," + column + "] value too large");
                code += expr.getEncValue(true);
            } else {
                code += expr.getEncValue(false);
            }
        }
        hex.putCode(code);
    }
}
