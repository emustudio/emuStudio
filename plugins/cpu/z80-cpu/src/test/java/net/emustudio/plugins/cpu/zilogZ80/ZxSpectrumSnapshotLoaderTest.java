/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.cpu.zilogZ80.suite.TimingMemoryStub;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZxSpectrumSnapshotLoaderTest {

    @Test
    public void loads48kSnaRegistersRamAndStackedProgramCounter() throws Exception {
        byte[] data = new byte[27 + ZxSpectrumSnapshot.RAM_SIZE];
        data[0] = 0x11;
        word(data, 1, 0x2233);
        word(data, 3, 0x4455);
        word(data, 5, 0x6677);
        data[7] = (byte) 0x88;
        data[8] = (byte) 0x99;
        word(data, 9, 0xAABB);
        word(data, 11, 0xCCDD);
        word(data, 13, 0xEEFF);
        word(data, 15, 0x1234);
        word(data, 17, 0x5678);
        data[19] = 0x04;
        data[20] = (byte) 0x81;
        data[21] = (byte) 0xA5;
        data[22] = (byte) 0x5A;
        word(data, 23, 0x8000);
        data[25] = 2;
        data[26] = 6;
        int stackOffset = 27 + 0x8000 - ZxSpectrumSnapshot.RAM_START;
        word(data, stackOffset, 0x9ABC);

        ZxSpectrumSnapshot snapshot = ZxSpectrumSnapshotLoader.loadSna(data);

        assertEquals(0x9ABC, snapshot.programCounter);
        assertEquals(0x8002, snapshot.stackPointer);
        assertEquals(0xAA, snapshot.registers[EmulatorEngine.REG_H]);
        assertEquals(0xBB, snapshot.registers[EmulatorEngine.REG_L]);
        assertEquals(0x99, snapshot.alternateRegisters[EmulatorEngine.REG_A]);
        assertEquals(0x88, snapshot.alternateFlags);
        assertEquals(0x5678, snapshot.indexX);
        assertEquals(0x1234, snapshot.indexY);
        assertEquals(0x81, snapshot.refresh);
        assertTrue(snapshot.iff1);
        assertTrue(snapshot.iff2);
        assertEquals(2, snapshot.interruptMode);
        assertEquals(6, snapshot.border);
    }

    @Test
    public void loadsUncompressedZ80VersionOne() throws Exception {
        byte[] data = new byte[30 + ZxSpectrumSnapshot.RAM_SIZE];
        data[0] = 0x12;
        data[1] = 0x34;
        word(data, 6, 0x5678);
        word(data, 8, 0x9ABC);
        data[11] = 0x01;
        data[12] = 0x0D;
        data[27] = 1;
        data[28] = 0;
        data[29] = 1;
        Arrays.fill(data, 30, data.length, (byte) 0x5A);

        ZxSpectrumSnapshot snapshot = ZxSpectrumSnapshotLoader.loadZ80(data);

        assertEquals(0x12, snapshot.registers[EmulatorEngine.REG_A]);
        assertEquals(0x34, snapshot.flags);
        assertEquals(0x5678, snapshot.programCounter);
        assertEquals(0x9ABC, snapshot.stackPointer);
        assertEquals(0x81, snapshot.refresh);
        assertEquals(6, snapshot.border);
        assertTrue(snapshot.iff1);
        assertFalse(snapshot.iff2);
        assertEquals(0x5A, snapshot.ram[0] & 0xFF);
        assertEquals(0x5A, snapshot.ram[snapshot.ram.length - 1] & 0xFF);
    }

    @Test
    public void loadsCompressedZ80VersionOne() throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] header = new byte[30];
        word(header, 6, 0x1234);
        header[12] = 0x20;
        data.write(header);
        int remaining = ZxSpectrumSnapshot.RAM_SIZE;
        while (remaining > 0) {
            int count = Math.min(255, remaining);
            data.write(0xED);
            data.write(0xED);
            data.write(count);
            data.write(0xA6);
            remaining -= count;
        }

        ZxSpectrumSnapshot snapshot = ZxSpectrumSnapshotLoader.loadZ80(data.toByteArray());

        assertEquals(0xA6, snapshot.ram[0] & 0xFF);
        assertEquals(0xA6, snapshot.ram[snapshot.ram.length - 1] & 0xFF);
    }

    @Test
    public void loadsExtended48kZ80Pages() throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] header = new byte[55];
        word(header, 30, 23);
        word(header, 32, 0x2468);
        header[34] = 0;
        data.write(header);
        writePage(data, 8, 0x11);
        writePage(data, 4, 0x22);
        writePage(data, 5, 0x33);

        ZxSpectrumSnapshot snapshot = ZxSpectrumSnapshotLoader.loadZ80(data.toByteArray());

        assertEquals(0x2468, snapshot.programCounter);
        assertEquals(0x11, snapshot.ram[0] & 0xFF);
        assertEquals(0x22, snapshot.ram[0x4000] & 0xFF);
        assertEquals(0x33, snapshot.ram[0x8000] & 0xFF);
    }

    @Test(expected = IOException.class)
    public void rejects128kZ80Snapshot() throws Exception {
        byte[] data = new byte[55];
        word(data, 30, 23);
        data[34] = 3;
        ZxSpectrumSnapshotLoader.loadZ80(data);
    }

    @Test
    public void appliesSnapshotToEngineAndBorderDevice() {
        TimingMemoryStub memory = new TimingMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.setMemory(new byte[0x10000]);
        ContextZ80Impl context = new ContextZ80Impl();
        FakeByteDevice borderDevice = new FakeByteDevice();
        context.attachDevice(0xFE, borderDevice);
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        ZxSpectrumSnapshot snapshot = new ZxSpectrumSnapshot();
        snapshot.programCounter = 0x1234;
        snapshot.stackPointer = 0xABCD;
        snapshot.indexX = 0x5678;
        snapshot.indexY = 0x9ABC;
        snapshot.interrupt = 0x11;
        snapshot.refresh = 0x82;
        snapshot.flags = 0xA5;
        snapshot.alternateFlags = 0x5A;
        snapshot.registers[EmulatorEngine.REG_A] = 0x44;
        snapshot.alternateRegisters[EmulatorEngine.REG_B] = 0x55;
        snapshot.iff1 = true;
        snapshot.iff2 = false;
        snapshot.interruptMode = 2;
        snapshot.border = 7;
        snapshot.ram[0] = 0x66;
        snapshot.ram[snapshot.ram.length - 1] = 0x77;

        engine.loadSnapshot(snapshot);

        assertEquals(0x1234, engine.PC);
        assertEquals(0xABCD, engine.SP);
        assertEquals(0x5678, engine.IX);
        assertEquals(0x9ABC, engine.IY);
        assertEquals(0x44, engine.regs[EmulatorEngine.REG_A]);
        assertEquals(0x55, engine.regs2[EmulatorEngine.REG_B]);
        assertEquals(0xA5, engine.flags);
        assertEquals(0x5A, engine.flags2);
        assertArrayEquals(new boolean[]{true, false}, engine.IFF);
        assertEquals(2, engine.interruptMode);
        assertEquals(0x66, memory.read(0x4000) & 0xFF);
        assertEquals(0x77, memory.read(0xFFFF) & 0xFF);
        assertEquals(7, borderDevice.getValue() & 0xFF);
    }

    private static void writePage(ByteArrayOutputStream data, int page, int value) throws IOException {
        data.write(0xFF);
        data.write(0xFF);
        data.write(page);
        byte[] contents = new byte[0x4000];
        Arrays.fill(contents, (byte) value);
        data.write(contents);
    }

    private static void word(byte[] data, int offset, int value) {
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
    }
}
