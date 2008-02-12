/*
 * compiler8080.java
 *
 * Created on Piatok, 2007, august 10, 8:22
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package compiler8080;
import plugins.compiler.*;
import tree8080.*;

import java.util.*;

/**
 *
 * @author vbmacher
 */
public class compiler8080 implements ICompiler {
    private lexer8080 lex;
    private parser8080 par;
    private IMessageReporter reporter;
    
    /** Creates a new instance of compiler8080 */
    public compiler8080() {}
    
    // create lexer and parser, return lexer
    public ILexer getLexer(java.io.Reader in, IMessageReporter reporter) { 
        lex = new lexer8080(in);
        par = new parser8080(lex, reporter);
        this.reporter = reporter;
        return lex;
    }
    
    private void print_text(String mes) {
        if (reporter != null) reporter.reportMessage(mes);
        else System.out.println(mes);
    }
    
    @SuppressWarnings("empty-statement")
    public boolean compile(String filename) {
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
        if (parser8080.errorCount != 0)
            return false;
        
        // do several passes for compiling
        try {
            Statement stat = (Statement)s;
            compileEnv env = new compileEnv();
            stat.pass1(env); // create symbol table
            stat.pass2(0); // try to evaulate all expressions + compute relative addresses
            while (stat.pass3(env) == true) ;
            if (env.getPassNeedCount() != 0) {
                print_text("Error: can't evaulate all expressions");
                return false;
            }
            stat.pass4(hex,env);
            hex.generateFile(filename);
        } catch(Exception e) {
            print_text(e.getMessage());
            return false;
        }
        print_text("Compile was sucessfull. Output: " + filename);
        return true;
    }
    
    public String getName() { return "Intel 8080 Compiler"; }
    public String getVersion() { return "0.1b"; }
    public String getCopyright() { return "\u00A9 Copyright 2007, Peter Jakubčo"; }
    public String getDescription() {
        return "It is a clone of original Intel's assembler. For syntax look"
                + " at users manual.";
    }
    
    public void destroy() {}

}
