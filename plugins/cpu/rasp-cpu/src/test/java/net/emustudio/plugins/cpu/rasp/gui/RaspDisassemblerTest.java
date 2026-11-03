/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RaspDisassemblerTest {
    private RaspMemoryContext memory;
    private RaspDisassembler disassembler;

    @Before
    public void setUp() {
        memory = createNiceMock(RaspMemoryContext.class);
    }

    @Test
    public void explicitBankDecoderReadsLiveWordCellsAndLabelNames() throws Exception {
        AtomicReference<String> name = new AtomicReference<>("OLD");
        RaspLabel label = createNiceMock(RaspLabel.class);
        expect(label.getAddress()).andReturn(10).anyTimes();
        expect(label.getLabel()).andAnswer(name::get).anyTimes();
        replay(label);
        expect(memory.read(0)).andReturn(JMP).anyTimes();
        expect(memory.read(1)).andReturn(10).anyTimes();
        expect(memory.disassembleMnemo(JMP)).andReturn(Optional.of("JMP")).anyTimes();
        expect(memory.isInstruction(JMP)).andReturn(true).anyTimes();
        expect(memory.getLabel(10)).andReturn(Optional.of(label)).anyTimes();
        replay(memory);
        RaspDisassembler live = new RaspDisassembler(memory);
        net.emustudio.emulib.plugins.cpu.Disassembler bank = live.forBank(0).orElseThrow();
        assertTrue(bank.disassemble(0).mnemo.contains("OLD"));

        name.set("NEW");

        assertTrue(bank.disassemble(0).mnemo.contains("NEW"));
        assertTrue(live.disassemble(0).mnemo.contains("NEW"));
        assertEquals(2, bank.getNextInstructionPosition(0));
        assertThrows(IllegalArgumentException.class, () -> live.forBank(1));
    }

    @Test
    public void testDisassembleHalt() {
        expect(memory.read(0)).andReturn(HALT).anyTimes();
        expect(memory.disassembleMnemo(HALT)).andReturn(Optional.of("HLT")).anyTimes();
        expect(memory.isInstruction(HALT)).andReturn(true).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertTrue(instr.mnemo.toLowerCase().contains("hlt"));
    }

    @Test
    public void testDisassembleRead() {
        expect(memory.read(0)).andReturn(READ).anyTimes();
        expect(memory.read(1)).andReturn(5).anyTimes();
        expect(memory.disassembleMnemo(READ)).andReturn(Optional.of("READ")).anyTimes();
        expect(memory.isInstruction(READ)).andReturn(true).anyTimes();
        expect(memory.getLabel(anyInt())).andReturn(Optional.empty()).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertTrue(instr.mnemo.toLowerCase().contains("read"));
    }

    @Test
    public void testDisassembleJmp() {
        expect(memory.read(0)).andReturn(JMP).anyTimes();
        expect(memory.read(1)).andReturn(10).anyTimes();
        expect(memory.disassembleMnemo(JMP)).andReturn(Optional.of("JMP")).anyTimes();
        expect(memory.isInstruction(JMP)).andReturn(true).anyTimes();
        expect(memory.getLabel(anyInt())).andReturn(Optional.empty()).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertTrue(instr.mnemo.toLowerCase().contains("jmp"));
    }

    @Test
    public void testDisassembleJmpWithLabel() {
        RaspLabel label = createNiceMock(RaspLabel.class);
        expect(label.getLabel()).andReturn("START").anyTimes();
        replay(label);

        expect(memory.read(0)).andReturn(JMP).anyTimes();
        expect(memory.read(1)).andReturn(10).anyTimes();
        expect(memory.disassembleMnemo(JMP)).andReturn(Optional.of("JMP")).anyTimes();
        expect(memory.isInstruction(JMP)).andReturn(true).anyTimes();
        expect(memory.getLabel(10)).andReturn(Optional.of(label)).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);

        var instr = disassembler.disassemble(0);
        assertNotNull(instr);
        assertTrue(instr.mnemo.toUpperCase().contains("START"));
    }

    @Test
    public void testGetNextInstructionPositionForHalt() {
        expect(memory.read(0)).andReturn(HALT).anyTimes();
        expect(memory.isInstruction(HALT)).andReturn(true).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);
        assertEquals(1, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testGetNextInstructionPositionForRead() {
        expect(memory.read(0)).andReturn(READ).anyTimes();
        expect(memory.isInstruction(READ)).andReturn(true).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);
        assertEquals(2, disassembler.getNextInstructionPosition(0));
    }

    @Test
    public void testGetNextInstructionPositionForInvalidInstruction() {
        expect(memory.read(0)).andReturn(99).anyTimes();
        expect(memory.isInstruction(99)).andReturn(false).anyTimes();
        replay(memory);
        disassembler = new RaspDisassembler(memory);
        assertEquals(1, disassembler.getNextInstructionPosition(0));
    }
}
