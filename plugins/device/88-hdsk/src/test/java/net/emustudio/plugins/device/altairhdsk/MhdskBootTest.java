/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.Context8080Impl;
import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88pio.PioBoard;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MhdskBootTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    @Test public void unmodifiedSimhBootRomLoadsSectorThroughReal8080And4Pio() throws Exception {
        Byte[] ram = new Byte[65536];
        Arrays.fill(ram, (byte) 0);
        @SuppressWarnings("unchecked") MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        expect(memory.read(anyInt())).andAnswer(() -> ram[(int) getCurrentArguments()[0]]).anyTimes();
        expect(memory.read(anyInt(), anyInt())).andAnswer(() -> {
            int address = (int) getCurrentArguments()[0], count = (int) getCurrentArguments()[1];
            return Arrays.copyOfRange(ram, address, address + count);
        }).anyTimes();
        memory.write(anyInt(), anyObject(Byte.class));
        expectLastCall().andAnswer(() -> {
            ram[(int) getCurrentArguments()[0]] = (Byte) getCurrentArguments()[1]; return null;
        }).anyTimes();
        memory.write(anyInt(), anyObject(Byte[].class), anyInt());
        expectLastCall().andAnswer(() -> {
            Object[] args = getCurrentArguments();
            System.arraycopy(args[1], 0, ram, (int) args[0], (int) args[2]); return null;
        }).anyTimes();
        replay(memory);

        try (InputStream input = getClass().getResourceAsStream("/mhdsk-boot.bin")) {
            assertNotNull(input);
            byte[] rom = input.readAllBytes(); assertEquals(256, rom.length);
            for (int i = 0; i < rom.length; i++) { ram[0xFC00 + i] = rom[i]; }
        }
        byte[] sector = new byte[256];
        for (int i = 0; i < sector.length; i++) { sector[i] = (byte) (i * 7); }
        sector[0] = 0x76; // Loaded program halts at address zero.
        sector[40] = sector[41] = sector[43] = 0;
        sector[42] = 1; // HDBL header: first sector zero, one sector to load.
        Path image = files.getRoot().toPath().resolve("boot.dsk");
        MhdskController.createImage(image);
        try (RandomAccessFile file = new RandomAccessFile(image.toFile(), "rw")) { file.write(sector); }

        Context8080Impl cpu = new Context8080Impl();
        PioBoard pio = new PioBoard(0xA0, 2);
        for (int port = 0xA0; port <= 0xA7; port++) { assertTrue(cpu.attachDevice(port, pio)); }
        Context8080.CpuPortDevice switchesAndConsole = new Context8080.CpuPortDevice() {
            @Override public byte read(int port) { return (byte) ((port & 255) == 0x10 ? 2 : 0); }
            @Override public void write(int port, byte data) { }
            @Override public String getName() { return "Boot console and switches"; }
        };
        cpu.attachDevice(0x10, switchesAndConsole);
        cpu.attachDevice(0x11, switchesAndConsole);
        cpu.attachDevice(0xFF, switchesAndConsole);
        MhdskController controller = new MhdskController(pio);
        try {
            controller.attach(0, image, false);
            pio.attachPeripheral(controller);
            EmulatorEngine engine = new EmulatorEngine(memory, cpu);
            cpu.setCpu(engine);
            engine.reset(0xFC00);
            CPU.RunState state = CPU.RunState.STATE_STOPPED_BREAK;
            for (int steps = 0; steps < 200000 && state != CPU.RunState.STATE_STOPPED_NORMAL; steps++) {
                state = engine.step();
            }
            assertEquals("Boot must reach loaded HLT", CPU.RunState.STATE_STOPPED_NORMAL, state);
            assertEquals(1, engine.PC);
            for (int i = 0; i < sector.length; i++) { assertEquals("Loaded byte " + i, sector[i], ram[i].byteValue()); }
        } finally { controller.close(); }
    }
}
