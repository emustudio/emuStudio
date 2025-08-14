/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.interaction.GuiUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.emustudio.plugins.device.zxspectrum.ula.gui.Keyboard;
import net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@SuppressWarnings("unused")
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "ZX Spectrum48K ULA")
public class DeviceImpl extends AbstractDevice {

    private final boolean guiSupported;
    private final Keyboard keyboard = new Keyboard();
    private boolean guiIOset = false;

    private ULA ula;
    private PassedCyclesMediator passedCyclesMediator;
    private DisplayWindow gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ZxSpectrumBus bus = applicationApi.getContextPool().getDeviceContext(pluginID, ZxSpectrumBus.class);
        this.ula = new ULA(bus);
        this.passedCyclesMediator = new PassedCyclesMediator(ula);
        bus.addPassedCyclesListener(passedCyclesMediator);
        keyboard.addOnKeyListener(ula);
        bus.attachDevice(0xFE, ula);
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
                this.gui = new DisplayWindow(parent, ula, keyboard);
                passedCyclesMediator.setCanvas(gui.getCanvas());
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
