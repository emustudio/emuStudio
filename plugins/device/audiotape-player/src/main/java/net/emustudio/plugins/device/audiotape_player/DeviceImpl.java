/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.audiotape_player.gui.TapePlayerGui;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Audio Tape Player")
public class DeviceImpl extends AbstractDevice {

    private final boolean guiSupported;
    private boolean guiIOset = false;

    private TapePlayerGui gui;
    private TapePlaybackController controller;
    private TapePlaybackImpl cassetteListener;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        ContextPool contextPool = applicationApi.getContextPool();

        // a cassette player needs a device to which it will write at its own pace
        CPUContext cpu = contextPool.getCPUContext(pluginID);
        DeviceContext<Byte> lineIn = contextPool.getDeviceContext(pluginID, DeviceContext.class);
        if (lineIn.getDataType() != Byte.class) {
            throw new PluginInitializationException("Could not find line-in device");
        }
        this.cassetteListener = new TapePlaybackImpl(lineIn, cpu::getCPUFrequency);
        cpu.addPassedCyclesListener(this.cassetteListener);
        this.controller = new TapePlaybackController(cassetteListener);
    }

    @Override
    public void reset() {
        this.controller.reset();
    }

    @Override
    public void destroy() {
        this.controller.close();
        if (guiIOset || gui != null) {
            gui = null;
            cassetteListener.setGui(null);
            guiIOset = false;
        }
    }

    @Override
    public void showSettings(JFrame jFrame) {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (!guiIOset) {
                this.gui = new TapePlayerGui(parent, applicationApi.getDialogs(), controller, applicationApi.getGUI());
                guiIOset = true;
                this.cassetteListener.setGui(gui);
            }
            this.gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }


    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Audio Tape Player";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.audiotape_player.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
