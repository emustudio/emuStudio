/*
 * Identifier.java
 *
 * Created on Streda, 2007, október 10, 15:50
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.NeedMorePassException;
import impl.compileEnv;
import treeZ80Abstract.Expression;

/**
 *
 * @author vbmacher
 */
public class Identifier extends Expression {
    private String name;
    
    /** Creates a new instance of Identifier */
    public Identifier(String name) {
        this.name = name;
    }

    /// compile time ///
    public int getSize() { return 0; }

    public int eval(compileEnv env, int curr_addr) throws Exception {
        // identifier in expression can be only label, equ, or set statement. macro NOT
        // search in env for labels
        Label lab = env.getLabel(this.name);
        if ((lab != null) && (lab.getAddress() == null))
            throw new NeedMorePassException(this, lab.getLine(), lab.getColumn());
        else if (lab != null) {
            this.value = lab.getAddress();
            return this.value;
        }

        PseudoEQU equ = env.getEqu(this.name);
        if (equ != null) {
            this.value = equ.getValue();
            return this.value;
        }
        
        PseudoVAR set = env.getVar(this.name);
        if (set != null) {
            this.value = set.getValue();
            return this.value;
        }
        else
            throw new Exception("Unknown identifier (" + this.name + ")");
    }
    
}
