/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.abstracttape;

import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.gui.SettingsDialog;
import net.emustudio.plugins.device.abstracttape.gui.TapeGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "Abstract tape"
)
@SuppressWarnings("unused")
public class AbstractTape extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTape.class);

    private final AbstractTapeContextImpl context;
    private final boolean guiSupported;
    private final boolean automaticEmulation;

    private String guiTitle = super.getTitle();
    private TapeGui gui;

    public AbstractTape(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        this.automaticEmulation = settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO, false);
        this.context = new AbstractTapeContextImpl(this::setGUITitle);

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
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "The device represents a tape used by abstract machines";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public void initialize() {
        context.setLogSymbols(automaticEmulation);

        boolean showAtStartup = settings.getBoolean("showAtStartup", false);
        if (showAtStartup) {
            showGUI(null);
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                boolean alwaysOnTop = settings.getBoolean("alwaysOnTop", false);
                gui = new TapeGui(parent, getTitle(), context, alwaysOnTop, applicationApi.getDialogs());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public String getTitle() {
        return (guiTitle == null) ? super.getTitle() : guiTitle;
    }


    public void setGUITitle(String title) {
        this.guiTitle = Objects.requireNonNull(title);
        if (gui != null) {
            gui.setTitle(title);
            context.setLogSymbols(false);
            context.setLogSymbols(automaticEmulation);
        }
    }

    @Override
    public void destroy() {
        context.setLogSymbols(false);
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public void showSettings(JFrame parent) {
        if (guiSupported) {
            new SettingsDialog(parent, settings, applicationApi.getDialogs(), gui, guiTitle).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.abstracttape.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
