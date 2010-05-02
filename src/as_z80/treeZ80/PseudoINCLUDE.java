/*
 * PseudoINCLUDE.java
 *
 * Created on 14.8.2008, 9:27:10
 * hold to: KISS, YAGNI
 *
 */

package as_z80.treeZ80;

import as_z80.impl.HEXFileHandler;
import as_z80.impl.Namespace;
import as_z80.impl.lexerZ80;
import as_z80.impl.parserZ80;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import plugins.compiler.IMessageReporter;
import as_z80.treeZ80Abstract.Pseudo;

/**
 *
 * @author vbmacher
 */
public class PseudoINCLUDE extends Pseudo {
    private String filename;
    private String shortFileName;
    private Program program;
    private Namespace namespace;
    
    private class MRep implements IMessageReporter {
        private IMessageReporter old;
        public MRep(IMessageReporter r) {
            old = r;
        }
        public void report(String message, int type) {
            old.report(shortFileName + ": " + message, type);
        }
        public void report(int row, int col, String message, int type) {
            old.report(row,col,shortFileName + ": " + message, type);
        }
    }
    
    public PseudoINCLUDE(String filename, int line, int column) {
        super(line,column);
        this.filename = filename;
        this.shortFileName = new File(filename).getName();
    }
    
    public int getSize() { return program.getSize(); }

    /**
     * Method compare filename (in the include statement)
     * with filename given by the parameter
     * @return true if filenames equal, false if not
     */
    public boolean isEqualName(String filename) {
        File f1 = new File(this.filename);
        File f2 = new File(filename);
        String ff1 = f1.getAbsolutePath();
        String ff2 = f2.getAbsolutePath();
        
        if (ff1.equals(ff2)) return true;
        else return false;
    }
    
    public void pass1(IMessageReporter r) throws Exception {}
    public void pass1(IMessageReporter r, Vector<String> includefiles,
            Namespace parent) throws Exception {
        try {
            MRep rep = new MRep(r);
            FileReader f = new FileReader(new File(filename));
            lexerZ80 lex = new lexerZ80(f);
            parserZ80 par = new parserZ80(lex, rep);
            
            Object s = par.parse().value;
            if (s == null) 
                throw new Exception("[" + line + "," + column + "] "+
                        "Error: Unexpected end of file (" + shortFileName + ")");
            program = (Program)s;
            program.addIncludeFiles(includefiles);
            namespace = parent;
            
            if (program.getIncludeLoops(filename))
                throw new Exception("[" + line + "," + column + "] "+
                        "Error: Infinite INCLUDE loop (" + shortFileName + ")");
            program.pass1(namespace,rep); // create symbol table
        } catch (IOException e) {
            throw new Exception(shortFileName + ": I/O Error");
        } catch (Exception e) {
            throw new Exception("[" + line + "," + column + "] "+
                    e.getMessage());
        }
    }
  

    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
    	// try to evaluate all expressions + compute relative addresses
    	return program.pass2(addr_start); 
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        while (program.pass3(namespace) == true) ;
        if (namespace.getPassNeedCount() != 0)
            throw new Exception("Error: can't evaulate all expressions");
        program.pass4(hex);
    }

}
