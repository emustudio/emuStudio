/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.cpu.rasp.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.rasp.gui.RaspDisassembler;
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
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugins.debugger.Debugging {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final RaspCpuContextImpl context = new RaspCpuContextImpl();
    private final ContextPool contextPool;

    private EmulatorEngine engine;
    private RaspMemoryContext memory;
    private RaspDisassembler disassembler;
    private RaspStatusPanel gui;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.contextPool = applicationApi.getContextPool();
        contextPool.register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().get(pluginID, RaspMemoryContext.class);
        disassembler = new RaspDisassembler(memory);
        context.init(pluginID, contextPool);
        engine = new EmulatorEngine(memory, context.getInputTape(), context.getOutputTape());
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns() {
        return List.of(DebuggerColumn.address("%d"), new LabelDebugColumn(memory), DebuggerColumn.mnemo(disassembler));
    }

    @Override
    public JPanel getStatusPanel() {
        if (gui == null) {
            if (applicationApi.getGUI() == null) {
                return null;
            }
            gui = new RaspStatusPanel(this, context.getInputTape(), context.getOutputTape(), applicationApi.getGUI(), applicationApi.getExecutionState());
        }
        return gui;
    }

    @Override
    public void reset(int location) {
        engine.reset(location);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    public RunState execute() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RunState tmpRunState = step();
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

    @Override
    public RunState step() throws IOException {
        return engine.step();
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public void onBoundary(Runnable callback) { engine.debugger.onBoundary(callback); }

    @Override
    public AutoCloseable observe(Observer observer, boolean capture) { return engine.debugger.observe(observer, capture); }

    public JPanel debuggingEditor() { return engine.debugger.editor(applicationApi.getExecutionState()::update); }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    public int getInstructionLocation() {
        return engine.IP.get();
    }

    @Override
    public int getAddressSpaceSize() {
        // Program memory grows with the loaded program; addresses are not bounded by its current size.
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean setInstructionLocation(int location) {
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

}
