/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ExecutionState;
import net.emustudio.emulib.Numbers;
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
    public void testConstructorSubscribesToExecution() {
        ExecutionState cpu = createMock(ExecutionState.class);
        expect(cpu.subscribe(anyObject())).andReturn(() -> {}).once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(cpu, engine, memory);

        verify(cpu);
    }

    @Test
    public void testDestroyClosesSubscription() {
        ExecutionState cpu = createMock(ExecutionState.class);
        ExecutionState.Subscription subscription = createMock(ExecutionState.Subscription.class);
        expect(cpu.subscribe(anyObject())).andReturn(subscription).once();
        subscription.close();
        expectLastCall().once();
        replay(subscription);

        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        AutomaticEmulation ae = new AutomaticEmulation(cpu, engine, memory);
        ae.destroy();

        verify(cpu, subscription);
    }

    @Test
    public void testSnapshotCreatedOnStateTransitionFromRunningToStopped() {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        Capture<java.util.function.Consumer<ExecutionState.Snapshot>> listenerCapture = Capture.newInstance();
        expect(cpu.subscribe(capture(listenerCapture))).andReturn(() -> {}).once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(cpu, engine, memory);

        java.util.function.Consumer<ExecutionState.Snapshot> listener = listenerCapture.getValue();

        // Simulate state transition: RUNNING -> STOPPED
        listener.accept(state(CPU.RunState.STATE_RUNNING));
        listener.accept(state(CPU.RunState.STATE_STOPPED_NORMAL));

        File outputFile = new File(OUTPUT_FILE);
        assertTrue("Snapshot file should be created", outputFile.exists());
        assertTrue("Snapshot file should not be empty", outputFile.length() > 0);
    }

    @Test
    public void testNoSnapshotWithoutRunningFirst() {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        Capture<java.util.function.Consumer<ExecutionState.Snapshot>> listenerCapture = Capture.newInstance();
        expect(cpu.subscribe(capture(listenerCapture))).andReturn(() -> {}).once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(cpu, engine, memory);

        java.util.function.Consumer<ExecutionState.Snapshot> listener = listenerCapture.getValue();

        // Just stopping without running first should not create snapshot
        listener.accept(state(CPU.RunState.STATE_STOPPED_NORMAL));

        File outputFile = new File(OUTPUT_FILE);
        assertFalse("Snapshot should not be created without prior RUNNING state", outputFile.exists());
    }

    @Test
    public void testRunningStateAloneDoesNotCreateSnapshot() {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        Capture<java.util.function.Consumer<ExecutionState.Snapshot>> listenerCapture = Capture.newInstance();
        expect(cpu.subscribe(capture(listenerCapture))).andReturn(() -> {}).once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(cpu, engine, memory);

        java.util.function.Consumer<ExecutionState.Snapshot> listener = listenerCapture.getValue();

        // Just entering running state should not create snapshot
        listener.accept(state(CPU.RunState.STATE_RUNNING));

        File outputFile = new File(OUTPUT_FILE);
        assertFalse("Snapshot should not be created while still running", outputFile.exists());
    }

    @Test
    public void testPausedPublicationDoesNotThrow() {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        Capture<java.util.function.Consumer<ExecutionState.Snapshot>> listenerCapture = Capture.newInstance();
        expect(cpu.subscribe(capture(listenerCapture))).andReturn(() -> {}).once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);

        new AutomaticEmulation(cpu, engine, memory);

        java.util.function.Consumer<ExecutionState.Snapshot> listener = listenerCapture.getValue();
        // Should not throw
        listener.accept(state(CPU.RunState.STATE_STOPPED_BREAK));
    }

    @Test
    public void testSnapshotContainsAccAndCI() throws Exception {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        Capture<java.util.function.Consumer<ExecutionState.Snapshot>> listenerCapture = Capture.newInstance();
        expect(cpu.subscribe(capture(listenerCapture))).andReturn(() -> {}).once();
        replay(cpu);

        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        engine.Acc.set(42);
        engine.CI.set(8);

        new AutomaticEmulation(cpu, engine, memory);

        java.util.function.Consumer<ExecutionState.Snapshot> listener = listenerCapture.getValue();
        listener.accept(state(CPU.RunState.STATE_RUNNING));
        listener.accept(state(CPU.RunState.STATE_STOPPED_NORMAL));

        File outputFile = new File(OUTPUT_FILE);
        assertTrue(outputFile.exists());

        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue("Should contain ACC value", content.contains("ACC=0x" + Integer.toHexString(42)));
        assertTrue("Should contain CI value", content.contains("CI=0x" + Integer.toHexString(8)));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullExecution() {
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        new AutomaticEmulation(null, engine, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullEngine() {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        replay(cpu);
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        new AutomaticEmulation(cpu, null, memory);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRejectsNullMemory() {
        ExecutionState cpu = createNiceMock(ExecutionState.class);
        replay(cpu);
        ByteMemoryStub memory = new ByteMemoryStub(Numbers.Strategy.REVERSE_BITS);
        memory.setMemory(new short[32 * 4]);
        EmulatorEngine engine = new EmulatorEngine(memory);
        new AutomaticEmulation(cpu, engine, null);
    }
    private static ExecutionState.Snapshot state(CPU.RunState runState) {
        return new ExecutionState.Snapshot(runState, false, false, 0, Float.NaN);
    }
}
