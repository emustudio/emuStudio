/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.gui.MemoryGui;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "RAM Program Tape",
        description = "Read-only program tape for abstract RAM machine."
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractPlugin implements Memory {
    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui gui;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        ContextPool contextPool = applicationApi.getContextPool();

        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        context = new MemoryContextImpl();
        contextPool.register(pluginID, context, RamMemoryContext.class);
        contextPool.register(pluginID, context, MemoryContext.class);
    }

    @Override
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (gui == null) {
                gui = new MemoryGui(parent, context, applicationApi.getDialogs(), applicationApi.getGUI(), applicationApi.getExecutionState());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public void destroy() {
        context.destroy();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
