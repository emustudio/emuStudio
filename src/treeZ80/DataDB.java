/*
 * DBData.java
 *
 * Created on Streda, 2008, august 13, 11:46
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.compileEnv;
import treeZ80Abstract.DataValue;
import treeZ80Abstract.Expression;
import treeZ80Abstract.Instruction;

/**
 *
 * @author vbmacher
 */
public class DataDB extends DataValue {
    private Expression expression = null;
    private String literalString = null;
    private Instruction opcode = null;
    
    /** Creates a new instance of DBData */
    public DataDB(Expression expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }
    
    public DataDB(String literalString, int line, int column) {
        super(line,column);
        this.literalString = literalString;
    }
    
    public DataDB(Instruction opcode, int line, int column) {
        super(line,column);
        this.opcode = opcode;
    }
 
    public String getDataType() { return "db"; }
    
    /// compile time ///
    public int getSize() { 
        if (expression != null) return 1;
        else if (literalString != null) return literalString.length();
        else if (opcode != null) return opcode.getSize();
        return 0;
    }
    
    public void pass1() throws Exception {
        if (opcode != null) opcode.pass1();
    }
    
    public int pass2(compileEnv env, int addr_start) throws Exception {
        if (expression != null) {
            expression.eval(env,addr_start);
            return addr_start + 1;
        }
        else if (literalString != null) return addr_start + literalString.length();
        else if (opcode != null) return opcode.pass2(env,addr_start);
        return addr_start;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        if (expression != null) {
            if (expression.encodeValue(true).length() > 2)
                throw new Exception("[" + line + "," + column + "] value too large");
            hex.putCode(expression.encodeValue(true));
        } else if (literalString != null)
            hex.putCode(this.encodeValue(literalString));
        else if (opcode != null) opcode.pass4(hex);
    }
}
