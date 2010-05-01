/*
 * MacroCallPseudo.java
 *
 * Created on Pondelok, 2007, október 8, 17:03
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_8080.tree8080;

import as_8080.impl.HEXFileHandler;
import as_8080.impl.NeedMorePassException;
import as_8080.impl.compileEnv;
import as_8080.tree8080Abstract.ExprNode;
import as_8080.tree8080Abstract.PseudoNode;

import java.util.Vector;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
public class MacroCallPseudo extends PseudoNode {
    private Vector<ExprNode> params; // vector of expressions
    private MacroPseudoNode macro; // only pointer...
    private HEXFileHandler statHex; // hex file for concrete macro
    private String mnemo;
    
    /** Creates a new instance of MacroCallPseudo */
    public MacroCallPseudo(String name, Vector<ExprNode> params, int line, int column) {
        super(line, column);
        this.mnemo = name;
        if (params == null) this.params = new Vector<ExprNode>();
        else this.params = params;
        statHex = new HEXFileHandler();
    }

    /// compile time ///
    
    public int getSize() {
        return macro.getStatSize();
    }

    public void pass1(IMessageReporter r) {}
    
    
    // this is a call for expanding a macro
    // also generate code for pass4
    public int pass2(compileEnv env, int addr_start) throws Exception {
        // first find a macro
        this.macro = env.getMacro(this.mnemo); 
        if (macro == null)
            throw new Exception("[" + line + "," + column
                    + "] Undefined macro: " + this.mnemo);
        // do pass2 for expressions (real macro parameters)
        try {
            for (int i = 0; i < params.size(); i++)
                params.get(i).eval(env, addr_start);
            macro.setCallParams(params);
            int a = macro.pass2(env, addr_start);
            statHex.setNextAddress(addr_start);
            macro.pass4(statHex); // generate code for concrete macro
            return a;
        } catch(NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] MACRO expression can't be ambiguous");
        }
    }

    public String getName() { return this.mnemo; }

    public void pass4(HEXFileHandler hex) {
        hex.addTable(statHex.getTable());
    }
    
}
