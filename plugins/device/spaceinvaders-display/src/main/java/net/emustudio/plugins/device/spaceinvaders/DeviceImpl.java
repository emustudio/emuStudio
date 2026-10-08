/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.audio.SamplePlayer;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Space Invaders display")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice implements SoundOutput {
    static final int SOUND_SAMPLE_RATE = 48_000;
    private static final System.Logger LOGGER = System.getLogger(DeviceImpl.class.getName());
    private final SamplePlayer<Sample> sound = new SamplePlayer<>(SOUND_SAMPLE_RATE);
    private final SpaceInvadersHardware hardware = new SpaceInvadersHardware(this);
    private final boolean guiSupported;
    private int scale;
    private boolean colorOverlay;
    private boolean soundEnabled;
    private Path soundSamplesDirectory;
    private Context8080 cpu;
    private int attachedPorts;
    private MemoryContext<Byte> memory;
    private FrameClock frameClock;
    private volatile DisplayWindow window;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        sound.setVolumePercent(25);
        readSettings();
    }

    private void readSettings() {
        scale = settings.getInt("scale", 2);
        colorOverlay = settings.getBoolean("colorOverlay", true);
        soundEnabled = settings.getBoolean("soundEnabled", guiSupported);
        soundSamplesDirectory = Path.of(settings.getString("soundSamplesDirectory", "examples/space-invaders/sounds"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        if (memory.getCellTypeClass() != Byte.class || memory.getSize() <= 0x3FFF) {
            throw new PluginInitializationException(this, "Space Invaders requires byte memory through address 3FFFh");
        }
        for (int port = SpaceInvadersHardware.INPUT_1_PORT; port <= SpaceInvadersHardware.SOUND_2_PORT; port++) {
            if (!cpu.attachDevice(port, hardware)) {
                for (int cleanup = SpaceInvadersHardware.INPUT_1_PORT; cleanup < port; cleanup++) {
                    cpu.detachDevice(cleanup);
                }
                attachedPorts = 0;
                throw new PluginInitializationException(this,
                        String.format("Space Invaders cannot attach to CPU port %02Xh", port));
            }
            attachedPorts++;
        }
        if (soundEnabled) {
            openSoundSamples();
        }
        frameClock = new FrameClock(cpu, this::frameReady);
        frameClock.start();
    }

    private void frameReady() {
        DisplayWindow current = window;
        if (current != null) {
            current.frameReady();
        }
    }

    private void openSoundSamples() {
        sound.close();
        if (!Files.isDirectory(soundSamplesDirectory)) {
            LOGGER.log(System.Logger.Level.WARNING, "Space Invaders sound samples not found at {0}",
                    soundSamplesDirectory);
            return;
        }
        for (Sample sample : Sample.values()) {
            Path path = soundSamplesDirectory.resolve(sample.ordinal() + ".wav");
            if (!Files.isRegularFile(path)) {
                path = soundSamplesDirectory.resolve(sample.ordinal() + ".WAV");
            }
            try {
                sound.load(sample, path);
            } catch (IOException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException e) {
                LOGGER.log(System.Logger.Level.WARNING, "Could not load Space Invaders sound {0}: {1}", path, e.toString());
            }
        }
    }

    @Override
    public void play(Sample sample, boolean loop) {
        sound.play(sample, loop);
    }

    @Override
    public void stop(Sample sample) {
        sound.stop(sample);
    }

    @Override
    public void stopAll() {
        sound.stopAll();
    }

    @Override
    public void reset() {
        hardware.reset();
    }

    @Override
    public void destroy() {
        if (frameClock != null) {
            frameClock.close();
            frameClock = null;
        }
        if (cpu != null) {
            for (int port = SpaceInvadersHardware.INPUT_1_PORT; port < SpaceInvadersHardware.INPUT_1_PORT + attachedPorts; port++) {
                cpu.detachDevice(port);
            }
            attachedPorts = 0;
        }
        sound.close();
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (window == null) {
                window = new DisplayWindow(parent, memory, hardware, scale, colorOverlay, sound,
                        applicationApi.getGUI(), applicationApi.getDialogs());
            }
            window.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public void showSettings(JFrame parent) {
        if (guiSupported) {
            new SettingsDialog(parent, settings, applicationApi.getDialogs(), applicationApi.getGUI(),
                    this::applySettings).setVisible(true);
        }
    }

    private void applySettings() {
        boolean previousSoundEnabled = soundEnabled;
        Path previousDirectory = soundSamplesDirectory;
        readSettings();
        if (frameClock != null && (soundEnabled != previousSoundEnabled
                || !soundSamplesDirectory.equals(previousDirectory))) {
            if (soundEnabled) {
                openSoundSamples();
            } else {
                sound.close();
            }
        }
        if (window != null) {
            window.applySettings(scale, colorOverlay);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Space Invaders framebuffer, controls, shift register, sampled sound, and raster interrupts.";
    }
}
