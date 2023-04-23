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
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.zxspectrum.ula.api.LineInPort;
import net.emustudio.plugins.device.zxspectrum.ula.gui.Keyboard;
import net.emustudio.plugins.device.zxspectrum.ula.gui.TerminalWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "ZX Spectrum48K ULA")
public class DeviceImpl extends AbstractDevice {
    private final static Logger LOGGER = LoggerFactory.getLogger(DeviceImpl.class);

    private final boolean guiSupported;
    private final Keyboard keyboard = new Keyboard();
    private boolean guiIOset = false;

    private ULA ula;
    private TerminalWindow gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);

        try {
            ContextPool contextPool = applicationApi.getContextPool();
            contextPool.register(pluginID, new LineInPort(), DeviceContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register memory context", e);
            applicationApi.getDialogs().showError(
                    "Could not register memory. Please see log file for more details", getTitle()
            );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        MemoryContext<Byte> memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        Class<?> cellTypeClass = memory.getCellTypeClass();
        if (cellTypeClass != Byte.class) {
            throw new PluginInitializationException("Could not find Byte-cell memory");
        }
        Context8080 cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);

        this.ula = new ULA(memory, cpu);
        keyboard.addOnKeyListener(ula);

        cpu.attachDevice(0xFE, ula);
    }

    @Override
    public void reset() {
        ula.reset();
    }

    @Override
    public void destroy() {
        keyboard.close();
        if (guiIOset || gui != null) {
            gui.destroy();
            gui = null;
            guiIOset = false;
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
        if (guiSupported) {
            if (!guiIOset) {
                this.gui = new TerminalWindow(parent, ula);
                GuiUtils.addKeyListener(gui, keyboard);
                guiIOset = true;
                this.gui.setVisible(true);
            }
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
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
        return "ZX Spectrum48K ULA";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }


    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.zxspectrum.ula.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
