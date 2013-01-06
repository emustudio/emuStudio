/*
 * TerminalImpl.java
 *
 * Created on 28.7.2008, 19:12:19
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
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
import emulib.runtime.StaticDialogs;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.devices.adm3a.gui.ConfigDialog;
import net.sf.emustudio.devices.adm3a.gui.TerminalWindow;

@PluginType(type = PLUGIN_TYPE.DEVICE,
title = "LSI ADM-3A terminal",
copyright = "\u00A9 Copyright 2007-2012, Peter Jakubčo",
description = "Custom implementation of terminal ADM-3A from LSI")
public class TerminalImpl extends AbstractDevice {

    private TerminalWindow terminalGUI;
    private TerminalDisplay terminal; // male
    private TerminalFemale female;

    public TerminalImpl(Long pluginID) {
        super(pluginID);
        terminal = new TerminalDisplay(80, 25);
        try {
            ContextPool.getInstance().register(pluginID, terminal, DeviceContext.class);
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Could not register ADM-3A terminal",
                    TerminalImpl.class.getAnnotation(PluginType.class).title());
        }
        female = new TerminalFemale();
    }

    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);

        try {
            // try to connect to a serial I/O board
            DeviceContext device = ContextPool.getInstance().getDeviceContext(pluginID,
                    DeviceContext.class);

            if (device != null) {
                female.attachDevice(device);
            }

            terminalGUI = new TerminalWindow(terminal, female);
            readSettings();
            return true;
        } catch (InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not get serial I/O board Context",
                    TerminalImpl.class.getAnnotation(PluginType.class).title());
            return false;
        }
    }

    private void readSettings() {
        String s;

        s = settings.readSetting(pluginID, "verbose");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            terminal.setVerbose(true);
            terminalGUI.setVisible(true);
        } else {
            terminal.setVerbose(false);
        }

        s = settings.readSetting(pluginID, "duplex_mode");
        if (s != null && s.toUpperCase().equals("HALF")) {
            terminalGUI.setHalfDuplex(true);
        } else {
            terminalGUI.setHalfDuplex(false);
        }

        s = settings.readSetting(pluginID, "always_on_top");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            terminalGUI.setAlwaysOnTop(true);
        } else {
            terminalGUI.setAlwaysOnTop(false);
        }

        s = settings.readSetting(pluginID, "anti_aliasing");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            terminal.setAntiAliasing(true);
        } else {
            terminal.setAntiAliasing(false);
        }
    }

    @Override
    public void showGUI() {
        terminalGUI.setVisible(true);
    }

    @Override
    public void reset() {
        terminal.clearScreen();
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
        if (terminalGUI != null) {
            terminalGUI.destroyMe();
        }
    }

    @Override
    public void showSettings() {
        new ConfigDialog(settings, pluginID, terminalGUI,
                terminal).setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
