/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AutomaticEmulationTest {
    private static final String OUTPUT_FILE = "ssem.out";

    @After
    public void tearDown() {
        File file = new File(OUTPUT_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testConstructorRegistersCPUListener() {
        CPU cpu = createMock(CPU.class);
        cpu.addCPUListener(isA(CPU.CPUListener.class));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);

        new AutomaticEmulation(cpu, engine, memory);

        verify(cpu);
    }

    @Test
    public void testDestroyRemovesListener() {
        CPU cpu = createMock(CPU.class);
        cpu.addCPUListener(isA(CPU.CPUListener.class));
        expectLastCall().once();
        cpu.removeCPUListener(isA(CPU.CPUListener.class));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);

        AutomaticEmulation ae = new AutomaticEmulation(cpu, engine, memory);
        ae.destroy();

        verify(cpu);
    }

    @Test
    public void testSnapshotCreatedOnStateTransitionFromRunningToStopped() {
        CPU cpu = createNiceMock(CPU.class);
        Capture<CPU.CPUListener> listenerCapture = Capture.newInstance();
        cpu.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);

        new AutomaticEmulation(cpu, engine, memory);

        CPU.CPUListener listener = listenerCapture.getValue();

        // Simulate state transition: RUNNING -> STOPPED
        listener.runStateChanged(CPU.RunState.STATE_RUNNING);
        listener.runStateChanged(CPU.RunState.STATE_STOPPED_NORMAL);

        File outputFile = new File(OUTPUT_FILE);
        assertTrue("Snapshot file should be created", outputFile.exists());
        assertTrue("Snapshot file should not be empty", outputFile.length() > 0);
    }

    @Test
    public void testNoSnapshotWithoutRunningFirst() {
        CPU cpu = createNiceMock(CPU.class);
        Capture<CPU.CPUListener> listenerCapture = Capture.newInstance();
        cpu.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);

        new AutomaticEmulation(cpu, engine, memory);

        CPU.CPUListener listener = listenerCapture.getValue();

        // Just stopping without running first should not create snapshot
        listener.runStateChanged(CPU.RunState.STATE_STOPPED_NORMAL);

        File outputFile = new File(OUTPUT_FILE);
        assertFalse("Snapshot should not be created without prior RUNNING state", outputFile.exists());
    }

    @Test
    public void testRunningStateAloneDoesNotCreateSnapshot() {
        CPU cpu = createNiceMock(CPU.class);
        Capture<CPU.CPUListener> listenerCapture = Capture.newInstance();
        cpu.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);

        new AutomaticEmulation(cpu, engine, memory);

        CPU.CPUListener listener = listenerCapture.getValue();

        // Just entering running state should not create snapshot
        listener.runStateChanged(CPU.RunState.STATE_RUNNING);

        File outputFile = new File(OUTPUT_FILE);
        assertFalse("Snapshot should not be created while still running", outputFile.exists());
    }

    @Test
    public void testInternalStateChangedDoesNotThrow() {
        CPU cpu = createNiceMock(CPU.class);
        Capture<CPU.CPUListener> listenerCapture = Capture.newInstance();
        cpu.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);

        new AutomaticEmulation(cpu, engine, memory);

        CPU.CPUListener listener = listenerCapture.getValue();
        // Should not throw
        listener.internalStateChanged();
    }

    @Test
    public void testSnapshotContainsAccAndCI() throws Exception {
        CPU cpu = createNiceMock(CPU.class);
        Capture<CPU.CPUListener> listenerCapture = Capture.newInstance();
        cpu.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);
        engine.Acc.set(42);
        engine.CI.set(8);

        new AutomaticEmulation(cpu, engine, memory);

        CPU.CPUListener listener = listenerCapture.getValue();
        listener.runStateChanged(CPU.RunState.STATE_RUNNING);
        listener.runStateChanged(CPU.RunState.STATE_STOPPED_NORMAL);

        File outputFile = new File(OUTPUT_FILE);
        assertTrue(outputFile.exists());

        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue("Should contain ACC value", content.contains("ACC=0x" + Integer.toHexString(42)));
        assertTrue("Should contain CI value", content.contains("CI=0x" + Integer.toHexString(8)));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullCpu() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);
        new AutomaticEmulation(null, engine, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullEngine() {
        CPU cpu = createNiceMock(CPU.class);
        replay(cpu);
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        new AutomaticEmulation(cpu, null, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullMemory() {
        CPU cpu = createNiceMock(CPU.class);
        replay(cpu);
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory, p -> false);
        new AutomaticEmulation(cpu, engine, null);
    }
}

