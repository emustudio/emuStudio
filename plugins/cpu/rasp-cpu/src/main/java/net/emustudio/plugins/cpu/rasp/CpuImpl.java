/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;


import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.cpu.AbstractCPU;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.cpu.rasp.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.rasp.gui.RaspDecoder;
import net.emustudio.plugins.cpu.rasp.gui.RaspStatusPanel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "Random Access Stored Program (RASP)",
        description = "RASP machine emulator"
)
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugin.debugger.Debugging {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RaspCpuContextImpl context = new RaspCpuContextImpl();
    private final ContextPool contextPool;

    private EmulatorEngine engine;
    private RaspMemoryContext memory;
    private RaspDecoder disassembler;
    private RaspStatusPanel gui;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.contextPool = applicationApi.getContextPool();
        contextPool.register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().get(pluginID, RaspMemoryContext.class);
        disassembler = new RaspDecoder(memory);
        context.init(pluginID, contextPool);
        engine = new EmulatorEngine(memory, context.getInputTape(), context.getOutputTape());
        context.setEngine(engine);
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns(Decoder instructions) {
        return List.of(DebuggerColumn.address("%d"), new LabelDebugColumn(memory), DebuggerColumn.mnemo(instructions));
    }

    @Override
    public JPanel getStatusPanel() {
        if (gui == null) {
            if (applicationApi.getGUI() == null) {
                return null;
            }
            gui = new RaspStatusPanel(this, context.getInputTape(), context.getOutputTape(), applicationApi.getGUI(), context);
        }
        return gui;
    }

    private void resetInternal(int location) {
        engine.reset(location);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    private RunState executeInternal() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RunState tmpRunState = stepInternal();
                if (engine.debugStopped) return RunState.STATE_STOPPED_BREAK;

                /*if RunState.STATE_STOPPED_BREAK is returned, it means that
                 the instruction was successful so just go on executing next instruction.
                 If HALT was executed or something else was returned, just return it*/
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

    @Override
    public int getAddressSpaceSize() {
        // Program memory grows with the loaded program; addresses are not bounded by its current size.
        return Integer.MAX_VALUE;
    }

    private boolean setInstructionLocationInternal(int location) {
        return engine.setInstructionLocation(location);
    }

    /**
     * Get current value of the accumulator (memory cell at address [0]).
     *
     * @return current value of the accumulator (memory cell at address [0])
     */
    public int getACC() {
        return memory.read(0);
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
