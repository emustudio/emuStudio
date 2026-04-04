/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.plugins.cpu.brainduck.MemoryStub;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisassemblerImplTest {
    private MemoryStub memory;
    private DecoderImpl decoder;
    private DisassemblerImpl disassembler;

    @Before
    public void setUp() {
        memory = new MemoryStub();
        decoder = new DecoderImpl(memory);
        disassembler = new DisassemblerImpl(memory, decoder);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMemory() {
        new DisassemblerImpl(null, decoder);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullDecoder() {
        new DisassemblerImpl(memory, null);
    }

    @Test
    public void testDisassembleHalt() {
        memory.setProgram(new byte[]{0x00});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals(0, instr.address);
        assertEquals("halt", instr.mnemo);
        assertEquals("00", instr.opCode);
    }

    @Test
    public void testDisassembleIncrement() {
        memory.setProgram(new byte[]{0x01});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("> (P++)", instr.mnemo);
        assertEquals("01", instr.opCode);
    }

    @Test
    public void testDisassembleDecrement() {
        memory.setProgram(new byte[]{0x02});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("< (P--)", instr.mnemo);
        assertEquals("02", instr.opCode);
    }

    @Test
    public void testDisassembleIncrementValue() {
        memory.setProgram(new byte[]{0x03});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("+ (*P++)", instr.mnemo);
        assertEquals("03", instr.opCode);
    }

    @Test
    public void testDisassembleDecrementValue() {
        memory.setProgram(new byte[]{0x04});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("- (*P--)", instr.mnemo);
        assertEquals("04", instr.opCode);
    }

    @Test
    public void testDisassemblePrint() {
        memory.setProgram(new byte[]{0x05});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals(". (print)", instr.mnemo);
        assertEquals("05", instr.opCode);
    }

    @Test
    public void testDisassembleLoad() {
        memory.setProgram(new byte[]{0x06});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals(", (load)", instr.mnemo);
        assertEquals("06", instr.opCode);
    }

    @Test
    public void testDisassembleLoopStart() {
        memory.setProgram(new byte[]{0x07});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("[ (loop)", instr.mnemo);
        assertEquals("07", instr.opCode);
    }

    @Test
    public void testDisassembleLoopEnd() {
        memory.setProgram(new byte[]{0x08});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("] (endloop)", instr.mnemo);
        assertEquals("08", instr.opCode);
    }

    @Test
    public void testDisassembleInvalidInstruction() {
        memory.setProgram(new byte[]{(byte) 0xFF});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("unknown", instr.mnemo);
        assertEquals("FF", instr.opCode);
    }

    @Test
    public void testDisassembleInvalidInstructionOpcode0x09() {
        memory.setProgram(new byte[]{0x09});
        DisassembledInstruction instr = disassembler.disassemble(0);

        assertEquals("unknown", instr.mnemo);
        assertEquals("09", instr.opCode);
    }

    @Test
    public void testGetNextInstructionPositionValid() {
        memory.setProgram(new byte[]{0x01, 0x02, 0x03});

        assertEquals(1, disassembler.getNextInstructionPosition(0));
        assertEquals(2, disassembler.getNextInstructionPosition(1));
        assertEquals(3, disassembler.getNextInstructionPosition(2));
    }

    @Test
    public void testGetNextInstructionPositionInvalid() {
        memory.setProgram(new byte[]{(byte) 0xFF});

        // Invalid instructions should advance by 1
        assertEquals(1, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testDisassembleAllInstructions() {
        byte[] program = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        String[] expectedMnemonics = {
                "halt", "> (P++)", "< (P--)", "+ (*P++)", "- (*P--)",
                ". (print)", ", (load)", "[ (loop)", "] (endloop)"
        };
        String[] expectedOpcodes = {"00", "01", "02", "03", "04", "05", "06", "07", "08"};

        memory.setProgram(program);

        for (int i = 0; i < program.length; i++) {
            DisassembledInstruction instr = disassembler.disassemble(i);
            assertEquals(i, instr.address);
            assertEquals("Mnemonic at position " + i, expectedMnemonics[i], instr.mnemo);
            assertEquals("Opcode at position " + i, expectedOpcodes[i], instr.opCode);
        }
    }

    @Test
    public void testDisassembleSequentialWalk() {
        byte[] program = new byte[]{0x01, 0x03, 0x05, 0x07, 0x08, 0x00};
        memory.setProgram(program);

        StringBuilder mnemonics = new StringBuilder();
        for (int i = 0; i < program.length; i = disassembler.getNextInstructionPosition(i)) {
            mnemonics.append(disassembler.disassemble(i).mnemo);
            if (i + 1 < program.length) {
                mnemonics.append("|");
            }
        }

        assertEquals("> (P++)|+ (*P++)|. (print)|[ (loop)|] (endloop)|halt", mnemonics.toString());
    }

    @Test
    public void testDisassembleCacheHit() {
        memory.setProgram(new byte[]{0x01});

        DisassembledInstruction first = disassembler.disassemble(0);
        DisassembledInstruction second = disassembler.disassemble(0);

        // Render cache should be hit - same content
        assertEquals(first.mnemo, second.mnemo);
        assertEquals(first.opCode, second.opCode);
    }

    @Test
    public void testGetNextInstructionPositionSequential() {
        memory.setProgram(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});

        int pos = 0;
        for (int i = 0; i < 5; i++) {
            int next = disassembler.getNextInstructionPosition(pos);
            assertEquals(pos + 1, next);
            pos = next;
        }
    }

    @Test
    public void testDisassembleAndGetNextInstructionPositionSameAddress() {
        memory.setProgram(new byte[]{0x01, 0x02});

        // First disassemble, then getNextInstructionPosition for same address
        // should use the cached decode
        DisassembledInstruction instr = disassembler.disassemble(0);
        assertEquals("> (P++)", instr.mnemo);

        int next = disassembler.getNextInstructionPosition(0);
        assertEquals(1, next);
    }
}

