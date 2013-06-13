/*
 * TerminalImpl.java
 *
 * Created on 28.7.2008, 19:12:19
 *
 * Copyright (C) 2008-2013 Peter Jakubčo
 * KISS, YAGNI, DRY
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
import emulib.runtime.InvalidContextException;
import emulib.runtime.LoggerFactory;
import emulib.runtime.StaticDialogs;
import emulib.runtime.interfaces.Logger;
import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.devices.adm3a.InputProvider;
import net.sf.emustudio.devices.adm3a.gui.ConfigDialog;
import net.sf.emustudio.devices.adm3a.gui.TerminalWindow;

@PluginType(type = PLUGIN_TYPE.DEVICE,
title = "LSI ADM-3A terminal",
copyright = "\u00A9 Copyright 2007-2013, Peter Jakubčo",
description = "Custom implementation of LSI ADM-3A terminal")
public class TerminalImpl extends AbstractDevice implements TerminalSettings.ChangedObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalImpl.class);

    private TerminalWindow terminalGUI;
    private Display display;
    private InputProvider keyboard;
    private DeviceContext<Short> connectedDevice;
    private TerminalSettings terminalSettings;

    public TerminalImpl(Long pluginID) {
        super(pluginID);
        terminalSettings = new TerminalSettings(pluginID);
        display = new Display(80, 25, terminalSettings);
        try {
            ContextPool.getInstance().register(pluginID, display, DeviceContext.class);
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Could not register ADM-3A terminal",
                    TerminalImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);
        terminalSettings.addChangedObserver(this);
        terminalSettings.setSettingsManager(settings);

        try {
            // try to connect to a serial I/O board
            DeviceContext device = ContextPool.getInstance().getDeviceContext(pluginID, DeviceContext.class);
            if (device != null) {
                if (device.getDataType() != Short.class) {
                    throw new InvalidContextException("Connected device is not supported");
                }
                connectedDevice = device;
            } else {
                LOGGER.warning("The terminal is not connected to any I/O device.");
            }
            terminalGUI = new TerminalWindow(display);
            terminalSettings.read();
            return true;
        } catch (InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not get serial I/O board Context", getTitle());
            return false;
        }
    }

    @Override
    public void showGUI() {
        if (!terminalSettings.isNoGUI()) {
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
    }

    @Override
    public void showSettings() {
        new ConfigDialog(terminalSettings, terminalGUI, display).setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
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
            LOGGER.warning("Keyboard is unconnected");
        }
    }

    private void createKeyboardFromFile() {
        destroyKeyboard();
        KeyboardFromFile tmp = new KeyboardFromFile();
        tmp.setInputFile(new File(terminalSettings.getInputFileName()));
        keyboard = tmp;
        connectKeyboard();
    }

    private void createKeyboard() {
        destroyKeyboard();
        Keyboard tmp = new Keyboard();
        tmp.addListenerRecursively(terminalGUI);
        keyboard = tmp;
        connectKeyboard();
    }

    @Override
    public void settingsChanged() {
        if (terminalSettings.isNoGUI() && !(keyboard instanceof KeyboardFromFile)) {
            createKeyboardFromFile();
        } else if (!(keyboard instanceof Keyboard)) {
            createKeyboard();
        }
        if (terminalSettings.isHalfDuplex()) {
            keyboard.addDeviceObserver(display);
        } else {
            keyboard.removeDeviceObserver(display);
        }
    }
}
