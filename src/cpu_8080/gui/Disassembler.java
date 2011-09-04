/*
 *  Disassembler.java
 *
 *  Copyright (C) 2011 vbmacher
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package cpu_8080.gui;

import emuLib8.plugins.cpu.CPUInstruction;
import emuLib8.plugins.cpu.SimpleDisassembler;
import emuLib8.plugins.memory.IMemoryContext;

/**
 *
 * @author vbmacher
 */
public class Disassembler extends SimpleDisassembler {

    private IMemoryContext mem;

    private final static byte isize[] = {
        1,3,1,1,1,1,2,1,0,1,1,1,1,1,2,1,0,3,1,1,1,1,2,1,0,1,1,1,1,1,2,1,0,3,3,1,
        1,1,2,1,0,1,3,1,1,1,2,1,0,3,3,1,1,1,2,1,0,1,3,1,1,1,2,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,3,3,3,1,2,1,1,1,3,0,3,3,2,1,1,1,3,2,3,1,2,1,
        1,0,3,2,3,0,2,1,1,1,3,1,3,1,2,1,1,1,3,1,3,0,2,1,1,1,3,1,3,1,2,1,1,1,3,1,
        3,0,2,1
    };

    private final static String regMnemo[] = {
      "B", "C", "D", "E", "H", "L", "M", "A"
    };



    /**
     * The constructor creates an instance of the Disassembler class
     * @param mem memory object
     */
    public Disassembler(IMemoryContext mem) {
        super();
        this.mem = mem;
    }

    /**
     * Dissassemble one instruction at location memLocation.
     *
     * @param memLocation
     *   Memory address where to begin disassembling.
     * @return
     *   Object that represents instruction.
     */
    @Override
    public CPUInstruction disassemble(int memLocation) {
        short val;
        int addr;
        int pos = memLocation;

        String mnemo, oper;

        val = ((Short) mem.read(pos++)).shortValue();
        oper = String.format("%02X", val);
        if ((val >= 64) && (val <= 127) && (val != 118)) {
            mnemo = "mov " + regMnemo[(val & 56) >> 3] + ","
                    + regMnemo[(byte) (val & 7)];
        } else if ((val >= 128) && (val <= 135)) {
            mnemo = "add " + regMnemo[val & 7];
        } else if ((val >= 136) && (val <= 143)) {
            mnemo = "adc " + regMnemo[val & 7];
        } else if ((val >= 144) && (val <= 151)) {
            mnemo = "sub " + regMnemo[val & 7];
        } else if ((val >= 152) && (val <= 159)) {
            mnemo = "sbb " + regMnemo[val & 7];
        } else if ((val >= 160) && (val <= 167)) {
            mnemo = "ana " + regMnemo[val & 7];
        } else if ((val >= 168) && (val <= 175)) {
            mnemo = "xra " + regMnemo[val & 7];
        } else if ((val >= 176) && (val <= 183)) {
            mnemo = "ora " + regMnemo[val & 7];
        } else if ((val >= 184) && (val <= 191)) {
            mnemo = "cmp " + regMnemo[val & 7];
        } else {
            switch (val) {
                case 58: // lda addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("lda %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 50: // sta addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("sta %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 10:
                    mnemo = "ldax BC";
                    break;
                case 26:
                    mnemo = "ldax DE";
                    break;
                case 2:
                    mnemo = "stax BC";
                    break;
                case 18:
                    mnemo = "stax DE";
                    break;
                case 42: // lhld addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("lhld %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 34: // shld addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("shld %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 235:
                    mnemo = "xchg";
                    break;
                case 6: // mvi b, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi B,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 14: // mvi c, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi C,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 22: // mvi d, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi D,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 30: // mvi e, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi E,%02Xh", val);
                    oper = String.format(" %02X", val);
                    break;
                case 38: // mvi h, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi H,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 46: // mvi l, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi L,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 54: // mvi m, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi M,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 62: // mvi a, byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("mvi A,%02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 1: // lxi bc, dble
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("lxi BC,%04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 17: // lxi de, dble
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("lxi DE,%04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 33: // lxi hl, dble
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("lxi HL,%04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 49: // lxi sp, dble
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("lxi SP,%04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 249:
                    mnemo = "sphl";
                    break;
                case 227:
                    mnemo = "xthl";
                    break;
                case 193:
                    mnemo = "pop BC";
                    break;
                case 209:
                    mnemo = "pop DE";
                    break;
                case 225:
                    mnemo = "pop HL";
                    break;
                case 241:
                    mnemo = "pop PSW";
                    break;
                case 197:
                    mnemo = "push BC";
                    break;
                case 213:
                    mnemo = "push DE";
                    break;
                case 229:
                    mnemo = "push HL";
                    break;
                case 245:
                    mnemo = "push PSW";
                    break;
                case 219: // in port
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("in %Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 211: // out port
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("out %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 198: // adi byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("adi %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 206: // aci byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("aci %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 9:
                    mnemo = "dad BC";
                    break;
                case 25:
                    mnemo = "dad DE";
                    break;
                case 41:
                    mnemo = "dad HL";
                    break;
                case 57:
                    mnemo = "dad SP";
                    break;
                case 214: // sui byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("sui %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 222: // sbi byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("sbi %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 4:
                    mnemo = "inr B";
                    break;
                case 12:
                    mnemo = "inr C";
                    break;
                case 20:
                    mnemo = "inr D";
                    break;
                case 28:
                    mnemo = "inr E";
                    break;
                case 36:
                    mnemo = "inr H";
                    break;
                case 44:
                    mnemo = "inr L";
                    break;
                case 52:
                    mnemo = "inr M";
                    break;
                case 60:
                    mnemo = "inr A";
                    break;
                case 3:
                    mnemo = "inx BC";
                    break;
                case 19:
                    mnemo = "inx DE";
                    break;
                case 35:
                    mnemo = "inx HL";
                    break;
                case 51:
                    mnemo = "inx SP";
                    break;
                case 5:
                    mnemo = "dcr B";
                    break;
                case 13:
                    mnemo = "dcr C";
                    break;
                case 21:
                    mnemo = "dcr D";
                    break;
                case 29:
                    mnemo = "dcr E";
                    break;
                case 37:
                    mnemo = "dcr H";
                    break;
                case 45:
                    mnemo = "dcr L";
                    break;
                case 53:
                    mnemo = "dcr M";
                    break;
                case 61:
                    mnemo = "dcr A";
                    break;
                case 11:
                    mnemo = "dcx BC";
                    break;
                case 27:
                    mnemo = "dcx DE";
                    break;
                case 43:
                    mnemo = "dcx HL";
                    break;
                case 59:
                    mnemo = "dcx SP";
                    break;
                case 254: // cpi byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("cpi %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 39:
                    mnemo = "daa";
                    oper = "27";
                    break;
                case 230: // ani byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("ani %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 246: // ori byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("ori %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 238: // xri byte
                    val = ((Short) mem.read(pos++)).shortValue();
                    mnemo = String.format("xri %02Xh", val);
                    oper += String.format(" %02X", val);
                    break;
                case 47:
                    mnemo = "cma";
                    break;
                case 7:
                    mnemo = "rlc";
                    break;
                case 15:
                    mnemo = "rrc";
                    break;
                case 23:
                    mnemo = "ral";
                    break;
                case 31:
                    mnemo = "rar";
                    break;
                case 55:
                    mnemo = "stc";
                    break;
                case 63:
                    mnemo = "cmc";
                    break;
                case 233:
                    mnemo = "pchl";
                    break;
                case 195: // jmp addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jmp %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 194: // jnz addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jnz %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 202: // jz addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jz %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 210: // jnc addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jnc %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 218: // jc addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jc %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 226: // jpo addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jpo %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 234: // jpe addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jpe %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 242: // jp addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jp %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 250: // jm addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("jm %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 205: // call addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("call %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 196: // cnz addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cnz %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 204: // cz addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cz %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 212: // cnc addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cnc %04Xh", addr);
                    oper += String.format("%02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 220: // cc addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cc %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 228: // cpo addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cpo %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 236: // cpe addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cpe %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 244: // cp addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cp %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 252: // cm addr
                    addr = (Integer) mem.readWord(pos);
                    mnemo = String.format("cm %04Xh", addr);
                    oper += String.format(" %02X %02X", addr & 0xFF, (addr >> 8) & 0xFF);
                    pos += 2;
                    break;
                case 201:
                    mnemo = "ret";
                    break;
                case 192:
                    mnemo = "rnz";
                    break;
                case 200:
                    mnemo = "rz";
                    break;
                case 208:
                    mnemo = "rnc";
                    break;
                case 216:
                    mnemo = "rc";
                    break;
                case 224:
                    mnemo = "rpo";
                    break;
                case 232:
                    mnemo = "rpe";
                    break;
                case 240:
                    mnemo = "rp";
                    break;
                case 248:
                    mnemo = "rm";
                    break;
                case 199:
                    mnemo = "rst 0";
                    break;
                case 207:
                    mnemo = "rst 1";
                    break;
                case 215:
                    mnemo = "rst 2";
                    break;
                case 223:
                    mnemo = "rst 3";
                    break;
                case 231:
                    mnemo = "rst 4";
                    break;
                case 239:
                    mnemo = "rst 5";
                    break;
                case 247:
                    mnemo = "rst 6";
                    break;
                case 255:
                    mnemo = "rst 7";
                    break;
                case 251:
                    mnemo = "ei";
                    break;
                case 243:
                    mnemo = "di";
                    break;
                case 118:
                    mnemo = "hlt";
                    break;
                case 0:
                    mnemo = "nop";
                    break;
                default:
                    mnemo = "unknown instruction";
            }
        }
        return new CPUInstruction(memLocation, mnemo, oper);
    }

    @Override
    public int getNextInstructionLocation(int memLocation)
            throws IndexOutOfBoundsException {
        int diff = isize[(Short)mem.read(memLocation)];

        // unknown instruction?
        if (diff == 0)
            diff = 1;

        // if the address exceeds boundaries, exception will be thrown
        short test = (Short)mem.read(memLocation + diff);

        return memLocation + diff;
    }

   
}
