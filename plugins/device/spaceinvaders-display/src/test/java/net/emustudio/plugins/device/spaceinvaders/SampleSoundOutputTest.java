/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import org.junit.Test;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.Map;
import static net.emustudio.plugins.device.spaceinvaders.SoundOutput.Sample.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SampleSoundOutputTest {
    @Test
    public void restartsEffectsAndLoopsUfoWithoutWaiting() {
        Clip shot = createStrictMock(Clip.class);
        noVolumeControls(shot);
        shot.stop();
        shot.setFramePosition(0);
        shot.start();
        Clip ufo = createStrictMock(Clip.class);
        noVolumeControls(ufo);
        ufo.stop();
        ufo.setFramePosition(0);
        ufo.loop(Clip.LOOP_CONTINUOUSLY);
        ufo.stop();
        replay(shot, ufo);
        SampleSoundOutput output = new SampleSoundOutput(Map.of(SHOT, shot, UFO, ufo));
        output.play(SHOT, false);
        output.play(UFO, true);
        output.stop(UFO);
        verify(shot, ufo);
    }

    @Test
    public void missingSampleIsSilentAndCloseReleasesEveryClipOnce() {
        Clip clip = createStrictMock(Clip.class);
        noVolumeControls(clip);
        clip.stop();
        clip.stop();
        clip.close();
        replay(clip);
        SampleSoundOutput output = new SampleSoundOutput(Map.of(SHOT, clip));
        output.play(BONUS, false);
        output.stop(BONUS);
        output.stopAll();
        output.close();
        output.close();
        output.play(SHOT, false);
        verify(clip);
    }

    @Test
    public void volumeControlsAllClipsAndZeroMutes() {
        FloatControl shotGain = gainControl();
        FloatControl ufoGain = gainControl();
        BooleanControl shotMute = new BooleanControl(BooleanControl.Type.MUTE, false) { };
        BooleanControl ufoMute = new BooleanControl(BooleanControl.Type.MUTE, false) { };
        Clip shot = clipWithControls(shotGain, shotMute);
        Clip ufo = clipWithControls(ufoGain, ufoMute);
        SampleSoundOutput output = new SampleSoundOutput(Map.of(SHOT, shot, UFO, ufo));
        assertEquals(25, output.getVolumePercent());
        assertEquals(-12.0412f, shotGain.getValue(), 0.001f);
        assertEquals(shotGain.getValue(), ufoGain.getValue(), 0);
        output.setVolumePercent(0);
        assertTrue(shotMute.getValue());
        assertTrue(ufoMute.getValue());
        assertEquals(shotGain.getMinimum(), shotGain.getValue(), 0);
        output.setVolumePercent(50);
        assertFalse(shotMute.getValue());
        assertFalse(ufoMute.getValue());
        assertEquals(-6.0206f, shotGain.getValue(), 0.001f);
        assertEquals(shotGain.getValue(), ufoGain.getValue(), 0);
        verify(shot, ufo);
    }

    @Test
    public void volumeIsClampedAndRetainedWhenNoClipsAreLoaded() {
        SampleSoundOutput output = new SampleSoundOutput();
        output.setVolumePercent(150);
        assertEquals(100, output.getVolumePercent());
        output.setVolumePercent(-1);
        assertEquals(0, output.getVolumePercent());
        output.setVolumePercent(40);
        output.close();
        assertEquals(40, output.getVolumePercent());
    }

    private static FloatControl gainControl() {
        return new FloatControl(FloatControl.Type.MASTER_GAIN, -80, 6, 0.1f, -1, 0, "dB") { };
    }

    private static Clip clipWithControls(FloatControl gain, BooleanControl mute) {
        Clip clip = createMock(Clip.class);
        expect(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)).andReturn(true).anyTimes();
        expect(clip.getControl(FloatControl.Type.MASTER_GAIN)).andReturn(gain).anyTimes();
        expect(clip.isControlSupported(BooleanControl.Type.MUTE)).andReturn(true).anyTimes();
        expect(clip.getControl(BooleanControl.Type.MUTE)).andReturn(mute).anyTimes();
        replay(clip);
        return clip;
    }

    private static void noVolumeControls(Clip clip) {
        expect(clip.isControlSupported(FloatControl.Type.MASTER_GAIN)).andReturn(false).anyTimes();
        expect(clip.isControlSupported(BooleanControl.Type.MUTE)).andReturn(false).anyTimes();
    }
}
