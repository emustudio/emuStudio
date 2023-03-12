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
package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.adm3a.api.ContextAdm3A;
import net.emustudio.plugins.device.adm3a.api.Display;
import net.emustudio.plugins.device.adm3a.api.Keyboard;
import net.emustudio.plugins.device.adm3a.gui.SettingsDialog;
import net.emustudio.plugins.device.adm3a.gui.TerminalWindow;
import net.emustudio.plugins.device.adm3a.interaction.Cursor;
import net.emustudio.plugins.device.adm3a.interaction.DisplayImpl;
import net.emustudio.plugins.device.adm3a.interaction.KeyboardFromFile;
import net.emustudio.plugins.device.adm3a.interaction.KeyboardGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import static net.emustudio.plugins.device.adm3a.gui.DisplayFont.fromTerminalFont;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "LSI ADM-3A terminal"
)
public class DeviceImpl extends AbstractDevice implements TerminalSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);
    public static final int DEFAULT_COLUMNS = 80;
    public static final int DEFAULT_ROWS = 24;

    private final TerminalSettings terminalSettings;
    private final ContextAdm3A context;
    private final Keyboard keyboard;
    private final Display display;
    private boolean guiIOset = false;
    private TerminalWindow terminalGUI;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.terminalSettings = new TerminalSettings(settings, applicationApi.getDialogs());

        Cursor cursor = new Cursor(DEFAULT_COLUMNS, DEFAULT_ROWS);
        this.display = new DisplayImpl(cursor, terminalSettings);

        if (terminalSettings.isGuiSupported()) {
            LOGGER.debug("Creating GUI-based keyboard");
            this.keyboard = new KeyboardGui(cursor);
        } else {
            LOGGER.debug("Creating file-based keyboard ({})", terminalSettings.getInputPath());
            this.keyboard = new KeyboardFromFile(terminalSettings);
        }
        this.context = new ContextAdm3A(terminalSettings::isHalfDuplex);
        this.keyboard.addOnKeyHandler(context::onKeyFromKeyboard);

        try {
            applicationApi.getContextPool().register(pluginID, context, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register ADM-3A terminal", e);
            applicationApi.getDialogs().showError(
                    "Could not register ADM-3A terminal. Please see log file for more details", getTitle()
            );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        try {
            // get serial I/O board
            DeviceContext<Byte> device = applicationApi.getContextPool().getDeviceContext(
                    pluginID, DeviceContext.class, terminalSettings.getDeviceIndex()
            );
            if (device.getDataType() != Byte.class) {
                throw new PluginInitializationException(
                        "Unexpected device data type. Expected Byte but was: " + device.getDataType()
                );
            }
            context.setExternalDevice(device);
            context.setDisplay(display);
        } catch (ContextNotFoundException e) {
            LOGGER.warn("The terminal is not connected to any I/O device.", e);
        }

        keyboard.process();
        terminalSettings.addChangedObserver(this);
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiIOset) {
            terminalGUI.setVisible(true);
        } else if (terminalSettings.isGuiSupported()) {
            terminalGUI = new TerminalWindow(parent, display, fromTerminalFont(terminalSettings.getFont()));
            terminalGUI.setAlwaysOnTop(terminalSettings.isAlwaysOnTop());
            GuiUtils.addKeyListener(terminalGUI, (KeyboardGui) keyboard);
            terminalGUI.startPainting();
            guiIOset = true;
            terminalGUI.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return terminalSettings.isGuiSupported();
    }

    @Override
    public void reset() {
        if (terminalGUI != null) {
            terminalGUI.clearScreen();
        }
        keyboard.process();
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
        return "Custom implementation of LSI ADM-3A terminal";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public void destroy() {
        terminalSettings.removeChangedObserver(this);
        if (terminalGUI != null) {
            terminalGUI.destroy();
        }
        keyboard.close();
        try {
            display.close();
        } catch (Exception e) {
            LOGGER.error("Could not close ADM-3A display", e);
        }
    }

    @Override
    public void showSettings(JFrame parent) {
        if (isShowSettingsSupported()) {
            new SettingsDialog(parent, terminalSettings, terminalGUI, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return terminalSettings.isGuiSupported();
    }

    @Override
    public void settingsChanged() {
        if (terminalGUI != null) {
            terminalGUI.setAlwaysOnTop(terminalSettings.isAlwaysOnTop());
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.adm3a.version"));
        } catch (MissingResourceException e) {
            LOGGER.error("Could not find resource bundle", e);
            return Optional.empty();
        }
    }
}
