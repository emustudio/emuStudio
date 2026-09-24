/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.swing.JFrame;
import java.io.IOException;
import java.nio.file.Path;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "SIMH Altair HDSK")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final PluginSettings settings;
    private final boolean guiSupported;
    private Context8080 cpu;
    private HdskController controller;
    private HdskGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.settings = settings;
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        MemoryContext<Byte> memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        if (memory.getCellTypeClass() != Byte.class) {
            throw new PluginInitializationException(this, "HDSK requires byte-addressable memory");
        }
        controller = new HdskController(memory);
        if (!cpu.attachDevice(HdskController.PORT, controller)) {
            throw new PluginInitializationException(this, "HDSK cannot attach to CPU port FDh");
        }
        try {
            loadSettings();
        } catch (IOException | IllegalArgumentException e) {
            cpu.detachDevice(HdskController.PORT);
            controller.close();
            throw new PluginInitializationException(this, "Could not configure HDSK", e);
        }
    }

    private void loadSettings() throws IOException {
        for (int drive = 0; drive < HdskController.DRIVE_COUNT; drive++) {
            controller.configure(drive,
                    settings.getInt("sectorSize" + drive, HardDisk.DEFAULT_SECTOR_SIZE),
                    settings.getInt("sectorsPerTrack" + drive, HardDisk.DEFAULT_SECTORS_PER_TRACK));
            Path path = settings.getString("image" + drive).map(Path::of).orElse(null);
            if (path != null) {
                controller.attach(drive, path);
            }
        }
    }

    @Override
    public void reset() {
        if (controller != null) {
            controller.reset();
        }
    }

    @Override
    public void destroy() {
        if (cpu != null) {
            cpu.detachDevice(HdskController.PORT);
        }
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        if (controller != null) {
            controller.close();
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported && controller != null) {
            if (gui == null) {
                gui = new HdskGui(parent, controller);
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
        return "SIMH-compatible Altair HDSK raw-image extension on port FDh.";
    }
}
