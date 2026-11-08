/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.abstracttape;

import net.emustudio.emulib.plugins.AbstractPlugin;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.gui.SettingsDialog;
import net.emustudio.plugins.device.abstracttape.gui.TapeGui;

import javax.swing.*;
import java.util.Objects;

@PluginRoot(
        type = PLUGIN_TYPE.DEVICE,
        title = "Abstract tape",
        description = "The device represents a tape used by abstract machines"
)
@SuppressWarnings("unused")
public class AbstractTape extends AbstractPlugin implements Device {
    private final AbstractTapeContextImpl context;
    private final boolean guiSupported;
    private final boolean automaticEmulation;

    private String guiTitle = Device.super.getTitle();
    private TapeGui gui;

    public AbstractTape(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        this.automaticEmulation = settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO).orElse(false);
        this.context = new AbstractTapeContextImpl(this::setGUITitle);

        applicationApi.getContextPool().register(pluginID, context, AbstractTapeContext.class);
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    @Override
    public void initialize() {
        boolean showAtStartup = settings.getBoolean("showAtStartup").orElse(false);
        if (showAtStartup) {
            showGUI(null);
        }
    }

    @Override
    public void showGUI(JFrame parent) {
        if (guiSupported) {
            if (gui == null) {
                boolean alwaysOnTop = settings.getBoolean("alwaysOnTop").orElse(false);
                gui = new TapeGui(parent, getTitle(), context, alwaysOnTop, applicationApi.getDialogs(), applicationApi.getGUI());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public String getTitle() {
        return (guiTitle == null) ? Device.super.getTitle() : guiTitle;
    }

    public void setGUITitle(String title) {
        this.guiTitle = Objects.requireNonNull(title);
        if (gui != null) {
            gui.setTitle(title);
        }
    }

    @Override
    public void destroy() {
        context.stopLoggingSymbols();
        if (gui != null) {
            gui.dispose();
            gui = null;
        }
    }

    @Override
    public void reset() {
        context.reset();
        if (automaticEmulation && !guiSupported) {
            context.stopLoggingSymbols();
            context.startLoggingSymbols();
        }
    }

    @Override
    public void showSettings(JFrame parent) {
        if (guiSupported) {
            new SettingsDialog(parent, settings, applicationApi.getDialogs(), gui, guiTitle, applicationApi.getGUI()).setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return guiSupported;
    }

}
