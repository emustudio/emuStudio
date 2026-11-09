/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.plugins.cpu.brainduck.MemoryStub;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisassemblerImplTest {
    private MemoryStub memory;
    private DecoderImpl decoder;
    private DecoderImpl disassembler;

    @Before
    public void setUp() throws Exception {
        memory = new MemoryStub();
        decoder = new DecoderImpl(memory);
        disassembler = decoder;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemory() throws Exception {
        new DecoderImpl(null);
    }

    @Test
    public void testDisassembleHalt() throws Exception {
        memory.setProgram(new byte[]{0x00});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals(0, instr.address());
        assertEquals("halt", instr.mnemonic());
        assertEquals("00", instr.encoding());
    }

    @Test
    public void testDisassembleIncrement() throws Exception {
        memory.setProgram(new byte[]{0x01});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals("> (P++)", instr.mnemonic());
        assertEquals("01", instr.encoding());
    }

    @Test
    public void testDisassembleDecrement() throws Exception {
        memory.setProgram(new byte[]{0x02});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals("< (P--)", instr.mnemonic());
        assertEquals("02", instr.encoding());
    }

    @Test
    public void testDisassembleIncrementValue() throws Exception {
        memory.setProgram(new byte[]{0x03});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals("+ (*P++)", instr.mnemonic());
        assertEquals("03", instr.encoding());
    }

    @Test
    public void testDisassembleDecrementValue() throws Exception {
        memory.setProgram(new byte[]{0x04});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals("- (*P--)", instr.mnemonic());
        assertEquals("04", instr.encoding());
    }

    @Test
    public void testDisassemblePrint() throws Exception {
        memory.setProgram(new byte[]{0x05});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals(". (print)", instr.mnemonic());
        assertEquals("05", instr.encoding());
    }

    @Test
    public void testDisassembleLoad() throws Exception {
        memory.setProgram(new byte[]{0x06});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals(", (load)", instr.mnemonic());
        assertEquals("06", instr.encoding());
    }

    @Test
    public void testDisassembleLoopStart() throws Exception {
        memory.setProgram(new byte[]{0x07});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals("[ (loop)", instr.mnemonic());
        assertEquals("07", instr.encoding());
    }

    @Test
    public void testDisassembleLoopEnd() throws Exception {
        memory.setProgram(new byte[]{0x08});
        DecodedInstruction instr = disassembler.decode(0);

        assertEquals("] (endloop)", instr.mnemonic());
        assertEquals("08", instr.encoding());
    }

    @Test
    public void testDisassembleInvalidInstruction() {
        memory.setProgram(new byte[]{(byte) 0xFF});
        assertThrows(InvalidInstructionException.class, () -> disassembler.decode(0));
    }

    @Test
    public void testDisassembleInvalidInstructionOpcode0x09() {
        memory.setProgram(new byte[]{0x09});
        assertThrows(InvalidInstructionException.class, () -> disassembler.decode(0));
    }

    @Test
    public void testGetNextInstructionPositionValid() throws Exception {
        memory.setProgram(new byte[]{0x01, 0x02, 0x03});

        assertEquals(1, (0 + disassembler.decode(0).length()));
        assertEquals(2, (1 + disassembler.decode(1).length()));
        assertEquals(3, (2 + disassembler.decode(2).length()));
    }

    @Test
    public void invalidInstructionDoesNotInventExtent() {
        memory.setProgram(new byte[]{(byte) 0xFF});
        assertThrows(InvalidInstructionException.class, () -> disassembler.decode(0));
    }

    @Test
    public void testDisassembleAllInstructions() throws Exception {
        byte[] program = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        String[] expectedMnemonics = {
                "halt", "> (P++)", "< (P--)", "+ (*P++)", "- (*P--)",
                ". (print)", ", (load)", "[ (loop)", "] (endloop)"
        };
        String[] expectedOpcodes = {"00", "01", "02", "03", "04", "05", "06", "07", "08"};

        memory.setProgram(program);

        for (int i = 0; i < program.length; i++) {
            DecodedInstruction instr = disassembler.decode(i);
            assertEquals(i, instr.address());
            assertEquals("Mnemonic at position " + i, expectedMnemonics[i], instr.mnemonic());
            assertEquals("Opcode at position " + i, expectedOpcodes[i], instr.encoding());
        }
    }

    @Test
    public void testDisassembleSequentialWalk() throws Exception {
        byte[] program = new byte[]{0x01, 0x03, 0x05, 0x07, 0x08, 0x00};
        memory.setProgram(program);

        StringBuilder mnemonics = new StringBuilder();
        for (int i = 0; i < program.length; i = (i + disassembler.decode(i).length())) {
            mnemonics.append(disassembler.decode(i).mnemonic());
            if (i + 1 < program.length) {
                mnemonics.append("|");
            }
        }

        assertEquals("> (P++)|+ (*P++)|. (print)|[ (loop)|] (endloop)|halt", mnemonics.toString());
    }

    @Test
    public void testDisassembleCacheHit() throws Exception {
        memory.setProgram(new byte[]{0x01});

        DecodedInstruction first = disassembler.decode(0);
        DecodedInstruction second = disassembler.decode(0);

        // Render cache should be hit - same content
        assertEquals(first.mnemonic(), second.mnemonic());
        assertEquals(first.encoding(), second.encoding());
    }

    @Test
    public void testGetNextInstructionPositionSequential() throws Exception {
        memory.setProgram(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});

        int pos = 0;
        for (int i = 0; i < 5; i++) {
            int next = (pos + disassembler.decode(pos).length());
            assertEquals(pos + 1, next);
            pos = next;
        }
    }

    @Test
    public void repeatedDecodePreservesPresentationAndLength() throws Exception {
        memory.setProgram(new byte[]{0x01, 0x02});

        // Repeated decoding reuses the captured presentation and length.
        DecodedInstruction instr = disassembler.decode(0);
        assertEquals("> (P++)", instr.mnemonic());

        int next = (0 + disassembler.decode(0).length());
        assertEquals(1, next);
    }
}

