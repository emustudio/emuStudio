/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Plays independent effects without waiting for them on the CPU thread. */
final class SampleSoundOutput implements SoundOutput, AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(SampleSoundOutput.class.getName());
    private final Map<Sample, Clip> clips = new EnumMap<>(Sample.class);

    SampleSoundOutput() { }

    SampleSoundOutput(Map<Sample, Clip> clips) {
        this.clips.putAll(clips);
    }

    synchronized void open(Path directory) {
        close();
        if (!Files.isDirectory(directory)) {
            LOGGER.log(System.Logger.Level.WARNING, "Space Invaders sound samples not found at {0}", directory);
            return;
        }
        for (Sample sample : Sample.values()) {
            Path path = directory.resolve(sample.ordinal() + ".wav");
            if (!Files.isRegularFile(path)) {
                path = directory.resolve(sample.ordinal() + ".WAV");
            }
            Clip clip = null;
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile())) {
                clip = AudioSystem.getClip();
                clip.open(stream);
                clips.put(sample, clip);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException e) {
                if (clip != null) {
                    clip.close();
                }
                LOGGER.log(System.Logger.Level.WARNING, "Could not load Space Invaders sound {0}: {1}", path, e.toString());
            }
        }
    }

    @Override
    public synchronized void play(Sample sample, boolean loop) {
        Clip clip = clips.get(sample);
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        }
    }

    @Override
    public synchronized void stop(Sample sample) {
        Clip clip = clips.get(sample);
        if (clip != null) {
            clip.stop();
        }
    }

    @Override
    public synchronized void stopAll() {
        clips.values().forEach(Clip::stop);
    }

    @Override
    public synchronized void close() {
        clips.values().forEach(clip -> {
            clip.stop();
            clip.close();
        });
        clips.clear();
    }
}
