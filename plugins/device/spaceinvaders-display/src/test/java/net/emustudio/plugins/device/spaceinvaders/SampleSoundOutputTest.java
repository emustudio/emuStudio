/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import org.junit.Test;
import javax.sound.sampled.Clip;
import java.util.Map;
import static net.emustudio.plugins.device.spaceinvaders.SoundOutput.Sample.*;
import static org.easymock.EasyMock.*;

public class SampleSoundOutputTest {
    @Test
    public void restartsEffectsAndLoopsUfoWithoutWaiting() {
        Clip shot = createStrictMock(Clip.class);
        shot.stop();
        shot.setFramePosition(0);
        shot.start();
        Clip ufo = createStrictMock(Clip.class);
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
}
