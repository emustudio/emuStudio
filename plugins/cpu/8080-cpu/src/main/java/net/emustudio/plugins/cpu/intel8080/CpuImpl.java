/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.cpu.CPUSnapshot;

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
import net.emustudio.plugins.cpu.intel8080.gui.StatusPanel;

import javax.swing.*;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Intel 8080 CPU",
        description = "Emulator of Intel 8080 CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugins.debugger.Debugging {
    private final Context8080Impl context = new Context8080Impl();
    private final InitializerFor8080 initializer;

    private EmulatorEngine engine;
    private StatusPanel statusPanel;
    private Disassembler disassembler;

    private final FrequencyCalculator frequencyCalculator = new FrequencyCalculator();

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        applicationApi.getContextPool().register(pluginID, context, Context8080.class);
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
        context.setCPUFrequency(settings.getInt("frequency_khz").orElse(Context8080Impl.DEFAULT_FREQUENCY_KHZ));
        initializer = new InitializerFor8080(
                pluginID, applicationApi.getContextPool(), context
        );
    }

    @Override
    public void initialize() throws PluginInitializationException {
        initializer.initialize();
        engine = initializer.getEngine();
        context.setCpu(engine);
        disassembler = initializer.getDisassembler();
        context.addPassedCyclesListener(frequencyCalculator);
        if (applicationApi.getGUI() != null) {
            statusPanel = new StatusPanel(this, context, applicationApi.getGUI(), context);
        }
    }

    @Override
    public float sampleFrequency() {
        measuredFrequency = frequencyCalculator.sample();
        internalStateChanged();
        return measuredFrequency;
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

    public JPanel debuggingEditor() { return engine.debugger.editor(this::internalStateChanged); }

    public EmulatorEngine getEngine() {
        return engine;
    }

    private void resetInternal(int startPos) {
        engine.reset(startPos);
        frequencyCalculator.stop();
    }

    private RunState stepInternal() throws Exception {
        return engine.step();
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

    private boolean setInstructionLocationInternal(int position) {
        if (position < 0) {
            return false;
        }
        engine.PC = position & 0xFFFF;
        return true;
    }

    private float measuredFrequency = Float.NaN;

    private void publishState(RunState state, boolean busy) {
        context.publishState(new CPUSnapshot(
                state, busy, false, getInstructionLocation(), measuredFrequency));
    }

    private void internalStateChanged() {
        publishState(context.snapshot().runState(), context.snapshot().isBusy());
    }

    @Override
    public RunState execute() {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING, false);
        try {
            result = executeInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result, false);
        }
    }

    @Override
    public RunState step() throws Exception {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING, true);
        try {
            result = stepInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result, false);
        }
    }

    @Override
    public void reset(int startPos) {
        resetInternal(startPos);
        publishState(RunState.STATE_STOPPED_BREAK, false);
    }

    @Override
    public boolean setInstructionLocation(int position) {
        boolean changed = setInstructionLocationInternal(position);
        if (changed) internalStateChanged();
        return changed;
    }
}
