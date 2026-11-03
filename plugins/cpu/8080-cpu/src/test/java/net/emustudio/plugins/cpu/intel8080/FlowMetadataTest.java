/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import static org.junit.Assert.*;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.*;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.cpu.intel8080.api.FlowDisassembler;
import net.emustudio.plugins.cpu.intel8080.gui.*;

import org.junit.Test;

public class FlowMetadataTest {
    private InstructionFlow flow(int... bytes) {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        for (int i = 0; i < bytes.length; i++) memory.write(i, (byte) bytes[i]);
        Disassembler decoder = new DisassemblerImpl(memory, new DecoderImpl(memory));
        return new FlowDisassembler(decoder, memory, false).getFlow(0).orElseThrow();
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
    public void capturedOperandAndDisassemblyRemainStableAfterMemoryWrite() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0xcd);
        memory.write(1, (byte) 0x34);
        memory.write(2, (byte) 0x12);
        Disassembler live = new SnapshotDisassembler<>(memory, value -> value, captured ->
                new FlowDisassembler(new DisassemblerImpl(captured, new DecoderImpl(captured)), captured, false));
        Disassembler captured = live.capture(0).orElseThrow();
        String mnemonic = captured.disassemble(0).mnemo;
        memory.write(1, (byte) 0x78);
        assertEquals(java.util.List.of(0x1234), captured.getFlow(0).orElseThrow().targets);
        assertEquals(mnemonic, captured.disassemble(0).mnemo);
        assertEquals(java.util.List.of(0x1278), live.getFlow(0).orElseThrow().targets);
    }
}
