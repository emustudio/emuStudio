/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram;

import net.emustudio.emulib.plugins.AbstractPlugin;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
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
                gui = new MemoryGui(parent, context, applicationApi.getDialogs(), applicationApi.getGUI());
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
