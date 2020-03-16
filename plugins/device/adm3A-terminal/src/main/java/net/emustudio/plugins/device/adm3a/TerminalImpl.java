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
package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.*;
import net.emustudio.plugins.device.adm3a.gui.ConfigDialog;
import net.emustudio.plugins.device.adm3a.gui.TerminalWindow;
import net.emustudio.plugins.device.adm3a.interaction.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
    type = PLUGIN_TYPE.DEVICE,
    title = "LSI ADM-3A terminal"
)
@SuppressWarnings("unused")
public class TerminalImpl extends AbstractDevice implements TerminalSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalImpl.class);
    private static final int COLUMNS_COUNT = 80;
    private static final int ROWS_COUNT = 24;

    private final Display display;
    private final Cursor cursor = new Cursor(COLUMNS_COUNT, ROWS_COUNT);
    private final LoadCursorPosition loadCursorPosition = new LoadCursorPosition(cursor);
    private final TerminalSettings terminalSettings;

    private TerminalWindow terminalGUI;
    private InputProvider keyboard;
    private DeviceContext<Short> connectedDevice;

    public TerminalImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        terminalSettings = new TerminalSettings(settings, applicationApi.getDialogs());
        display = new Display(cursor, loadCursorPosition, terminalSettings);

        try {
            applicationApi.getContextPool().register(pluginID, display, DeviceContext.class);
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
        terminalSettings.addChangedObserver(this);

        // try to connect to a serial I/O board
        try {
            this.connectedDevice = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            if (connectedDevice.getDataType() != Short.class) {
                throw new PluginInitializationException(
""
                );
            }
        } catch (ContextNotFoundException e) {
            LOGGER.warn("The terminal is not connected to any I/O device.");
        }
        terminalGUI = new TerminalWindow(display);
        display.start();
    }

    @Override
    public void showGUI() {
        if (isGUIAllowed()) {
            terminalGUI.setVisible(true);
        }
    }

    @Override
    public void reset() {
        display.clearScreen();
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
    public void destroy() {
        terminalSettings.removeChangedObserver(this);
        if (terminalGUI != null) {
            terminalGUI.destroy();
        }
        display.destroy();
        cursor.destroy();
    }

    @Override
    public void showSettings() {
        if (terminalSettings.isGuiSupported()) {
            new ConfigDialog(terminalSettings, terminalGUI, display, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return terminalSettings.isGuiSupported();
    }

    private boolean isGUIAllowed() {
        return terminalSettings.isGuiSupported();
    }

    private void destroyKeyboard() {
        if (keyboard != null) {
            keyboard.destroy();
        }
    }

    private void connectKeyboard() {
        if (connectedDevice != null) {
            keyboard.addDeviceObserver(connectedDevice);
        } else {
            LOGGER.warn("Keyboard is unconnected");
        }
    }

    private void createKeyboardFromFile() throws FileNotFoundException {
        destroyKeyboard();
        KeyboardFromFile tmp = new KeyboardFromFile(new File(terminalSettings.getInputFileName()));
        keyboard = tmp;
        connectKeyboard();
        tmp.processInputFile(terminalSettings.getInputReadDelay());
    }

    private void createKeyboard() {
        destroyKeyboard();
        Keyboard tmp = new Keyboard(loadCursorPosition);
        tmp.addListenerRecursively(terminalGUI);
        keyboard = tmp;
        connectKeyboard();
    }

    @Override
    public void settingsChanged() throws FileNotFoundException {
        if (isGUIAllowed() && !(keyboard instanceof Keyboard)) {
            createKeyboard();
        } else if (!(keyboard instanceof KeyboardFromFile)) {
            createKeyboardFromFile();
        }
        if (terminalSettings.isHalfDuplex()) {
            keyboard.addDeviceObserver(display);
        } else {
            keyboard.removeDeviceObserver(display);
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.devices.adm3a.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
