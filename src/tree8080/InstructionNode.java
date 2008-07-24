package tree8080;

import compiler8080.HEXFileHandler;
import compiler8080.NeedMorePassException;
import compiler8080.compileEnv;
import tree8080Abstract.CodePseudoNode;

/*
 * Instruction.java
 *
 * Created on Piatok, 2007, september 21, 8:12
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

/**
 *
 * @author vbmacher
 */
public class InstructionNode {
    protected LabelNode label;
    protected CodePseudoNode codePseudo;
    private int current_address; // its computed in pass2
    
    public InstructionNode(LabelNode label, CodePseudoNode codePseudo) {
        this.label = label;
        this.codePseudo = codePseudo;
    }
    
    public InstructionNode(CodePseudoNode codePseudo) {
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
            if ((codePseudo instanceof MacroPseudoNode) == false)
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
            if ((codePseudo instanceof MacroPseudoNode) == false)
                codePseudo.pass4(hex);
    }

}
