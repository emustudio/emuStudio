/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.device.DeviceContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.device.audiotapeplayer.gui.SettingsDialog;
import net.emustudio.plugins.device.audiotapeplayer.gui.TapePlayerGui;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Audio Tape Player", description = "Audio Tape Player")
public class DeviceImpl extends AbstractPlugin implements Device {

    private final boolean guiSupported;
    private TapePlayerGui gui;
    private TapePlaybackController controller;
    private TapePlaybackImpl cassetteListener;
    private AutomationController automationController;
    private CPUContext cpu;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ContextPool contextPool = applicationApi.getContextPool();

        // a cassette player needs a device to which it will write at its own pace
        cpu = contextPool.get(pluginID, CPUContext.class);
        DeviceContext<Byte> lineIn = contextPool.getDevice(pluginID, Byte.class);
        this.cassetteListener = new TapePlaybackImpl(lineIn, cpu::getCPUFrequency);
        cpu.addPassedCyclesListener(this.cassetteListener);
        this.controller = new TapePlaybackController(cassetteListener);
        this.automationController = new AutomationController(controller);

        if (guiSupported && settings.getBoolean(SettingsDialog.SETTINGS_KEY_SHOW_GUI_AT_STARTUP).orElse(false)) {
            showGUI(null);
        }
    }

    @Override
    public void reset() {
        automationController.reset();
        controller.reset();
    }

    @Override
    public CompletableFuture<Void> startAutomation() {
        List<String> events = settings.getArray(SettingsDialog.SETTINGS_KEY_EVENTS);
        return automationController.play(AutomationEvent.deserializeAll(events));
    }

    @Override
    public CompletableFuture<Void> stopAutomation() {
        CompletableFuture<Void> eventsStopped = automationController.shutdown();
        CompletableFuture<Void> playbackStopped = controller.shutdown();
        return CompletableFuture.allOf(eventsStopped, playbackStopped);
    }

    @Override
    public void destroy() {
        try {
            if (automationController != null) {
                stopAutomation().handle((ignored, failure) -> null).join();
            } else if (controller != null) {
                controller.close();
            }
        } finally {
            if (cassetteListener != null) {
                cpu.removePassedCyclesListener(cassetteListener);
                cassetteListener.setGui(null);
            }
            if (gui != null) {
                gui.dispose();
                gui = null;
            }
        }
    }

    @Override
    public void showSettings(JFrame jFrame) {
        if (guiSupported) {
            new SettingsDialog(jFrame, settings, applicationApi.getDialogs(), applicationApi.getGUI())
                    .setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                this.gui = new TapePlayerGui(parent, applicationApi.getDialogs(), controller, automationController,
                        settings, applicationApi.getGUI());
                this.cassetteListener.setGui(gui);
            }
            this.gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

}
