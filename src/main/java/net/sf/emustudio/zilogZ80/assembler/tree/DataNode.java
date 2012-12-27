/*
 * DataNode.java
 *
 * Created on Streda, 2008, august 13, 11:35
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.tree;

import emulib.plugins.compiler.HEXFileHandler;
import java.util.ArrayList;
import java.util.List;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.DataValue;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.InstrData;

/**
 *
 * @author vbmacher
 */
public class DataNode extends InstrData {

    private List<DataValue> list; // this list stores only data values

    public void addElement(DataValue node) {
        list.add(node);
    }

    public void addAll(ArrayList<DataValue> vec) {
        list.addAll(vec);
    }

    /** Creates a new instance of DataNode */
    public DataNode(int line, int column) {
        super(line, column);
        this.list = new ArrayList<DataValue>();
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
            DataValue n = (DataValue) list.get(i);
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
