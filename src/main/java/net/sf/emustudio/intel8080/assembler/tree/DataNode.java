/*
 * DataNode.java
 *
 * Created on Sobota, 2007, september 22, 9:05
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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

import net.sf.emustudio.intel8080.assembler.treeAbstract.CodeNode;
import net.sf.emustudio.intel8080.assembler.treeAbstract.DataValueNode;
import emulib.plugins.compiler.HEXFileHandler;
import java.util.ArrayList;
import java.util.List;
import net.sf.emustudio.intel8080.assembler.impl.CompileEnv;

/**
 *
 * @author vbmacher
 */
public class DataNode extends CodeNode {
    private List<DataValueNode> list; // this vector stores only data values

    public void addElement(DataValueNode node) {
        list.add(node);
    }

    public void addAll(List<DataValueNode> vec) {
        list.addAll(vec);
    }

    /** Creates a new instance of DataNode */
    public DataNode(int line, int column) {
        super(line, column);
        this.list = new ArrayList<DataValueNode>();
    }

    /// compile time ///
    @Override
    public int getSize() {
        DataValueNode dv;
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            dv = (DataValueNode) list.get(i);
            size += dv.getSize();
        }
        return size;
    }

    @Override
    public int pass2(CompileEnv env, int addr_start) throws Exception {
        DataValueNode dv;
        for (int i = 0; i < list.size(); i++) {
            dv = (DataValueNode) list.get(i);
            addr_start = dv.pass2(env, addr_start);
        }
        return addr_start;
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        DataValueNode dv;
        for (int i = 0; i < list.size(); i++) {
            dv = (DataValueNode) list.get(i);
            dv.pass4(hex);
        }
    }
}
