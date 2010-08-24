/*
 * DataNode.java
 *
 * Created on Streda, 2008, august 13, 11:35
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package as_z80.treeZ80;

import as_z80.impl.HEXFileHandler;
import as_z80.impl.Namespace;
import java.util.Vector;
import as_z80.treeZ80Abstract.InstrData;
import as_z80.treeZ80Abstract.DataValue;

/**
 *
 * @author vbmacher
 */
public class DataNode extends InstrData {

    private Vector<DataValue> list; // this vector stores only data values

    public void addElement(DataValue node) {
        list.addElement(node);
    }

    public void addAll(Vector<DataValue> vec) {
        list.addAll(vec);
    }

    /** Creates a new instance of DataNode */
    public DataNode(int line, int column) {
        super(line, column);
        this.list = new Vector<DataValue>();
    }

    /// compile time ///
    @Override
    public int getSize() {
        DataValue dv;
        int size = 0;
        for (int i = 0; i < list.size(); i++) {
            dv = (DataValue) list.get(i);
            size += dv.getSize();
        }
        return size;
    }

    @Override
    public void pass1() throws Exception {
        for (int i = 0; i < list.size(); i++) {
            DataValue n = (DataValue) list.elementAt(i);
            n.pass1();
        }
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        DataValue dv;
        for (int i = 0; i < list.size(); i++) {
            dv = (DataValue) list.get(i);
            addr_start = dv.pass2(env, addr_start);
        }
        return addr_start;
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        DataValue dv;
        for (int i = 0; i < list.size(); i++) {
            dv = (DataValue) list.get(i);
            dv.pass4(hex);
        }
    }
}
