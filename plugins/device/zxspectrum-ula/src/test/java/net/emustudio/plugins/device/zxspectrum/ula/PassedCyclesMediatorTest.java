/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import org.junit.Test;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.*;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class PassedCyclesMediatorTest {

    @Test
    public void testInterruptIsClearedAfterExact32TStates() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        long frameCycles = (long) (PRE_SCREEN_LINES + SCREEN_HEIGHT_PIXELS + POST_SCREEN_LINES) * DISPLAY_LINE_TSTATES;
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
            replay(bus);
            return bus;
        }
    }
}
