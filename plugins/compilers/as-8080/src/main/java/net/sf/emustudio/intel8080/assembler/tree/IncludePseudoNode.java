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
package net.sf.emustudio.intel8080.assembler.tree;

import emulib.runtime.HEXFileManager;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.impl.CompilerImpl;
import net.sf.emustudio.intel8080.assembler.impl.LexerImpl;
import net.sf.emustudio.intel8080.assembler.impl.ParserImpl;
import net.sf.emustudio.intel8080.assembler.treeAbstract.PseudoNode;

public class IncludePseudoNode extends PseudoNode {
    private final String fileName;
    private final CompilerImpl asm;
    private Statement program;
    private CompileEnv namespace;

    public IncludePseudoNode(String filename, int line, int column, CompilerImpl asm) {
        super(line, column);
        this.fileName = filename.replace("\\", File.separator);        
        this.asm = asm;
    }

    @Override
    public int getSize() {
        return program.getSize();
    }
    
    private File findIncludeFile(String tmpFileName) {
        File tmpFile = new File(tmpFileName);
        if (tmpFile.isAbsolute()) {
            return tmpFile;
        } else {
            return new File(namespace.getInputFile().getParent()
                    + File.separator + tmpFileName);
        }        
    }

    /**
     * Method will compare filename (in the include statement)
     * with filename given by the parameter.
     * 
     * @param tmpFileName 
     * @return true if filenames equal, false if not
     */
    public boolean isEqualName(String tmpFileName) {
        return findIncludeFile(fileName).equals(findIncludeFile(tmpFileName));
    }

    public void pass1(List<String> includefiles, CompileEnv parentEnv) throws Exception {
        try {
            namespace = new CompileEnv(parentEnv.getInputFile().getAbsolutePath());
            
            File file = findIncludeFile(fileName);
            FileReader f = new FileReader(file);
            LexerImpl lex = new LexerImpl(f);
            ParserImpl par = new ParserImpl(lex, asm);

            par.setReportPrefixString(file.getName() + ": ");
            Object s = par.parse().value;
            par.setReportPrefixString(null);

            if (s == null) {
                throw new Exception("[" + line + "," + column + "] "
                        + "Error: Unexpected end of file (" + file.getName() + ")");
            }

            program = (Statement) s;
            program.addIncludeFiles(includefiles);
            namespace = parentEnv;

            if (program.getIncludeLoops(file.getAbsolutePath())) {
                throw new Exception("[" + line + "," + column + "] "
                        + "Error: Infinite INCLUDE loop (" + file.getName() + ")");
            }
            program.pass1(namespace); // create symbol table
        } catch (IOException e) {
            throw new Exception(fileName + ": I/O Error");
        } catch (Exception e) {
            throw new Exception("[" + line + "," + column + "] "
                    + e.getMessage());
        }
    }

    @Override
    public int pass2(CompileEnv parentEnv, int addr_start) throws Exception {
        // try to evaulate all expressions + compute relative addresses
        return program.pass2(addr_start);
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        while (program.pass3(namespace) == true) {
        }
        if (namespace.getPassNeedCount() != 0) {
            throw new Exception("Error: can't evaulate all expressions");
        }
        program.pass4(hex);
    }

    @Override
    public String getName() {
        return fileName;
    }
}
