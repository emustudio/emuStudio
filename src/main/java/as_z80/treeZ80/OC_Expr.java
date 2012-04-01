/*
 * OC_Expr.java
 *
 * Created on Sobota, 2008, august 16, 11:50
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
import as_z80.treeZ80Abstract.Expression;
import as_z80.treeZ80Abstract.Instruction;
import emulib.plugins.compiler.HEXFileHandler;

/**
 *
 * @author vbmacher
 */
public class OC_Expr extends Instruction {

    public static final int ADC = 0xCE00; // ADC A,N
    public static final int ADC_A_IIX_NN = 0xDD8E00; // ADC A, (IX+N)
    public static final int ADC_A_IIY_NN = 0xFD8E00; // ADC A, (IY+N)
    public static final int ADD = 0xC600; // ADD A,N
    public static final int ADD_A_IIX_NN = 0xDD8600; // ADD A, (IX+N)
    public static final int ADD_A_IIY_NN = 0xFD8600; // ADD A, (IY+N)
    public static final int AND = 0xE600; // AND N
    public static final int AND_IIX_NN = 0xDDA600; // AND (IX+N)
    public static final int AND_IIY_NN = 0xFDA600; // AND (IY+N)
    public static final int BIT = 0xCB46; // BIT b,(HL)
    public static final int CALL = 0xCD0000; // CALL NN
    public static final int CP = 0xFE00; // CP N
    public static final int CP_IIX_NN = 0xDDBE00; // CP (IX+N)
    public static final int CP_IIY_NN = 0xFDBE00; // CP (IY+N)
    public static final int DEC_IIX_NN = 0xDD3500; // DEC (IX+N)
    public static final int DEC_IIY_NN = 0xFD3500; // DEC (IY+N)
    public static final int DJNZ = 0x1000; // DJNZ N
    public static final int IM = 0xED00; // IM N
    public static final int IN = 0xDB00; // IN A, (N)
    public static final int INC_IIX_NN = 0xDD3400; // INC (IX+N)
    public static final int INC_IIY_NN = 0xFD3400; // INC (IY+N)
    public static final int JP = 0xC30000; // JP NN
    public static final int JR = 0x1800; // JR N
    public static final int LD_A = 0x3E00; // LD A,N
    public static final int LD_A_IIX_NN = 0xDD7E00; // LD A,(IX+N)
    public static final int LD_A_IIY_NN = 0xFD7E00; // LD A,(IY+N)
    public static final int LD_A_NN = 0x3A0000; // LD A,(NN)
    public static final int LD_B = 0x0600; // LD B,N
    public static final int LD_C = 0x0E00; // LD C,N
    public static final int LD_D = 0x1600; // LD D,N
    public static final int LD_E = 0x1E00; // LD E,N
    public static final int LD_H = 0x2600; // LD H,N
    public static final int LD_L = 0x2E00; // LD L,N
    public static final int LD_B_IIX_NN = 0xDD4600; // LD B,(IX+N)
    public static final int LD_B_IIY_NN = 0xFD4600; // LD B,(IY+N)
    public static final int LD_C_IIX_NN = 0xDD4E00; // LD C,(IX+N)
    public static final int LD_C_IIY_NN = 0xFD4E00; // LD C,(IY+N)
    public static final int LD_D_IIX_NN = 0xDD5600; // LD D,(IX+N)
    public static final int LD_D_IIY_NN = 0xFD5600; // LD D,(IY+N)
    public static final int LD_E_IIX_NN = 0xDD5E00; // LD E,(IX+N)
    public static final int LD_E_IIY_NN = 0xFD5E00; // LD E,(IY+N)
    public static final int LD_H_IIX_NN = 0xDD6600; // LD H,(IX+N)
    public static final int LD_H_IIY_NN = 0xFD6600; // LD H,(IY+N)
    public static final int LD_L_IIX_NN = 0xDD6E00; // LD L,(IX+N)
    public static final int LD_L_IIY_NN = 0xFD6E00; // LD L,(IY+N)
    public static final int LD_BC_NN = 0xED4B0000; // LD BC,(NN)
    public static final int LD_DE_NN = 0xED5B0000; // LD DE,(NN)
    public static final int LD_HL_NN = 0x2A0000; // LD HL,(NN)
    public static final int LD_SP_NN = 0xED7B0000; // LD SP,(NN)
    public static final int LD_IX_NN = 0xDD2A0000; // LD IX,(NN)
    public static final int LD_IY_NN = 0xFD2A0000; // LD IY,(NN)
    public static final int LD_IX = 0xDD210000; // LD IX,NN
    public static final int LD_IY = 0xFD210000; // LD IY,NN
    public static final int LD_HHLL = 0x3600; // LD (HL),N
    public static final int LD_NN_A = 0x320000; // LD (NN),A
    public static final int LD_NN_BC = 0xED430000; // LD (NN),BC
    public static final int LD_NN_DE = 0xED530000; // LD (NN),DE
    public static final int LD_NN_HL = 0x220000; // LD (NN),HL
    public static final int LD_NN_SP = 0xED730000; // LD (NN),SP
    public static final int LD_NN_IX = 0xDD220000; // LD (NN),IX
    public static final int LD_NN_IY = 0xFD220000; // LD (NN),IY
    public static final int OR = 0xF600; // OR N
    public static final int OR_IIX_NN = 0xDDB600; // OR (IX+N)
    public static final int OR_IIY_NN = 0xFDB600; // OR (IY+N)
    public static final int OUT = 0xD300; // OUT (N),A
    public static final int RES = 0xCB86; // RES b,(HL)
    public static final int RL_IIX_NN = 0xDDCB0016; // RL (IX+N)
    public static final int RL_IIY_NN = 0xFDCB0016; // RL (IY+N)
    public static final int RLC_IIX_NN = 0xDDCB0006; // RLC (IX+N)
    public static final int RLC_IIY_NN = 0xFDCB0006; // RLC (IY+N)
    public static final int RR_IIX_NN = 0xDDCB001E; // RR (IX+N)
    public static final int RR_IIY_NN = 0xFDCB001E; // RR (IY+N)
    public static final int RRC_IIX_NN = 0xDDCB000E; // RRC (IX+N)
    public static final int RRC_IIY_NN = 0xFDCB000E; // RRC (IY+N)
    public static final int RST = 0xC7;
    public static final int SBC = 0xDE00; // SBC A,N
    public static final int SBC_A_IIX_NN = 0xDD9E00; // SBC A,(IX+N)
    public static final int SBC_A_IIY_NN = 0xFD9E00; // SBC A,(IY+N)
    public static final int SET = 0xCBC6; // SET b,(HL)
    public static final int SLA_IIX_NN = 0xDDCB0026; // SLA (IX+N)
    public static final int SLA_IIY_NN = 0xFDCB0026; // SLA (IY+N)
    public static final int SRA_IIX_NN = 0xDDCB002E; // SRA (IX+N)
    public static final int SRA_IIY_NN = 0xFDCB002E; // SRA (IY+N)
    public static final int SLL_IIX_NN = 0xDDCB0036; // SLL (IX+N)
    public static final int SLL_IIY_NN = 0xFDCB0036; // SLL (IY+N)
    public static final int SRL_IIX_NN = 0xDDCB003E; // SRL (IX+N)
    public static final int SRL_IIY_NN = 0xFDCB003E; // SRL (IY+N)
    public static final int SUB = 0xD600; // SUB N
    public static final int SUB_IIX_NN = 0xDD9600; // SUB (IX+N)
    public static final int SUB_IIY_NN = 0xFD9600; // SUB (IY+N)
    public static final int XOR = 0xEE00; // XOR N
    public static final int XOR_IIX_NN = 0xDDAE00; // XOR (IX+N)
    public static final int XOR_IIY_NN = 0xFDAE00; // XOR (IY+N)    
    private Expression expr;
    private boolean oneByte;
    private int old_opcode;

    /** Creates a new instance of OC_Expr */
    public OC_Expr(int opcode, Expression expr, boolean oneByte, int line, int column) {
        super(opcode, line, column);
        this.old_opcode = opcode;
        this.expr = expr;
        this.oneByte = oneByte;
    }

    /// compile time ///
    @Override
    public void pass1() {
    }

    @Override
    public int pass2(Namespace parentEnv, int addr_start) throws Exception {
        expr.eval(parentEnv, addr_start);
        int val = expr.getValue();

        if (oneByte && (Expression.getSize(val) > 1)) {
            throw new Exception("[" + line + "," + column
                    + "] Error: value too large");
        }
        opcode = old_opcode;
        switch (opcode) {
            case DJNZ:
                val--;
                break;
            case BIT:
            case RES:
            case SET:
                if ((val > 7) || (val < 0)) {
                    throw new Exception("[" + line + "," + column + "]"
                            + " Error: value can be only in range 0-7");
                }
                opcode += (8 * val);
                break;
            case IM:
                switch (val) {
                    case 0:
                        opcode += 0x46;
                        break;
                    case 1:
                        opcode += 0x56;
                        break;
                    case 2:
                        opcode += 0x5E;
                        break;
                    default:
                        throw new Exception("[" + line + "," + column + "]"
                                + " Error: value can be only 0,1 or 2");
                }
                break;
            case RL_IIX_NN:
            case RL_IIY_NN:
            case RLC_IIX_NN:
            case RLC_IIY_NN:
            case RR_IIX_NN:
            case RR_IIY_NN:
            case RRC_IIX_NN:
            case RRC_IIY_NN:
            case SLA_IIX_NN:
            case SLA_IIY_NN:
            case SRA_IIX_NN:
            case SRA_IIY_NN:
            case SLL_IIX_NN:
            case SLL_IIY_NN:
            case SRL_IIX_NN:
            case SRL_IIY_NN:
                opcode += ((val << 8) & 0xFF00);
                break;
            case RST:
                switch (val) {
                    case 0:
                        break;
                    case 8:
                        opcode = 0xCF;
                        break;
                    case 0x10:
                        opcode = 0xD7;
                        break;
                    case 0x18:
                        opcode = 0xDF;
                        break;
                    case 0x20:
                        opcode = 0xE7;
                        break;
                    case 0x28:
                        opcode = 0xEF;
                        break;
                    case 0x30:
                        opcode = 0xF7;
                        break;
                    case 0x38:
                        opcode = 0xFF;
                        break;
                    default:
                        throw new Exception("[" + line + "," + column + "]"
                                + " Error: value can be only 0,8h,10h,18h,20h,"
                                + "28h,30h or 38h");
                }
                break;
            case JR:
                //   if (val < 0) val = (0xFF-(val+1))&0xff;
                // else
                val = (val - 2) & 0xff;
                opcode += Expression.reverseBytes(val, 1);
                break;
            default:
                if (oneByte) {
                    opcode += Expression.reverseBytes(val, 1);
                } else {
                    opcode += Expression.reverseBytes(val, 2);
                }
        }
        return (addr_start + getSize());
    }

    @Override
    public void pass4(HEXFileHandler hex) throws Exception {
        String s;
        if (getSize() == 1) {
            s = "%1$02X";
        } else if (getSize() == 2) {
            s = "%1$04X";
        } else if (getSize() == 3) {
            s = "%1$06X";
        } else {
            s = "%1$08X";
        }
        hex.putCode(String.format(s, opcode));
    }
}
