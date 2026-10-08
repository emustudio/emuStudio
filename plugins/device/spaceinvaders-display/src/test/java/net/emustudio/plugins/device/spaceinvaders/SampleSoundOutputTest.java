/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import org.junit.Test;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    @Test
    public void recordedAudioMixesEffectsAtCurrentPositionAndVolume() {
        Clip shot = playingClip(1);
        Clip hit = playingClip(0);
        SampleSoundOutput output = new SampleSoundOutput(Map.of(SHOT, shot, PLAYER_HIT, hit),
                Map.of(SHOT, new short[]{0, 2000, -2000}, PLAYER_HIT, new short[]{1000, 1000}));
        output.setVolumePercent(50);
        assertArrayEquals(new short[]{1500, 1500, -500, -500, 0, 0}, pcm(output.captureAudio(3)));
        output.setVolumePercent(0);
        assertArrayEquals(new short[6], pcm(output.captureAudio(3)));
    }

    @Test
    public void recordedAudioLoopsUfoAndClampsOverlappingEffects() {
        Clip ufo = playingClip(3);
        Clip shot = playingClip(0);
        SampleSoundOutput output = new SampleSoundOutput(Map.of(UFO, ufo, SHOT, shot),
                Map.of(UFO, new short[]{20000, -20000}, SHOT, new short[]{-20000, 20000}));
        output.setVolumePercent(100);
        output.play(UFO, true);
        assertArrayEquals(new short[]{Short.MIN_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, Short.MAX_VALUE},
                pcm(output.captureAudio(2)));
    }

    @Test
    public void missingOrStoppedEffectsRecordSilence() {
        Clip stopped = createNiceMock(Clip.class);
        replay(stopped);
        SampleSoundOutput output = new SampleSoundOutput(Map.of(SHOT, stopped), Map.of(SHOT, new short[]{2000}));
        assertArrayEquals(new short[4], pcm(output.captureAudio(2)));
        assertArrayEquals(new short[4], pcm(new SampleSoundOutput().captureAudio(2)));
    }

    private static Clip playingClip(long framePosition) {
        Clip clip = createNiceMock(Clip.class);
        expect(clip.isRunning()).andReturn(true).anyTimes();
        expect(clip.getLongFramePosition()).andReturn(framePosition).anyTimes();
        replay(clip);
        return clip;
    }

    private static short[] pcm(byte[] data) {
        short[] samples = new short[data.length / Short.BYTES];
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
        return samples;
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
