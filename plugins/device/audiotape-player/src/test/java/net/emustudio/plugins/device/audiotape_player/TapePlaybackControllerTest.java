/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.plugins.device.audiotape_player.loaders.Loader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class TapePlaybackControllerTest {

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

    @Test
    public void testInitialStateIsUnloaded() {
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test(expected = NullPointerException.class)
    public void testNullListenerThrows() {
        new TapePlaybackController(null).close();
    }

    @Test
    public void testLoadChangesStateToStopped() {
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());
    }

    @Test
    public void testLoadUnknownExtensionStaysUnloaded() {
        controller.load(Path.of("test.unknown"));
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testPlayFromUnloadedDoesNotChangeState() {
        controller.play();
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testStopFromUnloadedDoesNotChangeState() {
        controller.stop(false);
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testStopWithUnloadFromUnloaded() {
        controller.stop(true);
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testStopWithUnloadFromStopped() {
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());

        controller.stop(true);
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testStopWithoutUnloadFromStopped() {
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());

        controller.stop(false);
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());
    }

    @Test
    public void testResetUnloadsAndStops() {
        controller.load(Path.of("test.tap"));
        controller.reset();
        assertEquals(TapePlaybackController.CassetteState.UNLOADED, controller.getState());
    }

    @Test
    public void testCloseSetsClosed() {
        controller.close();
        assertEquals(TapePlaybackController.CassetteState.CLOSED, controller.getState());
    }

    @Test
    public void testLoadWhileClosedDoesNotChangeState() {
        controller.close();
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.CLOSED, controller.getState());
    }

    @Test
    public void testPlayWhileClosedDoesNotChangeState() {
        controller.close();
        controller.play();
        assertEquals(TapePlaybackController.CassetteState.CLOSED, controller.getState());
    }

    @Test
    public void testLoadTzxChangesStateToStopped() {
        controller.load(Path.of("test.tzx"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());
    }

    @Test
    public void testMultipleLoadsStayStopped() {
        controller.load(Path.of("first.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());

        controller.load(Path.of("second.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());
    }

    @Test
    public void testPlayWithNoLoaderStaysStopped() {
        controller.load(Path.of("test.tap"));
        assertEquals(TapePlaybackController.CassetteState.STOPPED, controller.getState());

        // Play will actually submit a task that will fail since "test.tap" doesn't exist,
        // but the state should change to PLAYING first
        controller.play();
        TapePlaybackController.CassetteState state = controller.getState();
        // State is either PLAYING (task submitted) or STOPPED (task already finished/failed)
        assert state == TapePlaybackController.CassetteState.PLAYING
                || state == TapePlaybackController.CassetteState.STOPPED;
    }
}

