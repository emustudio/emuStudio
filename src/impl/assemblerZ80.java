/*
 * assemblerZ80.java
 *
 * Created on Piatok, 2007, august 10, 8:22
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package impl;

import java.io.Reader;

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
	private long hash;
    private lexerZ80 lex = null;
    private parserZ80 par;
    private IMessageReporter reporter;
    @SuppressWarnings("unused")
	private ISettingsHandler settings;
    private int programStart = 0; // actualize after compile 
    
    /** Creates a new instance of assemblerZ80 */
    public assemblerZ80(Long hash) {
    	this.hash = hash;
    	// lex has to be reset WITH a reader object before compile
    	lex = new lexerZ80((Reader)null);
    }
        
    private void print_text(String mes, int type) {
        if (reporter != null) reporter.report(mes, type);
        else System.out.println(mes);
    }
    
	@Override
    public String getTitle() { return "Z80 Assembler"; }
	@Override
    public String getVersion() { return "0.1b1"; }
	@Override
    public String getCopyright() { return "\u00A9 Copyright 2007-2008, Peter Jakubčo"; }
	@Override
    public String getDescription() {
        return "It is my version of Z80 assembler. For syntax look"
                + " at users manual.";
    }

	@Override
	public long getHash() { return hash; }
    
	@Override
	public boolean initialize(ISettingsHandler sHandler, IMessageReporter reporter) {
        this.settings = sHandler;
        this.reporter = reporter;

        par = new parserZ80(lex, reporter);
		return true;
	}

	@Override
    public void destroy() {}

	@Override
    public void reset() {}

	@Override
    public int getProgramStartAddress() {
        return programStart;
    }

	/**
	 * Compile the source code into HEXFileHadler
	 * 
	 * @param in  Reader object of the source code
	 * @return HEXFileHandler object
	 */
    private HEXFileHandler compile(Reader in) throws Exception {
        if (par == null) return null;
        if (in == null) return null;

        Object s = null;
        HEXFileHandler hex = new HEXFileHandler();

        print_text(getTitle()+", version "+getVersion(), IMessageReporter.TYPE_INFO);
        lex.reset(in,0,0,0);
        s = par.parse().value;

        if (s == null) {
            print_text("Unexpected end of file", IMessageReporter.TYPE_ERROR);
            return null;
        }
        if (parserZ80.errorCount != 0)
            return null;
        
        // do several passes for compiling
        Program program = (Program)s;
        Namespace env = new Namespace();
        program.pass1(env,reporter); // create symbol table
        program.pass2(0); // try to evaluate all expressions + compute relative addresses
        while (program.pass3(env) == true) ;
        if (env.getPassNeedCount() != 0) {
            print_text("Error: can't evaulate all expressions", IMessageReporter.TYPE_ERROR);
            return null;
        }
        program.pass4(hex,env);
        return hex;
    }
    
	@Override
	public boolean compile(String fileName, Reader in) {
        try {
    		HEXFileHandler hex = compile(in);
    		if (hex == null) return false;
			hex.generateFile(fileName);
	        print_text("Compile was sucessfull. Output: " + fileName, IMessageReporter.TYPE_INFO);
	        programStart = hex.getProgramStart();
	        return true;
		} catch (Exception e) {
            print_text(e.getMessage(), IMessageReporter.TYPE_ERROR);
            return false;
		}
	}

	@Override
	public boolean compile(String fileName, Reader in, IMemoryContext mem) {
        try {
    		HEXFileHandler hex = compile(in);
			hex.generateFile(fileName);
	        print_text("Compile was sucessfull. Output: " + fileName, IMessageReporter.TYPE_INFO);
	        programStart = hex.getProgramStart();
	        boolean r = hex.loadIntoMemory(mem);
	        if (r) print_text("Compiled file was loaded into operating memory.", IMessageReporter.TYPE_INFO);
	        else print_text("Compiled file couldn't be loaded into operating"
	                + "memory due to an error.",IMessageReporter.TYPE_ERROR);
	        return true;
		} catch (Exception e) {
            print_text(e.getMessage(), IMessageReporter.TYPE_ERROR);
            return false;
		}
	}

	@Override
	public ILexer getLexer(Reader in) { return new lexerZ80(in); }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
	}

}
