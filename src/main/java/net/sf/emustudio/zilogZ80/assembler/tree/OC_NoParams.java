/*
 * OC_NoParams.java
 *
 * Created on Štvrtok, 2008, august 14, 12:49
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

import emulib.runtime.HEXFileManager;
import net.sf.emustudio.zilogZ80.assembler.impl.Namespace;
import net.sf.emustudio.zilogZ80.assembler.treeAbstract.Instruction;

public class OC_NoParams extends Instruction {

    public static final int ADC_A_HHLL = 0x8E;
    public static final int ADD_A_HHLL = 0x86;
    public static final int AND_HHLL = 0xA6;
    public static final int CCF = 0x3F;
    public static final int CP_HHLL = 0xBE;
    public static final int CPD = 0xEDA9;
    public static final int CPDR = 0xEDB9;
    public static final int CPI = 0xEDA1;
    public static final int CPIR = 0xEDB1;
    public static final int CPL = 0x2F;
    public static final int DAA = 0x27;
    public static final int DEC = 0x0B;
    public static final int DEC_A = 0x3D;
    public static final int DEC_B = 0x05;
    public static final int DEC_C = 0x0D;
    public static final int DEC_D = 0x15;
    public static final int DEC_E = 0x1D;
    public static final int DEC_H = 0x25;
    public static final int DEC_L = 0x2D;
    public static final int DEC_HHLL = 0x35;
    public static final int DEC_IX = 0xDD2B;
    public static final int DEC_IY = 0xFD2B;
    public static final int DI = 0xF3;
    public static final int EI = 0xFB;
    public static final int EX_SSPP_HL = 0xE3;
    public static final int EX_SSPP_IX = 0xDDE3;
    public static final int EX_SSPP_IY = 0xFDE3;
    public static final int EX_AF_AFF = 0x08;
    public static final int EX_DDEE_HL = 0xEB;
    public static final int EXX = 0xD9;
    public static final int HALT = 0x76;
    public static final int IN_A = 0xED78;
    public static final int IN_B = 0xED40;
    public static final int IN_C = 0xED48;
    public static final int IN_D = 0xED50;
    public static final int IN_E = 0xED58;
    public static final int IN_H = 0xED60;
    public static final int IN_L = 0xED68;
    public static final int INC_A = 0x3C;
    public static final int INC_B = 0x04;
    public static final int INC_C = 0x0C;
    public static final int INC_D = 0x14;
    public static final int INC_E = 0x1C;
    public static final int INC_H = 0x24;
    public static final int INC_L = 0x2C;
    public static final int INC_IX = 0xDD23;
    public static final int INC_IY = 0xFD23;
    public static final int INC_HHLL = 0x34;
    public static final int IND = 0xEDAA;
    public static final int INDR = 0xEDBA;
    public static final int INI = 0xEDA2;
    public static final int INIR = 0xEDB2;
    public static final int JP_HHLL = 0xE9;
    public static final int JP_IIXX = 0xDDE9;
    public static final int JP_IIYY = 0xFDE9;
    public static final int LD_I = 0xED47; // LD I,A
    public static final int LD_R = 0xED4F; // LD R,A
    public static final int LD_A_I = 0xED57; // LD A,I
    public static final int LD_A_R = 0xED5F; // LD A,R
    public static final int LD_A_BBCC = 0x0A;
    public static final int LD_A_DDEE = 0x1A;
    public static final int LD_A_HHLL = 0x7E;
    public static final int LD_B_HHLL = 0x46;
    public static final int LD_C_HHLL = 0x4E;
    public static final int LD_D_HHLL = 0x56;
    public static final int LD_E_HHLL = 0x5E;
    public static final int LD_H_HHLL = 0x66;
    public static final int LD_L_HHLL = 0x6E;
    public static final int LD_SP_HL = 0xF9;
    public static final int LD_SP_IX = 0xDDF9;
    public static final int LD_SP_IY = 0xFDF9;
    public static final int LD_BBCC_A = 0x02;
    public static final int LD_DDEE_A = 0x12;
    public static final int LDD = 0xEDA8;
    public static final int LDDR = 0xEDB8;
    public static final int LDI = 0xA0;
    public static final int LDIR = 0xB0;
    public static final int NEG = 0xED44;
    public static final int NOP = 0;
    public static final int OR_HHLL = 0xB6;
    public static final int OTDR = 0xEDBB;
    public static final int OTIR = 0xEDB3;
    public static final int OUT_A = 0xED79;
    public static final int OUT_B = 0xED41;
    public static final int OUT_C = 0xED49;
    public static final int OUT_D = 0xED51;
    public static final int OUT_E = 0xED59;
    public static final int OUT_H = 0xED61;
    public static final int OUT_L = 0xED69;
    public static final int OUTD = 0xEDAB;
    public static final int OUTI = 0xEDA3;
    public static final int POP_IX = 0xDDE1;
    public static final int POP_IY = 0xFDE1;
    public static final int PUSH_IX = 0xDDE5;
    public static final int PUSH_IY = 0xFDE5;
    public static final int RET = 0xC9;
    public static final int RETI = 0xED4D;
    public static final int RETN = 0xED45;
    public static final int RL_HHLL = 0xCB16;
    public static final int RLA = 0x17;
    public static final int RLC_HHLL = 0xCB06;
    public static final int RLCA = 0x7;
    public static final int RLD = 0xED6F;
    public static final int RR_HHLL = 0xCB1E;
    public static final int RRA = 0x1F;
    public static final int RRC_HHLL = 0xCB0E;
    public static final int RRCA = 0x0F;
    public static final int RRD = 0xED67;
    public static final int SBC_A_HHLL = 0x9E;
    public static final int SCF = 0x37;
    public static final int SLA_HHLL = 0xCB26;
    public static final int SRA_HHLL = 0xCB2E;
    public static final int SLL_HHLL = 0xCB36;
    public static final int SRL_HHLL = 0xCB3E;
    public static final int SUB_HHLL = 0x96;
    public static final int XOR_HHLL = 0xAE;

    /** Creates a new instance of OpcodeWParamsNode */
    public OC_NoParams(int opcode, int line, int column) {
        super(opcode, line, column);
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
    public void pass4(HEXFileManager hex) throws Exception {
        String s = (getSize() == 1) ? "%1$02X" : "%1$04X";
        hex.putCode(String.format(s, opcode));
    }
}
