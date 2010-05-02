/*
 * Assembler8080.java
 *
 * Created on Piatok, 2007, august 10, 8:22
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package as_8080.impl;

import as_8080.tree8080.Statement;

import java.io.Reader;

import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.compiler.ILexer;
import plugins.compiler.IMessageReporter;
import plugins.memory.IMemoryContext;


/**
 * Main implementation class of the plugin (assembler for 8080 processor).
 *
 * @author Peter Jakubčo <pjakubco at gmail.com>
 */
public class Assembler8080 implements ICompiler {
	private long hash;
    private lexer8080 lex;
    private parser8080 par;
    private IMessageReporter reporter;
    @SuppressWarnings("unused")
	private ISettingsHandler settings;
    private int programStart = 0; // actualize after compile 
    
    /** Creates a new instance of compiler8080 */
    public Assembler8080(Long hash) {
    	this.hash = hash;
    	lex = new lexer8080((Reader)null);
    }
    
    private void print_text(String mes, int type) {
        if (reporter != null) reporter.report(mes,type);
        else System.out.println(mes);
    }
    
    @Override
    public String getTitle() { return "Intel 8080 Assembler"; }
    @Override
    public String getVersion() { return "0.29b"; }
    @Override
    public String getCopyright() { return "\u00A9 Copyright 2007-2009, P.Jakubčo"; }
    @Override
    public String getDescription() {
        return "Light modified clone of original Intel's assembler. For syntax look"
                + " at users manual.";
    }
    
	@Override
	public long getHash() {
		return hash;
	}

    @Override
    public boolean initialize(ISettingsHandler sHandler, IMessageReporter reporter) {
        this.settings = sHandler;
        par = new parser8080(lex, reporter);
        this.reporter = reporter;
        return true;
    }
    @Override
    public void reset() {}

    @Override
    public int getProgramStartAddress() {
        return programStart;
    }

    @Override
    public ILexer getLexer(Reader in) { 
        return new lexer8080(in);
    }

    @Override
    public void destroy() {}

	/**
	 * Compile the source code into HEXFileHadler
	 * 
	 * @return HEXFileHandler object
	 */
    public HEXFileHandler compile(Reader in) throws Exception {
        if (par == null) return null;
        if (in == null) return null;

        Object s = null;
        HEXFileHandler hex = new HEXFileHandler();

        print_text(getTitle()+", version "+getVersion(), IMessageReporter.TYPE_INFO);
        lex.reset(in, 0, 0, 0);
        s = par.parse().value;

        if (s == null) {
            print_text("Unexpected end of file",IMessageReporter.TYPE_ERROR);
            return null;
        }
        if (parser8080.errorCount != 0)
            return null;
        
        // do several passes for compiling
        Statement stat = (Statement)s;
        compileEnv env = new compileEnv();
        stat.pass1(env,reporter); // create symbol table
        stat.pass2(0); // try to evaluate all expressions + compute relative addresses
        while (stat.pass3(env) == true) ;
        if (env.getPassNeedCount() != 0) {
            print_text("Error: can't evaulate all expressions",IMessageReporter.TYPE_ERROR);
            return null;
        }
        stat.pass4(hex,env);
        return hex;
    }

    @Override
    public boolean compile(String fileName, Reader in) {
        try {
    		HEXFileHandler hex = compile(in);
    		if (hex == null) return false;
			hex.generateFile(fileName);
	        print_text("Compile was sucessfull. Output: " + fileName,IMessageReporter.TYPE_INFO);
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
    		if (hex == null) return false;
			hex.generateFile(fileName);
	        print_text("Compile was sucessfull. Output: " + fileName,IMessageReporter.TYPE_INFO);
	        
	        boolean r = hex.loadIntoMemory(mem);
	        if (r) print_text("Compiled file was loaded into operating memory.",IMessageReporter.TYPE_INFO);
	        else print_text("Compiled file couldn't be loaded into operating"
	                + "memory due to an error.", IMessageReporter.TYPE_ERROR);
	        programStart = hex.getProgramStart();
	        return true;
		} catch (Exception e) {
            print_text(e.getMessage(), IMessageReporter.TYPE_ERROR);
            return false;
		}
    }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
		
	}

}
