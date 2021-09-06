/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.as8080.tree;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.as8080.CompilerImpl;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.exceptions.CompilerException;
import net.emustudio.plugins.compiler.as8080.exceptions.UnexpectedEOFException;
import net.emustudio.plugins.compiler.as8080.treeAbstract.PseudoNode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class IncludePseudoNode extends PseudoNode {
    private final String fileName;
    private final CompilerImpl compiler;
    private Statement program;
    private Namespace namespace;

    public IncludePseudoNode(String filename, int line, int column, CompilerImpl compiler) {
        super(line, column);
        this.fileName = filename.replace("\\", File.separator);
        this.compiler = compiler;
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
            return new File(namespace.getInputFile().getParent() + File.separator + tmpFileName);
        }
    }

    /**
     * Method will compare filename (in the include statement)
     * with filename given by the parameter.
     *
     * @param tmpFileName provided file name
     * @return true if filenames equal, false if not
     */
    boolean isEqualName(String tmpFileName) {
        return findIncludeFile(fileName).equals(findIncludeFile(tmpFileName));
    }

    void pass1(List<String> includefiles, Namespace parentEnv) throws Exception {
        try {
            namespace = new Namespace(parentEnv.getInputFile().getAbsolutePath());

            File file = findIncludeFile(fileName);
            FileReader f = new FileReader(file);
            LexerImpl lexer = new LexerImpl(f);
            ParserImpl parser = new ParserImpl(lexer, compiler);

            parser.setReportPrefixString(file.getName() + ": ");
            Object s = parser.parse().value;
            parser.setReportPrefixString(null);

            if (s == null) {
                throw new UnexpectedEOFException(line, column, file.getAbsolutePath());
            }

            program = (Statement) s;
            program.addIncludeFiles(includefiles);
            namespace = parentEnv;

            if (program.getIncludeLoops(file.getAbsolutePath())) {
                throw new CompilerException(line, column, "Infinite INCLUDE loop (" + file.getAbsolutePath() + ")");
            }
            program.pass1(namespace); // create symbol table
        } catch (CompilerException e) {
            throw e;
        } catch (IOException e) {
            throw new CompilerException(line, column, fileName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CompilerException(line, column, e.getMessage(), e);
        }
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        // try to evaluate all expressions + compute relative addresses
        return program.pass2(addr_start);
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
        while (program.pass3(namespace)) {
            // ignore
        }
        if (namespace.getPassNeedCount() != 0) {
            throw new Exception("Error: can't evaluate all expressions");
        }
        program.pass4(hex);
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "IncludePseudoNode{" +
            "fileName='" + fileName + '\'' +
            ", compiler=" + compiler +
            ", program=" + program +
            ", namespace=" + namespace +
            '}';
    }
}
