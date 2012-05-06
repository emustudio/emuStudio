/**
 *  BrainDisassembler.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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
package braincpu.gui;

import emuLib8.plugins.cpu.CPUInstruction;
import emuLib8.plugins.cpu.SimpleDisassembler;
import emuLib8.plugins.memory.IMemoryContext;

public class BrainDisassembler extends SimpleDisassembler {

    private IMemoryContext mem;     // pamäť bude potrebná pre
    // čítanie bytov pre dekódovanie
    // inštrukcií

    // 
    private final static byte isize[] = { 1,2,2,2,2,2,2,1,1 };
    
    /**
     * V konštruktore vytvorím stĺpce ako objekty
     * triedy ColumnInfo.
     * 
     * @param mem  kontext operačnej pamäte, ktorý bude
     *             potrebný pre dekódovanie inštrukcií
     */
    public BrainDisassembler(IMemoryContext mem) {
        this.mem = mem;
    }

    /**
     * Disassembler. Vykoná dekódovanie jednej inštrukcie
     * začínajúcej na adrese memPos.
     * 
     * @param memLocation  adresa, kde začína inštrukcia
     * @return dekódovaná inštrukcia
     */
    @Override
    public CPUInstruction disassemble(int memLocation) {
        short val, param;
        CPUInstruction instr;
        String mnemo, oper;
        int oldmem = memLocation;

        val = ((Short) mem.read(memLocation++)).shortValue();
        oper = String.format("%02X", val);

        switch (val) {
            case 0:
                mnemo = "halt";
                break;
            case 1:
                mnemo = "inc";
                param = ((Short) mem.read(memLocation++)).shortValue();
                oper += String.format(" %02X", param);
                if (param != 0xFF) {
                    mnemo += String.format(" %02X", param);
                }
                break;
            case 2:
                mnemo = "dec";
                param = ((Short) mem.read(memLocation++)).shortValue();
                oper += String.format(" %02X", param);
                if (param != 0xFF) {
                    mnemo += String.format(" %02X", param);
                }
                break;
            case 3:
                mnemo = "incv";
                param = ((Short) mem.read(memLocation++)).shortValue();
                oper += String.format(" %02X", param);
                if (param != 0xFF) {
                    mnemo += String.format(" %02X", param);
                }
                break;
            case 4:
                mnemo = "decv";
                param = ((Short) mem.read(memLocation++)).shortValue();
                oper += String.format(" %02X", param);
                if (param != 0xFF) {
                    mnemo += String.format(" %02X", param);
                }
                break;
            case 5:
                mnemo = "print";
                param = ((Short) mem.read(memLocation++)).shortValue();
                oper += String.format(" %02X", param);
                if (param != 0xFF) {
                    mnemo += String.format(" %02X", param);
                }
                break;
            case 6:
                mnemo = "load";
                param = ((Short) mem.read(memLocation++)).shortValue();
                oper += String.format(" %02X", param);
                if (param != 0xFF) {
                    mnemo += String.format(" %02X", param);
                }
                break;
            case 7:
                mnemo = "loop";
                break;
            case 8:
                mnemo = "endl";
                break;
            default:
                mnemo = "unknown";
        }
        instr = new CPUInstruction(oldmem,mnemo, oper);
        return instr;
    }

    @Override
    public int getNextInstructionLocation(int memLocation) throws IndexOutOfBoundsException {
        short val,diff=0;
        val = (short)(((Short) mem.read(memLocation)).shortValue() & 0xFF);
        
        if (val >= isize.length)
            diff = 1;
        else 
            diff = isize[val];
        return memLocation + diff;
    }
}
