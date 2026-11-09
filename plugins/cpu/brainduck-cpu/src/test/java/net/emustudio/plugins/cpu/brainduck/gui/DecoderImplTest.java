/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.plugins.cpu.brainduck.MemoryStub;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DecoderImplTest {
    private MemoryStub memory;
    private DecoderImpl decoder;

    @Before
    public void setUp() throws Exception {
        memory = new MemoryStub();
        decoder = new DecoderImpl(memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemory() throws Exception {
        new DecoderImpl(null);
    }

    @Test
    public void testDecodeHalt() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x00});
        DecodedInstruction instr = decoder.decode(0);

        assertNotNull(instr);
        assertEquals(1, instr.length());
        assertEquals("00", instr.encoding());
        assertEquals("halt", instr.mnemonic());

    }

    @Test
    public void testDecodeIncrement() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x01});
        DecodedInstruction instr = decoder.decode(0);

        assertNotNull(instr);
        assertEquals(1, instr.length());
        assertEquals("01", instr.encoding());
        assertEquals("> (P++)", instr.mnemonic());

    }

    @Test
    public void testDecodeDecrement() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x02});
        DecodedInstruction instr = decoder.decode(0);

        assertNotNull(instr);
        assertEquals("< (P--)", instr.mnemonic());

    }

    @Test
    public void testDecodeIncrementValue() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x03});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("+ (*P++)", instr.mnemonic());

    }

    @Test
    public void testDecodeDecrementValue() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x04});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("- (*P--)", instr.mnemonic());

    }

    @Test
    public void testDecodePrint() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x05});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals(". (print)", instr.mnemonic());

    }

    @Test
    public void testDecodeLoad() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x06});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals(", (load)", instr.mnemonic());

    }

    @Test
    public void testDecodeLoopStart() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x07});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("[ (loop)", instr.mnemonic());

    }

    @Test
    public void testDecodeLoopEnd() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x08});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("] (endloop)", instr.mnemonic());

    }

    @Test(expected = InvalidInstructionException.class)
    public void testDecodeInvalidInstruction() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x09});
        decoder.decode(0);
    }

    @Test(expected = InvalidInstructionException.class)
    public void testDecodeInvalid0xFF() throws InvalidInstructionException {
        memory.setProgram(new byte[]{(byte) 0xFF});
        decoder.decode(0);
    }

    @Test
    public void testDecodeAllValidInstructions() throws InvalidInstructionException {
        byte[] program = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        memory.setProgram(program);

        for (int i = 0; i < program.length; i++) {
            DecodedInstruction instr = decoder.decode(i);
            assertNotNull(instr);
            assertEquals(1, instr.length());
        }
    }

    @Test
    public void testDecodeAtDifferentPositions() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x01, 0x03, 0x05});

        DecodedInstruction instr0 = decoder.decode(0);
        assertEquals("> (P++)", instr0.mnemonic());

        DecodedInstruction instr1 = decoder.decode(1);
        assertEquals("+ (*P++)", instr1.mnemonic());

        DecodedInstruction instr2 = decoder.decode(2);
        assertEquals(". (print)", instr2.mnemonic());
    }

    @Test
    public void testDecodeCacheHit() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x01});

        DecodedInstruction first = decoder.decode(0);
        DecodedInstruction second = decoder.decode(0);

        // Cache hit should return same object
        assertSame(first, second);
    }

    @Test
    public void testDecodeCacheInvalidationOnMemoryChange() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x01});
        DecodedInstruction first = decoder.decode(0);
        assertEquals("> (P++)", first.mnemonic());

        // Change memory at position 0
        memory.write(0, (byte) 0x03);
        DecodedInstruction second = decoder.decode(0);
        assertEquals("+ (*P++)", second.mnemonic());
        assertNotSame(first, second);
    }

    @Test
    public void testDecodeImageLength() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x00});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals(1, instr.length());
    }

    @Test
    public void capturedInstructionsExposeExtentAndEncodingWithoutGeneratorKeys() throws Exception {
        for (int opcode = 0; opcode <= 8; opcode++) {
            memory.write(0, (byte) opcode);

            DecodedInstruction instruction = decoder.decode(0);

            assertEquals(0, instruction.address());
            assertEquals(1, instruction.length());
            assertEquals(String.format("%02X", opcode), instruction.encoding());
            assertFalse(instruction.mnemonic().isBlank());
        }
    }
}
