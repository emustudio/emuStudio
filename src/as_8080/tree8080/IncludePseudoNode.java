/*
 * PseudoINCLUDE.java
 *
 * Created on 14.8.2008, 9:27:10
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package as_8080.tree8080;

import as_8080.impl.Assembler8080;
import as_8080.impl.CompileEnv;
import as_8080.impl.Lexer8080;
import as_8080.impl.Parser8080;
import as_8080.tree8080Abstract.PseudoNode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import emuLib8.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class IncludePseudoNode extends PseudoNode {

    private String filename;
    private String shortFileName;
    private Statement program;
    private CompileEnv namespace;
    private Assembler8080 asm;

    public IncludePseudoNode(String filename, int line, int column,
            Assembler8080 asm) {
        super(line, column);

        // change "\"'s to /'s
        this.filename = filename.replace("\\", "/");
        this.shortFileName = new File(filename).getName();
        this.asm = asm;
    }

    @Override
    public int getSize() {
        return program.getSize();
    }

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

        if (ff1.equals(ff2)) {
            return true;
        } else {
            return false;
        }
    }

    public void pass1(Vector<String> includefiles,
            CompileEnv parentEnv)
            throws Exception {
        try {
            FileReader f = new FileReader(new File(filename));
            Lexer8080 lex = new Lexer8080(f);
            Parser8080 par = new Parser8080(lex, asm);

            par.setReportAppendString(shortFileName + ": ");
            Object s = par.parse().value;
            par.setReportAppendString(null);
            
            if (s == null) {
                throw new Exception("[" + line + "," + column + "] "
                        + "Error: Unexpected end of file (" + shortFileName + ")");
            }

            program = (Statement) s;
            program.addIncludeFiles(includefiles);
            namespace = parentEnv;

            if (program.getIncludeLoops(filename)) {
                throw new Exception("[" + line + "," + column + "] "
                        + "Error: Infinite INCLUDE loop (" + shortFileName + ")");
            }
            program.pass1(namespace); // create symbol table
        } catch (IOException e) {
            throw new Exception(shortFileName + ": I/O Error");
        } catch (Exception e) {
            throw new Exception("[" + line + "," + column + "] "
                    + e.getMessage());
        }
    }

    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        return program.pass2(addr_start); // try to evaulate all expressions + compute relative addresses
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        while (program.pass3(namespace) == true)
            ;
        if (namespace.getPassNeedCount() != 0) {
            throw new Exception("Error: can't evaulate all expressions");
        }
        program.pass4(hex);
    }

    @Override
    public String getName() {
        return filename;
    }
}
