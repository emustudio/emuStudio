/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.ay38910.audio.SoundAudioSink;
import net.emustudio.plugins.device.ay38910.gui.Ay38910Gui;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JFrame;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import static net.emustudio.plugins.device.ay38910.Ay38910Chip.DEFAULT_SAMPLE_RATE;

@SuppressWarnings("unused")
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Audio AY-3-8910 Chip")
public class DeviceImpl extends AbstractDevice {
    private final boolean guiSupported;
    private Ay38910Chip chip;
    private Ay38910Gui gui;
    private Context8080 cpu;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ContextPool contextPool = applicationApi.getContextPool();

        try {
            this.cpu = contextPool.getCPUContext(pluginID, Context8080.class); // For the sake of generalization we allow to connect this chip also to 8080 CPU
            this.chip = new Ay38910Chip(new SoundAudioSink(DEFAULT_SAMPLE_RATE), DEFAULT_SAMPLE_RATE, cpu::getCPUFrequency);
            if (!cpu.attachDevice(Ay38910Chip.DATA_PORT & 0xFF, chip)) {
                throw new PluginInitializationException(
                        this, "AY-3-8910 cannot be attached to CPU port 0xFD (hardware conflict?)"
                );
            }
            cpu.addPassedCyclesListener(chip);
        } catch (InvalidContextException e) {
            throw new PluginInitializationException(this, "Could not access CPU context", e);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            throw new PluginInitializationException(this, "AY-3-8910 tone output is unavailable", e);
        }
    }

    @Override
    public void reset() {
        if (chip != null) {
            chip.reset();
        }
    }

    @Override
    public void destroy() {
        cpu.removePassedCyclesListener(chip);
        cpu.detachDevice(Ay38910Chip.DATA_PORT & 0xFF);

        if (gui != null) {
            gui.destroy();
            gui = null;
        }
        if (chip != null) {
            chip.close();
        }
    }

    @Override
    public void showSettings(JFrame parent) {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new Ay38910Gui(parent, chip, applicationApi.getGUI());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(bundle -> bundle.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(bundle -> bundle.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "AY-3-8910 programmable sound generator for ZX Spectrum style bus wiring.";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.ay38910.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
