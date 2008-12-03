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
import impl.Namespace;
import java.util.Vector;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
public class Program {
    private Vector list; // all instructions
    private Namespace namespace; // compile-time environment
    private Vector<String> includefiles; // list of files that
                                         // were checked for include-loops
                                         // in short: list of included files
    
    public Program() { 
        list = new Vector();
        namespace = new Namespace();
        includefiles = new Vector<String>();
    }

    public void addIncludeFiles(Vector<String> inclfiles) {
        includefiles.addAll(inclfiles);
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
    public Namespace getNamespace() { return namespace; }
    
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
        Row in;
        for (i = 0; i < list.size(); i++) {
            in = (Row)list.get(i);
            if (in.getIncludeLoops(filename) == true)
                return true;
        }
        return false;
    } 
    
    public void pass1(Namespace namespace,IMessageReporter r) throws Exception { 
        this.namespace = namespace; 
        pass1(r); 
    }
    
    // creates symbol table
    // return next current address
    public void pass1(IMessageReporter r) throws Exception {
        int i = 0;
        Row in;
        // only labels and macros have right to be all added to symbol table at once
        for (i = 0; i < list.size(); i++) {
            in = (Row)list.get(i);
            if (in.label != null)
                if (namespace.addLabelDef(in.label) == false)
                    throw new Exception("Error: Label already defined: " + in.label.getName());
            if ((in.statement != null) && (in.statement instanceof PseudoMACRO))
                if (namespace.addMacroDef((PseudoMACRO)in.statement) == false)
                    throw new Exception("Error: Macro already defined: " 
                            + ((PseudoMACRO)in.statement).getName());
            if ((in.statement != null) && (in.statement instanceof PseudoINCLUDE))
                in.pass1(r, includefiles, namespace);
            else
                in.pass1(r);
        }
    }
    
    // pass2 tries to evaulate all expressions and compute relative addresses
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
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
        return this.pass2(namespace,addr_start);
    }
    
    public boolean pass3(Namespace parentEnv) throws Exception {
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
    public void pass4(HEXFileHandler hex,Namespace env) throws Exception {
        this.namespace = env;
        pass4(hex);
    }
}
