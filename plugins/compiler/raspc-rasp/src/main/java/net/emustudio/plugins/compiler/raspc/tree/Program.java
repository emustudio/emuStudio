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

import net.emustudio.plugins.compiler.raspc.CompilerOutput;
import net.emustudio.plugins.compiler.raspc.Namespace;

import java.util.ArrayList;
import java.util.List;

public class Program extends AbstractTreeNode {
    private final List<Row> rows = new ArrayList<>();
    private final Namespace namespace = new Namespace();

    public void addRow(Row r) {
        rows.add(r);

        namespace.addInput(r.getInput());
        if (r.getProgramStart() > -1) {
            namespace.setProgramStart(r.getProgramStart());
        }
    }

    @Override
    public void pass() throws Exception {
        translateLabels();

        for (Row row : rows) {
            row.pass();
        }
    }

    public Namespace getNamespace() {
        return namespace;
    }

    private void translateLabels() {
        int programStart = CompilerOutput.getInstance().getProgramStart();
        for (Row row : rows) {
            Label label = row.getLabel();
            if (label != null) {
                label.setAddress(programStart + rows.indexOf(row) * 2);
                label.pass();
            }
        }
    }
}
