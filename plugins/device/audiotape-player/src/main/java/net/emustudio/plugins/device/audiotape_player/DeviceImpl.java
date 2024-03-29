/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;

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
        try {
            CPUContext cpu = contextPool.getCPUContext(pluginID);
            DeviceContext<Byte> lineIn = contextPool.getDeviceContext(pluginID, DeviceContext.class);
            if (lineIn.getDataType() != Byte.class) {
                throw new PluginInitializationException("Could not find line-in device");
            }
            this.cassetteListener = new TapePlaybackImpl(lineIn);
            cpu.addPassedCyclesListener(this.cassetteListener);
        } catch (PluginInitializationException ignored) {
            ZxSpectrumBus bus = contextPool.getDeviceContext(pluginID, ZxSpectrumBus.class);
            this.cassetteListener = new TapePlaybackImpl(bus);
            bus.addPassedCyclesListener(this.cassetteListener);
        }
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
                this.gui = new TapePlayerGui(parent, applicationApi.getDialogs(), controller);
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
