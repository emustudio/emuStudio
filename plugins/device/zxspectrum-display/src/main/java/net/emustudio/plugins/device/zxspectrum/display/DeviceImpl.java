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
package net.emustudio.plugins.device.zxspectrum.display;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.zxspectrum.display.io.Keyboard;
import net.emustudio.plugins.device.zxspectrum.display.io.OutputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "ZX Spectrum Display")
public class DeviceImpl extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);

    private final boolean guiSupported;
    private final ZxSpectrumDisplayContext terminal = new ZxSpectrumDisplayContext();
    private final Keyboard keyboard = new Keyboard();
    private boolean guiIOset = false;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        try {
            applicationApi.getContextPool().register(pluginID, terminal, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register ZX Spectrum display context", e);
            applicationApi.getDialogs().showError("Could not register BrainDuck terminal. Please see log file for more details.", getTitle());
        }
    }

    @Override
    public void initialize() throws PluginInitializationException {
        // try to connect to a serial I/O board
        try {
            DeviceContext<Byte> device = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            if (device.getDataType() != Byte.class) {
                throw new PluginInitializationException(
                        "Unexpected device data type. Expected Byte but was: " + device.getDataType()
                );
            }
            keyboard.connect(device);
        } catch (ContextNotFoundException e) {
            LOGGER.warn("The terminal is not connected to any I/O device.");
        }
    }

    @Override
    public void reset() {
        terminal.reset();
    }

    @Override
    public void destroy() {
        try {
            terminal.close();
        } catch (IOException e) {
            LOGGER.error("Could not close io provider", e);
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
                LOGGER.debug("Creating GUI-based keyboard");

                OutputProvider outputProvider = ZxSpectrumDisplayGui.create(parent, keyboard, applicationApi.getDialogs());
                terminal.setOutputProvider(outputProvider);
                guiIOset = true;
            }

            terminal.showGUI();
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
        return "ZX Spectrum display draft";
    }


    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.zxspectrum.display.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
