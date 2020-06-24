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
package net.emustudio.plugins.compiler.raspc.tree;

import net.emustudio.plugins.compiler.raspc.Statement;

import java.util.Objects;

public class Row extends AbstractTreeNode {

    private final Label label;
    private final Input input;
    private final int programStart;
    private final Statement statement;

    public Row(Label label, Input input, int programStart, Statement statement) {
        this.statement = statement;
        this.label = label;
        this.input = Objects.requireNonNullElse(input, new Input());
        this.programStart = programStart;
    }

    public Label getLabel() {
        return label;
    }

    public Input getInput() {
        return input;
    }

    public int getProgramStart() {
        return programStart;
    }

    @Override
    public void pass() throws Exception {
        // pass() only for statement, label was already passed in translateLabels() method in Program node
        if (statement != null) {
            statement.pass();
        }
    }
}
