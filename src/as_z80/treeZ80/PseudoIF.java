/*
 * PseudoIF.java
 *
 * Created on Sobota, 2007, september 29, 13:39
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package as_z80.treeZ80;

import as_z80.impl.HEXFileHandler;
import as_z80.impl.NeedMorePassException;
import as_z80.impl.Namespace;
import plugins.compiler.IMessageReporter;
import as_z80.treeZ80Abstract.Expression;
import as_z80.treeZ80Abstract.Pseudo;

/**
 *
 * @author vbmacher
 */
public class PseudoIF extends Pseudo {
    private Expression expr;
    private Program subprogram;
    private boolean condTrue; // => for pass4; if this is true,
                              // then generate code, otherwise not.
    
    /** Creates a new instance of PseudoIF */
    public PseudoIF(Expression expr, Program stat, int line, int column) {
        super(line,column);
        this.expr = expr;
        this.subprogram = stat;
        this.condTrue = false;
    }

    /// compile time ///
    
    public int getSize() {
        if (condTrue) return subprogram.getSize();
        else return 0;
    }

    public void pass1(IMessageReporter rep) throws Exception {
        subprogram.pass1(rep);
    }
    
    public int pass2(Namespace env, int addr_start) throws Exception {
        // now evaluate expression and then decide if block can be passed
        try {
            if (expr.eval(env, addr_start) != 0) {
                condTrue = true;
                return subprogram.pass2(env, addr_start);
            }
            else return addr_start;
        } catch (NeedMorePassException e) {
            throw new Exception("[" + line + "," + column
                    + "] Error: IF expression can't be ambiguous");
        }
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        if (condTrue) subprogram.pass4(hex);
    }
}
