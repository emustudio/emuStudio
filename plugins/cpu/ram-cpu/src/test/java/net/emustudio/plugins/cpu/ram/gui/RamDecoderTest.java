/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.plugins.memory.ram.api.*;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RamDecoderTest {
    @Test
    public void missingInstructionIsInvalid() {
        RamMemoryContext memory = createNiceMock(RamMemoryContext.class);
        replay(memory);

        assertThrows(InvalidInstructionException.class, () -> new RamDecoder(memory).decode(0));
    }

    @Test
    public void haltOccupiesOneObjectCell() throws Exception {
        RamMemoryContext memory = memory(instruction(RamInstruction.Opcode.HALT, null, null));

        DecodedInstruction decoded = new RamDecoder(memory).decode(5);

        assertEquals(5, decoded.address());
        assertEquals(1, decoded.length());
        assertTrue(decoded.mnemonic().contains("halt"));
    }

    @Test
    public void operandCaptureStaysImmutableAfterLiveValueChanges() throws Exception {
        AtomicReference<String> spelling = new AtomicReference<>("5");
        RamValue value = createNiceMock(RamValue.class);
        expect(value.getStringRepresentation()).andAnswer(spelling::get).anyTimes();
        replay(value);
        RamDecoder decoder = new RamDecoder(memory(instruction(RamInstruction.Opcode.READ, value, null)));
        DecodedInstruction before = decoder.decode(0);

        spelling.set("99");
        DecodedInstruction after = decoder.decode(0);

        assertTrue(before.mnemonic().contains("5"));
        assertTrue(after.mnemonic().contains("99"));
        assertEquals("READ DIRECT 5", before.encoding());
    }

    @Test
    public void jumpEncodingsCaptureLabelAddress() throws Exception {
        RamLabel label = createNiceMock(RamLabel.class);
        expect(label.getAddress()).andReturn(20).anyTimes();
        replay(label);
        DecodedInstruction direct = new RamDecoder(memory(instruction(RamInstruction.Opcode.JMP, null, label))).decode(3);
        DecodedInstruction conditional = new RamDecoder(memory(instruction(RamInstruction.Opcode.JZ, null, label))).decode(3);

        assertEquals("JMP DIRECT @20", direct.encoding());
        assertEquals("JZ DIRECT @20", conditional.encoding());
        assertEquals(1, direct.length());
        assertEquals(1, conditional.length());
    }

    @Test
    public void decodingPreservesNegativeLabelWithoutPredictingExecution() throws Exception {
        RamLabel label = createNiceMock(RamLabel.class);
        expect(label.getAddress()).andReturn(-1).anyTimes();
        replay(label);
        RamDecoder decoder = new RamDecoder(memory(instruction(RamInstruction.Opcode.JMP, null, label)));

        assertEquals("JMP DIRECT @-1", decoder.decode(0).encoding());
    }

    private static RamInstruction instruction(RamInstruction.Opcode opcode, RamValue value, RamLabel label) {
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(instruction.getOperand()).andReturn(value).anyTimes();
        expect(instruction.getLabel()).andReturn(label).anyTimes();
        replay(instruction);
        return instruction;
    }

    private static RamMemoryContext memory(RamInstruction instruction) {
        RamMemoryContext memory = createNiceMock(RamMemoryContext.class);
        expect(memory.read(anyInt())).andReturn(instruction).anyTimes();
        replay(memory);
        return memory;
    }
}

