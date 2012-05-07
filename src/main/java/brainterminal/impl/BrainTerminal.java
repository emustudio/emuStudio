/**
 * BrainTerminal.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
import emulib.plugins.ISettingsHandler;
import emulib.plugins.device.IDeviceContext;
import emulib.plugins.device.SimpleDevice;
import emulib.runtime.Context;
import emulib.runtime.StaticDialogs;
import interfaces.C7DC7DAD9D43BACD78DD57E84262789E50BB7D7D8;

public class BrainTerminal extends SimpleDevice {

    private C7DC7DAD9D43BACD78DD57E84262789E50BB7D7D8 cpu;
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
        return "0.13.1-SNAPSHOT";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2012, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Simple terminal device for BrainDuck architecture";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        cpu = (C7DC7DAD9D43BACD78DD57E84262789E50BB7D7D8)Context.getInstance()
                .getCPUContext(pluginID,
                C7DC7DAD9D43BACD78DD57E84262789E50BB7D7D8.class);
        if (cpu == null) {
            StaticDialogs.showErrorMessage("BrainTerminal needs to be connected"
                    + "to the BrainCPU.");
            return false;
        }
        cpu.attachDevice(terminal);

        // read settings
        String s = settings.readSetting(pluginID, "verbose");
        if ((s != null) && (s.toUpperCase().equals("TRUE"))) {
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
