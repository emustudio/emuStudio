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

package assembler8080.tree8080;

import assembler8080.impl.HEXFileHandler;
import assembler8080.impl.NeedMorePassException;
import assembler8080.impl.compileEnv;

import java.util.Vector;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
public class Statement {
    private Vector<InstructionNode> list; // all instructions
    private Vector<String> includefiles; // list of files that
                                         // were checked for include-loops
                                         // in short: list of included files

    public Statement() { 
        list = new Vector<InstructionNode>();
        this.env = new compileEnv();
        includefiles = new Vector<String>();
    }
    
    public void addElement(InstructionNode node) {
        list.addElement(node);
    }
    
    public void addVector(Vector<InstructionNode> vec) {
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
    
    public void pass1(compileEnv env,IMessageReporter r) throws Exception { this.env = env; pass1(r); }
    // creates symbol table
    // return next current address
    public void pass1(IMessageReporter r) throws Exception {
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
            if ((in.codePseudo != null) && (in.codePseudo instanceof IncludePseudoNode))
                in.pass1(r, includefiles, env);
            else
                in.pass1(r);
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

    /**
     * Method check whether this "subprogram" contains include
     * pseudocode(s) and if yes, whether the statement calls for
     * filename given by parameter.
     * @param filename name of the file that "include" pseudocode should contain
     * @return true if subprogram contains "include filename" pseudocode
     */
    public boolean getIncludeLoops(String filename) {
        int i;
        for (i = 0; i < includefiles.size(); i++) {
            String s = includefiles.elementAt(i);
            if (s.equals(filename)) return true;
        }
        includefiles.add(filename);
        InstructionNode in;
        for (i = 0; i < list.size(); i++) {
            in = (InstructionNode)list.get(i);
            if (in.getIncludeLoops(filename) == true)
                return true;
        }
        return false;
    }

    public void addIncludeFiles(Vector<String> inclfiles) {
        includefiles.addAll(inclfiles);
    }

}
