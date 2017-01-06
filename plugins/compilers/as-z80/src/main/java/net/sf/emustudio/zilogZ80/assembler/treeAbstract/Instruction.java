/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.treeAbstract;

import net.sf.emustudio.zilogZ80.assembler.exceptions.ValueOutOfBoundsException;

public abstract class Instruction extends InstrData {
    protected int opcode;
    
    public Instruction(int opcode, int line, int column) {
        super(line,column);
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
