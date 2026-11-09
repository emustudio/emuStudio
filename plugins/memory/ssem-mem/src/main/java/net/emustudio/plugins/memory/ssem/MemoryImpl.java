/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.memory.ssem.gui.MemoryGui;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "SSEM memory (Williams–Kilburn Tube)",
        description = "Main store for SSEM machine"
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractPlugin implements Memory {
    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui memoryGUI;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.context = new MemoryContextImpl();
        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        applicationApi.getContextPool().register(pluginID, context, MemoryContext.class);
    }

    @Override
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (memoryGUI == null) {
                memoryGUI = new MemoryGui(parent, context, applicationApi, applicationApi.getGUI());
            }
            memoryGUI.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
