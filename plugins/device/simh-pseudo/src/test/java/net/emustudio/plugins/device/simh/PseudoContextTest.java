/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class PseudoContextTest {
    private PseudoContext context;
    private Context8080 cpu;
    private ByteMemoryContext memory;

    @Before
    public void setUp() {
        cpu = createNiceMock(Context8080.class);
        memory = createNiceMock(ByteMemoryContext.class);

        context = new PseudoContext();
        context.setCpu(cpu);
        context.setMemory(memory);
        context.reset();
    }

    @Test
    public void testGetName() {
        assertNotNull(context.getName());
    }

    @Test
    public void testToString() {
        assertEquals("SIMH-pseudo context", context.toString());
    }

    @Test
    public void testUnknownCommandIsIgnored() {
        replay(cpu, memory);
        // Write an unknown command (e.g., 255)
        context.write(0xFE, (byte) 0xFF);
        // Should not throw, just ignore
    }

    // Test command dispatch: write a known command, then read result

    @Test
    public void testGetSimhVersionCommand() {
        replay(cpu, memory);

        // Send getSimhVersion command (ordinal = 6)
        context.write(0xFE, (byte) Commands.getSIMHVersionCmd.ordinal());

        // Read version string
        StringBuilder version = new StringBuilder();
        byte b;
        while ((b = context.read(0xFE)) != 0) {
            version.append((char) b);
        }
        assertEquals("SIMH004", version.toString());
    }

    @Test
    public void testGetBankSelectCommand() {
        expect(memory.getSelectedBank()).andReturn(2);
        replay(cpu, memory);

        // Send getBankSelect command (ordinal = 11)
        context.write(0xFE, (byte) Commands.getBankSelectCmd.ordinal());

        byte result = context.read(0xFE);
        assertEquals(2, result);

        verify(memory);
    }

    @Test
    public void testSetBankSelectCommand() {
        memory.selectBank(3);
        expectLastCall();
        replay(cpu, memory);

        // Send setBankSelect command (ordinal = 12)
        context.write(0xFE, (byte) Commands.setBankSelectCmd.ordinal());
        // Write the bank number
        context.write(0xFE, (byte) 3);

        verify(memory);
    }

    @Test
    public void testHasBankedMemoryCommand() {
        expect(memory.getBanksCount()).andReturn(4);
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.hasBankedMemoryCmd.ordinal());

        byte result = context.read(0xFE);
        assertEquals(4, result);

        verify(memory);
    }

    @Test
    public void testGetCommonCommand() {
        int boundary = 0xC000;
        expect(memory.getCommonBoundary()).andReturn(boundary).anyTimes();
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.getCommonCmd.ordinal());

        byte low = context.read(0xFE);
        byte high = context.read(0xFE);

        int result = (low & 0xFF) | ((high & 0xFF) << 8);
        assertEquals(boundary, result);

        verify(memory);
    }

    @Test
    public void testGetCPUClockFrequencyCommand() {
        int frequency = 2000;
        expect(cpu.getCPUFrequency()).andReturn(frequency).anyTimes();
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.getCPUClockFrequency.ordinal());

        byte b0 = context.read(0xFE);
        byte b1 = context.read(0xFE);
        byte b2 = context.read(0xFE);
        byte b3 = context.read(0xFE);

        int result = (b0 & 0xFF) | ((b1 & 0xFF) << 8) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 24);
        assertEquals(frequency, result);

        verify(cpu);
    }

    @Test
    public void testSetCPUClockFrequencyCommand() {
        int frequency = 4000;

        cpu.setCPUFrequency(frequency);
        expectLastCall();
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.setCPUClockFrequency.ordinal());
        context.write(0xFE, (byte) (frequency & 0xFF));
        context.write(0xFE, (byte) ((frequency >> 8) & 0xFF));
        context.write(0xFE, (byte) ((frequency >> 16) & 0xFF));
        context.write(0xFE, (byte) ((frequency >> 24) & 0xFF));

        verify(cpu);
    }

    @Test
    public void testGetHostOSPathSeparatorCommand() {
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.getHostOSPathSeparatorCmd.ordinal());

        byte result = context.read(0xFE);
        assertEquals((byte) File.separatorChar, result);
    }

    @Test
    public void testPrintTimeCommand() {
        replay(cpu, memory);

        // Should not throw
        context.write(0xFE, (byte) Commands.printTimeCmd.ordinal());
    }

    @Test
    public void testResetSIMHInterfaceCommand() {
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.resetSIMHInterfaceCmd.ordinal());
        // Should not throw
    }

    @Test
    public void testResetClearsAllCommandState() {
        replay(cpu, memory);

        // Send a command that requires parameters
        context.write(0xFE, (byte) Commands.setTimerDeltaCmd.ordinal());
        // Don't send all parameters, then reset
        context.write(0xFE, (byte) 0x10); // first byte of timerDelta only

        context.reset();

        // After reset, should accept new commands normally
        // Send printTime (simple command)
        context.write(0xFE, (byte) Commands.printTimeCmd.ordinal());
        // Should not throw
    }

    @Test
    public void testGenInterruptCommand() {
        byte interruptData = (byte) 0xCF;

        cpu.signalInterrupt(aryEq(new byte[]{interruptData}));
        expectLastCall();
        replay(cpu, memory);

        context.write(0xFE, (byte) Commands.genInterruptCmd.ordinal());
        context.write(0xFE, (byte) 0); // interrupt vector
        context.write(0xFE, interruptData); // data bus value

        verify(cpu);
    }

    @Test
    public void testReadFromUnknownCommandReturnsZero() {
        replay(cpu, memory);

        // Read without writing a valid command
        byte result = context.read(0xFE);
        assertEquals(0, result);
    }
}

