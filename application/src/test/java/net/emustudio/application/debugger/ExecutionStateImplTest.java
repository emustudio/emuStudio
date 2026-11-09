/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.application.ExecutionStateImpl;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.application.ExecutionState;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionStateImplTest {
    @Test
    public void unchangedSnapshotDoesNotSendDuplicateNotifications() throws Exception {
        ExecutionStateImpl state = new ExecutionStateImpl();
        CPU cpu = mock(CPU.class);
        when(cpu.sampleFrequency()).thenReturn(Float.NaN);
        AtomicInteger changes = new AtomicInteger();
        try (EmulationController owner = new EmulationController(cpu, null, List.of(), null, null, state);
             ExecutionState.Subscription subscription = state.subscribe(value -> changes.incrementAndGet())) {
            state.publish(owner, snapshot(4));

            state.publish(owner, snapshot(4));

            assertEquals(1, changes.get());
        }
    }

    @Test
    public void pausedEditNotifiesBusyThenIdleOnceEvenWhenPcDoesNotChange() throws Exception {
        ExecutionStateImpl state = new ExecutionStateImpl();
        CPU cpu = mock(CPU.class);
        when(cpu.sampleFrequency()).thenReturn(Float.NaN);
        List<ExecutionState.Snapshot> changes = new CopyOnWriteArrayList<>();
        AtomicInteger register = new AtomicInteger();
        try (EmulationController owner = new EmulationController(cpu, null, List.of(), null, null, state);
             ExecutionState.Subscription subscription = state.subscribe(changes::add)) {
            state.mutate(() -> register.set(42)).get(2, TimeUnit.SECONDS);

            assertEquals(42, register.get());
            assertEquals(2, changes.size());
            assertTrue(changes.get(0).isBusy());
            assertFalse(changes.get(1).isBusy());
            assertEquals(changes.get(0).currentInstruction(), changes.get(1).currentInstruction());
            assertEquals(CPU.RunState.STATE_STOPPED_BREAK, changes.get(1).runState());
        }
    }

    @Test
    public void instructionLocationChangesOnlyWithPublishedSnapshot() throws Exception {
        ExecutionStateImpl state = new ExecutionStateImpl();
        CPU cpu = mock(CPU.class);
        try (EmulationController owner = new EmulationController(cpu, null, List.of(), null, null, state)) {
            state.publish(owner, snapshot(4));
            ExecutionState.Snapshot first = state.snapshot();

            state.publish(owner, snapshot(20));

            assertEquals(4, first.currentInstruction());
            assertEquals(20, state.snapshot().currentInstruction());
        }
    }

    @Test
    public void closedSubscriptionReceivesNoLaterNotifications() throws Exception {
        ExecutionStateImpl state = new ExecutionStateImpl();
        AtomicInteger changes = new AtomicInteger();
        try (EmulationController owner = new EmulationController(mock(CPU.class), null, List.of(), null, null, state)) {
            ExecutionState.Subscription subscription = state.subscribe(value -> changes.incrementAndGet());
            state.publish(owner, snapshot(4));
            assertEquals(1, changes.get());

            subscription.close();
            state.publish(owner, snapshot(20));

            assertEquals(1, changes.get());
            assertEquals(20, state.snapshot().currentInstruction());
        }
    }

    @Test
    public void formerOwnerCannotPublishOrUnbindReplacement() throws Exception {
        ExecutionStateImpl state = new ExecutionStateImpl();
        try (EmulationController first = new EmulationController(mock(CPU.class), null, List.of(), null, null, state);
             EmulationController second = new EmulationController(mock(CPU.class), null, List.of(), null, null, state)) {
            state.publish(second, snapshot(20));

            state.unbind(first);
            state.publish(first, snapshot(99));
            state.publish(second, snapshot(21));

            assertEquals(21, state.snapshot().currentInstruction());
        }
    }

    private static ExecutionState.Snapshot snapshot(int location) {
        return new ExecutionState.Snapshot(CPU.RunState.STATE_STOPPED_BREAK, false, false, location, Float.NaN);
    }
}
