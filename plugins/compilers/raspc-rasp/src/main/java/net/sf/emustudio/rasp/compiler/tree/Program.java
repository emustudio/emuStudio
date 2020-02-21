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

import java.util.ArrayList;
import java.util.List;

/**
 * @author miso
 */
public class Program extends AbstractTreeNode {

    private final List<Row> rows = new ArrayList<>();

    public void addRow(Row r) {
        rows.add(r);
    }

    @Override
    public void pass() throws Exception {
        translateLabels();

        for (Row row : rows) {
            row.pass();
        }
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
