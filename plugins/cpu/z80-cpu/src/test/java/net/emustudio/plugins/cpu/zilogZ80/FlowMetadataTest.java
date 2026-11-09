/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import static org.junit.Assert.*;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugin.cpu.*;
import net.emustudio.emulib.NumberUtils;
import net.emustudio.plugins.cpu.intel8080.api.FlowDisassembler;
import net.emustudio.plugins.cpu.zilogZ80.gui.*;

import org.junit.Test;

public class FlowMetadataTest {
    private InstructionFlow flow(int... bytes) {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        for (int i = 0; i < bytes.length; i++) memory.write(i, (byte) bytes[i]);
        Disassembler decoder = new DisassemblerImpl(memory, new DecoderImpl(memory));
        return new FlowDisassembler(decoder, memory, true).getFlow(0).orElseThrow();
    }

    @Test
    public void directCallsBranchesReturnsAndIndirectTargets() {
        InstructionFlow call = flow(0xcd, 0x34, 0x12);
        assertEquals(InstructionFlow.Kind.CALL, call.kind);
        assertEquals(java.util.List.of(0x1234), call.targets);
        assertTrue(call.fallThrough);
        assertEquals(3, call.next);
        InstructionFlow jump = flow(0xc3, 0x34, 0x12);
        assertFalse(jump.fallThrough);
        assertEquals(InstructionFlow.Kind.JUMP, jump.kind);
        assertTrue(flow(0xc2, 0x34, 0x12).fallThrough);
        assertFalse(flow(0xc9).fallThrough);
        assertTrue(flow(0xc0).fallThrough);
        InstructionFlow indirect = flow(0xe9);
        assertEquals(InstructionFlow.Kind.JUMP, indirect.kind);
        assertTrue(indirect.targets.isEmpty());
        assertFalse(indirect.fallThrough);
        assertEquals(java.util.List.of(0x38), flow(0xff).targets);
        assertEquals(InstructionFlow.Kind.STOP, flow(0x76).kind);
    }

    @Test
    public void relativeOffsetsAndIndexAndInterruptReturns() {
        assertEquals(java.util.List.of(0), flow(0x18, 0xfe).targets);
        assertFalse(flow(0x18, 0).fallThrough);
        assertEquals(java.util.List.of(3), flow(0x20, 1).targets);
        assertTrue(flow(0x20, 1).fallThrough);
        assertTrue(flow(0x10, 1).fallThrough);
        assertTrue(flow(0xdd, 0xe9).targets.isEmpty());
        assertFalse(flow(0xfd, 0xe9).fallThrough);
        assertEquals(InstructionFlow.Kind.RETURN, flow(0xed, 0x4d).kind);
    }

    @Test
    public void decoderReadsUpdatedRelativeOperandsAndDisassembly() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0x18);
        memory.write(1, (byte) 0x02);
        Disassembler live = new FlowDisassembler(new DisassemblerImpl(memory, new DecoderImpl(memory)), memory, true);
        assertEquals(java.util.List.of(4), live.getFlow(0).orElseThrow().targets);
        String mnemonic = live.disassemble(0).mnemo;

        memory.write(1, (byte) 0xfe);

        assertEquals(java.util.List.of(0), live.getFlow(0).orElseThrow().targets);
        assertNotEquals(mnemonic, live.disassemble(0).mnemo);
    }
}
