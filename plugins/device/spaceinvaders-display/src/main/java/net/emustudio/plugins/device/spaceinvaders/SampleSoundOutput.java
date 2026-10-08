/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Plays independent effects without waiting for them on the CPU thread. */
final class SampleSoundOutput implements SoundOutput, AutoCloseable {
    static final int RECORDING_SAMPLE_RATE = 48_000;
    private static final AudioFormat SAMPLE_FORMAT = new AudioFormat(RECORDING_SAMPLE_RATE, 16, 1, true, false);
    private static final System.Logger LOGGER = System.getLogger(SampleSoundOutput.class.getName());
    private final Map<Sample, Clip> clips = new EnumMap<>(Sample.class);
    private final Map<Sample, short[]> samples = new EnumMap<>(Sample.class);
    private final EnumSet<Sample> looping = EnumSet.noneOf(Sample.class);
    private int volumePercent = 25;

    SampleSoundOutput() { }

    SampleSoundOutput(Map<Sample, Clip> clips) {
        this(clips, Map.of());
    }

    SampleSoundOutput(Map<Sample, Clip> clips, Map<Sample, short[]> samples) {
        this.clips.putAll(clips);
        this.samples.putAll(samples);
        this.clips.values().forEach(this::applyVolume);
    }

    synchronized int getVolumePercent() {
        return volumePercent;
    }

    synchronized void setVolumePercent(int volumePercent) {
        this.volumePercent = Math.max(0, Math.min(100, volumePercent));
        clips.values().forEach(this::applyVolume);
    }

    private void applyVolume(Clip clip) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = volumePercent == 0 ? gain.getMinimum() : (float) (20 * Math.log10(volumePercent / 100.0));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        }
        if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
            BooleanControl mute = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
            mute.setValue(volumePercent == 0);
        }
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
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(path.toFile());
                 AudioInputStream pcm = AudioSystem.getAudioInputStream(SAMPLE_FORMAT, stream)) {
                byte[] data = pcm.readAllBytes();
                clip = AudioSystem.getClip();
                clip.open(SAMPLE_FORMAT, data, 0, data.length);
                applyVolume(clip);
                clips.put(sample, clip);
                short[] sampleData = new short[data.length / Short.BYTES];
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(sampleData);
                samples.put(sample, sampleData);
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
                looping.add(sample);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                looping.remove(sample);
                clip.start();
            }
        }
    }

    @Override
    public synchronized void stop(Sample sample) {
        looping.remove(sample);
        Clip clip = clips.get(sample);
        if (clip != null) {
            clip.stop();
        }
    }

    @Override
    public synchronized void stopAll() {
        looping.clear();
        clips.values().forEach(Clip::stop);
    }

    /** Mixes the current effects into stereo PCM for one recorded video frame. */
    synchronized byte[] captureAudio(int frameCount) {
        int[] mixed = new int[frameCount];
        for (Map.Entry<Sample, Clip> entry : clips.entrySet()) {
            short[] data = samples.get(entry.getKey());
            Clip clip = entry.getValue();
            if (data == null || data.length == 0 || !clip.isRunning()) {
                continue;
            }
            long position = clip.getLongFramePosition();
            boolean loop = looping.contains(entry.getKey());
            for (int frame = 0; frame < frameCount; frame++) {
                long index = position + frame;
                if (loop || index < data.length) {
                    mixed[frame] += data[(int) (index % data.length)];
                }
            }
        }
        ByteBuffer pcm = ByteBuffer.allocate(frameCount * 2 * Short.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int value : mixed) {
            short sample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * volumePercent / 100));
            pcm.putShort(sample).putShort(sample);
        }
        return pcm.array();
    }

    @Override
    public synchronized void close() {
        clips.values().forEach(clip -> {
            clip.stop();
            clip.close();
        });
        clips.clear();
        samples.clear();
        looping.clear();
    }
}
