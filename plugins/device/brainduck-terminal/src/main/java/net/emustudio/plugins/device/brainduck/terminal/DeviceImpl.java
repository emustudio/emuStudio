/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.brainduck.terminal;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.brainduck.BrainCPUContext;
import net.emustudio.plugins.device.brainduck.terminal.api.BrainTerminalContext;
import net.emustudio.plugins.device.brainduck.terminal.api.Keyboard;
import net.emustudio.plugins.device.brainduck.terminal.gui.TerminalWindow;
import net.emustudio.plugins.device.brainduck.terminal.interaction.Cursor;
import net.emustudio.plugins.device.brainduck.terminal.interaction.Display;
import net.emustudio.plugins.device.brainduck.terminal.interaction.KeyboardFromFile;
import net.emustudio.plugins.device.brainduck.terminal.interaction.KeyboardGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import static net.emustudio.plugins.device.brainduck.terminal.api.BrainTerminalContext.INPUT_FILE_NAME;
import static net.emustudio.plugins.device.brainduck.terminal.api.BrainTerminalContext.OUTPUT_FILE_NAME;
import static net.emustudio.plugins.device.brainduck.terminal.interaction.Display.COLUMNS;
import static net.emustudio.plugins.device.brainduck.terminal.interaction.Display.ROWS;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "BrainDuck terminal")
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);

    private final BrainTerminalContext terminalContext;
    private final Keyboard keyboard;
    private final Display display;
    private final boolean guiSupported;

    private boolean guiIOset = false;
    private TerminalWindow terminalGUI;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        Cursor cursor = new Cursor(COLUMNS, ROWS);
        this.display = new Display(cursor, OUTPUT_FILE_NAME.toPath(), guiSupported);

        if (guiSupported) {
            LOGGER.debug("Creating GUI-based keyboard");
            this.keyboard = new KeyboardGui();
        } else {
            LOGGER.debug("Creating file-based keyboard ({})", INPUT_FILE_NAME);
            this.keyboard = new KeyboardFromFile(INPUT_FILE_NAME.toPath(), 0);
        }
        this.terminalContext = new BrainTerminalContext(this.keyboard);

        try {
            applicationApi.getContextPool().register(pluginID, terminalContext, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register BrainTerminal context", e);
            applicationApi.getDialogs().showError("Could not register BrainDuck terminal. Please see log file for more details.", getTitle());
        }
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "BrainDuck terminal device";
    }

    @Override
    public void initialize() throws PluginInitializationException {
        BrainCPUContext cpu = applicationApi.getContextPool().getCPUContext(pluginID, BrainCPUContext.class);
        cpu.attachDevice(terminalContext);
        terminalContext.setDisplay(display);

        keyboard.process();
    }

    @Override
    public void reset() {
        terminalContext.reset();
    }

    @Override
    public void destroy() {
        try {
            terminalContext.close();
        } catch (Exception e) {
            LOGGER.error("Could not close BrainTerminal context", e);
        }
        if (terminalGUI != null) {
            terminalGUI.destroy();
        }
    }

    @Override
    public void showSettings(JFrame jFrame) {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiIOset) {
            terminalGUI.setVisible(true);
        } else if (guiSupported) {
            terminalGUI = new TerminalWindow(parent, display, applicationApi.getDialogs(), (KeyboardGui) keyboard);
            GuiUtils.addKeyListener(terminalGUI, (KeyboardGui) keyboard);
            terminalGUI.startPainting();
            guiIOset = true;
            terminalGUI.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.brainduck.terminal.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
