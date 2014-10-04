/*
 * Created on 14.8.2008, 9:27:10
 *
 * Copyright (C) 2008-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.runtime.HEXFileManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import net.sf.emustudio.zilogZ80.assembler.impl.CompilerImpl;
import net.sf.emustudio.zilogZ80.assembler.impl.LexerImpl;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.impl.ParserImpl;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Pseudo;

public class PseudoINCLUDE extends Pseudo {
    private final String filename;
    private final String shortFileName;
    private Program program;
    private Namespace namespace;
    private final CompilerImpl asm;

    public PseudoINCLUDE(String filename, int line, int column, CompilerImpl asm) {
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

    @Override
    public void pass1() throws Exception {
    }

    public void pass1(ArrayList<String> includefiles,
            Namespace parent) throws Exception {
        try {
            FileReader f = new FileReader(new File(filename));
            LexerImpl lex = new LexerImpl(f);
            ParserImpl par = new ParserImpl(lex, asm);

            par.setReportPrefixString(shortFileName + ": ");
            Object s = par.parse().value;
            par.setReportPrefixString(null);
            if (s == null) {
                throw new Exception("[" + line + "," + column + "] "
                        + "Error: Unexpected end of file (" + shortFileName + ")");
            }
            program = (Program) s;
            program.addIncludeFiles(includefiles);
            namespace = parent;

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
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        // try to evaluate all expressions + compute relative addresses
        return program.pass2(addr_start);
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        while (program.pass3(namespace) == true) {
            // :-)
        }
        if (namespace.getPassNeedCount() != 0) {
            throw new Exception("Error: can't evaulate all expressions");
        }
        program.pass4(hex);
    }
}
