/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;


import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.cpu.AbstractCPU;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.cpu.ram.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.ram.gui.RamDecoder;
import net.emustudio.plugins.cpu.ram.gui.RamStatusPanel;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Random Access Machine (RAM)",
        description = "Emulator of abstract RAM machine"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugin.debugger.Debugging {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RamCpuContextImpl context;

    private EmulatorEngine engine;
    private RamMemoryContext memory;
    private RamDecoder disassembler;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        context = new RamCpuContextImpl(applicationApi.getContextPool());
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().get(pluginID, RamMemoryContext.class);
        disassembler = new RamDecoder(memory);
        context.init(pluginID);
        engine = new EmulatorEngine(context.getInputTape(), context.getOutputTape(), context.getStorageTape(), memory);
        context.setEngine(engine);
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns(Decoder instructions) {
        return List.of(new LabelDebugColumn(memory), DebuggerColumn.mnemo(instructions));
    }

    @Override
    public JPanel getStatusPanel() {
        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new RamStatusPanel(this, context.getInputTape(), context.getOutputTape(), applicationApi.getGUI(), context);
    }

    private boolean setInstructionLocationInternal(int location) {
        return engine.setInstructionLocation(location);
    }

    @Override
    public int getAddressSpaceSize() {
        // Program memory grows with the loaded program; addresses are not bounded by its current size.
        return Integer.MAX_VALUE;
    }

    public TapeSymbol getR0() {
        AbstractTapeContext storage = context.getStorageTape();
        if (storage == null) {
            return TapeSymbol.EMPTY;
        }
        return storage.getSymbolAt(0).orElse(TapeSymbol.EMPTY);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    private void resetInternal(int location) {
        engine.reset(location);
    }

    private RunState executeInternal() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RunState tmpRunState = stepInternal();
                if (engine.debugStopped) return RunState.STATE_STOPPED_BREAK;
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Exception ex) {
                LOGGER.debug("Unexpected error", ex);
                return RunState.STATE_STOPPED_BAD_INSTR;
            }
        }
        return RunState.STATE_RUNNING;
    }

    private RunState stepInternal() throws IOException {
        return engine.step();
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public AutoCloseable observe(Observer observer, Capture capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(this::internalStateChanged); }

    @Override
    public Decoder getDecoder() {
        return disassembler;
    }

    private void publishState(RunState state) {
        context.publishState(state, context.getInstructionLocation(), context.getCpuFrequencyKhz());
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
    public RunState step() throws IOException {
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
    public void reset(int location) {
        resetInternal(location);
        publishState(RunState.STATE_STOPPED_BREAK);
    }

    @Override
    public boolean setInstructionLocation(int location) {
        boolean changed = setInstructionLocationInternal(location);
        if (changed) internalStateChanged();
        return changed;
    }
}
