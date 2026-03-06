/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import org.junit.Test;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus.LINE_CYCLES;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class PassedCyclesMediatorTest {

    @Test
    public void testInterruptIsClearedAfterExact32TStates() {
        TestULA ula = new TestULA();
        PassedCyclesMediator mediator = new PassedCyclesMediator(ula);

        long frameCycles = (long) (ZxParameters.PRE_SCREEN_LINES + ZxParameters.SCREEN_HEIGHT + ZxParameters.POST_SCREEN_LINES) * LINE_CYCLES;
        for (long i = 0; i < frameCycles; i++) {
            mediator.passedCycles(1);
        }

        assertEquals(1, ula.framesStarted);
        assertEquals(0, ula.interruptCleared);

        for (int i = 0; i < ZxParameters.INT_DURATION - 1; i++) {
            mediator.passedCycles(1);
        }
        assertEquals(0, ula.interruptCleared);

        mediator.passedCycles(1);
        assertEquals(1, ula.interruptCleared);
    }

    private static final class TestULA extends ULA {
        private int framesStarted;
        private int interruptCleared;

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

        private static ZxSpectrumBus newMockBus() {
            ZxSpectrumBus bus = createNiceMock(ZxSpectrumBus.class);
            replay(bus);
            return bus;
        }
    }
}
