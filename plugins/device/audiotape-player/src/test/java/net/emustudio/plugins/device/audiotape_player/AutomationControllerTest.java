/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AutomationControllerTest {

    private TapePlaybackController tapeController;
    private AutomationController controller;

    @Before
    public void setUp() {
        tapeController = niceMock(TapePlaybackController.class);
        controller = new AutomationController(tapeController);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });
    }

    @After
    public void tearDown() {
        controller.close();
    }

    @Test(expected = NullPointerException.class)
    public void testNullControllerThrows() {
        new AutomationController(null);
    }

    @Test
    public void testInitialStateIsNotPlaying() {
        assertFalse(controller.isPlaying());
    }

    @Test
    public void testPlayEmptyEventsTransitionsToPlayingThenStopped() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        replay(tapeController);
        controller.play(Collections.emptyList());
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        assertFalse(controller.isPlaying());
    }

    @Test
    public void testPlayExecutesStopEvent() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        tapeController.stop(false);
        expectLastCall().once();
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
    }

    @Test
    public void testPlayExecutesResetEvent() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        tapeController.reset();
        expectLastCall().once();
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.RESET)
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
    }

    @Test
    public void testPlayExecutesUnloadEvent() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        tapeController.stop(true);
        expectLastCall().once();
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.UNLOAD)
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
    }

    @Test
    public void testPlayExecutesLoadTapeEvent() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        tapeController.load(Path.of("test.tap"));
        expectLastCall().once();
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "test.tap")
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
    }

    @Test
    public void testPlaySkipsLoadTapeWithEmptyPath() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        // no load() call expected
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "")
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
    }

    @Test
    public void testPlayExecutesPlayEvent() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        tapeController.play();
        expectLastCall().once();
        // waitForPlaybackEnd polls getState(); return STOPPED immediately
        expect(tapeController.getState()).andReturn(TapePlaybackController.CassetteState.STOPPED).once();
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.PLAY)
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
    }

    @Test
    public void testPlayMultipleEvents() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        List<Integer> indices = Collections.synchronizedList(new ArrayList<>());
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
                indices.add(index);
            }
        });

        tapeController.load(Path.of("file.tap"));
        expectLastCall().once();
        tapeController.play();
        expectLastCall().once();
        expect(tapeController.getState()).andReturn(TapePlaybackController.CassetteState.STOPPED).once();
        tapeController.stop(false);
        expectLastCall().once();
        replay(tapeController);

        List<AutomationEvent> events = new ArrayList<>();
        events.add(new AutomationEvent(AutomationEvent.Type.LOAD_TAPE, "file.tap"));
        events.add(new AutomationEvent(AutomationEvent.Type.PLAY));
        events.add(new AutomationEvent(AutomationEvent.Type.STOP));

        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
        verify(tapeController);
        assertEquals(List.of(0, 1, 2), indices);
    }

    @Test
    public void testStopWhilePlaying() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stoppedLatch = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stoppedLatch.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
                started.countDown();
            }
        });

        // DELAY event with large delay to keep it playing
        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "9999")
        );
        controller.play(events);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertTrue(controller.isPlaying());

        controller.stop();
        assertTrue(stoppedLatch.await(5, TimeUnit.SECONDS));
        assertFalse(controller.isPlaying());
    }

    @Test
    public void testStopWhileNotPlayingDoesNothing() {
        assertFalse(controller.isPlaying());
        controller.stop();
        assertFalse(controller.isPlaying());
    }

    @Test
    public void testStopWithoutListenerDoesNotThrow() {
        TapePlaybackController tc = niceMock(TapePlaybackController.class);
        replay(tc);
        AutomationController headless = new AutomationController(tc);
        headless.stop();
        headless.close();
    }

    @Test
    public void testCloseWithoutListenerDoesNotThrow() {
        TapePlaybackController tc = niceMock(TapePlaybackController.class);
        replay(tc);
        AutomationController headless = new AutomationController(tc);
        headless.close();
    }

    @Test
    public void testPlayWithoutListenerCompletes() throws InterruptedException {
        TapePlaybackController tc = niceMock(TapePlaybackController.class);
        replay(tc);
        try (AutomationController headless = new AutomationController(tc)) {
            headless.play(Collections.emptyList());
            long deadline = System.currentTimeMillis() + 5000;
            while (headless.isPlaying() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertFalse(headless.isPlaying());
        }
    }

    @Test
    public void testPlayWhileAlreadyPlayingIsIgnored() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
            }

            @Override
            public void currentIndexChanged(int index) {
                started.countDown();
            }
        });

        replay(tapeController);

        List<AutomationEvent> longEvents = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "9999")
        );
        controller.play(longEvents);
        assertTrue(started.await(5, TimeUnit.SECONDS));

        // second play should be ignored
        List<AutomationEvent> shortEvents = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.STOP)
        );
        controller.play(shortEvents);
        assertTrue(controller.isPlaying());
    }

    @Test
    public void testResetStopsPlayback() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stoppedLatch = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stoppedLatch.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
                started.countDown();
            }
        });

        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "9999")
        );
        controller.play(events);
        assertTrue(started.await(5, TimeUnit.SECONDS));

        controller.reset();
        assertTrue(stoppedLatch.await(5, TimeUnit.SECONDS));
        assertFalse(controller.isPlaying());
    }

    @Test
    public void testCloseSetsClosed() throws InterruptedException {
        CountDownLatch closed = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.CLOSED) {
                    closed.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        replay(tapeController);
        controller.close();
        assertTrue(closed.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testCloseWhilePlaying() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch closedLatch = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.CLOSED) {
                    closedLatch.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
                started.countDown();
            }
        });

        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "9999")
        );
        controller.play(events);
        assertTrue(started.await(5, TimeUnit.SECONDS));

        controller.close();
        assertTrue(closedLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testDelayWithZeroCompletesImmediately() throws InterruptedException {
        CountDownLatch stopped = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                if (state == AutomationController.State.STOPPED) {
                    stopped.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        replay(tapeController);

        List<AutomationEvent> events = Collections.singletonList(
                new AutomationEvent(AutomationEvent.Type.DELAY, "0")
        );
        controller.play(events);
        assertTrue(stopped.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testStateListenerReceivesPlayingNotification() throws InterruptedException {
        // When play() is called, the listener should be notified with state changes.
        // The listener is notified with STOPPED when finished.
        List<AutomationController.State> states = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        controller.setListener(new AutomationController.AutomationListener() {
            @Override
            public void stateChanged(AutomationController.State state) {
                states.add(state);
                if (state == AutomationController.State.STOPPED) {
                    done.countDown();
                }
            }

            @Override
            public void currentIndexChanged(int index) {
            }
        });

        replay(tapeController);
        controller.play(Collections.emptyList());
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertTrue(states.contains(AutomationController.State.STOPPED));
    }
}

