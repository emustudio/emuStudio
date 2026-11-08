/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer;

import net.emustudio.emulib.plugins.AbstractPlugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.audiotapeplayer.gui.SettingsDialog;
import net.emustudio.plugins.device.audiotapeplayer.gui.TapePlayerGui;

import javax.swing.*;
import java.util.List;

@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Audio Tape Player", description = "Audio Tape Player")
public class DeviceImpl extends AbstractPlugin implements Device {

    private final boolean guiSupported;
    private final boolean automaticEmulation;
    private boolean guiIOset = false;

    private TapePlayerGui gui;
    private TapePlaybackController controller;
    private TapePlaybackImpl cassetteListener;
    private AutomationController automationController;
    private JFrame parentFrame;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        this.automaticEmulation = settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO).orElse(false);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        ContextPool contextPool = applicationApi.getContextPool();

        // a cassette player needs a device to which it will write at its own pace
        CPUContext cpu = contextPool.get(pluginID, CPUContext.class);
        DeviceContext<Byte> lineIn = contextPool.getDevice(pluginID, Byte.class);
        this.cassetteListener = new TapePlaybackImpl(lineIn, cpu::getCPUFrequency);
        cpu.addPassedCyclesListener(this.cassetteListener);
        this.controller = new TapePlaybackController(cassetteListener);

        if (guiSupported && settings.getBoolean(SettingsDialog.SETTINGS_KEY_SHOW_GUI_AT_STARTUP).orElse(false)) {
            showGUI(null);
        }
    }

    @Override
    public void reset() {
        this.controller.reset();
        if (automaticEmulation && !guiSupported) {
            automationController = new AutomationController(controller);
        }
    }

    @Override
    public void destroy() {
        if (automationController != null) {
            automationController.close();
        }
        this.controller.close();
        if (guiIOset || gui != null) {
            gui = null;
            cassetteListener.setGui(null);
            guiIOset = false;
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
        this.parentFrame = parent;
        if (guiSupported) {
            if (!guiIOset) {
                this.gui = new TapePlayerGui(parent, applicationApi.getDialogs(), controller, settings, applicationApi.getGUI());
                guiIOset = true;
                this.cassetteListener.setGui(gui);
            }
            this.gui.setVisible(true);

            // Start automation if controller is ready
            if (automationController != null && !automationController.isPlaying()) {
                List<String> storedEvents = settings.getArray(SettingsDialog.SETTINGS_KEY_EVENTS);
                List<AutomationEvent> events = AutomationEvent.deserializeAll(storedEvents);
                automationController.play(events);
            }
        } else if (automationController != null && !automationController.isPlaying()) {
            // No GUI - just start automation
            List<String> storedEvents = settings.getArray(SettingsDialog.SETTINGS_KEY_EVENTS);
            List<AutomationEvent> events = AutomationEvent.deserializeAll(storedEvents);
            automationController.play(events);
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
