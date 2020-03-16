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

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compiler.as8080.Namespace;
import net.emustudio.plugins.compiler.as8080.treeAbstract.CodeNode;
import net.emustudio.plugins.compiler.as8080.treeAbstract.DataValueNode;

import java.util.ArrayList;
import java.util.List;

public class DataNode extends CodeNode {
    private final List<DataValueNode> dataValues = new ArrayList<>();

    public void addElement(DataValueNode node) {
        dataValues.add(node);
    }

    public DataNode(int line, int column) {
        super(line, column);
    }

    @Override
    public int getSize() {
        int size = 0;
        for (DataValueNode dataValue : dataValues) {
            size += dataValue.getSize();
        }
        return size;
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        for (DataValueNode dataValue : dataValues) {
            addr_start = dataValue.pass2(env, addr_start);
        }
        return addr_start;
    }

    @Override
    public void pass4(IntelHEX hex) throws Exception {
        for (DataValueNode dataValue : dataValues) {
            dataValue.pass4(hex);
        }
    }
}
