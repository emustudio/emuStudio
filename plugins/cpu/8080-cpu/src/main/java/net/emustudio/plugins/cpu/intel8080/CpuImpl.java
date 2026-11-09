/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.cpu.AbstractCPU;
import net.emustudio.emulib.plugin.cpu.Disassembler;
import net.emustudio.emulib.plugin.cpu.FrequencyCalculator;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.intel8080.gui.StatusPanel;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Intel 8080 CPU",
        description = "Emulator of Intel 8080 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugin.debugger.Debugging {
    private final Context8080Impl context = new Context8080Impl();
    private final InitializerFor8080 initializer;

    private EmulatorEngine engine;
    private StatusPanel statusPanel;
    private Disassembler disassembler;

    private final FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        applicationApi.getContextPool().register(pluginID, context, Context8080.class);
        context.setCPUFrequency(settings.getInt("frequency_khz").orElse(Context8080Impl.DEFAULT_FREQUENCY_KHZ));
        initializer = new InitializerFor8080(
                pluginID, applicationApi.getContextPool(), context
        );
    }

    @Override
    public void initialize() throws PluginInitializationException {
        initializer.initialize();
        engine = initializer.getEngine();
        engine.debugger.onBoundary(() -> applicationApi.getExecutionState().processUpdates());
        context.setCpu(engine);
        disassembler = initializer.getDisassembler();
        context.addPassedCyclesListener(frequencyCalculator);
        if (applicationApi.getGUI() != null) {
            statusPanel = new StatusPanel(this, context, applicationApi.getGUI(), applicationApi.getExecutionState());
        }
    }

    @Override
    public float sampleFrequency() {
        return frequencyCalculator.sample();
    }

    @Override
    protected void destroyInternal() {
        context.removePassedCyclesListener(frequencyCalculator);
        frequencyCalculator.stop();
        context.clearDevices();
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public AutoCloseable observe(Observer observer, boolean capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(applicationApi.getExecutionState()::update); }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public void reset(int startPos) {
        engine.reset(startPos);
        frequencyCalculator.stop();
    }

    @Override
    public RunState step() throws Exception {
        return engine.step();
    }

    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    @Override
    public RunState execute() {
        try {
            frequencyCalculator.start();
            return engine.run();
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
    public int getAddressSpaceSize() {
        return 0x10000;
    }

    @Override
    public boolean setInstructionLocation(int position) {
        if (position < 0) {
            return false;
        }
        engine.PC = position & 0xFFFF;
        return true;
    }

}
