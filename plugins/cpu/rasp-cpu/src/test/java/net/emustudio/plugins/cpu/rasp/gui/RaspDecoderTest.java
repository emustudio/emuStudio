/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugin.cpu.*;
import net.emustudio.plugins.memory.rasp.api.*;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RaspDecoderTest {
    @Test
    public void invalidOpcodeIsNotAValidOneCellInstruction() {
        RaspMemoryContext memory = createNiceMock(RaspMemoryContext.class);
        expect(memory.read(0)).andReturn(99).anyTimes();
        expect(memory.disassembleMnemo(99)).andReturn(Optional.empty()).anyTimes();
        replay(memory);

        assertThrows(InvalidInstructionException.class, () -> new RaspDecoder(memory).decode(0));
    }

    @Test
    public void haltOccupiesOneIntegerCell() throws Exception {
        DecodedInstruction instruction = new RaspDecoder(memory(HALT, "HLT", null)).decode(0);

        assertEquals(1, instruction.length());
        assertEquals("hlt", instruction.mnemonic());
    }

    @Test
    public void ordinaryInstructionOccupiesTwoIntegerCells() throws Exception {
        DecodedInstruction instruction = new RaspDecoder(memory(READ, "READ", null)).decode(0);

        assertEquals(2, instruction.length());
        assertEquals("read 10", instruction.mnemonic());
    }

    @Test
    public void labelsAreCapturedWithoutChangingEncoding() throws Exception {
        AtomicReference<String> name = new AtomicReference<>("OLD");
        RaspLabel label = createNiceMock(RaspLabel.class);
        expect(label.getLabel()).andAnswer(name::get).anyTimes();
        replay(label);
        RaspDecoder decoder = new RaspDecoder(memory(JMP, "JMP", label));
        DecodedInstruction before = decoder.decode(0);

        name.set("NEW");
        DecodedInstruction after = decoder.decode(0);

        assertEquals("jmp OLD", before.mnemonic());
        assertEquals("jmp NEW", after.mnemonic());
        assertEquals(before.encoding(), after.encoding());
    }

    @Test
    public void conditionalJumpDecodingCapturesOperandAndExtent() throws Exception {
        DecodedInstruction instruction = new RaspDecoder(memory(JZ, "JZ", null)).decode(0);

        assertEquals("jz 10", instruction.mnemonic());
        assertEquals(2, instruction.length());
    }

    private static RaspMemoryContext memory(int opcode, String mnemonic, RaspLabel label) {
        return memory(opcode, mnemonic, label, 10);
    }

    @Test
    public void negativeJumpOperandIsCapturedWithoutPredictingExecution() throws Exception {
        RaspDecoder decoder = new RaspDecoder(memory(JMP, "JMP", null, -1));

        assertEquals("jmp -1", decoder.decode(0).mnemonic());
    }

    private static RaspMemoryContext memory(int opcode, String mnemonic, RaspLabel label, int operand) {
        RaspMemoryContext memory = createNiceMock(RaspMemoryContext.class);
        expect(memory.read(0)).andReturn(opcode).anyTimes();
        expect(memory.read(1)).andReturn(operand).anyTimes();
        expect(memory.disassembleMnemo(opcode)).andReturn(Optional.of(mnemonic)).anyTimes();
        expect(memory.getLabel(operand)).andReturn(Optional.ofNullable(label)).anyTimes();
        replay(memory);
        return memory;
    }
}
