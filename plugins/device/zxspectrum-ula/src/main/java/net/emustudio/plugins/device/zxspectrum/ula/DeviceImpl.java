/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.zxspectrum.bus.api.ModelProfile;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;
import net.emustudio.plugins.device.zxspectrum.ula.gui.DisplayWindow;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@SuppressWarnings("unused")
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "ZX Spectrum ULA")
public class DeviceImpl extends AbstractDevice {

    private final boolean guiSupported;
    private boolean guiIOset = false;

    private ULA ula;
    private ModelProfile profile;
    private PassedCyclesMediator passedCyclesMediator;
    private DisplayWindow gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ZxSpectrumBus bus = applicationApi.getContextPool().getDeviceContext(pluginID, ZxSpectrumBus.class);
        this.profile = bus.getProfile();
        // Bus exposes CPU frequency in kHz (forwarded from CPUContext); convert to Hz for the Beeper
        // sample-rate conversion math, and consult on every sample so a runtime clock change is
        // immediately reflected in audio profile.
        this.ula = new ULA(bus, Beeper.createDefault(profile, () -> bus.getCPUFrequency() * 1000L));
        this.passedCyclesMediator = new PassedCyclesMediator(ula, profile);
        bus.addPassedCyclesListener(passedCyclesMediator);
        for (int port = 0; port < 0x100; port++) {
            if (profile.isUlaPort(port)) {
                bus.attachDevice(port, ula);
            }
        }
    }

    @Override
    public void reset() {
        ula.reset();
        passedCyclesMediator.reset();
    }

    @Override
    public void destroy() {
        if (ula != null) {
            ula.close();
            ula = null;
        }
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
                this.gui = new DisplayWindow(parent, ula, profile, applicationApi.getDialogs(), applicationApi.getGUI());
                this.gui.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        passedCyclesMediator.setCanvas(null);
                        gui = null;
                        guiIOset = false;
                    }
                });
                passedCyclesMediator.setCanvas(gui.getCanvas());
                guiIOset = true;
            }
            this.gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public String getDescription() {
        return "ULA (Uncommitted Logic Array) handles ZX Spectrum keyboard, video, and beeper I/O.";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

}
