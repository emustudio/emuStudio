/*
 * DataNode.java
 *
 * Created on Sobota, 2007, september 22, 9:05
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package as_8080.tree8080;

import as_8080.impl.CompileEnv;
import as_8080.tree8080Abstract.CodeNode;
import as_8080.tree8080Abstract.DataValueNode;

import java.util.Vector;
import emuLib8.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class DataNode extends CodeNode {

    private Vector<DataValueNode> list; // this vector stores only data values

    public void addElement(DataValueNode node) {
        list.addElement(node);
    }

    public void addAll(Vector<DataValueNode> vec) {
        list.addAll(vec);
    }

    /** Creates a new instance of DataNode */
    public DataNode(int line, int column) {
        super(line, column);
        this.list = new Vector<DataValueNode>();
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
