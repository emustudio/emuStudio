/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class SpaceInvadersHardwareTest {
    @Test
    public void shiftsTwoWrittenBytesBySelectedAmount() {
        SpaceInvadersHardware hardware = new SpaceInvadersHardware();
        hardware.write(4, (byte) 0xAA);
        hardware.write(4, (byte) 0xCC);
        hardware.write(2, (byte) 3);

        assertEquals(0x65, hardware.read(3) & 0xFF);
    }

    @Test
    public void mapsMomentaryInputBits() {
        SpaceInvadersHardware hardware = new SpaceInvadersHardware();
        assertEquals(SpaceInvadersHardware.ALWAYS_ON, hardware.read(1) & 0xFF);
        hardware.setInput1(SpaceInvadersHardware.COIN, true);
        hardware.setInput1(SpaceInvadersHardware.LEFT, true);
        assertEquals(0x29, hardware.read(1) & 0xFF);
        hardware.setInput1(SpaceInvadersHardware.COIN, false);
        assertEquals(0x28, hardware.read(1) & 0xFF);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mapsRotatedFramebufferPixel() {
        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        expect(memory.read(DisplayPanel.FRAMEBUFFER + 31)).andReturn((byte) 0x80).times(2);
        replay(memory);

        assertTrue(DisplayPanel.pixelOn(memory, 0, 0));
        assertFalse(DisplayPanel.pixelOn(memory, 0, 1));
        verify(memory);
    }

    @Test
    public void alternatesRasterInterrupts() {
        Context8080 cpu = createMock(Context8080.class);
        expect(cpu.isInterruptSupported()).andReturn(true).times(2);
        cpu.signalInterrupt(aryEq(new byte[]{(byte) 0xCF}));
        expectLastCall();
        cpu.signalInterrupt(aryEq(new byte[]{(byte) 0xD7}));
        expectLastCall();
        replay(cpu);

        int[] frames = {0};
        FrameClock clock = new FrameClock(cpu, () -> frames[0]++);
        clock.tick();
        clock.tick();
        clock.close();

        assertEquals(1, frames[0]);
        verify(cpu);
    }
}
