/**
 *  RAMDisassembler.java
 * 
 *  KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package ramcpu.gui;

import emulib.plugins.cpu.CPUInstruction;
import interfaces.C8E258161A30C508D5E8ED07CE943EEF7408CA508;
import emulib.plugins.cpu.SimpleDisassembler;
import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;

public class RAMDisassembler extends SimpleDisassembler {

    private C8E258161A30C508D5E8ED07CE943EEF7408CA508 mem;

    /**
     * V konštruktore vytvorím stĺpce ako objekty
     * triedy ColumnInfo.
     * 
     * @param mem  kontext operačnej pamäte, ktorý bude
     *             potrebný pre dekódovanie inštrukcií
     */
    public RAMDisassembler(C8E258161A30C508D5E8ED07CE943EEF7408CA508 mem) {
        this.mem = mem;
    }

    @Override
    public CPUInstruction disassemble(int memLocation) {
        String mnemo, oper = "";
        int addr = memLocation;
    
        C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E in = 
                (C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) mem.read(memLocation++);
        if (in == null) {
            mnemo = "unknown instruction";
            return new CPUInstruction(addr,mnemo,oper);
        }
        String label = mem.getLabel(addr);
        if (label == null)
            label = "";
        mnemo = label + " " + in.getCodeStr() + " " + in.getOperandStr();
        return new CPUInstruction(addr,mnemo,oper);
    }

    @Override
    public int getNextInstructionLocation(int memLocation) throws IndexOutOfBoundsException {
        return memLocation+1;
    }

   
}
