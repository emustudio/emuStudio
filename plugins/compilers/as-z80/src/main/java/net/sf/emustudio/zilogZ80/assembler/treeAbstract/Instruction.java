/*
 * Instruction.java
 *
 * Created on Štvrtok, 2008, august 14, 12:46
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

package net.sf.emustudio.zilogZ80.assembler.treeAbstract;

public abstract class Instruction extends InstrData {
    protected int opcode;
    
    public Instruction(int opcode, int line, int column) {
        super(line,column);
        this.opcode = opcode;
    }

    @Override
    public int getSize() { 
        return Expression.getSize(opcode);
    }
}
