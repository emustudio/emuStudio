/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.AbstractDevice;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.ay38910.gui.Ay38910Gui;

import javax.swing.JFrame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Supplier;

@SuppressWarnings("unused")
@PluginRoot(type = PLUGIN_TYPE.DEVICE, title = "Audio AY-3-8910 Chip")
public class DeviceImpl extends AbstractDevice {
    private final Supplier<Ay38910Chip> chipFactory;
    private final boolean guiSupported;

    private Ay38910Chip chip;
    private Runnable detachAction = () -> {
    };
    private Ay38910Gui gui;

    public DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        this(pluginID, applicationApi, settings, Ay38910Chip::createDefault);
    }

    DeviceImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings, Supplier<Ay38910Chip> chipFactory) {
        super(pluginID, applicationApi, settings);
        this.chipFactory = chipFactory;
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false) && applicationApi.getGUI() != null;
    }

    @Override
    public void initialize() throws PluginInitializationException {
        this.chip = chipFactory.get();
        this.detachAction = attachChip(applicationApi.getContextPool(), chip);
    }

    @Override
    public void reset() {
        if (chip != null) {
            chip.reset();
        }
    }

    @Override
    public void destroy() {
        detachAction.run();
        detachAction = () -> {
        };
        if (gui != null) {
            gui.destroy();
            gui = null;
        }
        if (chip != null) {
            chip.close();
            chip = null;
        }
    }

    @Override
    public void showSettings(JFrame parent) {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public void showGUI(JFrame parent) {
        if (!guiSupported || chip == null) {
            return;
        }
        if (gui == null) {
            gui = new Ay38910Gui(parent, chip, applicationApi.getGUI());
            gui.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    gui = null;
                }
            });
        }
        gui.setVisible(true);
    }

    @Override
    public boolean isGuiSupported() {
        return guiSupported;
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(bundle -> bundle.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(bundle -> bundle.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "AY-3-8910 programmable sound generator for ZX Spectrum style bus wiring.";
    }

    @Override
    public boolean isAutomationSupported() {
        return true;
    }

    private Runnable attachChip(ContextPool contextPool, Ay38910Chip chip) throws PluginInitializationException {
        try {
            Context8080 cpu = contextPool.getCPUContext(pluginID, Context8080.class);
            configureChipFrequency(chip, cpu.getCPUFrequency());
            if (!cpu.attachDevice(Ay38910Chip.DATA_PORT & 0xFF, chip)) {
                throw new PluginInitializationException(
                        this, "AY-3-8910 cannot be attached to CPU port 0xFD (hardware conflict?)"
                );
            }
            cpu.addPassedCyclesListener(chip);
            return () -> {
                cpu.removePassedCyclesListener(chip);
                cpu.detachDevice(Ay38910Chip.DATA_PORT & 0xFF);
            };
        } catch (ContextNotFoundException e) {
            return attachThroughDeviceContext(contextPool, chip);
        } catch (InvalidContextException e) {
            throw new PluginInitializationException(this, "Could not access CPU context", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Runnable attachThroughDeviceContext(ContextPool contextPool, Ay38910Chip chip) throws PluginInitializationException {
        try {
            DeviceContext<?> deviceContext = contextPool.getDeviceContext(pluginID, DeviceContext.class);
            Method attachDevice = deviceContext.getClass().getMethod("attachDevice", int.class, Context8080.CpuPortDevice.class);
            Method addPassedCyclesListener = deviceContext.getClass().getMethod("addPassedCyclesListener", CPUContext.PassedCyclesListener.class);
            Method removePassedCyclesListener = deviceContext.getClass().getMethod("removePassedCyclesListener", CPUContext.PassedCyclesListener.class);

            configureChipFrequency(deviceContext, chip);
            attachDevice.invoke(deviceContext, Ay38910Chip.DATA_PORT & 0xFF, chip);
            addPassedCyclesListener.invoke(deviceContext, chip);

            return () -> invokeQuietly(removePassedCyclesListener, deviceContext, chip);
        } catch (InvalidContextException | ContextNotFoundException e) {
            throw new PluginInitializationException(
                    this,
                    "AY-3-8910 requires either a direct 8080/Z80 CPU connection or a connected device context exposing attachDevice/addPassedCyclesListener",
                    e
            );
        } catch (NoSuchMethodException e) {
            throw new PluginInitializationException(
                    this,
                    "Connected device does not expose the port attachment methods required by AY-3-8910",
                    e
            );
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PluginInitializationException(this, "Could not attach AY-3-8910 through connected device context", e);
        }
    }

    private void invokeQuietly(Method method, Object target, Object argument) {
        try {
            method.invoke(target, argument);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
        }
    }

    private void configureChipFrequency(Ay38910Chip chip, int cpuFrequencyKHz) throws PluginInitializationException {
        try {
            chip.setCpuFrequencyKHz(cpuFrequencyKHz);
        } catch (IllegalArgumentException e) {
            throw new PluginInitializationException(this, "Invalid CPU frequency reported for AY-3-8910", e);
        }
    }

    private void configureChipFrequency(Object deviceContext, Ay38910Chip chip) throws PluginInitializationException {
        Integer cpuFrequencyKHz = invokeIntGetter(deviceContext, "getCPUFrequency");
        if (cpuFrequencyKHz == null) {
            cpuFrequencyKHz = invokeIntGetter(deviceContext, "getCpuFrequency");
        }
        if (cpuFrequencyKHz == null) {
            throw new PluginInitializationException(
                    this,
                    "Connected device does not expose getCPUFrequency/getCpuFrequency required by AY-3-8910"
            );
        }
        configureChipFrequency(chip, cpuFrequencyKHz);
    }

    private Integer invokeIntGetter(Object target, String methodName) throws PluginInitializationException {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            throw new PluginInitializationException(this, "CPU frequency getter did not return a number");
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new PluginInitializationException(this, "Could not read CPU frequency for AY-3-8910", e);
        }
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.ay38910.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
