/*
 * Row.java
 *
 * Created on Streda, 2008, august 13, 11:25
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */
package treeZ80;

import impl.HEXFileHandler;
import impl.NeedMorePassException;
import impl.compileEnv;
import treeZ80Abstract.Statement;

/**
 *
 * @author vbmacher
 */
public class Row {
    protected Label label;
    protected Statement codePseudo;
    private int current_address; // its computed in pass2
    
    public Row(Label label, Statement codePseudo) {
        this.label = label;
        this.codePseudo = codePseudo;
    }
    
    public Row(Statement codePseudo) {
        this.label = null;
        this.codePseudo = codePseudo;
    }
    

    /// compile time ///
    public int getSize() { 
        if (codePseudo !=null) return codePseudo.getSize();
        else return 0;
    }
    
    // do pass1 for all elements
    public void pass1() throws Exception {
        if (codePseudo != null) codePseudo.pass1();
    }
    
    public int pass2(compileEnv prev_env, int addr_start) throws Exception {
        this.current_address = addr_start;
        if (label != null) label.setAddress(new Integer(addr_start));
        // pass2 pre definiciu makra nemozem volat. ide totiz o samotnu expanziu
        // makra. preto pass2 mozem volat az pri samotnom volani makra (pass2 triedy
        // MacroCallPseudo)
        if (codePseudo != null) 
            if ((codePseudo instanceof PseudoMACRO) == false)
                addr_start = codePseudo.pass2(prev_env, addr_start);
        return addr_start;
    }
    
    public int getCurrentAddress() { return this.current_address; }
    
    public boolean pass3(compileEnv env) throws Exception {
        try {
            if (codePseudo != null)
                codePseudo.pass2(env,this.current_address);
        } catch (NeedMorePassException e) {
            return false;
        }
        return true;
    }
    
    // code generation
    public void pass4(HEXFileHandler hex) throws Exception {
        if (codePseudo != null) 
            if ((codePseudo instanceof PseudoMACRO) == false)
                codePseudo.pass4(hex);
    }

}
