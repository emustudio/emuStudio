/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisassemblerTest {
    private final ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
    private final DecoderImpl decoder = new DecoderImpl(memory);
    private final DecoderImpl disassembler = decoder;

    @Test
    public void testLDN() throws Exception {
        memory.setMemory(new short[]{
                0x9B, 0xE2, 0xFC, 0x3F
        });

        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("LDN 25", instr.mnemonic());
        assertEquals("9B E2 FC 3F", instr.encoding());
    }

    @Test
    public void testSTO() throws Exception {
        memory.setMemory(new short[]{
                0, 0, 0, 0,
                0x68, 0x06, 0x00, 0x00
        });

        DecodedInstruction instr = disassembler.decode(4);
        assertEquals("STO 22", instr.mnemonic());
        assertEquals("68 06 00 00", instr.encoding());
    }

    @Test
    public void testJMP() throws Exception {
        // line raw=20 (10100), reversed=5 (00101); opcode 000 → JMP
        // byte[0]=0xA0 (10100_000), byte[1]=0x00 (...000)
        memory.setMemory(new short[]{0xA0, 0x00, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("JMP 5", instr.mnemonic());
        assertEquals("A0 00 00 00", instr.encoding());
    }

    @Test
    public void testJPR() throws Exception {
        // line raw=20 (10100), reversed=5; opcode 100 → JPR
        // byte[0]=0xA0, byte[1]=0x04
        memory.setMemory(new short[]{0xA0, 0x04, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("JPR 5", instr.mnemonic());
        assertEquals("A0 04 00 00", instr.encoding());
    }

    @Test
    public void testSUB() throws Exception {
        // line raw=20 (10100), reversed=5; opcode 001 → SUB
        // byte[0]=0xA0, byte[1]=0x01
        memory.setMemory(new short[]{0xA0, 0x01, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("SUB 5", instr.mnemonic());
        assertEquals("A0 01 00 00", instr.encoding());
    }

    @Test
    public void testCMP() throws Exception {
        // line must be 00000; opcode 011 → CMP
        // byte[0]=0x00, byte[1]=0x03
        memory.setMemory(new short[]{0x00, 0x03, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("CMP", instr.mnemonic());
        assertEquals("00 03 00 00", instr.encoding());
    }

    @Test
    public void testSTP() throws Exception {
        // line must be 00000; opcode 111 → STP
        // byte[0]=0x00, byte[1]=0x07
        memory.setMemory(new short[]{0x00, 0x07, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("STP", instr.mnemonic());
        assertEquals("00 07 00 00", instr.encoding());
    }

    @Test
    public void testDataInstruction() throws Exception {
        // opcode 101 with non-zero line → not a valid instruction → data
        memory.setMemory(new short[]{0x80, 0x05, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        // data instructions are formatted as hex, not as instruction mnemonics
        assertNotNull(instr.mnemonic());
        assertNotEquals("unknown", instr.mnemonic());
        assertFalse(instr.mnemonic().startsWith("JMP"));
        assertFalse(instr.mnemonic().startsWith("LDN"));
        assertFalse(instr.mnemonic().startsWith("SUB"));
    }

    @Test
    public void testGetNextInstructionPosition() throws Exception {
        memory.setMemory(new short[]{0x9B, 0xE2, 0xFC, 0x3F});
        // SSEM instructions are always 4 bytes
        assertEquals(4, (0 + disassembler.decode(0).length()));
    }

    @Test
    public void testGetNextInstructionPositionAtOffset() throws Exception {
        memory.setMemory(new short[]{
                0, 0, 0, 0,
                0x68, 0x06, 0x00, 0x00
        });
        assertEquals(8, (4 + disassembler.decode(4).length()));
    }

    @Test
    public void testDisassembleTwiceSameAddress() throws Exception {
        // Tests the cached render path in DecoderImpl
        memory.setMemory(new short[]{0x9B, 0xE2, 0xFC, 0x3F});
        DecodedInstruction first = disassembler.decode(0);
        DecodedInstruction second = disassembler.decode(0);
        assertEquals(first.mnemonic(), second.mnemonic());
        assertEquals(first.encoding(), second.encoding());
    }

    @Test
    public void testDecoderDecode() throws InvalidInstructionException {
        memory.setMemory(new short[]{0x00, 0x07, 0x00, 0x00});
        var instr = decoder.decode(0);
        assertEquals(4, instr.length());
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
    public void testJMPWithLineZero() throws Exception {
        // JMP 0: line raw = 0 (00000), reversed = 0; opcode 000
        memory.setMemory(new short[]{0x00, 0x00, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("JMP 0", instr.mnemonic());
    }

    @Test
    public void testJMPWithLine31() throws Exception {
        // line displayed = 31 (11111), reversed = 11111 = 31 (palindrome)
        // raw = 31, byte[0] top 5 bits = 11111 → byte[0] = 0xF8
        memory.setMemory(new short[]{0xF8, 0x00, 0x00, 0x00});
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("JMP 31", instr.mnemonic());
    }
}
