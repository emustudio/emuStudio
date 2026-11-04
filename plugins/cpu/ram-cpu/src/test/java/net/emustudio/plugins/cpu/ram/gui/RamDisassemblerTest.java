/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RamDisassemblerTest {
    private RamMemoryContext memory;
    private RamDisassembler disassembler;

    @Before
    public void setUp() {
        memory = createNiceMock(RamMemoryContext.class);
    }

    @Test
    public void testDisassembleNullInstruction() {
        expect(memory.read(0)).andReturn(null).anyTimes();
        replay(memory);
        disassembler = new RamDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertEquals("", instr.mnemo);
    }

    @Test
    public void testDisassembleHalt() {
        RamInstruction halt = createNiceMock(RamInstruction.class);
        expect(halt.getOpcode()).andReturn(RamInstruction.Opcode.HALT).anyTimes();
        expect(halt.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(halt.getOperand()).andReturn(null).anyTimes();
        replay(halt);

        expect(memory.read(0)).andReturn(halt).anyTimes();
        replay(memory);
        disassembler = new RamDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertTrue(instr.mnemo.toLowerCase().contains("halt"));
    }

    @Test
    public void testDisassembleReadWithOperand() {
        RamValue value = createNiceMock(RamValue.class);
        expect(value.getStringRepresentation()).andReturn("5").anyTimes();
        replay(value);

        RamInstruction read = createNiceMock(RamInstruction.class);
        expect(read.getOpcode()).andReturn(RamInstruction.Opcode.READ).anyTimes();
        expect(read.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(read.getOperand()).andReturn(value).anyTimes();
        replay(read);

        expect(memory.read(0)).andReturn(read).anyTimes();
        replay(memory);
        disassembler = new RamDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertTrue(instr.mnemo.contains("5"));
    }

    @Test
    public void testGetNextInstructionPosition() {
        replay(memory);
        disassembler = new RamDisassembler(memory);
        assertEquals(1, disassembler.getNextInstructionPosition(0));
        assertEquals(6, disassembler.getNextInstructionPosition(5));
    }

    @Test
    public void decoderReadsLiveRamOperands() throws Exception {
        AtomicReference<String> spelling = new AtomicReference<>("5");
        RamValue value = createNiceMock(RamValue.class);
        expect(value.getStringRepresentation()).andAnswer(spelling::get).anyTimes();
        replay(value);
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(RamInstruction.Opcode.READ).anyTimes();
        expect(instruction.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(instruction.getOperand()).andReturn(value).anyTimes();
        replay(instruction);
        expect(memory.read(0)).andReturn(instruction).anyTimes();
        replay(memory);
        RamDisassembler live = new RamDisassembler(memory);
        assertTrue(live.disassemble(0).mnemo.contains("5"));

        spelling.set("99");

        assertTrue(live.disassemble(0).mnemo.contains("99"));
    }
}

