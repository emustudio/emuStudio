/*
 * Program.java
 *
 * Created on Streda, 2008, august 13, 11:19
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 */

package treeZ80;

import impl.HEXFileHandler;
import impl.NeedMorePassException;
import impl.compileEnv;
import java.util.Vector;

/**
 *
 * @author vbmacher
 */
public class Program {
    private Vector list; // all instructions
    private compileEnv env; // compile-time environment
    
    public Program() { 
        list = new Vector();
        this.env = new compileEnv();
    }
    
    /**
     * Adds one row into program
     */
    public void addRow(Row node) {
        list.addElement(node);
    }
    
    /**
     * Adds several rows into program
     */
    public void addRowsVector(Vector vec) {
        list.addAll(vec);
    }
    
    /// compile time ///
    
    /**
     * Determine size in bytes for all elements in Program
     */
    public int getSize() { 
        Row in;
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            in = (Row)list.get(i);
            size += in.getSize();
        }
        return size;
    }
     
    /* PASS1 = symbol table
     * 1. get all label definitions
     * 2. get all macro definitions
     */
    public compileEnv getCompileEnv() { return env; }
    
    public void pass1(compileEnv env) throws Exception { 
        this.env = env; 
        pass1(); 
    }
    
    // creates symbol table
    // return next current address
    public void pass1() throws Exception {
        int i = 0;
        Row in;
        // only labels and macros have right to be all added to symbol table at once
        for (i = 0; i < list.size(); i++) {
            in = (Row)list.get(i);
            if (in.label != null)
                if (env.addLabelDef(in.label) == false)
                    throw new Exception("Label already defined: " + in.label.getName());
            if ((in.codePseudo != null) && (in.codePseudo instanceof PseudoMACRO))
                if (env.addMacroDef((PseudoMACRO)in.codePseudo) == false)
                    throw new Exception("Macro already defined: " 
                            + ((PseudoMACRO)in.codePseudo).getName());
            in.pass1();
        }
    }
    
    // pass2 tries to evaulate all expressions and compute relative addresses
    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        int curr_addr = addr_start;
        for (int i = 0; i < list.size(); i++) {
            Row in = (Row)list.get(i);
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
            Row in = (Row)list.get(i);
            in.pass4(hex);
        }
    }
    public void pass4(HEXFileHandler hex,compileEnv env) throws Exception {
        this.env = env;
        pass4(hex);
        
    }
}
