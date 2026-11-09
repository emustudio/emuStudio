/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import net.emustudio.plugins.memory.rasp.gui.MemoryGui;

import javax.swing.*;
import java.util.Optional;

@SuppressWarnings("unused")
@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "RASP Memory",
        description = "RASP memory containing the program as well as the data"
)
public class MemoryImpl extends AbstractPlugin implements Memory {
    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui gui;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.context = new MemoryContextImpl();

        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            pool.register(pluginID, context, RaspMemoryContext.class);
            pool.register(pluginID, context, MemoryContext.class);
        });
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
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (gui == null) {
                gui = new MemoryGui(parent, context, applicationApi, applicationApi.getGUI());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
