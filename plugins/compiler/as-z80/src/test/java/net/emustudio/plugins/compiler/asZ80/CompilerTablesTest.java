/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80;

import org.junit.Test;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.*;

public class CompilerTablesTest {

    @Test
    public void testRegisters() {
        assertEquals(0, (int) CompilerTables.registers.get(REG_B));
        assertEquals(1, (int) CompilerTables.registers.get(REG_C));
        assertEquals(2, (int) CompilerTables.registers.get(REG_D));
        assertEquals(3, (int) CompilerTables.registers.get(REG_E));
        assertEquals(4, (int) CompilerTables.registers.get(REG_H));
        assertEquals(5, (int) CompilerTables.registers.get(REG_L));
        assertEquals(6, (int) CompilerTables.registers.get(REG_HL));
        assertEquals(7, (int) CompilerTables.registers.get(REG_A));
    }

    @Test
    public void testIXHIXLRegisters() {
        assertEquals(4, (int) CompilerTables.registers.get(REG_IXH));
        assertEquals(4, (int) CompilerTables.registers.get(REG_IYH));
        assertEquals(5, (int) CompilerTables.registers.get(REG_IXL));
        assertEquals(5, (int) CompilerTables.registers.get(REG_IYL));
    }

    @Test
    public void testRegPairs() {
        assertEquals(0, (int) CompilerTables.regPairs.get(REG_BC));
        assertEquals(1, (int) CompilerTables.regPairs.get(REG_DE));
        assertEquals(2, (int) CompilerTables.regPairs.get(REG_HL));
        assertEquals(3, (int) CompilerTables.regPairs.get(REG_SP));
    }

    @Test
    public void testRegPairsII() {
        assertEquals(0, (int) CompilerTables.regPairsII.get(REG_BC));
        assertEquals(1, (int) CompilerTables.regPairsII.get(REG_DE));
        assertEquals(2, (int) CompilerTables.regPairsII.get(REG_IX));
        assertEquals(2, (int) CompilerTables.regPairsII.get(REG_IY));
        assertEquals(3, (int) CompilerTables.regPairsII.get(REG_SP));
    }

    @Test
    public void testRegPairs2() {
        assertEquals(0, (int) CompilerTables.regPairs2.get(REG_BC));
        assertEquals(1, (int) CompilerTables.regPairs2.get(REG_DE));
        assertEquals(2, (int) CompilerTables.regPairs2.get(REG_HL));
        assertEquals(3, (int) CompilerTables.regPairs2.get(REG_AF));
    }

    @Test
    public void testConditions() {
        assertEquals(0, (int) CompilerTables.conditions.get(COND_NZ));
        assertEquals(1, (int) CompilerTables.conditions.get(COND_Z));
        assertEquals(2, (int) CompilerTables.conditions.get(COND_NC));
        assertEquals(3, (int) CompilerTables.conditions.get(COND_C));
        assertEquals(4, (int) CompilerTables.conditions.get(COND_PO));
        assertEquals(5, (int) CompilerTables.conditions.get(COND_PE));
        assertEquals(6, (int) CompilerTables.conditions.get(COND_P));
        assertEquals(7, (int) CompilerTables.conditions.get(COND_M));
    }

    @Test
    public void testAlu() {
        assertEquals(0, (int) CompilerTables.alu.get(OPCODE_ADD));
        assertEquals(1, (int) CompilerTables.alu.get(OPCODE_ADC));
        assertEquals(2, (int) CompilerTables.alu.get(OPCODE_SUB));
        assertEquals(3, (int) CompilerTables.alu.get(OPCODE_SBC));
        assertEquals(4, (int) CompilerTables.alu.get(OPCODE_AND));
        assertEquals(5, (int) CompilerTables.alu.get(OPCODE_XOR));
        assertEquals(6, (int) CompilerTables.alu.get(OPCODE_OR));
        assertEquals(7, (int) CompilerTables.alu.get(OPCODE_CP));
    }

    @Test
    public void testRot() {
        assertEquals(0, (int) CompilerTables.rot.get(OPCODE_RLC));
        assertEquals(1, (int) CompilerTables.rot.get(OPCODE_RRC));
        assertEquals(2, (int) CompilerTables.rot.get(OPCODE_RL));
        assertEquals(3, (int) CompilerTables.rot.get(OPCODE_RR));
        assertEquals(4, (int) CompilerTables.rot.get(OPCODE_SLA));
        assertEquals(5, (int) CompilerTables.rot.get(OPCODE_SRA));
        assertEquals(6, (int) CompilerTables.rot.get(OPCODE_SLL));
        assertEquals(7, (int) CompilerTables.rot.get(OPCODE_SRL));
    }

    @Test
    public void testIm() {
        assertEquals(0, (int) CompilerTables.im.get(IM_0));
        assertEquals(1, (int) CompilerTables.im.get(IM_01));
        assertEquals(2, (int) CompilerTables.im.get(IM_1));
        assertEquals(3, (int) CompilerTables.im.get(IM_2));
    }

    @Test
    public void testBlock() {
        assertEquals(4, (int) CompilerTables.block.get(OPCODE_LDI).l);
        assertEquals(0, (int) CompilerTables.block.get(OPCODE_LDI).r);
        assertEquals(5, (int) CompilerTables.block.get(OPCODE_LDD).l);
        assertEquals(6, (int) CompilerTables.block.get(OPCODE_LDIR).l);
        assertEquals(7, (int) CompilerTables.block.get(OPCODE_LDDR).l);

        assertEquals(4, (int) CompilerTables.block.get(OPCODE_CPI).l);
        assertEquals(1, (int) CompilerTables.block.get(OPCODE_CPI).r);

        assertEquals(4, (int) CompilerTables.block.get(OPCODE_INI).l);
        assertEquals(2, (int) CompilerTables.block.get(OPCODE_INI).r);

        assertEquals(4, (int) CompilerTables.block.get(OPCODE_OUTI).l);
        assertEquals(3, (int) CompilerTables.block.get(OPCODE_OUTI).r);
    }

    @Test
    public void testPrefix() {
        assertEquals(0xDD, (int) CompilerTables.prefix.get(REG_IX));
        assertEquals(0xDD, (int) CompilerTables.prefix.get(REG_IXH));
        assertEquals(0xDD, (int) CompilerTables.prefix.get(REG_IXL));
        assertEquals(0xFD, (int) CompilerTables.prefix.get(REG_IY));
        assertEquals(0xFD, (int) CompilerTables.prefix.get(REG_IYH));
        assertEquals(0xFD, (int) CompilerTables.prefix.get(REG_IYL));
    }
}

