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
package net.emustudio.plugins.compilers.asZ80.tree;

import net.emustudio.emulib.runtime.helpers.IntelHEX;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.DataValue;
import net.emustudio.plugins.compilers.asZ80.treeAbstract.InstrData;
import net.emustudio.plugins.compilers.asZ80.Namespace;

import java.util.ArrayList;
import java.util.List;

public class DataNode extends InstrData {
    private final List<DataValue> dataValues = new ArrayList<>();

    public void addElement(DataValue node) {
        dataValues.add(node);
    }

    public DataNode(int line, int column) {
        super(line, column);
    }

    @Override
    public int getSize() {
        int size = 0;
        for (DataValue dataValue : dataValues) {
            size += dataValue.getSize();
        }
        return size;
    }

    @Override
    public void pass1() throws Exception {
        for (DataValue dataValue : dataValues) {
            dataValue.pass1();
        }
    }

    @Override
    public int pass2(Namespace env, int addr_start) throws Exception {
        for (DataValue dataValue : dataValues) {
            addr_start = dataValue.pass2(env, addr_start);
        }
        return addr_start;
    }

    @Override
    public void generateCode(IntelHEX hex) throws Exception {
        for (DataValue dataValue : dataValues) {
            dataValue.pass4(hex);
        }
    }

}
