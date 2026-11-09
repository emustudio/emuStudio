/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;


import net.emustudio.emulib.plugins.cpu.CPUContext;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.FrequencyCalculator;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.cpu.zilogZ80.gui.StatusPanel;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Zilog Z80 CPU",
        description = "Emulator of Zilog Z80 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugins.debugger.Debugging {
    private final ContextZ80Impl context = new ContextZ80Impl();
    private final InitializerZ80 initializer;

    private StatusPanel statusPanel;
    private Disassembler disassembler;
    private EmulatorEngine engine;

    private final FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        applicationApi.getContextPool().register(pluginID, context, Context8080.class);
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
        applicationApi.getContextPool().register(pluginID, context, ContextZ80.class);
        context.setCPUFrequency(settings.getInt("frequency_khz").orElse(ContextZ80Impl.DEFAULT_FREQUENCY_KHZ));
        initializer = new InitializerZ80(
                pluginID, applicationApi.getContextPool(), context
        );
    }

    @Override
    public int getInstructionLocation() {
        return engine.PC;
    }

    @Override
    public int getAddressSpaceSize() {
        return 0x10000;
    }

    private boolean setInstructionLocationInternal(int location) {
        if (location < 0) {
            return false;
        }
        engine.PC = location & 0xFFFF;
        return true;
    }

    @Override
    public void initialize() throws PluginInitializationException {
        initializer.initialize();
        disassembler = initializer.getDisassembler();
        engine = initializer.getEngine();
        context.setEngine(engine);
        context.addPassedCyclesListener(frequencyCalculator);
        if (applicationApi.getGUI() != null) {
            statusPanel = new StatusPanel(
                    this, context, applicationApi.getGUI(), context
            );
        }
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public AutoCloseable observe(Observer observer, boolean capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(this::internalStateChanged); }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    private RunState executeInternal() {
        try {
            frequencyCalculator.start();
            return engine.run();
        } finally {
            frequencyCalculator.stop();
        }
    }

    private void resetInternal(int startPos) {
        frequencyCalculator.stop();
        engine.reset(startPos);
    }

    private RunState stepInternal() throws Exception {
        return engine.step();
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    protected void destroyInternal() {
        context.removePassedCyclesListener(frequencyCalculator);
        frequencyCalculator.stop();
        context.clearDevices();
    }

    @Override
    public float sampleFrequency() {
        measuredFrequency = frequencyCalculator.sample();
        internalStateChanged();
        return measuredFrequency;
    }

    private float measuredFrequency = Float.NaN;

    private void publishState(RunState state) {
        context.publishState(state, getInstructionLocation(), measuredFrequency);
    }

    private void internalStateChanged() {
        publishState(context.getRunState());
    }

    @Override
    public RunState execute() {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING);
        try {
            result = executeInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result);
        }
    }

    @Override
    public RunState step() throws Exception {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING);
        try {
            result = stepInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result);
        }
    }

    @Override
    public void reset(int startPos) {
        resetInternal(startPos);
        publishState(RunState.STATE_STOPPED_BREAK);
    }

    @Override
    public boolean setInstructionLocation(int location) {
        boolean changed = setInstructionLocationInternal(location);
        if (changed) internalStateChanged();
        return changed;
    }
}
