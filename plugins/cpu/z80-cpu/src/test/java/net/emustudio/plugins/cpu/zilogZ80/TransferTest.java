/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.RunnerContext;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

import java.util.function.Function;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.get8MSBplus8LSB;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.predicate8MSBplus8LSB;

/**
 * Data-transfer instructions: LD (all forms), EX/EXX, block moves (LDI/LDIR/LDD/LDDR),
 * block compares (CPI/CPIR/CPD/CPDR) and RLD/RRD. One test per logical instruction; all
 * addressing-mode variants are exercised in the same method with the matching cycle/R counts.
 * <p>
 * Cycle reference (Z80 official):
 * - LD r,r' / r,(HL) / (HL),r:        4T (regs) | 7T (memory side); R+=1
 * - LD r,n:                            7T  R+=1
 * - LD (HL),n:                        10T  R+=1
 * - LD r,(IX+d) / (IX+d),r / (IX+d),n:19T  R+=2
 * - LD A,(BC|DE) / (BC|DE),A:          7T  R+=1
 * - LD A,(nn) / (nn),A:               13T  R+=1
 * - LD A,I / A,R / I,A / R,A:          9T  R+=2
 * - LD rr,nn:                         10T  R+=1
 * - LD IX/IY,nn:                      14T  R+=2
 * - LD HL,(nn) / (nn),HL:             16T  R+=1
 * - LD rr,(nn) / (nn),rr (ED):        20T  R+=2
 * - LD IX/IY,(nn) / (nn),IX/IY:       20T  R+=2
 * - LD SP,HL:                          6T  R+=1
 * - LD SP,IX/IY:                      10T  R+=2
 * - LD IXh|IXl|IYh|IYl,n:             11T  R+=2
 * - LD r,IXh|IXl|IYh|IYl / vice versa: 8T  R+=2
 * - EX DE,HL / AF,AF' / EXX:           4T  R+=1
 * - EX (SP),HL:                       19T  R+=1
 * - EX (SP),IX/IY:                    23T  R+=2
 * - LDI/LDD/CPI/CPD:                  16T  R+=2
 * - LDIR/LDDR/CPIR/CPDR repeat:       21T  R+=2 (or 16T on final iteration)
 * - RLD/RRD:                          18T  R+=2
 */
public class TransferTest extends InstructionsTest {

    // ---- LD r,r' / r,(HL) / (HL),r --------------------------------------------------------

    @Test
    public void testLD__r__r() {
        // LD r,r' covers 7 destinations × 7 sources (B,C,D,E,H,L,A). r=(HL) is excluded here.
        // Opcodes are 0x40 + dst*8 + src per Z80 encoding.
        int[] regs = {REG_B, REG_C, REG_D, REG_E, REG_H, REG_L, REG_A};
        int[] lo3 = {0, 1, 2, 3, 4, 5, 7};
        for (int d = 0; d < regs.length; d++) {
            for (int s = 0; s < regs.length; s++) {
                int dst = regs[d];
                int src = regs[s];
                int opcode = 0x40 | (lo3[d] << 3) | lo3[s];
                if (opcode == 0x76) continue; // HALT, not an LD
                ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .firstIsRegister(dst)
                        .secondIsRegister(src)
                        .verifyRegister(dst, c -> c.second & 0xFF)
                        .verifyR(1).verifyCycles(4);
                Generator.forSome8bitBinary(test.run(opcode));
            }
        }
    }

    @Test
    public void testLD__r__mHL() {
        // LD r,(HL): opcodes 0x46 + r*8 — for r=B,C,D,E,H,L,A.
        int[][] regOpc = {{REG_B, 0x46}, {REG_C, 0x4E}, {REG_D, 0x56}, {REG_E, 0x5E},
                {REG_H, 0x66}, {REG_L, 0x6E}, {REG_A, 0x7E}};
        for (int[] ro : regOpc) {
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsRegister(ro[0])
                    .secondIsMemoryByteAt(0x303).setPair(REG_PAIR_HL, 0x303)
                    .verifyRegister(ro[0], c -> c.second & 0xFF)
                    .verifyR(1).verifyCycles(7);
            Generator.forSome8bitBinary(test.run(ro[1]));
        }
    }

    @Test
    public void testLD__mHL__r() {
        int[][] regOpc = {{REG_B, 0x70}, {REG_C, 0x71}, {REG_D, 0x72}, {REG_E, 0x73},
                {REG_A, 0x77}};   // H=0x74, L=0x75 are special (write H/L to (HL))
        for (int[] ro : regOpc) {
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsMemoryByteAt(0x303).setPair(REG_PAIR_HL, 0x303)
                    .secondIsRegister(ro[0])
                    .verifyByte(0x303, c -> c.second & 0xFF)
                    .verifyR(1).verifyCycles(7);
            Generator.forSome8bitBinary(test.run(ro[1]));
        }

        // LD (HL),H and LD (HL),L: source comes from H/L of HL, so result = H or L of address itself.
        IntegerTestBuilder testHL = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .verifyR(1).verifyCycles(7)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitBinary(1,
                testHL.verifyByte(c -> c.first, c -> (c.first >>> 8) & 0xFF).run(0x74),
                testHL.verifyByte(c -> c.first, c -> c.first & 0xFF).run(0x75)
        );
    }

    @Test
    public void testLD__r__n() {
        // LD r,n — 7T R+=1
        int[][] regOpc = {{REG_A, 0x3E}, {REG_B, 0x06}, {REG_C, 0x0E}, {REG_D, 0x16},
                {REG_E, 0x1E}, {REG_H, 0x26}, {REG_L, 0x2E}};
        for (int[] ro : regOpc) {
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsRegister(ro[0])
                    .verifyRegister(ro[0], c -> c.second & 0xFF)
                    .verifyR(1).verifyCycles(7);
            Generator.forSome8bitBinary(test.runWithSecondOperand(ro[1]));
        }
    }

    @Test
    public void testLD__mHL__n() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryByteAt(0x303).setPair(REG_PAIR_HL, 0x303)
                .verifyByte(0x303, c -> c.second & 0xFF)
                .verifyR(1).verifyCycles(10);
        Generator.forSome8bitBinary(test.runWithSecondOperand(0x36));
    }

    @Test
    public void testLD__r__mII_plus_d() {
        // 19T R+=2; opcodes 0x46+r*8 same as (HL) but DD/FD prefixed.
        int[][] regOpc = {{REG_A, 0x7E}, {REG_B, 0x46}, {REG_C, 0x4E}, {REG_D, 0x56},
                {REG_E, 0x5E}, {REG_H, 0x66}, {REG_L, 0x6E}};
        for (int[] ro : regOpc) {
            for (int prefix : new int[]{0xDD, 0xFD}) {
                IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                        .first8LSBisRegister(ro[0])
                        .verifyRegister(ro[0], c -> c.second & 0xFF)
                        .verifyR(2).verifyCycles(19)
                        .keepCurrentInjectorsAfterRun();
                if (prefix == 0xDD) test = test.first8MSBisIX();
                else test = test.first8MSBisIY();
                Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                        test.runWithFirst8bitOperand(prefix, ro[1]));
            }
        }
    }

    @Test
    public void testLD__mII_plus_d__r() {
        // 19T R+=2; opcodes 0x70+r — for r=A,B,C,D,E,H,L
        int[][] regOpc = {{REG_A, 0x77}, {REG_B, 0x70}, {REG_C, 0x71}, {REG_D, 0x72},
                {REG_E, 0x73}, {REG_H, 0x74}, {REG_L, 0x75}};
        for (int[] ro : regOpc) {
            for (int prefix : new int[]{0xDD, 0xFD}) {
                IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                        .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                        .first8LSBisRegister(ro[0])
                        .verifyByte(c -> get8MSBplus8LSB(c.first), c -> c.first & 0xFF)
                        .verifyR(2).verifyCycles(19)
                        .keepCurrentInjectorsAfterRun();
                if (prefix == 0xDD) test = test.first8MSBisIX();
                else test = test.first8MSBisIY();
                Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(3),
                        test.runWithFirst8bitOperand(prefix, ro[1]));
            }
        }
    }

    @Test
    public void testLD__mII_plus_d__n() {
        for (int prefix : new int[]{0xDD, 0xFD}) {
            IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .first8MSBplus8LSBisMemoryAddressAndSecondIsMemoryByte()
                    .verifyByte(c -> get8MSBplus8LSB(c.first), c -> c.first & 0xFF)
                    .verifyR(2).verifyCycles(19)
                    .keepCurrentInjectorsAfterRun();
            if (prefix == 0xDD) test = test.first8MSBisIX();
            else test = test.first8MSBisIY();
            Generator.forSome16bitBinaryFirstSatisfying(predicate8MSBplus8LSB(4),
                    test.runWithFirst8bitOperandTwoTimes(prefix, 0x36));
        }
    }

    // ---- LD A,(rr) / (rr),A / A,(nn) / (nn),A --------------------------------------------

    @Test
    public void testLD__A__mrr() {
        // LD A,(BC) = 0x0A; LD A,(DE) = 0x1A; 7T R+=1
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .verifyRegister(REG_A, c -> c.second & 0xFF)
                .verifyR(1).verifyCycles(7)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinary(1,
                test.firstIsPair(REG_PAIR_BC).run(0x0A),
                test.firstIsPair(REG_PAIR_DE).run(0x1A)
        );
    }

    @Test
    public void testLD__mrr__A() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyByte(c -> c.first, c -> c.first & 0xFF)
                .verifyR(1).verifyCycles(7)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinary(1,
                test.firstIsPair(REG_PAIR_BC).run(0x02),
                test.firstIsPair(REG_PAIR_DE).run(0x12)
        );
    }

    @Test
    public void testLD__A__mnn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .verifyRegister(REG_A, c -> c.second & 0xFF)
                .verifyR(1).verifyCycles(13);
        Generator.forSome16bitBinary(3, test.runWithFirstOperand(0x3A));
    }

    @Test
    public void testLD__mnn__A() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .first8LSBisRegister(REG_A)
                .verifyByte(c -> c.first, c -> c.first & 0xFF)
                .verifyR(1).verifyCycles(13);
        Generator.forSome16bitBinary(test.runWithFirstOperand(0x32));
    }

    // ---- LD A,I / A,R / I,A / R,A ---------------------------------------------------------

    @Test
    public void testLD__A__I() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .secondIsRegisterI()
                .setFlags(FLAG_H | FLAG_N)
                .verifyRegister(REG_A, c -> c.second & 0xFF)
                .verifyR(2).verifyCycles(9);
        Generator.forSome8bitBinary(test.run(0xED, 0x57));
    }

    @Test
    public void testLD__A__R() {
        // R increments twice during ED 5F (once per opcode fetch). So expected A = (initial R + 2)
        // with the high (sign) bit preserved. R itself is the operand here so verifyR cannot be used.
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegister(REG_A)
                .secondIsRegisterR()
                .setFlags(FLAG_H | FLAG_N)
                .verifyRegister(REG_A, c -> (c.second & 0x80) | (((c.second & 0x7F) + 2) & 0x7F))
                .verifyCycles(9);
        Generator.forSome8bitBinary(test.run(0xED, 0x5F));
    }

    @Test
    public void testLD__I__A() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegisterI()
                .secondIsRegister(REG_A)
                .verifyRegisterI(c -> c.second & 0xFF)
                .verifyR(2).verifyCycles(9);
        Generator.forSome8bitBinary(test.run(0xED, 0x47));
    }

    @Test
    public void testLD__R__A() {
        // R is the destination register, then incremented by 2 by the engine. Verify only the
        // explicit destination through verifyRegisterR; cannot use verifyR (R is the operand).
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsRegisterR()
                .secondIsRegister(REG_A)
                .verifyRegisterR(c -> c.second & 0xFF)
                .verifyCycles(9);
        Generator.forSome8bitBinary(test.run(0xED, 0x4F));
    }

    // ---- LD rr,nn / IX,IY,nn --------------------------------------------------------------

    @Test
    public void testLD__rr__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyR(1).verifyCycles(10)
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, c -> c.first).runWithFirstOperand(0x01),
                test.verifyPair(REG_PAIR_DE, c -> c.first).runWithFirstOperand(0x11),
                test.verifyPair(REG_PAIR_HL, c -> c.first).runWithFirstOperand(0x21),
                test.verifyPair(REG_SP, c -> c.first).runWithFirstOperand(0x31)
        );
    }

    @Test
    public void testLD__II__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyR(2).verifyCycles(14)
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitUnary(
                test.verifyIX(c -> c.first).runWithFirstOperand(0xDD, 0x21),
                test.verifyIY(c -> c.first).runWithFirstOperand(0xFD, 0x21)
        );
    }

    // ---- LD HL/rr/IX/IY,(nn) / (nn),HL/rr/IX/IY -------------------------------------------

    @Test
    public void testLD__HL__mnn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyPair(REG_PAIR_HL, c -> c.second)
                .verifyR(1).verifyCycles(16);
        Generator.forSome16bitBinary(3, test.runWithFirstOperand(0x2A));
    }

    @Test
    public void testLD__rr__mnn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyR(2).verifyCycles(20)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitBinary(4,
                test.verifyPair(REG_PAIR_BC, c -> c.second).runWithFirstOperand(0xED, 0x4B),
                test.verifyPair(REG_PAIR_DE, c -> c.second).runWithFirstOperand(0xED, 0x5B),
                test.verifyPair(REG_PAIR_HL, c -> c.second).runWithFirstOperand(0xED, 0x6B),
                test.verifyPair(REG_SP, c -> c.second).runWithFirstOperand(0xED, 0x7B)
        );
    }

    @Test
    public void testLD__II__mnn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyR(2).verifyCycles(20)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitBinary(4,
                test.verifyIX(c -> c.second).runWithFirstOperand(0xDD, 0x2A),
                test.verifyIY(c -> c.second).runWithFirstOperand(0xFD, 0x2A)
        );
    }

    @Test
    public void testLD__mnn__HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_PAIR_HL)
                .verifyWord(c -> c.first, c -> c.first)
                .verifyR(1).verifyCycles(16);
        Generator.forSome16bitBinary(3, test.runWithFirstOperand(0x22));
    }

    @Test
    public void testLD__mnn__rr() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyWord(c -> c.first, c -> c.first)
                .verifyR(2).verifyCycles(20)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinary(4,
                test.firstIsPair(REG_PAIR_BC).runWithFirstOperand(0xED, 0x43),
                test.firstIsPair(REG_PAIR_DE).runWithFirstOperand(0xED, 0x53),
                test.firstIsPair(REG_PAIR_HL).runWithFirstOperand(0xED, 0x63),
                test.firstIsPair(REG_SP).runWithFirstOperand(0xED, 0x73)
        );
    }

    @Test
    public void testLD__mnn__II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .verifyWord(c -> c.first, c -> c.first)
                .verifyR(2).verifyCycles(20)
                .keepCurrentInjectorsAfterRun();
        Generator.forSome16bitBinary(4,
                test.firstIsIX().runWithFirstOperand(0xDD, 0x22),
                test.firstIsIY().runWithFirstOperand(0xFD, 0x22)
        );
    }

    // ---- LD SP,HL / LD SP,IX/IY -----------------------------------------------------------

    @Test
    public void testLD__SP__HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_SP, c -> c.first)
                .verifyR(1).verifyCycles(6);
        Generator.forSome16bitUnary(test.run(0xF9));
    }

    @Test
    public void testLD__SP__II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .verifyPair(REG_SP, c -> c.first)
                .verifyR(2).verifyCycles(10)
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitUnary(
                test.firstIsIX().run(0xDD, 0xF9),
                test.firstIsIY().run(0xFD, 0xF9)
        );
    }

    // ---- LD IIH/IIL,n  /  LD IIH/IIL,r  /  LD r,IIH/IIL ----------------------------------

    @Test
    public void testLD__IIhl__n() {
        // 11T R+=2 — DD/FD 26/2E nn
        runLDIIhlImm(0xDD, 0x26, true);   // IXh
        runLDIIhlImm(0xDD, 0x2E, false);  // IXl
        runLDIIhlImm(0xFD, 0x26, true);   // IYh
        runLDIIhlImm(0xFD, 0x2E, false);  // IYl
    }

    private void runLDIIhlImm(int prefix, int opcode, boolean high) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIX_or_IY(prefix == 0xFD)
                .verifyR(2).verifyCycles(11);
        if (high) test = test.verifyIX_or_IY(prefix == 0xFD,
                c -> (c.second & 0xFF) | ((c.first << 8) & 0xFF00));
        else test = test.verifyIX_or_IY(prefix == 0xFD,
                c -> (c.second & 0xFF00) | (c.first & 0xFF));
        Generator.forSome16bitBinary(test.runWithFirst8bitOperand(prefix, opcode));
    }

    @Test
    public void testLD__r__IIhl() {
        // LD r,IXh/IXl/IYh/IYl — 8T R+=2; opcodes 0x44/0x45/0x54/0x55/0x5C/0x5D/0x7C/0x7D etc.
        // For each register r in {B,C,D,E,A}, opcode high-source = 0x40+r*8+4, low-source = 0x40+r*8+5.
        int[][] regOpcH = {{REG_B, 0x44}, {REG_C, 0x4C}, {REG_D, 0x54}, {REG_E, 0x5C}, {REG_A, 0x7C}};
        int[][] regOpcL = {{REG_B, 0x45}, {REG_C, 0x4D}, {REG_D, 0x55}, {REG_E, 0x5D}, {REG_A, 0x7D}};
        for (int prefix : new int[]{0xDD, 0xFD}) {
            for (int[] ro : regOpcH) runLDrFromII(prefix, ro[0], ro[1], true);
            for (int[] ro : regOpcL) runLDrFromII(prefix, ro[0], ro[1], false);
        }
    }

    private void runLDrFromII(int prefix, int reg, int opcode, boolean high) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIX_or_IY(prefix == 0xFD)
                .first8LSBisRegister(reg)
                .verifyRegister(reg, c -> high ? (c.second >>> 8) & 0xFF : c.second & 0xFF)
                .verifyR(2).verifyCycles(8);
        Generator.forSome16bitBinary(test.run(prefix, opcode));
    }

    @Test
    public void testLD__IIhl__r() {
        // LD IXh/IYh,r — opcodes 0x60..0x67 (60..63=B/C/D/E, 67=A; 65=IXl/IYl)
        // LD IXl/IYl,r — opcodes 0x68..0x6F
        int[][] regOpcH = {{REG_B, 0x60}, {REG_C, 0x61}, {REG_D, 0x62}, {REG_E, 0x63}, {REG_A, 0x67}};
        int[][] regOpcL = {{REG_B, 0x68}, {REG_C, 0x69}, {REG_D, 0x6A}, {REG_E, 0x6B}, {REG_A, 0x6F}};
        for (int prefix : new int[]{0xDD, 0xFD}) {
            for (int[] ro : regOpcH) runLDIIfromR(prefix, ro[0], ro[1], true);
            for (int[] ro : regOpcL) runLDIIfromR(prefix, ro[0], ro[1], false);
        }

        // LD IXh,IXl / LD IYh,IYl (opcode 0x65) — copy low byte of II to its high byte.
        for (int prefix : new int[]{0xDD, 0xFD}) {
            IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsIX_or_IY(prefix == 0xFD)
                    .verifyIX_or_IY(prefix == 0xFD,
                            c -> ((c.first << 8) & 0xFF00) | (c.first & 0xFF))
                    .verifyR(2).verifyCycles(8);
            Generator.forSome16bitBinary(test.run(prefix, 0x65));
        }

        // LD IXl,IXh / LD IYl,IYh (opcode 0x6C)
        for (int prefix : new int[]{0xDD, 0xFD}) {
            IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsIX_or_IY(prefix == 0xFD)
                    .verifyIX_or_IY(prefix == 0xFD,
                            c -> (c.first >>> 8) | (c.first & 0xFF00))
                    .verifyR(2).verifyCycles(8);
            Generator.forSome16bitBinary(test.run(prefix, 0x6C));
        }
    }

    private void runLDIIfromR(int prefix, int reg, int opcode, boolean high) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsIX_or_IY(prefix == 0xFD)
                .first8LSBisRegister(reg)
                .verifyR(2).verifyCycles(8);
        if (high) test = test.verifyIX_or_IY(prefix == 0xFD,
                c -> (c.second & 0xFF) | ((c.first << 8) & 0xFF00));
        else test = test.verifyIX_or_IY(prefix == 0xFD,
                c -> (c.second & 0xFF00) | (c.first & 0xFF));
        Generator.forSome16bitBinary(test.run(prefix, opcode));
    }

    // ---- EX / EXX -------------------------------------------------------------------------

    @Test
    public void testEX__DE__HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_DE, c -> c.second)
                .verifyPair(REG_PAIR_HL, c -> c.first)
                .verifyR(1).verifyCycles(4);
        Generator.forSome16bitBinary(test.run(0xEB));
    }

    @Test
    public void testEX__AF__AF2() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAF()
                .secondIsAF2()
                .verifyAF(c -> c.second)
                .verifyAF2(c -> c.first)
                .verifyR(1).verifyCycles(4);
        Generator.forSome16bitBinary(test.run(0x08));
    }

    @Test
    public void testEXX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsPair(REG_PAIR_BC).firstIsPair(REG_PAIR_DE).firstIsPair(REG_PAIR_HL)
                .secondIsPair2(REG_PAIR_BC).secondIsPair2(REG_PAIR_DE).secondIsPair2(REG_PAIR_HL)
                .verifyPair(REG_PAIR_BC, c -> c.second)
                .verifyPair(REG_PAIR_DE, c -> c.second)
                .verifyPair(REG_PAIR_HL, c -> c.second)
                .verifyPair2(REG_PAIR_BC, c -> c.first)
                .verifyPair2(REG_PAIR_DE, c -> c.first)
                .verifyPair2(REG_PAIR_HL, c -> c.first)
                .verifyR(1).verifyCycles(4);
        Generator.forSome16bitBinary(test.run(0xD9));
    }

    @Test
    public void testEX__mSP__HL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .firstIsPair(REG_PAIR_HL)
                .verifyPair(REG_PAIR_HL, c -> c.second)
                .verifyWord(c -> c.first, c -> c.first)
                .verifyR(1).verifyCycles(19);
        Generator.forSome16bitBinary(1, test.run(0xE3));
    }

    @Test
    public void testEX__mSP__II() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyWord(c -> c.first, c -> c.first)
                .verifyR(2).verifyCycles(23)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();
        Generator.forSome16bitBinary(2,
                test.firstIsIX().verifyIX(c -> c.second).run(0xDD, 0xE3),
                test.firstIsIY().verifyIY(c -> c.second).run(0xFD, 0xE3)
        );
    }

    // ---- Block transfers / compares ------------------------------------------------------

    @Test
    public void testLDI() {
        Generator.forSome16bitBinary(2, 2,
                blockMoveBuilder(true).verifyR(2).verifyCycles(16).run(0xED, 0xA0));
    }

    @Test
    public void testLDIR() {
        Generator.forSome16bitBinary(2, 2,
                blockMoveBuilder(true)
                        .verifyR(2)
                        .verifyCycles(blockRepeatCycles())
                        .verifyPC(blockRepeatPC())
                        .run(0xED, 0xB0));
    }

    @Test
    public void testLDD() {
        Generator.forSome16bitBinary(2, 2,
                blockMoveBuilder(false).verifyR(2).verifyCycles(16).run(0xED, 0xA8));
    }

    @Test
    public void testLDDR() {
        Generator.forSome16bitBinary(2, 2,
                blockMoveBuilder(false)
                        .verifyR(2)
                        .verifyCycles(blockRepeatCycles())
                        .verifyPC(blockRepeatPC())
                        .run(0xED, 0xB8));
    }

    @Test
    public void testCPI() {
        Generator.forSome16bitBinary(3, blockCompareBuilder(true).verifyR(2).verifyCycles(16).run(0xED, 0xA1));
    }

    @Test
    public void testCPIR() {
        Generator.forSome16bitBinary(3,
                blockCompareBuilder(true)
                        .verifyR(2)
                        .verifyCycles(blockCompareCycles(true))
                        .verifyPC(blockComparePC(true))
                        .run(0xED, 0xB1));
    }

    @Test
    public void testCPD() {
        Generator.forSome16bitBinary(3, blockCompareBuilder(false).verifyR(2).verifyCycles(16).run(0xED, 0xA9));
    }

    @Test
    public void testCPDR() {
        Generator.forSome16bitBinary(3,
                blockCompareBuilder(false)
                        .verifyR(2)
                        .verifyCycles(blockCompareCycles(false))
                        .verifyPC(blockComparePC(false))
                        .run(0xED, 0xB9));
    }

    private IntegerTestBuilder blockMoveBuilder(boolean increment) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .secondIsAddressAndFirstIsMemoryByte()
                .firstIsPair(REG_PAIR_DE)
                .secondIsPair(REG_PAIR_HL)
                .firstIsPair(REG_PAIR_BC)
                .verifyByte(c -> c.first, c -> c.first & 0xFF)
                .verifyPair(REG_PAIR_BC, c -> (c.first - 1) & 0xFFFF);
        return increment
                ? test.verifyPair(REG_PAIR_DE, c -> (c.first + 1) & 0xFFFF)
                  .verifyPair(REG_PAIR_HL, c -> (c.second + 1) & 0xFFFF)
                : test.verifyPair(REG_PAIR_DE, c -> (c.first - 1) & 0xFFFF)
                  .verifyPair(REG_PAIR_HL, c -> (c.second - 1) & 0xFFFF);
    }

    private IntegerTestBuilder blockCompareBuilder(boolean increment) {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_A)
                .registerIsRandom(REG_B, 0xFF)
                .registerIsRandom(REG_C, 0xFF)
                .verifyPair(REG_PAIR_BC, c ->
                        ((c.getRegister(REG_B) << 8 | c.getRegister(REG_C)) - 1));
        return increment
                ? test.verifyPair(REG_PAIR_HL, c ->
                                               ((c.getRegister(REG_H) << 8 | c.getRegister(REG_L)) + 1))
                : test.verifyPair(REG_PAIR_HL, c ->
                                               ((c.getRegister(REG_H) << 8 | c.getRegister(REG_L)) - 1));
    }

    private static Function<RunnerContext<Integer>, Integer> blockRepeatCycles() {
        return c -> (((c.first - 1) & 0xFFFF) == 0) ? 16 : 21;
    }

    private static Function<RunnerContext<Integer>, Integer> blockRepeatPC() {
        return c -> (((c.first - 1) & 0xFFFF) == 0) ? (c.PC + 2) & 0xFFFF : c.PC;
    }

    private static Function<RunnerContext<Integer>, Integer> blockCompareCycles(boolean unused) {
        return c -> {
            boolean equal = c.registers.get(REG_A) == (c.second & 0xFF);
            boolean bcZero = (((c.getRegister(REG_B) << 8 | c.getRegister(REG_C)) - 1) & 0xFFFF) == 0;
            return (equal || bcZero) ? 16 : 21;
        };
    }

    private static Function<RunnerContext<Integer>, Integer> blockComparePC(boolean unused) {
        return c -> {
            boolean equal = c.registers.get(REG_A) == (c.second & 0xFF);
            boolean bcZero = (((c.getRegister(REG_B) << 8 | c.getRegister(REG_C)) - 1) & 0xFFFF) == 0;
            return (equal || bcZero) ? c.PC + 2 : c.PC;
        };
    }

    // ---- RLD / RRD -----------------------------------------------------------------------

    @Test
    public void testRLD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_A)
                .verifyByte(c -> c.first, c -> ((c.second << 4) & 0xF0) | (c.first & 0x0F))
                .verifyRegister(REG_A, c -> (c.first & 0xF0) | ((c.second >>> 4) & 0x0F))
                .verifyR(2).verifyCycles(18);
        Generator.forSome16bitBinary(test.run(0xED, 0x6F));
    }

    @Test
    public void testRRD() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryByte()
                .firstIsPair(REG_PAIR_HL)
                .first8LSBisRegister(REG_A)
                .verifyByte(c -> c.first, c -> ((c.first << 4) & 0xF0) | ((c.second >>> 4) & 0x0F))
                .verifyRegister(REG_A, c -> (c.first & 0xF0) | (c.second & 0x0F))
                .verifyR(2).verifyCycles(18);
        Generator.forSome16bitBinary(test.run(0xED, 0x67));
    }
}

