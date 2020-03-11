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
package net.emustudio.plugins.compilers.asZ80.treeAbstract;

import net.emustudio.plugins.compilers.asZ80.exceptions.ValueOutOfBoundsException;

public abstract class Instruction extends InstrData {
    protected int opcode;

    public Instruction(int opcode, int line, int column) {
        super(line, column);
        this.opcode = opcode;
    }

    protected static int computeRelativeAddress(int line, int column, int addressStart, int val) throws ValueOutOfBoundsException {
        int relativeAddress = (val - addressStart - 2);
        if (relativeAddress > 129 || relativeAddress < -126) {
            throw new ValueOutOfBoundsException(line, column, -126, 129, val);
        }
        return relativeAddress & 0xFF;
    }


    @Override
    public int getSize() {
        return Expression.getSize(opcode);
    }
}
