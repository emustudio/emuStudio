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
package net.emustudio.plugins.devices.abstracttape;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.devices.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.devices.abstracttape.gui.SettingsDialog;
import net.emustudio.plugins.devices.abstracttape.gui.TapeDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "Abstract tape"
)
@SuppressWarnings("unused")
public class AbstractTape extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTape.class);

    private final AbstractTapeContextImpl context;

    private String guiTitle;
    private TapeDialog gui;
    private boolean guiNotSupported;
    private boolean automaticEmulation;

    public AbstractTape(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.context = new AbstractTapeContextImpl(this);
        try {
            applicationApi.getContextPool().register(pluginID, context, AbstractTapeContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register Abstract tape context", e);
            applicationApi.getDialogs().showError(
                "Could not register abstract tape context. Please see log file for details.", super.getTitle()
            );
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.emustudio.plugins.devices.abstracttape.version");
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
        return "Abstract tape is used by abstract machines such as RAM machine";
    }

    @Override
    public void initialize() {
        automaticEmulation = settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO).orElse(false);
        guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);

        boolean showAtStartup = settings.getBoolean("showAtStartup").orElse(false);
        if (showAtStartup) {
            showGUI();
        }
        context.setVerbose(automaticEmulation);
    }

    @Override
    public void showGUI() {
        boolean alwaysOnTop = settings.getBoolean("alwaysOnTop").orElse(false);
        if (!guiNotSupported) {
            if (gui == null) {
                gui = new TapeDialog(getTitle(), context, alwaysOnTop, applicationApi.getDialogs());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public String getTitle() {
        return (guiTitle == null) ? super.getTitle() : guiTitle;
    }


    public void setGUITitle(String title) {
        this.guiTitle = Objects.requireNonNull(title);
        if (gui != null) {
            gui.setTitle(title);
            context.setVerbose(automaticEmulation);
        }
    }

    @Override
    public void destroy() {
        if (gui != null) {
            gui.dispose();
        }
        gui = null;
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public void showSettings() {
        if (!guiNotSupported) {
            new SettingsDialog(settings, applicationApi.getDialogs(), gui).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !guiNotSupported;
    }
}
