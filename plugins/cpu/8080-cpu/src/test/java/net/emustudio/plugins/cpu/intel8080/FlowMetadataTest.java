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
    public void decoderUsesPluginWiringForEveryInstructionByte() throws Exception {
        byte[][] cells = {{0, 0x34, 0}, {(byte) 0xcd, 0, 0x12}};
        int[] wiring = {1, 0, 1};
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN) {
            @Override public Byte read(int address) { return cells[wiring[address]][address]; }
            @Override public Byte[] read(int address, int count) {
                Byte[] values = new Byte[count];
                for (int i = 0; i < count; i++) values[i] = read(address + i);
                return values;
            }
            @Override public Byte readBank(int address, int bank) { throw new AssertionError("Use wired reads"); }
            @Override public int getSelectedBank() { throw new AssertionError("Decoder must not select banks"); }
        };
        Disassembler decoder = new FlowDisassembler(new DisassemblerImpl(memory, new DecoderImpl(memory)), memory, false);

        assertEquals(java.util.List.of(0x1234), decoder.getFlow(0).orElseThrow().targets);
        assertEquals(3, decoder.getNextInstructionPosition(0));
        String mnemonic = decoder.disassemble(0).mnemo;
        cells[1][1] = 0x78;
        wiring[1] = 1;

        assertEquals(java.util.List.of(0x1278), decoder.getFlow(0).orElseThrow().targets);
        assertNotEquals(mnemonic, decoder.disassemble(0).mnemo);
    }

    @Test
    public void decoderReadsUpdatedOperandsAndDisassembly() throws Exception {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.write(0, (byte) 0xcd);
        memory.write(1, (byte) 0x34);
        memory.write(2, (byte) 0x12);
        Disassembler live = new FlowDisassembler(new DisassemblerImpl(memory, new DecoderImpl(memory)), memory, false);
        assertEquals(java.util.List.of(0x1234), live.getFlow(0).orElseThrow().targets);
        String mnemonic = live.disassemble(0).mnemo;

        memory.write(1, (byte) 0x78);

        assertEquals(java.util.List.of(0x1278), live.getFlow(0).orElseThrow().targets);
        assertNotEquals(mnemonic, live.disassemble(0).mnemo);
    }
}
