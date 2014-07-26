/*
 * AbstractTape.java
 *
 * Copyright (C) 2009-2013 Peter Jakubčo
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
import emulib.runtime.LoggerFactory;
import emulib.runtime.interfaces.Logger;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;
import net.sf.emustudio.ram.abstracttape.gui.SettingsDialog;
import net.sf.emustudio.ram.abstracttape.gui.TapeDialog;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "Abstract tape",
copyright = "\u00A9 Copyright 2008-2013, Peter Jakubčo",
description = "Abstract tape device is used by abstract machines such as RAM or Turing machine")
public class AbstractTape extends AbstractDevice {
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractTape.class);
    private String guiTitle;
    private AbstractTapeContextImpl context;
    private TapeDialog gui;
    boolean nogui;
    boolean auto;

    public AbstractTape(Long pluginID) {
        super(pluginID);
        context = new AbstractTapeContextImpl(this);
        try {
            ContextPool.getInstance().register(pluginID, context, AbstractTapeContext.class);
        } catch (Exception e) {
            LOGGER.error("Could not register Abstract tape context", e);
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

        nogui = Boolean.parseBoolean(settings.readSetting(pluginID, "nogui"));
        auto = Boolean.parseBoolean(settings.readSetting(pluginID, "auto"));

        // show GUI at startup?
        String s = settings.readSetting(pluginID, "showAtStartup");
        if (!nogui && s != null && s.toLowerCase().equals("true")) {
            showGUI();
        }
        context.setVerbose(auto);
        return true;
    }

    @Override
    public void showGUI() {
        if (!nogui) {
            if (gui == null) {
                gui = new TapeDialog(this, context, settings, pluginID);
            }
            gui.setVisible(true);
        }
    }

    @Override
    public String getTitle() {
        return (guiTitle == null) ?
                AbstractTape.class.getAnnotation(PluginType.class).title()
                : guiTitle;
    }


    public void setGUITitle(String title) {
        this.guiTitle = title;
        if (gui != null) {
            gui.setTitle(title);
            context.setVerbose(auto);
        }
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
        if (!nogui) {
            new SettingsDialog(settings, pluginID, gui).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
