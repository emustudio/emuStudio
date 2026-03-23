/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
