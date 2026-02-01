/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;
import net.emustudio.plugins.device.mits88dcdd.gui.DiskGui;
import net.emustudio.plugins.device.mits88dcdd.gui.SettingsDialog;
import net.emustudio.plugins.device.mits88dcdd.ports.ControlPort;
import net.emustudio.plugins.device.mits88dcdd.ports.DataPort;
import net.emustudio.plugins.device.mits88dcdd.ports.StatusPort;

import javax.swing.*;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "MITS 88-DCDD"
)
public class DeviceImpl extends AbstractDevice {
    private final DiskSettings settings;
    private final Dialogs dialogs;
    private final boolean guiSupported;

    private Context8080 cpuContext;
    private DriveCollection drives;
    private DiskGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.settings = new DiskSettings(settings);
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        this.dialogs = applicationApi.getDialogs();
    }

    @Override
    public void initialize() throws PluginInitializationException {
        cpuContext = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        drives = new DriveCollection(cpuContext, settings, dialogs);

        StatusPort statusPort = new StatusPort(drives);
        ControlPort controlPort = new ControlPort(drives);
        DataPort dataPort = new DataPort(drives);
        drives.attach(statusPort, controlPort, dataPort);
        settings.addObserver(() -> {
            try {
                drives.attach(statusPort, controlPort, dataPort);
                drives.reset();
            } catch (PluginInitializationException e) {
                throw new RuntimeException(e);
            }
        });
        drives.reset();
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new DiskGui(parent, drives);
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public String getVersion() {
        return Resources.getVersion();
    }

    @Override
    public String getCopyright() {
        return Resources.getCopyright();
    }

    @Override
    public String getDescription() {
        return DIALOG_TITLE + " floppy disk controller.";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public void destroy() {
        settings.clearObservers();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        if (cpuContext != null) {
            cpuContext.detachDevice(0x8);
            cpuContext.detachDevice(0x9);
            cpuContext.detachDevice(0xA);
        }
        drives.destroy();
    }

    @Override
    public void showSettings(JFrame parent) {
        if (guiSupported) {
            new SettingsDialog(parent, settings, drives, applicationApi.getDialogs()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }
}
