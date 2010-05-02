/**
 * BrainDuck.java
 *
 * KISS, YAGNI
 * 
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package brainc_brainduck.impl;

import java.io.Reader;

import plugins.ISettingsHandler;
import plugins.compiler.ICompiler;
import plugins.compiler.ILexer;
import plugins.compiler.IMessageReporter;
import plugins.memory.IMemoryContext;
import brainc_brainduck.tree.Program;

/**
 * Main class implementing main compiler interface.
 *
 * @author Peter Jakubčo <pjakubco at gmail.com>
 */
public class BrainDuck implements ICompiler {
    private long hash;
    private BDLexer lex = null;
    private BDParser par;
    private IMessageReporter reporter;
    @SuppressWarnings("unused")
    private ISettingsHandler settings;
    private int programStart = 0; // actualize after compile 
    
    /**
     * A constructor.
     *
     * @param hash - long value to identify the plugin for settings.
     */
    public BrainDuck(Long hash) {
        this.hash = hash;
        // lex has to be reset WITH a reader object before compile
        lex = new BDLexer((Reader)null);
    }
        
    private void print_text(String mes, int type) {
        if (reporter != null) reporter.report(mes, type);
        else System.out.println(mes);
    }
    
    @Override
    public String getTitle() { return "BrainDuck Compiler"; }
    @Override
    public String getVersion() { return "1.0b1"; }
    @Override
    public String getCopyright() { return "\u00A9 Copyright 2009, P. Jakubčo"; }
    @Override
    public String getDescription() {
        return "Assembler for esoteric language BrainDuck derived from brainfuck";
    }

    @Override
    public long getHash() { return hash; }
    
    @Override
    public boolean initialize(ISettingsHandler sHandler, IMessageReporter reporter) {
        this.settings = sHandler;
        this.reporter = reporter;

        par = new BDParser(lex, reporter);
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
        if (par.errorCount != 0)
            return null;
        
        // do several passes for compiling
        Program program = (Program)s;
        program.pass1(0);
        program.pass2(hex);
        return hex;
    }
    
    @Override
    public boolean compile(String fileName, Reader in) {
        try {
            HEXFileHandler hex = compile(in);
            if (hex == null) return false;
            hex.generateFile(fileName);
            print_text("Compile was sucessfull. Output: "
                    + fileName, IMessageReporter.TYPE_INFO);
            programStart = hex.getProgramStart();
            return true;
        } catch (Exception e) {
        	e.printStackTrace();
            print_text(e.getMessage(), IMessageReporter.TYPE_ERROR);
            return false;
        }
    }

    @Override
    public boolean compile(String fileName, Reader in, IMemoryContext mem) {
        try {
            HEXFileHandler hex = compile(in);
            hex.generateFile(fileName);
            print_text("Compile was sucessfull. Output: "
                    + fileName, IMessageReporter.TYPE_INFO);
            programStart = hex.getProgramStart();
            boolean r = hex.loadIntoMemory(mem);
            if (r)
                print_text("Compiled file was loaded into operating memory.",
                        IMessageReporter.TYPE_INFO);
            else
                print_text("Compiled file couldn't be loaded into operating"
                    + "memory due to an error.",IMessageReporter.TYPE_ERROR);
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
    public ILexer getLexer(Reader in) { return new BDLexer(in); }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }

}
