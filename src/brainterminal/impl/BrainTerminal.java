/**
 * BrainTerminal.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package brainterminal.impl;

import brainterminal.gui.BrainTerminalDialog;
import plugins.ISettingsHandler;
import plugins.device.IDeviceContext;
import plugins.device.SimpleDevice;
import runtime.Context;
import runtime.StaticDialogs;
import braincpu.interfaces.C35E1D94FC14C94F76C904F09494B85079660C9BF;

public class BrainTerminal extends SimpleDevice {

    private C35E1D94FC14C94F76C904F09494B85079660C9BF cpu;
    private BrainTerminalContext terminal;
    private BrainTerminalDialog gui;

    public BrainTerminal(Long pluginID) {
        super(pluginID);
        gui = new BrainTerminalDialog();
        terminal = new BrainTerminalContext(gui);
        Context.getInstance().register(pluginID, terminal, IDeviceContext.class);
    }

    @Override
    public String getTitle() {
        return "BrainDuck terminal";
    }

    @Override
    public String getVersion() {
        return "0.11-rc2";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2010, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Simple terminal device for BrainDuck architecture";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        cpu = (C35E1D94FC14C94F76C904F09494B85079660C9BF)Context.getInstance()
                .getCPUContext(pluginID,
                C35E1D94FC14C94F76C904F09494B85079660C9BF.class);
        if (cpu == null) {
            StaticDialogs.showErrorMessage("BrainTerminal needs to be connected"
                    + "to the BrainCPU.");
            return false;
        }
        cpu.attachDevice(terminal);

        // read settings
        String s = settings.readSetting(pluginID, "verbose");
        if (s.toUpperCase().equals("TRUE")) {
            gui.setVerbose(true);
            gui.setVisible(true);
        } else {
            gui.setVerbose(false);
        }
        return true;
    }

    @Override
    public void reset() {
        // zmažeme obrazovku
        gui.clearScreen();
    }

    @Override
    public void destroy() {
        gui.dispose();
        gui = null;
    }
   

    @Override
    public void showGUI() {
        gui.setVisible(true);
    }

    @Override
    public void showSettings() {
        // nemáme GUI s nastaveniami
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
