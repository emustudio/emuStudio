/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;

import javax.swing.JFrame;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Altair PTR/PTP")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final PaperTapeUnit tape = new PaperTapeUnit();
    private final boolean guiSupported;
    private Context8080 cpu;
    private PaperTapeGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        try {
            applicationApi.getContextPool().register(pluginID, tape, PaperTapeContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            applicationApi.getDialogs().showError("Could not register paper tape context", getTitle());
        }
    }

    @Override
    public void initialize() throws PluginInitializationException {
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        if (!cpu.attachDevice(PaperTapeUnit.STATUS_PORT, tape)) {
            throw new PluginInitializationException(this, "PTR/PTP cannot attach to CPU status port 12h");
        }
        if (!cpu.attachDevice(PaperTapeUnit.DATA_PORT, tape)) {
            cpu.detachDevice(PaperTapeUnit.STATUS_PORT);
            throw new PluginInitializationException(this, "PTR/PTP cannot attach to CPU data port 13h");
        }
    }

    @Override
    public void reset() {
        tape.rewindReader();
    }

    @Override
    public void destroy() {
        if (cpu != null) {
            cpu.detachDevice(PaperTapeUnit.STATUS_PORT);
            cpu.detachDevice(PaperTapeUnit.DATA_PORT);
        }
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        tape.close();
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new PaperTapeGui(parent, tape);
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public void showSettings(JFrame parent) {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Altair paper tape reader and punch on SIMH-compatible ports 12h and 13h.";
    }
}
