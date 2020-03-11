/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.devices.ssem.display;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.PluginSettings;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "SSEM CRT display"
)
@SuppressWarnings("unused")
public class DisplaySSEM extends AbstractDevice {
    private DisplayDialog display;

    public DisplaySSEM(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        MemoryContext<Byte> memory = applicationApi.getContextPool().getMemoryContext(pluginID, ByteMemoryContext.class);

        boolean guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        if (!guiNotSupported) {
            display = new DisplayDialog(memory);
        }
    }

    @Override
    public void reset() {
        Optional.ofNullable(display).ifPresent(DisplayDialog::reset);
    }

    @Override
    public void destroy() {
        Optional.ofNullable(display).ifPresent(DisplayDialog::dispose);
    }

    @Override
    public void showGUI() {
        Optional.ofNullable(display).ifPresent(displayDialog -> displayDialog.setVisible(true));
    }

    @Override
    public void showSettings() {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.emustudio.plugins.devices.ssem.display.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2006-2020, Peter Jakubčo";
    }

    @Override
    public String getDescription() {
        return "CRT display for SSEM computer";
    }

    private interface ByteMemoryContext extends MemoryContext<Byte> {}
}
