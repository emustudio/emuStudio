/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.ssem.display;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "SSEM CRT display"
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractDevice {
    private final DisplayPanel displayPanel = new DisplayPanel();
    private final boolean guiSupported;
    private MemoryContext<Byte> memory;
    private DisplayGui display;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        if (memory.getDataType() != Byte.class) {
            throw new PluginInitializationException(
                    "Unexpected memory cell type. Expected Byte but was: " + memory.getDataType()
            );
        }
    }

    @Override
    public void reset() {
        displayPanel.reset(memory);
    }

    @Override
    public void destroy() {
        Optional.ofNullable(display).ifPresent(DisplayGui::dispose);
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (display == null) {
                display = new DisplayGui(parent, memory, displayPanel);
            }
            display.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public void showSettings(JFrame parent) {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
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
        return "CRT display for SSEM computer";
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.ssem.display.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
