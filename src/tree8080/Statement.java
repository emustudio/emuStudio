/*
 * Statement.java
 *
 * Created on Piatok, 2007, september 21, 8:08
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 *
 *
 Instruction,Code,Data,DB_Data,DW_Data,Pseudocode,
        Opcode,Reg,CodePseudocode,LabelOptional,CommentOptional,DBDataIter,
        DWDataIter,MacroOperOptional,MacroOperIter,RegPairBD,RegPairBDHP,
        RegPairBDHS;
non terminal Integer Expression; 
 *
 *
 */

package tree8080;

import compiler8080.HEXFileHandler;
import compiler8080.NeedMorePassException;
import compiler8080.compileEnv;
import java.util.Vector;

/**
 *
 * @author vbmacher
 */
public class Statement {
    private Vector list; // all instructions
    
    public Statement() { 
        list = new Vector();
        this.env = new compileEnv();
    }
    
    public void addElement(InstructionNode node) {
        list.addElement(node);
    }
    
    public void addVector(Vector vec) {
        list.addAll(vec);
    }
    
    /// compile time ///
    
    public int getSize() { 
        InstructionNode in;
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            in = (InstructionNode)list.get(i);
            size += in.getSize();
        }
        return size;
    }
     
    /* PASS1 = symbol table
     * 1. get all label definitions
     * 2. get all macro definitions
     */
    private compileEnv env;
    
    public compileEnv getCompileEnv() { return env; }
    
    public void pass1(compileEnv env) throws Exception { this.env = env; pass1(); }
    // creates symbol table
    // return next current address
    public void pass1() throws Exception {
        int i = 0;
        InstructionNode in;
        // only labels and macros have right to be all added to symbol table at once
        for (i = 0; i < list.size(); i++) {
            in = (InstructionNode)list.get(i);
            if (in.label != null)
                if (env.addLabelDef(in.label) == false)
                    throw new Exception("Label already defined: " + in.label.getName());
            if ((in.codePseudo != null) && (in.codePseudo instanceof MacroPseudoNode))
                if (env.addMacroDef((MacroPseudoNode)in.codePseudo) == false)
                    throw new Exception("Macro already defined: " 
                            + ((MacroPseudoNode)in.codePseudo).getName());
            in.pass1();
        }
    }
    
    // pass2 tries to evaulate all expressions and compute relative addresses
    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        int curr_addr = addr_start;
        for (int i = 0; i < list.size(); i++) {
            InstructionNode in = (InstructionNode)list.get(i);
            try  {
                curr_addr = in.pass2(parentEnv, addr_start);
                addr_start = curr_addr;
            } catch (NeedMorePassException e) {
                parentEnv.addPassNeed(in);
                addr_start += in.getSize();
            } 
        }
        return addr_start;
    }

    public int pass2(int addr_start) throws Exception {
        return this.pass2(env,addr_start);
    }
    
    public boolean pass3(compileEnv parentEnv) throws Exception {
        int pnCount = parentEnv.getPassNeedCount();
        for (int i = parentEnv.getPassNeedCount()-1; i >=0 ; i--) {
            if (parentEnv.getPassNeed(i).pass3(parentEnv) == true) {
                pnCount--;
                parentEnv.removePassNeed(i);
            }
        }
        if (pnCount < parentEnv.getPassNeedCount()) return true;
        else return false;
    }
    
    public void pass4(HEXFileHandler hex) throws Exception {
        for (int i = 0; i < list.size(); i++) {
            InstructionNode in = (InstructionNode)list.get(i);
            in.pass4(hex);
        }
    }
    public void pass4(HEXFileHandler hex,compileEnv env) throws Exception {
        this.env = env;
        pass4(hex);
        
    }
}
