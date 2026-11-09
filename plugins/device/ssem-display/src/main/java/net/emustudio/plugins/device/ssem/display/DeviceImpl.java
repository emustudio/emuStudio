/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ssem.display;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;

import javax.swing.*;
import java.util.Optional;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "SSEM CRT display",
        description = "CRT display for SSEM computer"
)
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractPlugin implements Device {
    private final DisplayPanel displayPanel = new DisplayPanel();
    private final boolean guiSupported;
    private MemoryContext<Byte> memory;
    private DisplayGui display;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemory(pluginID, Byte.class);
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
                display = new DisplayGui(parent, memory, displayPanel, applicationApi.getGUI());
            }
            display.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

}
