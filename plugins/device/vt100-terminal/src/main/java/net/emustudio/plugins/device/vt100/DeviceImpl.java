/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.brainduck.BrainCPUContext;
import net.emustudio.plugins.device.vt100.api.ContextVt100;
import net.emustudio.plugins.device.vt100.api.Keyboard;
import net.emustudio.plugins.device.vt100.gui.SettingsDialog;
import net.emustudio.plugins.device.vt100.gui.TerminalWindow;
import net.emustudio.plugins.device.vt100.interaction.Cursor;
import net.emustudio.plugins.device.vt100.interaction.DisplayImpl;
import net.emustudio.plugins.device.vt100.interaction.KeyboardFromFile;
import net.emustudio.plugins.device.vt100.interaction.KeyboardGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "VT100 Terminal")
@SuppressWarnings("unused")
public class DeviceImpl extends AbstractDevice {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);

    private final TerminalSettings terminalSettings;
    private final ContextVt100 context;
    private final Keyboard keyboard;
    private final DisplayImpl display;

    private boolean guiIOset = false;
    private TerminalWindow terminalGUI;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.terminalSettings = new TerminalSettings(settings, applicationApi.getDialogs());

        Cursor cursor = new Cursor(terminalSettings.getColumns(), terminalSettings.getRows());
        this.display = new DisplayImpl(cursor, terminalSettings);

        if (terminalSettings.isGuiSupported()) {
            LOGGER.debug("Creating GUI-based keyboard");
            this.keyboard = new KeyboardGui();
        } else {
            LOGGER.debug("Creating file-based keyboard ({})", terminalSettings.getInputPath());
            this.keyboard = new KeyboardFromFile(terminalSettings);
        }
        this.context = new ContextVt100(keyboard);

        try {
            applicationApi.getContextPool().register(pluginID, context, DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register BrainTerminal context", e);
            applicationApi.getDialogs().showError("Could not register VT100-terminal. Please see log file for more details.", getTitle());
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
        return "VT100-terminal device";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        try {
            BrainCPUContext cpu = applicationApi.getContextPool().getCPUContext(pluginID, BrainCPUContext.class);
            cpu.attachDevice(context);
        } catch (ContextNotFoundException e) {
            DeviceContext<Byte> device = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            if (device.getDataType() != Byte.class) {
                throw new PluginInitializationException(
                        "Unexpected device data type. Expected Byte but was: " + device.getDataType()
                );
            }
            context.setExternalDevice(device);
        }
        context.setDisplay(display);
        terminalSettings.addSizeChangedObserver(display::setSize);
        keyboard.process();
    }

    @Override
    public void reset() {
        context.reset();
    }

    @Override
    public void destroy() {
        terminalSettings.destroy();
        keyboard.close();
        if (terminalGUI != null) {
            terminalGUI.destroy();
        }
        display.close();
    }

    @Override
    public void showSettings(JFrame parent) {
        if (isShowSettingsSupported()) {
            new SettingsDialog(parent, terminalSettings, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return terminalSettings.isGuiSupported();
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiIOset) {
            terminalGUI.setVisible(true);
        } else if (terminalSettings.isGuiSupported()) {
            terminalGUI = new TerminalWindow(parent, display, applicationApi.getDialogs(), (KeyboardGui) keyboard);
            GUI.addKeyListenerRecursively(terminalGUI, (KeyboardGui) keyboard);
            terminalGUI.startPainting();
            guiIOset = true;
            terminalGUI.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return terminalSettings.isGuiSupported();
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.vt100.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
