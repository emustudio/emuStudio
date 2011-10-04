/**
 * AbstractTape.java
 * 
 *   KISS, YAGNI
 *
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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
package abstracttape.impl;

import abstracttape.gui.SettingsDialog;
import abstracttape.gui.TapeDialog;
import interfaces.C50E67F515A7C87A67947F8FB0F82558196BE0AC7;
import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.device.SimpleDevice;
import emuLib8.runtime.Context;
import emuLib8.runtime.StaticDialogs;

public class AbstractTape extends SimpleDevice {

    private TapeContext context;
    private String title = "Abstract tape"; // can change
    private TapeDialog gui;

    public AbstractTape(Long pluginID) {
        super(pluginID);
        context = new TapeContext(this);
        if (!Context.getInstance().register(pluginID, context,
                C50E67F515A7C87A67947F8FB0F82558196BE0AC7.class))
            StaticDialogs.showErrorMessage("Error: Could not register"
                    + " the abstract tape");
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getVersion() {
        return "0.22b";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2011, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "Abstract tape device is used by abstract machines"
                + "such as RAM or Turing machine. The mean and purpose"
                + "of the tape is given by the machine itself. Properties"
                + "such as read only tape or one-way direction tape is"
                + "also given by chosen machine. Therefore the tape is"
                + "universal.";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);
        this.settings = settings;

        // show GUI at startup?        
        String s = settings.readSetting(pluginID, "showAtStartup");
        if (s != null && s.toLowerCase().equals("true")) {
            showGUI();
        }
        return true;
    }

    @Override
    public void showGUI() {
        if (gui == null) {
            gui = new TapeDialog(this, context, settings, pluginID);
        }
        gui.setVisible(true);
    }

    public void setGUITitle(String title) {
        this.title = title;
        if (gui != null)
            gui.setTitle(title);
    }
    
    @Override
    public void destroy() {
        if (gui != null) {
            gui.dispose();
        }
        gui = null;
        context = null;
        settings = null;
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public void showSettings() {
        new SettingsDialog(settings, pluginID, gui).setVisible(true);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
