/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ssem.display;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import javax.swing.*;
import java.util.Optional;

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
        Class<?> cellTypeClass = memory.getCellTypeClass();
        if (cellTypeClass != Byte.class) {
            throw new PluginInitializationException(
                    "Unexpected memory cell type. Expected Byte but was: " + cellTypeClass
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
    public void showSettings(JFrame parent) {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getDescription() {
        return "CRT display for SSEM computer";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

}
