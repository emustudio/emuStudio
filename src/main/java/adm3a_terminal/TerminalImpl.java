/*
 * TerminalImpl.java
 *
 * Created on 28.7.2008, 19:12:19
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package adm3a_terminal;

import emulib.plugins.ISettingsHandler;
import emulib.plugins.device.IDeviceContext;
import adm3a_terminal.gui.ConfigDialog;
import adm3a_terminal.gui.TerminalWindow;
import emulib.plugins.device.SimpleDevice;
import emulib.runtime.Context;
import emulib.runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class TerminalImpl extends SimpleDevice {

    private TerminalWindow terminalGUI;
    private TerminalDisplay terminal; // male
    private TerminalFemale female;

    public TerminalImpl(Long pluginID) {
        super(pluginID);
        terminal = new TerminalDisplay(80, 25);
        if (!Context.getInstance().register(pluginID, terminal,
                IDeviceContext.class))
            StaticDialogs.showErrorMessage("Could not register the terminal!");

        female = new TerminalFemale();
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        // try to connect to a serial I/O board
        IDeviceContext device = Context.getInstance().getDeviceContext(pluginID,
                IDeviceContext.class, "RS232");

        if (device != null)
            female.attachDevice(device);

        terminalGUI = new TerminalWindow(terminal, female);
        readSettings();
        return true;
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
        terminal.clear_screen();
    }

    @Override
    public String getTitle() {
        return "Terminal LSI ADM-3A";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2007-2012, Peter Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Implementation of virtual terminal LSI ADM-3A";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
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
