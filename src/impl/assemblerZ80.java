/*
 * assemblerZ80.java
 *
 * Created on Piatok, 2007, august 10, 8:22
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package impl;

import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.compiler.ILexer;
import plugins.compiler.IMessageReporter;
import plugins.memory.IMemoryContext;
import treeZ80.Program;

/**
 *
 * @author vbmacher
 */
public class assemblerZ80 implements ICompiler {
    private lexerZ80 lex;
    private parserZ80 par;
    private IMessageReporter reporter;
    private ISettingsHandler settings;
    private int programStart = 0; // actualize after compile 
    
    /** Creates a new instance of assemblerZ80 */
    public assemblerZ80() {}
    
    // create lexer and parser, return lexer
    public ILexer getLexer(java.io.Reader in, IMessageReporter reporter) { 
        lex = new lexerZ80(in);
        par = new parserZ80(lex, reporter);
        this.reporter = reporter;
        return lex;
    }
    
    private void print_text(String mes) {
        if (reporter != null) reporter.report(mes);
        else System.out.println(mes);
    }
    
    public String getName() { return "Z80 Assembler"; }
    public String getVersion() { return "0.1b"; }
    public String getCopyright() { return "\u00A9 Copyright 2007-2008, Peter Jakubčo"; }
    public String getDescription() {
        return "It is my version of Z80 assembler. For syntax look"
                + " at users manual.";
    }
    
    public void destroy() {}

    public void initialize(ISettingsHandler sHandler) {
        this.settings = sHandler;
    }

    @SuppressWarnings("empty-statement")
    public boolean compile(String fileName, IMemoryContext mem, boolean use_mem) {
        if (par == null) return false;

        Object s = null;
        HEXFileHandler hex = new HEXFileHandler();

        print_text(getName()+", version "+getVersion());
        try { s = par.parse().value; }
        catch(Exception e) {
            print_text(e.getMessage());
            return false;
        }
        if (s == null) {
            print_text("Unexpected end of file");
            return false;
        }
        if (parserZ80.errorCount != 0)
            return false;
        
        // do several passes for compiling
        try {
            Program program = (Program)s;
            Namespace env = new Namespace();
            program.pass1(env,reporter); // create symbol table
            program.pass2(0); // try to evaulate all expressions + compute relative addresses
            while (program.pass3(env) == true) ;
            if (env.getPassNeedCount() != 0) {
                print_text("Error: can't evaulate all expressions");
                return false;
            }
            program.pass4(hex,env);
            hex.generateFile(fileName);
        } catch(Exception e) {
            print_text(e.getMessage());
            return false;
        }
        print_text("Compile was sucessfull. Output: " + fileName);
        
        if (use_mem) {
            boolean r = hex.loadIntoMemory(mem);
            if (r) print_text("Compiled file was loaded into operating memory.");
            else print_text("Compiled file couldn't be loaded into operating"
                    + "memory due to an error.");
        }
        programStart = hex.getProgramStart();
        return true;
    }

    public void reset() {}

    public int getProgramStartAddress() {
        return programStart;
    }

}
