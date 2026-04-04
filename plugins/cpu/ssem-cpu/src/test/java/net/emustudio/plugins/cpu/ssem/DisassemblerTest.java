/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisassemblerTest {
    private final ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
    private final DecoderImpl decoder = new DecoderImpl(memory);
    private final DisassemblerImpl disassembler = new DisassemblerImpl(memory, decoder);

    @Test
    public void testLDN() {
        memory.setMemory(new short[]{
                0x9B, 0xE2, 0xFC, 0x3F
        });

        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("LDN 25", instr.mnemo);
        assertEquals("9B E2 FC 3F", instr.opCode);
    }

    @Test
    public void testSTO() {
        memory.setMemory(new short[]{
                0, 0, 0, 0,
                0x68, 0x06, 0x00, 0x00
        });

        DisassembledInstruction instr = disassembler.disassemble(4);
        assertEquals("STO 22", instr.mnemo);
        assertEquals("68 06 00 00", instr.opCode);
    }

    @Test
    public void testJMP() {
        // line raw=20 (10100), reversed=5 (00101); opcode 000 → JMP
        // byte[0]=0xA0 (10100_000), byte[1]=0x00 (...000)
        memory.setMemory(new short[]{0xA0, 0x00, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("JMP 5", instr.mnemo);
        assertEquals("A0 00 00 00", instr.opCode);
    }

    @Test
    public void testJPR() {
        // line raw=20 (10100), reversed=5; opcode 100 → JPR
        // byte[0]=0xA0, byte[1]=0x04
        memory.setMemory(new short[]{0xA0, 0x04, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("JPR 5", instr.mnemo);
        assertEquals("A0 04 00 00", instr.opCode);
    }

    @Test
    public void testSUB() {
        // line raw=20 (10100), reversed=5; opcode 001 → SUB
        // byte[0]=0xA0, byte[1]=0x01
        memory.setMemory(new short[]{0xA0, 0x01, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("SUB 5", instr.mnemo);
        assertEquals("A0 01 00 00", instr.opCode);
    }

    @Test
    public void testCMP() {
        // line must be 00000; opcode 011 → CMP
        // byte[0]=0x00, byte[1]=0x03
        memory.setMemory(new short[]{0x00, 0x03, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("CMP", instr.mnemo);
        assertEquals("00 03 00 00", instr.opCode);
    }

    @Test
    public void testSTP() {
        // line must be 00000; opcode 111 → STP
        // byte[0]=0x00, byte[1]=0x07
        memory.setMemory(new short[]{0x00, 0x07, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("STP", instr.mnemo);
        assertEquals("00 07 00 00", instr.opCode);
    }

    @Test
    public void testDataInstruction() {
        // opcode 101 with non-zero line → not a valid instruction → data
        memory.setMemory(new short[]{0x80, 0x05, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        // data instructions are formatted as hex, not as instruction mnemonics
        assertNotNull(instr.mnemo);
        assertNotEquals("unknown", instr.mnemo);
        assertFalse(instr.mnemo.startsWith("JMP"));
        assertFalse(instr.mnemo.startsWith("LDN"));
        assertFalse(instr.mnemo.startsWith("SUB"));
    }

    @Test
    public void testGetNextInstructionPosition() {
        memory.setMemory(new short[]{0x9B, 0xE2, 0xFC, 0x3F});
        // SSEM instructions are always 4 bytes
        assertEquals(4, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testGetNextInstructionPositionAtOffset() {
        memory.setMemory(new short[]{
                0, 0, 0, 0,
                0x68, 0x06, 0x00, 0x00
        });
        assertEquals(8, disassembler.getNextInstructionPosition(4));
    }

    @Test
    public void testDisassembleTwiceSameAddress() {
        // Tests the cached render path in DisassemblerImpl
        memory.setMemory(new short[]{0x9B, 0xE2, 0xFC, 0x3F});
        DisassembledInstruction first = disassembler.disassemble(0);
        DisassembledInstruction second = disassembler.disassemble(0);
        assertEquals(first.mnemo, second.mnemo);
        assertEquals(first.opCode, second.opCode);
    }

    @Test
    public void testDecoderDecode() throws InvalidInstructionException {
        memory.setMemory(new short[]{0x00, 0x07, 0x00, 0x00});
        var instr = decoder.decode(0);
        assertEquals(4, instr.image.length);
    }

    @Test
    public void testDecoderCacheHit() throws InvalidInstructionException {
        memory.setMemory(new short[]{0xA0, 0x00, 0x00, 0x00});
        var first = decoder.decode(0);
        var second = decoder.decode(0);
        assertSame(first, second);
    }

    @Test
    public void testDecoderCacheInvalidation() throws InvalidInstructionException {
        memory.setMemory(new short[]{0xA0, 0x00, 0x00, 0x00});
        var first = decoder.decode(0);
        // Change memory content
        memory.write(1, (byte) 0x07);
        var second = decoder.decode(0);
        assertNotSame(first, second);
    }

    @Test
    public void testJMPWithLineZero() {
        // JMP 0: line raw = 0 (00000), reversed = 0; opcode 000
        memory.setMemory(new short[]{0x00, 0x00, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("JMP 0", instr.mnemo);
    }

    @Test
    public void testJMPWithLine31() {
        // line displayed = 31 (11111), reversed = 11111 = 31 (palindrome)
        // raw = 31, byte[0] top 5 bits = 11111 → byte[0] = 0xF8
        memory.setMemory(new short[]{0xF8, 0x00, 0x00, 0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("JMP 31", instr.mnemo);
    }
}
