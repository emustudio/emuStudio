/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.FrequencyCalculator;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.intel8080.gui.StatusPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Intel 8080 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final Context8080Impl context = new Context8080Impl();
    private final InitializerFor8080 initializer;

    private EmulatorEngine engine;
    private StatusPanel statusPanel;
    private Disassembler disassembler;

    private final FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        try {
            applicationApi.getContextPool().register(pluginID, context, Context8080.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register CPU context", e);
            applicationApi.getDialogs().showError(
                    "Could not register CPU Context. Please see log file for details.", super.getTitle()
            );
        }
        context.setCPUFrequency(settings.getInt("frequency_khz", Context8080Impl.DEFAULT_FREQUENCY_KHZ));
        initializer = new InitializerFor8080(
                this, pluginID, applicationApi.getContextPool(), settings, context
        );
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Emulator of Intel 8080 CPU";
    }

    @Override
    public void initialize() throws PluginInitializationException {
        initializer.initialize();
        engine = initializer.getEngine();
        context.setCpu(engine);
        disassembler = initializer.getDisassembler();
        context.addPassedCyclesListener(frequencyCalculator);
        statusPanel = new StatusPanel(this, context, initializer.shouldDumpInstructions());
    }

    public FrequencyCalculator getFrequencyCalculator() {
        return frequencyCalculator;
    }

    @Override
    protected void destroyInternal() {
        context.removePassedCyclesListener(frequencyCalculator);
        frequencyCalculator.stop();
        frequencyCalculator.close();
        context.clearDevices();
        initializer.destroy();
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public void resetInternal(int startPos) {
        engine.reset(startPos);
        frequencyCalculator.stop();
    }

    @Override
    public void pause() {
        super.pause();
        frequencyCalculator.stop();
    }

    @Override
    public void stop() {
        super.stop();
        frequencyCalculator.stop();
    }

    @Override
    protected RunState stepInternal() throws Exception {
        return engine.step();
    }

    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    @Override
    public RunState call() {
        try {
            frequencyCalculator.start();
            return engine.run(this);
        } finally {
            frequencyCalculator.stop();
        }
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    public int getInstructionLocation() {
        return engine.PC;
    }

    @Override
    public boolean setInstructionLocation(int position) {
        if (position < 0) {
            return false;
        }
        engine.PC = position & 0xFFFF;
        return true;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.intel8080.version"));
        } catch (MissingResourceException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
