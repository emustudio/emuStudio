/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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
package net.emustudio.plugins.compilers.raspc.tree;

import net.emustudio.plugins.compilers.raspc.CompilerOutput;

import java.util.ArrayList;
import java.util.List;

public class SourceCode extends AbstractTreeNode {

    private final int programStart;
    private final List<Integer> inputs = new ArrayList<>();
    private final Program program;
    private boolean programStartZero;
    private boolean programStartUndefined;

    public SourceCode(int programStart, Input inputs, Program program) {
        if (programStart == 0) {
            this.programStart = 20;
            this.programStartZero = true;
        } else if (programStart == -1) {
            this.programStart = 20;
            this.programStartUndefined = true;
        } else {
            this.programStart = programStart;
        }

        this.inputs.addAll(inputs.getAll());
        this.program = program;
    }

    public SourceCode(int programStart, Program program) {
        this(programStart, new Input(), program);
    }

    public SourceCode(Input input, Program program) {
        this(-1, input, program);
    }

    public SourceCode(Program program) {
        this(-1, new Input(), program);
    }

    @Override
    public void pass() throws Exception {
        CompilerOutput.getInstance().setProgramStart(programStart);
        CompilerOutput.getInstance().addInputs(inputs);
        program.pass();
    }

    public boolean isProgramStartZero() {
        return programStartZero;
    }

    public boolean isProgramStartUndefined() {
        return programStartUndefined;
    }

}
