/*
 * IfPseudoNode.java
 *
 * Created on Sobota, 2007, september 29, 13:39
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package assembler8080.tree8080;

import assembler8080.impl.HEXFileHandler;
import assembler8080.impl.NeedMorePassException;
import assembler8080.impl.compileEnv;
import assembler8080.tree8080Abstract.ExprNode;
import assembler8080.tree8080Abstract.PseudoNode;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
public class IfPseudoNode extends PseudoNode {
    private ExprNode expr;
    private Statement stat;
    private boolean condTrue; // for pass4
    
    /** Creates a new instance of IfPseudoNode */
    public IfPseudoNode(ExprNode expr, Statement stat, int line, int column) {
        super(line,column);
        this.expr = expr;
        this.stat = stat;
        this.condTrue = false;
    }
    
    // if doesnt have and id
    public String getName() { return ""; }

    /// compile time ///
    
    public int getSize() {
        if (expr.getValue() != 0) return stat.getSize();
        else return 0;
    }

    public void pass1(IMessageReporter r) throws Exception {
        stat.pass1(r);
    }
    
    public int pass2(compileEnv env, int addr_start) throws Exception {
        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return stat.pass2(env, addr_start);
            }
            else return addr_start;
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] IF expression can't be ambiguous");
        }
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        if (condTrue) stat.pass4(hex);
    }
}
