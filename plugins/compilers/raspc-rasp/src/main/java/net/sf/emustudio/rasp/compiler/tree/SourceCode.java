/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.compiler.tree;

import net.sf.emustudio.rasp.compiler.CompilerOutput;

/**
 *
 * @author miso
 */
public class SourceCode extends AbstractTreeNode{

    private final int programStart;
    private final Program program;

    public SourceCode(int programStart, Program program) {
        this.programStart = programStart;
        this.program = program;
    }

    @Override
    public void pass() throws Exception {
        CompilerOutput.getInstance().setProgramStart(programStart);
        program.pass();
    }

}
