/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.swing.JFrame;
import java.io.IOException;
import java.nio.file.Path;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "MITS 88-MDS")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final MdsController controller = new MdsController();
    private final PluginSettings settings;
    private final boolean guiSupported;
    private Context8080 cpu;
    private MdsGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.settings = settings;
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        attachPort(MdsController.STATUS_PORT, controller.statusPort());
        try {
            attachPort(MdsController.CONTROL_PORT, controller.controlPort());
            attachPort(MdsController.DATA_PORT, controller.dataPort());
            loadConfiguredImages();
        } catch (PluginInitializationException e) {
            cpu.detachDevice(MdsController.STATUS_PORT);
            cpu.detachDevice(MdsController.CONTROL_PORT);
            throw e;
        }
    }

    private void attachPort(int port, Context8080.CpuPortDevice device) throws PluginInitializationException {
        if (!cpu.attachDevice(port, device)) {
            throw new PluginInitializationException(this, String.format("88-MDS cannot attach to CPU port %02Xh", port));
        }
    }

    private void loadConfiguredImages() throws PluginInitializationException {
        for (int drive = 0; drive < MdsController.DRIVE_COUNT; drive++) {
            Path path = settings.getString("image" + drive).map(Path::of).orElse(null);
            if (path != null) {
                try {
                    controller.attach(drive, path);
                } catch (IOException e) {
                    throw new PluginInitializationException(this, "Could not attach minidisk image " + path, e);
                }
            }
        }
    }

    @Override
    public void reset() {
        controller.reset();
    }

    @Override
    public void destroy() {
        if (cpu != null) {
            cpu.detachDevice(MdsController.STATUS_PORT);
            cpu.detachDevice(MdsController.CONTROL_PORT);
            cpu.detachDevice(MdsController.DATA_PORT);
        }
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        controller.close();
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new MdsGui(parent, controller);
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
        return "MITS 88-MDS programmed-I/O controller for 35-track Altair minidisks.";
    }
}
