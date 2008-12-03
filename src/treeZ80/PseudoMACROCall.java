/*
 * PseudoMACROCall.java
 *
 * Created on Pondelok, 2007, október 8, 17:03
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.NeedMorePassException;
import impl.Namespace;
import java.util.Vector;
import plugins.compiler.IMessageReporter;
import treeZ80Abstract.Expression;
import treeZ80Abstract.Pseudo;

/**
 *
 * @author vbmacher
 */
public class PseudoMACROCall extends Pseudo {
    private Vector params; // vector of expressions
    private PseudoMACRO macro; // only pointer...
    private HEXFileHandler statHex; // hex file for concrete macro
    private String mnemo;
    
    /** Creates a new instance of PseudoMACROCall */
    public PseudoMACROCall(String name, Vector params, int line, int column) {
        super(line, column);
        this.mnemo = name;
        if (params == null) this.params = new Vector();
        else this.params = params;
        statHex = new HEXFileHandler();
    }

    /// compile time ///
    
    public int getSize() {
        return macro.getStatSize();
    }

    public void pass1(IMessageReporter rep) {}
    
    
    // this is a call for expanding a macro
    // also generate code for pass4
    public int pass2(Namespace env, int addr_start) throws Exception {
        // first find a macro
        this.macro = env.getMacro(this.mnemo); 
        if (macro == null)
            throw new Exception("[" + line + "," + column
                    + "] Error: Undefined macro: " + this.mnemo);
        // do pass2 for expressions (real macro parameters)
        try {
            for (int i = 0; i < params.size(); i++)
                ((Expression)params.get(i)).eval(env, addr_start);
            macro.setCallParams(params);
            int a = macro.pass2(env, addr_start);
            statHex.setNextAddress(addr_start);
            macro.pass4(statHex); // generate code for concrete macro
            return a;
        } catch(NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] Error: MACRO expression can't be ambiguous");
        }
    }

    public void pass4(HEXFileHandler hex) {
        hex.addTable(statHex.getTable());
    }
    
}
