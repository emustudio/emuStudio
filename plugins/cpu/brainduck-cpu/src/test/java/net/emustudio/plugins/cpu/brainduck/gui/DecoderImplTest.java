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
    public void setUp() {
        memory = new MemoryStub();
        decoder = new DecoderImpl(memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemory() {
        new DecoderImpl(null);
    }

    @Test
    public void testDecodeHalt() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x00});
        DecodedInstruction instr = decoder.decode(0);

        assertNotNull(instr);
        assertEquals(1, instr.image.length);
        assertEquals(0x00, instr.image[0]);
        assertEquals("halt", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.HALT, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeIncrement() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x01});
        DecodedInstruction instr = decoder.decode(0);

        assertNotNull(instr);
        assertEquals(1, instr.image.length);
        assertEquals(0x01, instr.image[0]);
        assertEquals("> (P++)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.___P___, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeDecrement() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x02});
        DecodedInstruction instr = decoder.decode(0);

        assertNotNull(instr);
        assertEquals("< (P--)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.___P___, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeIncrementValue() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x03});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("+ (*P++)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.____P___, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeDecrementValue() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x04});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("- (*P--)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.____P___, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodePrint() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x05});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals(". (print)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.___PRINT_, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeLoad() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x06});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals(", (load)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.___LOAD_, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeLoopStart() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x07});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("[ (loop)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.___LOOP_, instr.constants[DecoderImpl.INSTRUCTION]);
    }

    @Test
    public void testDecodeLoopEnd() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x08});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals("] (endloop)", instr.strings[DecoderImpl.INSTRUCTION]);
        assertEquals(DecoderImpl.___ENDLOOP_, instr.constants[DecoderImpl.INSTRUCTION]);
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
            assertEquals(1, instr.image.length);
        }
    }

    @Test
    public void testDecodeAtDifferentPositions() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x01, 0x03, 0x05});

        DecodedInstruction instr0 = decoder.decode(0);
        assertEquals("> (P++)", instr0.strings[DecoderImpl.INSTRUCTION]);

        DecodedInstruction instr1 = decoder.decode(1);
        assertEquals("+ (*P++)", instr1.strings[DecoderImpl.INSTRUCTION]);

        DecodedInstruction instr2 = decoder.decode(2);
        assertEquals(". (print)", instr2.strings[DecoderImpl.INSTRUCTION]);
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
        assertEquals("> (P++)", first.strings[DecoderImpl.INSTRUCTION]);

        // Change memory at position 0
        memory.write(0, (byte) 0x03);
        DecodedInstruction second = decoder.decode(0);
        assertEquals("+ (*P++)", second.strings[DecoderImpl.INSTRUCTION]);
        assertNotSame(first, second);
    }

    @Test
    public void testDecodeImageLength() throws InvalidInstructionException {
        memory.setProgram(new byte[]{0x00});
        DecodedInstruction instr = decoder.decode(0);

        assertEquals(1, instr.image.length);
    }

    @Test
    public void testDecodeConstantValues() throws InvalidInstructionException {
        // Verify all instruction constant values
        byte[] opcodes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        int[] expectedConstants = {
                DecoderImpl.HALT,
                DecoderImpl.___P___,
                DecoderImpl.___P___,
                DecoderImpl.____P___,
                DecoderImpl.____P___,
                DecoderImpl.___PRINT_,
                DecoderImpl.___LOAD_,
                DecoderImpl.___LOOP_,
                DecoderImpl.___ENDLOOP_
        };

        for (int i = 0; i < opcodes.length; i++) {
            memory.write(0, opcodes[i]);
            // Force cache invalidation by creating new decoder
            DecoderImpl d = new DecoderImpl(memory);
            DecodedInstruction instr = d.decode(0);
            assertEquals("Opcode 0x" + Integer.toHexString(opcodes[i] & 0xFF),
                    expectedConstants[i], instr.constants[DecoderImpl.INSTRUCTION]);
        }
    }
}

