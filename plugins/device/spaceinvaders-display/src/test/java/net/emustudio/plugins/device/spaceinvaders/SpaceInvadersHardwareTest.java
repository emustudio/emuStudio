/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static net.emustudio.plugins.device.spaceinvaders.SoundOutput.Sample.*;

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
    private static class RecordingSound implements SoundOutput {
        final List<Sample> played = new ArrayList<>();
        int loops;
        int ufoStops;
        int stops;

        public void play(Sample sample, boolean loop) {
            played.add(sample);
            if (loop) loops++;
        }
        public void stop(Sample sample) {
            assertEquals(UFO, sample);
            ufoStops++;
        }
        public void stopAll() { stops++; }
    }

    @Test
    public void soundPortsTriggerOnlyOnRisingEdges() {
        RecordingSound sound = new RecordingSound();
        SpaceInvadersHardware hardware = new SpaceInvadersHardware(sound);
        hardware.write(3, (byte) 0x3E);
        hardware.write(3, (byte) 0x3E);
        hardware.write(5, (byte) 0x1F);
        hardware.write(5, (byte) 0x1F);
        assertEquals(Arrays.asList(SHOT, PLAYER_HIT, INVADER_HIT, BONUS,
                FLEET_1, FLEET_2, FLEET_3, FLEET_4, UFO_HIT), sound.played);

        hardware.write(3, (byte) 0x20);
        hardware.write(3, (byte) 0x22);
        assertEquals(SHOT, sound.played.get(9));
        // Reading port 3 remains the shifter even when writing it controls sound.
        hardware.write(4, (byte) 0xAA);
        hardware.write(4, (byte) 0xCC);
        hardware.write(2, (byte) 3);
        assertEquals(0x65, hardware.read(3) & 0xFF);
    }

    @Test
    public void ufoLoopsUntilClearedAndMuteStopsAllSounds() {
        RecordingSound sound = new RecordingSound();
        SpaceInvadersHardware hardware = new SpaceInvadersHardware(sound);
        hardware.write(3, (byte) 0x21);
        hardware.write(3, (byte) 0x21);
        assertEquals(1, sound.loops);
        hardware.write(3, (byte) 0x20);
        assertEquals(1, sound.ufoStops);
        hardware.write(3, (byte) 0x21);
        hardware.write(3, (byte) 0x01);
        assertEquals(1, sound.stops);
        hardware.write(5, (byte) 0x1F);
        assertEquals(Arrays.asList(UFO, UFO), sound.played);
        hardware.write(3, (byte) 0x21);
        assertEquals(3, sound.loops);
    }

    @Test
    public void resetStopsAudioAndClearsSoundLatches() {
        RecordingSound sound = new RecordingSound();
        SpaceInvadersHardware hardware = new SpaceInvadersHardware(sound);
        hardware.write(3, (byte) 0x22);
        hardware.write(5, (byte) 0x01);
        hardware.reset();
        hardware.write(5, (byte) 0x10);
        assertEquals(1, sound.stops);
        assertEquals(Arrays.asList(SHOT, FLEET_1), sound.played);
        hardware.write(3, (byte) 0x22);
        hardware.write(5, (byte) 0x01);
        assertEquals(Arrays.asList(SHOT, FLEET_1, SHOT, FLEET_1), sound.played);
    }

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
