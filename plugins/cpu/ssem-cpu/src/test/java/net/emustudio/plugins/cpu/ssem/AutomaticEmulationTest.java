/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.cpu.CPUSnapshot;
import net.emustudio.emulib.plugins.cpu.CPUContext;

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
    private final CPUContext context = createNiceMock(CPUContext.class);

    @After
    public void tearDown() {
        File file = new File(OUTPUT_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void testConstructorRegistersCPUListener() {
        context.addCPUListener(anyObject());
        expectLastCall().once();
        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(context, engine, memory);

        verify(context);
    }

    @Test
    public void testDestroyRemovesCPUListener() {
        context.addCPUListener(anyObject());
        expectLastCall().once();
        context.removeCPUListener(anyObject());
        expectLastCall().once();

        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        AutomaticEmulation ae = new AutomaticEmulation(context, engine, memory);
        ae.destroy();

        verify(context);
    }

    @Test
    public void testSnapshotCreatedOnStateTransitionFromRunningToStopped() {
        Capture<CPUContext.CPUListener> listenerCapture = Capture.newInstance();
        context.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        expect(context.snapshot()).andAnswer(() -> currentSnapshot).anyTimes();
        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(context, engine, memory);

        CPUContext.CPUListener listener = listenerCapture.getValue();

        // Simulate state transition: RUNNING -> STOPPED
        publish(listener, CPU.RunState.STATE_RUNNING);
        publish(listener, CPU.RunState.STATE_STOPPED_NORMAL);

        File outputFile = new File(OUTPUT_FILE);
        assertTrue("Snapshot file should be created", outputFile.exists());
        assertTrue("Snapshot file should not be empty", outputFile.length() > 0);
    }

    @Test
    public void testNoSnapshotWithoutRunningFirst() {
        Capture<CPUContext.CPUListener> listenerCapture = Capture.newInstance();
        context.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        expect(context.snapshot()).andAnswer(() -> currentSnapshot).anyTimes();
        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(context, engine, memory);

        CPUContext.CPUListener listener = listenerCapture.getValue();

        // Just stopping without running first should not create snapshot
        publish(listener, CPU.RunState.STATE_STOPPED_NORMAL);

        File outputFile = new File(OUTPUT_FILE);
        assertFalse("Snapshot should not be created without prior RUNNING state", outputFile.exists());
    }

    @Test
    public void testRunningStateAloneDoesNotCreateSnapshot() {
        Capture<CPUContext.CPUListener> listenerCapture = Capture.newInstance();
        context.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        expect(context.snapshot()).andAnswer(() -> currentSnapshot).anyTimes();
        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(context, engine, memory);

        CPUContext.CPUListener listener = listenerCapture.getValue();

        // Just entering running state should not create snapshot
        publish(listener, CPU.RunState.STATE_RUNNING);

        File outputFile = new File(OUTPUT_FILE);
        assertFalse("Snapshot should not be created while still running", outputFile.exists());
    }

    @Test
    public void testPausedPublicationDoesNotThrow() {
        Capture<CPUContext.CPUListener> listenerCapture = Capture.newInstance();
        context.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        expect(context.snapshot()).andAnswer(() -> currentSnapshot).anyTimes();
        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(context, engine, memory);

        CPUContext.CPUListener listener = listenerCapture.getValue();
        // Should not throw
        publish(listener, CPU.RunState.STATE_STOPPED_BREAK);
    }

    @Test
    public void testSnapshotContainsAccAndCI() throws Exception {
        Capture<CPUContext.CPUListener> listenerCapture = Capture.newInstance();
        context.addCPUListener(capture(listenerCapture));
        expectLastCall().once();
        expect(context.snapshot()).andAnswer(() -> currentSnapshot).anyTimes();
        replay(context);

        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.Acc.set(42);
        engine.CI.set(8);

        new AutomaticEmulation(context, engine, memory);

        CPUContext.CPUListener listener = listenerCapture.getValue();
        publish(listener, CPU.RunState.STATE_RUNNING);
        publish(listener, CPU.RunState.STATE_STOPPED_NORMAL);

        File outputFile = new File(OUTPUT_FILE);
        assertTrue(outputFile.exists());

        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue("Should contain ACC value", content.contains("ACC=0x" + Integer.toHexString(42)));
        assertTrue("Should contain CI value", content.contains("CI=0x" + Integer.toHexString(8)));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullExecution() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        new AutomaticEmulation(null, engine, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullEngine() {
        replay(context);
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        new AutomaticEmulation(context, null, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullMemory() {
        replay(context);
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        new AutomaticEmulation(context, engine, null);
    }
    private CPUSnapshot currentSnapshot = state(CPU.RunState.STATE_STOPPED_BREAK);

    private void publish(CPUContext.CPUListener listener, CPU.RunState runState) {
        currentSnapshot = state(runState);
        listener.runStateChanged(runState);
        listener.internalStateChanged();
    }

    private static CPUSnapshot state(CPU.RunState runState) {
        return new CPUSnapshot(runState, false, false, 0, Float.NaN);
    }
}
