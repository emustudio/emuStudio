/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.adm3a.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.ContextNotFoundException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.devices.adm3a.InputProvider;
import net.sf.emustudio.devices.adm3a.gui.ConfigDialog;
import net.sf.emustudio.devices.adm3a.gui.TerminalWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.DEVICE,
        title = "LSI ADM-3A terminal",
        copyright = "\u00A9 Copyright 2006-2020, Peter Jakubčo",
        description = "Custom implementation of LSI ADM-3A terminal"
)
@SuppressWarnings("unused")
public class TerminalImpl extends AbstractDevice implements TerminalSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalImpl.class);
    private static final int COLUMNS_COUNT = 80;
    private static final int ROWS_COUNT = 24;

    private final ContextPool contextPool;
    private final Display display;
    private final Cursor cursor;
    private final LoadCursorPosition loadCursorPosition;
    private final TerminalSettings terminalSettings;

    private TerminalWindow terminalGUI;
    private InputProvider keyboard;
    private DeviceContext<Short> connectedDevice;

    public TerminalImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        terminalSettings = new TerminalSettings(pluginID);
        cursor = new Cursor(COLUMNS_COUNT, ROWS_COUNT);
        loadCursorPosition = new LoadCursorPosition(cursor);
        display = new Display(cursor, loadCursorPosition, terminalSettings);
        try {
            contextPool.register(pluginID, display, DeviceContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register ADM-3A terminal", getTitle());
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        terminalSettings.addChangedObserver(this);
        terminalSettings.setSettingsManager(settings);

        // try to connect to a serial I/O board
        try {
            DeviceContext device = contextPool.getDeviceContext(pluginID, DeviceContext.class);
            if (device.getDataType() != Short.class) {
                throw new PluginInitializationException(this, "Connected device is not supported");
            }
            connectedDevice = device;
        } catch (ContextNotFoundException e) {
            LOGGER.warn("The terminal is not connected to any I/O device.");
        }
        terminalGUI = new TerminalWindow(display);
        display.start();
        try {
            terminalSettings.read();
        } catch (IOException e) {
            throw new PluginInitializationException(this, e);
        }
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
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.devices.adm3a.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
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
        if (!terminalSettings.isNoGUI()) {
          new ConfigDialog(terminalSettings, terminalGUI, display).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !terminalSettings.isNoGUI();
    }

    private boolean isGUIAllowed() {
        return !terminalSettings.isNoGUI();
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
}
