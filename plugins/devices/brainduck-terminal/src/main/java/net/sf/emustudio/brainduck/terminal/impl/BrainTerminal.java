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
package net.sf.emustudio.brainduck.terminal.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.brainduck.cpu.BrainCPUContext;
import net.sf.emustudio.brainduck.terminal.io.FileIOProvider;
import net.sf.emustudio.brainduck.terminal.io.InputProvider;
import net.sf.emustudio.brainduck.terminal.io.Keyboard;
import net.sf.emustudio.brainduck.terminal.io.OutputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
    type = PLUGIN_TYPE.DEVICE,
    title = "BrainDuck terminal",
    copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
    description = "Terminal device for abstract BrainDuck architecture."
)
@SuppressWarnings("unused")
public class BrainTerminal extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrainTerminal.class);

    private boolean nogui;
    private BrainTerminalContext terminal;
    private final ContextPool contextPool;

    public BrainTerminal(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        terminal = new BrainTerminalContext();

        try {
            contextPool.register(pluginID, terminal, DeviceContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context",
                getTitle());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.brainduck.terminal.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        BrainCPUContext cpu = contextPool.getCPUContext(pluginID, BrainCPUContext.class);

        String s = settings.readSetting(pluginID, SettingsManager.NO_GUI);
        nogui = (s != null) && s.toUpperCase().equals("TRUE");

        InputProvider inputProvider;
        OutputProvider outputProvider;

        try {
            if (nogui) {
                FileIOProvider fileIOProvider = new FileIOProvider();
                inputProvider = fileIOProvider;
                outputProvider = fileIOProvider;
            } else {
                Keyboard keyboard = new Keyboard();
                outputProvider = BrainTerminalDialog.create(keyboard);
                inputProvider = keyboard;
            }
            terminal.setInputProvider(inputProvider);
            terminal.setOutputProvider(outputProvider);

            cpu.attachDevice(terminal);
        } catch (IOException e) {
            throw new PluginInitializationException(this, e);
        }
    }

    @Override
    public void reset() {
        terminal.reset();
    }

    @Override
    public void destroy() {
        try {
            terminal.close();
        } catch (IOException e) {
            LOGGER.error("Could not close io provider", e);
        }
    }

    @Override
    public void showGUI() {
        if (!nogui) {
            terminal.showGUI();
        }
    }

    @Override
    public void showSettings() {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
