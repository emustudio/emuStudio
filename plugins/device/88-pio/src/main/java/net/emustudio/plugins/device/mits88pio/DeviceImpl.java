/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88pio.api.PioContext;

import javax.swing.JFrame;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "MITS 88-PIO / 88-4PIO")
@SuppressWarnings("unused")
public final class DeviceImpl extends AbstractDevice {
    private final PioUnit pio;
    private final PioBoard fourPio;
    private final Context8080.CpuPortDevice ports;
    private final int portCount;
    private final int basePort;
    private final int interruptVector;
    private int attachedPorts;
    private final boolean guiSupported;
    private final PluginSettings settings;
    private Context8080 cpu;
    private DeviceContext<?> attachedDevice;
    private PioGui gui;
    private Exception registrationError;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        this.settings = settings;
        String boardType = settings.getString("boardType", "88-4PIO");
        if (!boardType.equals("88-PIO") && !boardType.equals("88-4PIO")) {
            throw new IllegalArgumentException("boardType must be 88-PIO or 88-4PIO");
        }
        boolean originalPio = boardType.equals("88-PIO");
        basePort = settings.getInt("basePort", originalPio ? 0x04 : 0xA0);
        interruptVector = settings.getInt("interruptVector", 7);
        if (interruptVector < 0 || interruptVector > 7) {
            throw new IllegalArgumentException("interruptVector must be between 0 and 7");
        }
        pio = originalPio ? new PioUnit(basePort) : null;
        fourPio = originalPio ? null : new PioBoard(basePort, settings.getInt("piaCount", 2));
        ports = originalPio ? pio : fourPio;
        portCount = originalPio ? 2 : fourPio.getPortCount();
        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        try {
            if (originalPio) { applicationApi.getContextPool().register(pluginID, pio, DeviceContext.class); }
            else { applicationApi.getContextPool().register(pluginID, fourPio, PioContext.class); }
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            registrationError = e;
        }
    }

    @Override
    public void initialize() throws PluginInitializationException {
        if (registrationError != null) {
            throw new PluginInitializationException(this, "Could not register PIO context", registrationError);
        }
        cpu = applicationApi.getContextPool().getCPUContext(pluginID, Context8080.class);
        if (pio != null) {
            try {
                attachedDevice = applicationApi.getContextPool().getDeviceContext(pluginID, DeviceContext.class);
            } catch (ContextNotFoundException e) {
                attachedDevice = null;
            }
        }
        Runnable interrupt = () -> {
            if (cpu != null && cpu.isInterruptSupported()) {
                cpu.signalInterrupt(new byte[]{(byte) (0xC7 | (interruptVector << 3))});
            }
        };
        if (pio != null) { pio.setInterruptHandler(interrupt); }
        else { fourPio.setInterruptHandler(interrupt); }
        for (int port = basePort; port < basePort + portCount; port++) {
            if (!cpu.attachDevice(port, ports)) {
                detachPorts();
                throw new PluginInitializationException(this,
                        String.format("88-PIO cannot attach to CPU port %02Xh", port));
            }
            attachedPorts++;
        }
    }

    @Override
    public void reset() {
        if (pio != null) { pio.reset(); }
        else { fourPio.reset(); }
    }

    @Override
    public void destroy() {
        detachPorts();
        attachedDevice = null;
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    private void detachPorts() {
        if (cpu != null) {
            while (attachedPorts > 0) {
                cpu.detachDevice(basePort + --attachedPorts);
            }
            cpu = null;
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                gui = new PioGui(parent, pio, fourPio, basePort, applicationApi.getGUI(), this::getAttachedDeviceId);
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    String getAttachedDeviceId() {
        Object device = fourPio == null ? attachedDevice : fourPio.getPeripheral();
        if (device == null) { return "unknown"; }
        PluginContext context = device.getClass().getAnnotation(PluginContext.class);
        return context == null ? device.toString() : context.id();
    }

    @Override
    public void showSettings(JFrame parent) {
        if (guiSupported) {
            new SettingsDialog(parent, settings, applicationApi.getDialogs(), applicationApi.getGUI()).setVisible(true);
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
        return "MITS parallel interface: Intel 8212-based 88-PIO or Motorola 6820-based 88-4PIO.";
    }
}
