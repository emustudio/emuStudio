/*
 * AbstractTape.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
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
package net.sf.emustudio.ram.abstracttape.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;
import net.sf.emustudio.ram.abstracttape.gui.SettingsDialog;
import net.sf.emustudio.ram.abstracttape.gui.TapeDialog;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "Abstract tape",
copyright = "\u00A9 Copyright 2008-2012, Peter Jakubčo",
description = "Abstract tape device is used by abstract machines such as RAM or Turing machine")
public class AbstractTape extends AbstractDevice {
    private String guiTitle;
    private AbstractTapeContextImpl context;
    private TapeDialog gui;

    public AbstractTape(Long pluginID) {
        super(pluginID);
        context = new AbstractTapeContextImpl(this);
        try {
            ContextPool.getInstance().register(pluginID, context, AbstractTapeContext.class);
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Could not register Tape Context",
                    AbstractTape.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ram.abstracttape.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public boolean initialize(SettingsManager settings) {
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
        this.guiTitle = title;
        if (gui != null) {
            gui.setTitle(title);
        }
    }
    
    public String getGUITitle() {
        return (guiTitle == null) ?
                AbstractTape.class.getAnnotation(PluginType.class).title() 
                : guiTitle;
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
