/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class DisassemblerTest {

    @Test
    public void testIsInstructionForAllValidOpcodes() {
        int[] validOpcodes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
        for (int opcode : validOpcodes) {
            assertTrue("Opcode " + opcode + " should be an instruction", Disassembler.isInstruction(opcode));
        }
    }

    @Test
    public void testIsInstructionForInvalidOpcodes() {
        assertFalse(Disassembler.isInstruction(0));
        assertFalse(Disassembler.isInstruction(19));
        assertFalse(Disassembler.isInstruction(-1));
        assertFalse(Disassembler.isInstruction(100));
    }

    @Test
    public void testDisassembleMnemoRead() {
        assertEquals(Optional.of("READ"), Disassembler.disassembleMnemo(1));
    }

    @Test
    public void testDisassembleMnemoWriteImmediate() {
        assertEquals(Optional.of("WRITE ="), Disassembler.disassembleMnemo(2));
    }

    @Test
    public void testDisassembleMnemoWrite() {
        assertEquals(Optional.of("WRITE"), Disassembler.disassembleMnemo(3));
    }

    @Test
    public void testDisassembleMnemoLoadImmediate() {
        assertEquals(Optional.of("LOAD ="), Disassembler.disassembleMnemo(4));
    }

    @Test
    public void testDisassembleMnemoLoad() {
        assertEquals(Optional.of("LOAD"), Disassembler.disassembleMnemo(5));
    }

    @Test
    public void testDisassembleMnemoStore() {
        assertEquals(Optional.of("STORE"), Disassembler.disassembleMnemo(6));
    }

    @Test
    public void testDisassembleMnemoAddImmediate() {
        assertEquals(Optional.of("ADD ="), Disassembler.disassembleMnemo(7));
    }

    @Test
    public void testDisassembleMnemoAdd() {
        assertEquals(Optional.of("ADD"), Disassembler.disassembleMnemo(8));
    }

    @Test
    public void testDisassembleMnemoSubImmediate() {
        assertEquals(Optional.of("SUB ="), Disassembler.disassembleMnemo(9));
    }

    @Test
    public void testDisassembleMnemoSub() {
        assertEquals(Optional.of("SUB"), Disassembler.disassembleMnemo(10));
    }

    @Test
    public void testDisassembleMnemoMulImmediate() {
        assertEquals(Optional.of("MUL ="), Disassembler.disassembleMnemo(11));
    }

    @Test
    public void testDisassembleMnemoMul() {
        assertEquals(Optional.of("MUL"), Disassembler.disassembleMnemo(12));
    }

    @Test
    public void testDisassembleMnemoDivImmediate() {
        assertEquals(Optional.of("DIV ="), Disassembler.disassembleMnemo(13));
    }

    @Test
    public void testDisassembleMnemoDiv() {
        assertEquals(Optional.of("DIV"), Disassembler.disassembleMnemo(14));
    }

    @Test
    public void testDisassembleMnemoJmp() {
        assertEquals(Optional.of("JMP"), Disassembler.disassembleMnemo(Disassembler.JMP));
    }

    @Test
    public void testDisassembleMnemoJz() {
        assertEquals(Optional.of("JZ"), Disassembler.disassembleMnemo(Disassembler.JZ));
    }

    @Test
    public void testDisassembleMnemoJgtz() {
        assertEquals(Optional.of("JGTZ"), Disassembler.disassembleMnemo(Disassembler.JGTZ));
    }

    @Test
    public void testDisassembleMnemoHalt() {
        assertEquals(Optional.of("HALT"), Disassembler.disassembleMnemo(Disassembler.HALT));
    }

    @Test
    public void testDisassembleMnemoInvalidReturnsEmpty() {
        assertEquals(Optional.empty(), Disassembler.disassembleMnemo(0));
        assertEquals(Optional.empty(), Disassembler.disassembleMnemo(19));
        assertEquals(Optional.empty(), Disassembler.disassembleMnemo(-1));
    }

    @Test
    public void testConstants() {
        assertEquals(1, Disassembler.READ);
        assertEquals(15, Disassembler.JMP);
        assertEquals(16, Disassembler.JZ);
        assertEquals(17, Disassembler.JGTZ);
        assertEquals(18, Disassembler.HALT);
    }
}

