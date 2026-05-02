/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.emustudio.plugins.device.zxspectrum.bus.api.TimingProfile;
import net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas;
import org.junit.Test;

import static net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayCanvas.SCREEN_IMAGE_HEIGHT;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class PassedCyclesMediatorTest {
    private static final TimingProfile TIMING = TimingProfile.ZX_SPECTRUM_48K;
    private static final int DISPLAY_LINE_TSTATES = TIMING.displayLineTstates;
    private static final int DISPLAY_FRAME_TSTATES = TIMING.displayFrameTstates;
    private static final int INTERRUPT_TSTATES = TIMING.interruptTstates;

    @Test
    public void testInterruptIsClearedAfterExact32TStates() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        long frameCycles = TIMING.displayFrameTstates;
        for (long i = 0; i < frameCycles; i++) {
            mediator.passedCycles(1);
        }

        assertEquals(1, ula.framesStarted);
        assertEquals(0, ula.interruptCleared);
        assertEquals(frameCycles, ula.audioCycles);

        for (int i = 0; i < INTERRUPT_TSTATES - 1; i++) {
            mediator.passedCycles(1);
        }
        assertEquals(0, ula.interruptCleared);

        mediator.passedCycles(1);
        assertEquals(1, ula.interruptCleared);
    }

    @Test
    public void testNullCanvasDoesNotThrowDuringPassedCycles() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);
        // canvas is null by default — should not throw
        mediator.passedCycles(DISPLAY_LINE_TSTATES);
    }

    @Test
    public void testLineDrawnOnCanvasAtLineBoundary() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);
        DisplayCanvas canvas = createNiceMock(DisplayCanvas.class);
        // Expect drawNextLine(0) to be called once for the first line boundary
        canvas.drawNextLine(0);
        expectLastCall().once();
        replay(canvas);

        mediator.setCanvas(canvas);
        // Pass exactly one line worth of T-states
        mediator.passedCycles(DISPLAY_LINE_TSTATES);

        verify(canvas);
    }

    @Test
    public void testNoLinesDrawnBeyondScreenImageHeight() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);
        DisplayCanvas canvas = createStrictMock(DisplayCanvas.class);
        // Expect exactly SCREEN_IMAGE_HEIGHT drawNextLine calls (lines 0..311)
        for (int i = 0; i < SCREEN_IMAGE_HEIGHT; i++) {
            canvas.drawNextLine(i);
            expectLastCall().once();
        }
        canvas.repaint();
        expectLastCall().once();
        replay(canvas);

        mediator.setCanvas(canvas);
        // Pass one full frame (will draw all lines + trigger repaint)
        for (int i = 0; i < DISPLAY_FRAME_TSTATES; i++) {
            mediator.passedCycles(1);
        }

        verify(canvas);
    }

    @Test
    public void testFrameBoundaryTriggersOnNextFrameAndRepaint() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);
        DisplayCanvas canvas = createNiceMock(DisplayCanvas.class);
        canvas.repaint();
        expectLastCall().once();
        replay(canvas);

        mediator.setCanvas(canvas);
        // Pass exactly one full frame
        mediator.passedCycles(DISPLAY_FRAME_TSTATES);

        assertEquals(1, ula.framesStarted);
        verify(canvas);
    }

    @Test
    public void testMultipleFramesCycleCorrectly() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        // Run 3 full frames
        for (int frame = 0; frame < 3; frame++) {
            for (long i = 0; i < DISPLAY_FRAME_TSTATES; i++) {
                mediator.passedCycles(1);
            }
        }

        assertEquals(3, ula.framesStarted);
        // After exactly 3 frames, only 2 interrupts are cleared:
        // the 3rd frame boundary activates an interrupt that needs INTERRUPT_TSTATES more cycles to clear.
        assertEquals(2, ula.interruptCleared);
        assertEquals(3L * DISPLAY_FRAME_TSTATES, ula.audioCycles);

        // Clear the 3rd interrupt
        for (int i = 0; i < INTERRUPT_TSTATES; i++) {
            mediator.passedCycles(1);
        }
        assertEquals(3, ula.interruptCleared);
    }

    @Test
    public void testResetClearsAllInternalState() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        // Accumulate half a frame
        long halfFrame = DISPLAY_FRAME_TSTATES / 2;
        mediator.passedCycles(halfFrame);
        assertEquals(0, ula.framesStarted);

        mediator.reset();

        // After reset, we need a full frame again to trigger onNextFrame
        for (long i = 0; i < DISPLAY_FRAME_TSTATES; i++) {
            mediator.passedCycles(1);
        }
        assertEquals(1, ula.framesStarted);
    }

    @Test
    public void testBulkCyclesStillDrawLinesAndFrame() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        // Pass a whole frame in a single bulk call
        mediator.passedCycles(DISPLAY_FRAME_TSTATES);

        assertEquals(1, ula.framesStarted);
        assertEquals(DISPLAY_FRAME_TSTATES, ula.audioCycles);
    }

    @Test
    public void testAudioCyclesAlwaysForwardedEvenWithoutCanvas() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);
        // No canvas set

        mediator.passedCycles(12345);

        assertEquals(12345, ula.audioCycles);
    }

    @Test
    public void testSetCanvasToNullStopsDrawing() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);
        DisplayCanvas canvas = createNiceMock(DisplayCanvas.class);
        replay(canvas);

        mediator.setCanvas(canvas);
        mediator.setCanvas(null);

        // Should not throw even though canvas is null again
        mediator.passedCycles(DISPLAY_LINE_TSTATES);
    }

    @Test
    public void testInterruptNotClearedBeforeThreshold() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        // One full frame
        mediator.passedCycles(DISPLAY_FRAME_TSTATES);
        assertEquals(1, ula.framesStarted);
        assertEquals(0, ula.interruptCleared);

        // One T-state before the threshold
        mediator.passedCycles(INTERRUPT_TSTATES - 1);
        assertEquals(0, ula.interruptCleared);

        // Reach the threshold
        mediator.passedCycles(1);
        assertEquals(1, ula.interruptCleared);
    }

    @Test
    public void testInterruptNotActivatedUntilFrameBoundary() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        // Pass INTERRUPT_TSTATES cycles without a frame boundary — interrupt should not fire
        mediator.passedCycles(INTERRUPT_TSTATES);
        assertEquals(0, ula.interruptCleared);
    }

    private static final class TestULA extends ULA {
        private int framesStarted;
        private int interruptCleared;
        private long audioCycles;

        private TestULA() {
            super(newMockBus());
        }

        @Override
        public void onNextFrame() {
            framesStarted++;
        }

        @Override
        public void clearInterrupt() {
            interruptCleared++;
        }

        @Override
        public void passedCycles(long cycles) {
            audioCycles += cycles;
        }

        private static ZxSpectrumBus newMockBus() {
            ZxSpectrumBus bus = createNiceMock(ZxSpectrumBus.class);
            expect(bus.getProfile()).andStubReturn(TIMING);
            replay(bus);
            return bus;
        }
    }
}
