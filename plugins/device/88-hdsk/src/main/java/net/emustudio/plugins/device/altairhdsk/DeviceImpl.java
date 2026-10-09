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
import net.emustudio.plugins.device.mits88pio.api.PioContext;

import javax.swing.JFrame;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Altair HDSK / MITS MHDSK")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final PluginSettings settings;
    private final boolean guiSupported;
    private final boolean mits;
    private Context8080 cpu;
    private boolean portAttached;
    private HdskController controller;
    private MhdskController mitsController;
    private HdskGui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.settings = settings;
        String controllerType = settings.getString("controllerType", "SIMH");
        if (!controllerType.equals("SIMH") && !controllerType.equals("MITS")) {
            throw new IllegalArgumentException("controllerType must be SIMH or MITS");
        }
        mits = controllerType.equals("MITS");
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initialize() throws PluginInitializationException {
        if (mits) {
            initializeMits();
            return;
        }
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        MemoryContext<Byte> memory = applicationApi.getContextPool().getMemoryContext(pluginID, MemoryContext.class);
        if (memory.getCellTypeClass() != Byte.class) {
            throw new PluginInitializationException(this, "HDSK requires byte-addressable memory");
        }
        controller = new HdskController(memory);
        if (!cpu.attachDevice(HdskController.PORT, controller)) {
            controller.close();
            controller = null;
            throw new PluginInitializationException(this, "HDSK cannot attach to CPU port FDh");
        }
        portAttached = true;
        try {
            loadSettings();
        } catch (IOException | IllegalArgumentException e) {
            destroy();
            throw new PluginInitializationException(this, "Could not configure HDSK", e);
        }
    }

    private void initializeMits() throws PluginInitializationException {
        PioContext pio = applicationApi.getContextPool().getContext(pluginID, PioContext.class, 0);
        try {
            mitsController = new MhdskController(pio);
            loadSettings();
            pio.attachPeripheral(mitsController);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            destroy();
            throw new PluginInitializationException(this, "Could not configure MHDSK behind 88-4PIO", e);
        }
    }

    private void loadSettings() throws IOException {
        int count = mits ? MhdskController.PLATTER_COUNT : HdskController.DRIVE_COUNT;
        for (int drive = 0; drive < count; drive++) {
            Path path = settings.getString("image" + drive).filter(value -> !value.isBlank())
                    .map(Path::of).map(Path::toAbsolutePath).orElse(null);
            if (mits) {
                boolean readOnly = settings.getBoolean("readOnly" + drive, false);
                if (path == null) {
                    mitsController.detach(drive);
                } else if (!Objects.equals(path, mitsController.getImage(drive))
                        || readOnly != mitsController.isReadOnly(drive)) {
                    mitsController.attach(drive, path, readOnly);
                }
            } else {
                controller.configure(drive,
                        settings.getInt("sectorSize" + drive, HardDisk.DEFAULT_SECTOR_SIZE),
                        settings.getInt("sectorsPerTrack" + drive, HardDisk.DEFAULT_SECTORS_PER_TRACK));
                if (path == null) {
                    controller.detach(drive);
                } else if (!Objects.equals(path, controller.imagePath(drive).orElse(null))) {
                    controller.attach(drive, path);
                }
            }
        }
    }

    @Override
    public void reset() {
        if (mitsController != null) {
            mitsController.reset();
        }
        if (controller != null) {
            controller.reset();
        }
    }

    @Override
    public void destroy() {
        if (portAttached) {
            cpu.detachDevice(HdskController.PORT);
            portAttached = false;
        }
        cpu = null;
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
        if (controller != null) {
            controller.close();
            controller = null;
        }
        if (mitsController != null) {
            mitsController.close();
            mitsController = null;
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported && (controller != null || mitsController != null)) {
            if (gui == null) {
                gui = new HdskGui(parent, controller, mitsController, applicationApi.getDialogs(), applicationApi.getGUI());
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
        if (guiSupported) {
            new SettingsDialog(parent, settings, mits, controller, mitsController,
                    applicationApi.getDialogs(), applicationApi.getGUI(), this::applySettings).setVisible(true);
        }
    }

    void applySettings() {
        // A different controller needs different schema connections and a new initialization.
        if (!settings.getString("controllerType", "SIMH").equals(mits ? "MITS" : "SIMH")) {
            return;
        }
        if (controller != null || mitsController != null) {
            try {
                loadSettings();
            } catch (IOException e) {
                throw new IllegalStateException("Could not apply HDSK settings: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public String getDescription() {
        return mits ? "MITS hard disk controller connected through 88-4PIO."
                : "SIMH-compatible Altair HDSK raw-image extension on port FDh.";
    }
}
