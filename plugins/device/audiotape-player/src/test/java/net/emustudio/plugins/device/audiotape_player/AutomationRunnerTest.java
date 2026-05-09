/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.plugins.device.audiotape_player.loaders.Loader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AutomationRunnerTest {

    private TapePlaybackController controller;

    @Before
    public void setUp() {
        Loader.TapePlayback listener = niceMock(Loader.TapePlayback.class);
        replay(listener);
        controller = new TapePlaybackController(listener);
    }

    @After
    public void tearDown() {
        controller.close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullControllerThrows() {
        new AutomationRunner(null, Collections.emptyList());
    }

    @Test
    public void testEmptyEventsCompletes() throws InterruptedException {
        AutomationRunner runner = new AutomationRunner(controller, Collections.emptyList());
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        assertFalse(runner.isCancelled());
        assertEquals(0, runner.getCurrentEventIndex());
    }

    @Test
    public void testGetEventsReturnsUnmodifiable() {
        List<AutomationEvent> events = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.PLAY),
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        assertEquals(2, runner.getEvents().size());
        try {
            runner.getEvents().add(new AutomationEvent(AutomationEvent.Type.PLAY));
            fail("Should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testInitialIndexIsMinusOne() {
        AutomationRunner runner = new AutomationRunner(controller, Collections.emptyList());
        assertEquals(-1, runner.getCurrentEventIndex());
    }

    @Test
    public void testCancelSetsFlag() {
        AutomationRunner runner = new AutomationRunner(controller, Collections.emptyList());
        assertFalse(runner.isCancelled());
        runner.cancel();
        assertTrue(runner.isCancelled());
    }

    @Test
    public void testStopEventCallsControllerStop() throws InterruptedException {
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        // Controller should still be in UNLOADED (stop on unloaded = no-op)
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testResetEventCallsControllerReset() throws InterruptedException {
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.RESET)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testUnloadEventCallsControllerStopWithUnload() throws InterruptedException {
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.UNLOAD)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testLoadTapeEventCallsControllerLoad() throws InterruptedException {
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "test.tap")
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());
    }

    @Test
    public void testLoadTapeEmptyPathSkipped() throws InterruptedException {
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "")
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testCancelStopsExecution() throws InterruptedException {
        // Use a delay event so we have time to cancel
        List<AutomationEvent> events = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "10"),
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();

        // Wait a bit then cancel + interrupt (sleep won't check the flag)
        Thread.sleep(200);
        assertTrue(thread.isAlive());
        runner.cancel();
        thread.interrupt();
        thread.join(2000);
        assertFalse(thread.isAlive());
        assertTrue(runner.isCancelled());
        // Should have been interrupted during DELAY, so STOP event should not have run
        assertEquals(0, runner.getCurrentEventIndex());
    }

    @Test
    public void testEventIndexListenerNotified() throws InterruptedException {
        List<AutomationEvent> events = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.STOP),
                new AutomationEvent(AutomationEvent.Type.STOP),
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);

        List<Integer> notifiedIndices = Collections.synchronizedList(new ArrayList<>());
        runner.setEventIndexListener(notifiedIndices::add);

        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());

        // Should have been notified for indices 0, 1, 2, and final (3 = events.size())
        assertEquals(4, notifiedIndices.size());
        assertEquals(Integer.valueOf(0), notifiedIndices.get(0));
        assertEquals(Integer.valueOf(1), notifiedIndices.get(1));
        assertEquals(Integer.valueOf(2), notifiedIndices.get(2));
        assertEquals(Integer.valueOf(3), notifiedIndices.get(3));
    }

    @Test
    public void testNoListenerDoesNotThrow() throws InterruptedException {
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        // no listener set
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
    }

    @Test
    public void testMultipleEventsExecuteInOrder() throws InterruptedException {
        List<AutomationEvent> events = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "test.tap"),
                new AutomationEvent(AutomationEvent.Type.UNLOAD)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);

        List<Integer> notifiedIndices = Collections.synchronizedList(new ArrayList<>());
        runner.setEventIndexListener(notifiedIndices::add);

        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());

        // After LOAD then UNLOAD, state should be UNLOADED
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
        assertEquals(3, notifiedIndices.size()); // 0, 1, 2 (completed)
    }

    @Test
    public void testDelayWithInvalidValueSkipped() throws InterruptedException {
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "abc")
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
        // Should complete quickly without error
        assertEquals(1, runner.getCurrentEventIndex());
    }

    @Test
    public void testDelayWithZeroCompletesImmediately() throws InterruptedException {
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "0")
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
    }

    @Test
    public void testPlayOnUnloadedCompletesImmediately() throws InterruptedException {
        // Play without loading - controller stays UNLOADED, play() is no-op,
        // waitForPlaybackEnd sees not PLAYING → exits immediately
        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.PLAY)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertFalse(thread.isAlive());
    }

    @Test
    public void testFinalIndexIsEventsSize() throws InterruptedException {
        List<AutomationEvent> events = Arrays.asList(
                new AutomationEvent(AutomationEvent.Type.STOP),
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        AutomationRunner runner = new AutomationRunner(controller, events);
        Thread thread = new Thread(runner);
        thread.start();
        thread.join(2000);
        assertEquals(events.size(), runner.getCurrentEventIndex());
    }
}

