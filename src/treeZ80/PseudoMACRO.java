/*
 * PseudoMACRO.java
 *
 * Created on Sobota, 2007, september 29, 13:44
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.Namespace;
import java.util.Vector;
import plugins.compiler.IMessageReporter;
import treeZ80Abstract.Expression;
import treeZ80Abstract.Pseudo;

/**
 *
 * @author vbmacher
 */
public class PseudoMACRO extends Pseudo {
    private Vector<String> params; // macro parameters
    private Vector<Expression> call_params; // concrete parameters, they can change
    private Program subprogram;
    private String mnemo;
    
    /** Creates a new instance of PseudoMACRO */
    public PseudoMACRO(String name, Vector<String> params, Program s, int line,
            int column) {
        super(line,column);
        this.mnemo = name;
        if (params == null) this.params = new Vector<String>();
        else this.params = params;
        this.subprogram = s;
    }
    
    public String getName() { return mnemo; }
    
    public void setCallParams(Vector<Expression> params) { this.call_params = params; }

    /// compile time /// 
    public int getSize() { return 0; }
    public int getStatSize() { return subprogram.getSize(); }
    
    public void pass1(IMessageReporter rep) throws Exception {
        subprogram.pass1(rep); // pass1 creates block symbol table (local for block)
    }
    
    // for pass4
    private Namespace newEnv;
    // this is macro expansion ! can be called only in MacroCallPseudo class
    // call parameters have to be set
    public int pass2(Namespace env, int addr_start) throws Exception {
        newEnv = new Namespace();
        // add local statement env to newEnv
        subprogram.getNamespace().copyTo(newEnv);
        env.copyTo(newEnv); // add parent statement env to newEnv
        // remove all existing definitions of params name (from level-up environment)
        for (int i = 0; i < params.size(); i++)
            newEnv.removeAllDefinitions(params.get(i));
        // check of call_params
        if (call_params == null) throw new Exception("[" + line + "," + column
                + "] Error: Unknown macro parameters");
        if (call_params.size() != params.size())
            throw new Exception("[" + line + "," + column 
                    + "] Error: Incorrect macro paramers count");
        // create/rewrite symbols => parameters as equ pseudo instructions
        for (int i = 0; i < params.size(); i++) {
            newEnv.addEquDef(new PseudoEQU(params.get(i),
                    call_params.get(i),line,column));
        }
        return subprogram.pass2(newEnv, addr_start);
    }

    
    public void pass4(HEXFileHandler hex) throws Exception {
        subprogram.pass4(hex,newEnv);
    }

}
