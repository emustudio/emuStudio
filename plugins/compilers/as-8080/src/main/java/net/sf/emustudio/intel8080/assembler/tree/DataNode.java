/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;
import net.sf.emustudio.intel8080.assembler.treeAbstract.CodeNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.DataValueNode;

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
    public int pass2(CompileEnv env, int addr_start) throws Exception {
        for (DataValueNode dataValue : dataValues) {
            addr_start = dataValue.pass2(env, addr_start);
        }
        return addr_start;
    }

    @Override
    public void pass4(HEXFileManager hex) throws Exception {
        for (DataValueNode dataValue : dataValues) {
            dataValue.pass4(hex);
        }
    }
}
