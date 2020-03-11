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

package net.emustudio.plugins.compilers.raspc.tree;

import net.emustudio.plugins.compilers.raspc.Statement;

/**
 * @author miso
 */
public class Row extends AbstractTreeNode {

    private final Label label;
    private final Statement statement;

    public Row(Label label, Statement statement) {
        this.statement = statement;
        this.label = label;
    }

    public Label getLabel() {
        return label;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public void pass() throws Exception {
        /*pass() only for statement, label was already passed in translateLabels()
         method in Program node
         */
        statement.pass();
    }

}
