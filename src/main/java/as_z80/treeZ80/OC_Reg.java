/*
 * OC_Reg.java
 *
 * Created on Piatok, 2008, august 15, 11:03
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 * DON'T REPEAT YOURSELF
 *
 * Copyright (C) 2008-2012 Peter Jakubčo <pjakubco@gmail.com>
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

import as_z80.impl.Namespace;
import as_z80.treeZ80Abstract.Instruction;
import emulib.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class OC_Reg extends Instruction {

    public static final int ADC = 0x88; //ADC A,r
    public static final int ADC_HL = 0xED4A; //ADC HL,rr
    public static final int ADD = 0x80; //ADD A,r
    public static final int ADD_HL = 0x09; //ADD HL,rr
    public static final int ADD_IX = 0xDD09; //ADD IX,rx
    public static final int ADD_IY = 0xFD09; //ADD IY,ry
    public static final int AND = 0xA0; //AND r
    public static final int CP = 0xB8; //CP r
    public static final int DEC = 0x0B; // DEC rr
    public static final int INC = 0x03; // INC rr
    public static final int LD_A = 0x78; //LD A,r   
    public static final int LD_B = 0x40; //LD B,r
    public static final int LD_C = 0x48; //LD C,r
    public static final int LD_D = 0x50; //LD D,r
    public static final int LD_E = 0x58; //LD E,r
    public static final int LD_H = 0x60; //LD H,r
    public static final int LD_L = 0x68; //LD L,r
    public static final int LD_HHLL_r = 0x70; //LD (HL),r
    public static final int OR = 0xB0; // OR r
    public static final int POP = 0xC1; // POP qq
    public static final int PUSH = 0xC5; // PUSH qq
    public static final int RET = 0xC0; // RET cc
    public static final int RL = 0xCB10; // RL r
    public static final int RLC = 0xCB00; // RLC r
    public static final int RR = 0xCB18; // RR r
    public static final int RRC = 0xCB08; // RRC r
    public static final int SBC = 0x98; // SBC r
    public static final int SBC_HL = 0xED42; // SBC HL, rr
    public static final int SLA = 0xCB20; // SLA r
    public static final int SRA = 0xCB28; // SRA r
    public static final int SLL = 0xCB30; // SLL r
    public static final int SRL = 0xCB38; // SRL r
    public static final int SUB = 0x90; // SUB r
    public static final int XOR = 0xA8; // XOR r

    /** Creates a new instance of OC_RegNode */
    public OC_Reg(int opcode, int reg, int line, int column) {
        super(opcode + reg, line, column);
    }

    /// compile time ///
    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        return addr_start + getSize();
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        String s = (getSize() == 1) ? "%1$02X" : "%1$04X";
        hex.putCode(String.format(s, opcode));
    }
}
