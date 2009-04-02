/**
 * RAMCompiler.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package ram.impl;

import java.io.Reader;

import ram.compiled.CompiledFileHandler;
import ram.tree.Program;
import runtime.StaticDialogs;
import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.compiler.ILexer;
import plugins.compiler.IMessageReporter;
import plugins.memory.IMemoryContext;

public class RAMCompiler implements ICompiler {
    private long hash;
    private RAMLexer lex = null;
    private RAMParser par;
    private IMessageReporter reporter;
    @SuppressWarnings("unused")
    private ISettingsHandler settings;

    public RAMCompiler(Long hash) {
        this.hash = hash;
        // lex has to be reset WITH a reader object before compile
        lex = new RAMLexer((Reader)null);
    }
        
    private void print_text(String mes, int type) {
        if (reporter != null) reporter.report(mes, type);
        else System.out.println(mes);
    }

	@Override
	public String getTitle() { return "RAM compiler"; }

	@Override
	public String getCopyright() { return "\u00A9 Copyright 2009, P. Jakubčo"; }

	@Override
	public String getVersion() { return "0.12b"; }

	@Override
	public String getDescription() {
		return "This is a compiler of Random Access Machine. It uses syntax" +
				"and semantics of instructions that is used in the book:\n\n" +
				"\"HUDÁK, Š.: Strojovo orientované jazyky, ISBN 80-969071-3-1\".";
	}

    /**
     * Compile the source code into HEXFileHadler
     * 
     * @param in  Reader object of the source code
     * @return HEXFileHandler object
     */
    private CompiledFileHandler compile(Reader in) throws Exception {
        if (par == null) return null;
        if (in == null) return null;

        Object s = null;
        CompiledFileHandler hex = new CompiledFileHandler();

        print_text(getTitle()+", version "+getVersion(), IMessageReporter.TYPE_INFO);
        lex.reset(in,0,0,0);
        s = par.parse().value;

        if (s == null) {
            print_text("Unexpected end of file", IMessageReporter.TYPE_ERROR);
            return null;
        }
        if (par.errorCount != 0)
            return null;
        
        // do several passes for compiling
        Program program = (Program)s;
        program.pass1(0);
        program.pass2(hex);

        return hex;
    }
	
	@Override
	public boolean compile(String fileName, Reader reader) {
		StaticDialogs.showErrorMessage("This compiler doesn't support " +
				"compilation into a file.");
        return false;
	}

	@Override
	public boolean compile(String fileName, Reader reader, IMemoryContext mem) {
        try {
            CompiledFileHandler hex = compile(reader);
            print_text("Compile was sucessfull.", IMessageReporter.TYPE_INFO);
            boolean r = hex.loadIntoMemory(mem);
            if (r)
                print_text("Compiled file was loaded into operating memory.",
                        IMessageReporter.TYPE_INFO);
            else
                print_text("Compiled file couldn't be loaded into operating"
                    + " memory due to an error.",IMessageReporter.TYPE_ERROR);
            return true;
        } catch (Exception e) {
        	e.printStackTrace();
        	String h = e.getLocalizedMessage();
        	if (h == null || h.equals("")) h = "Unknown error";
            print_text(h, IMessageReporter.TYPE_ERROR);
            return false;
        }
	}

	@Override
	public ILexer getLexer(Reader reader) {
		return new RAMLexer(reader);
	}

	@Override
	public int getProgramStartAddress() {
		return 0;
	}

	@Override
	public boolean initialize(ISettingsHandler settings, IMessageReporter reporter) {
        this.settings = settings;
        this.reporter = reporter;

        par = new RAMParser(lex, reporter);
        return true;
	}

	@Override
	public void destroy() {	
		reporter = null;
		settings = null;
		par = null;
		lex = null;
	}

	@Override
	public long getHash() { return hash; }

	@Override
	public void reset() { }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
	}

}
