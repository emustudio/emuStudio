/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.AbstractMemory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import net.emustudio.plugins.memory.rasp.gui.MemoryGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Optional;

@SuppressWarnings("unused")
@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "RASP Memory"
)
public class MemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui gui;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.context = new MemoryContextImpl(getAnnotations());

        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        Optional.ofNullable(applicationApi.getContextPool()).ifPresent(pool -> {
            try {
                pool.register(pluginID, context, RaspMemoryContext.class);
                pool.register(pluginID, context, MemoryContext.class);
            } catch (InvalidContextException | ContextAlreadyRegisteredException ex) {
                LOGGER.error("Could not register RASP memory context", ex);
                applicationApi.getDialogs().showError(
                        "Could not register RASP memory context. Please see log file for details.", super.getTitle()
                );
            }
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

    @Override
    public String getDescription() {
        return "RASP memory containing the program as well as the data";
    }

    @Override
    public int getSize() {
        return context.getSize();
    }
}
