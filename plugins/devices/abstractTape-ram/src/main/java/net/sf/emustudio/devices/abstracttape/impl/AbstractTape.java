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
package net.sf.emustudio.devices.abstracttape.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.devices.abstracttape.api.AbstractTapeContext;
import net.sf.emustudio.devices.abstracttape.gui.SettingsDialog;
import net.sf.emustudio.devices.abstracttape.gui.TapeDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.CPU,
        title = "Abstract tape",
        copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description = "Abstract tape device is used by abstract machines such as RAM or Turing machine"
)
@SuppressWarnings("unused")
public class AbstractTape extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTape.class);

    private final AbstractTapeContextImpl context;

    private String guiTitle;
    private TapeDialog gui;

    boolean nogui;
    boolean auto;
    private SettingsManager settings;

    public AbstractTape(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        context = new AbstractTapeContextImpl(this);
        try {
            contextPool.register(pluginID, context, AbstractTapeContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
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
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        this.settings = Objects.requireNonNull(settings);

        nogui = Boolean.parseBoolean(settings.readSetting(pluginID, SettingsManager.NO_GUI));
        auto = Boolean.parseBoolean(settings.readSetting(pluginID, SettingsManager.AUTO));

        // show GUI at startup?
        String s = settings.readSetting(pluginID, "showAtStartup");
        if (!nogui && s != null && s.toLowerCase().equals("true")) {
            showGUI();
        }
        context.setVerbose(auto);
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
        return (guiTitle == null) ? super.getTitle() : guiTitle;
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
        return !nogui;
    }
}
