/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;


import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.cpu.AbstractCPU;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.cpu.brainduck.gui.DecoderImpl;
import net.emustudio.plugins.cpu.brainduck.gui.StatusPanel;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;

@PluginRoot(type = PLUGIN_TYPE.CPU, title = "BrainDuck CPU", description = "BrainDuck CPU emulator")
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugin.debugger.Debugging {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final BrainCPUContextImpl context = new BrainCPUContextImpl();

    private Decoder disassembler;
    private ByteMemoryContext memory;
    private EmulatorEngine engine;
    private volatile boolean optimize;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        applicationApi.getContextPool().register(pluginID, context, BrainCPUContext.class);
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().get(pluginID, ByteMemoryContext.class);

        disassembler = new DecoderImpl(memory);
        engine = new EmulatorEngine(memory, context, new Profiler(memory));
        context.setEngine(engine);
    }

    @Override
    public int getAddressSpaceSize() {
        return memory.getSize();
    }

    private boolean setInstructionLocationInternal(int location) {
        if (location < 0) {
            return false;
        }
        engine.IP = location;
        return true;
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public JPanel getStatusPanel() {
        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new StatusPanel(memory, this, applicationApi.getGUI(), context);
    }

    private void resetInternal(int adr) {
        engine.reset(adr);
        LOGGER.debug("Register P was reset to {}", engine.P);
    }

    private RunState executeInternal() {
        optimize = true;
        memory.setMemoryNotificationsEnabled(false);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RunState tmpRunState = stepInternal();
                    if (engine.debugStopped) return RunState.STATE_STOPPED_BREAK;
                    if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                        return tmpRunState;
                    }
                } catch (IndexOutOfBoundsException e) {
                    LOGGER.debug("Unexpected error", e);
                    return RunState.STATE_STOPPED_ADDR_FALLOUT;
                } catch (IOException e) {
                    LOGGER.error("Unexpected error", e);
                    return RunState.STATE_STOPPED_BAD_INSTR;
                }
            }
        } finally {
            optimize = false;
            memory.setMemoryNotificationsEnabled(true);
        }
        return RunState.STATE_RUNNING; // Host applies the requested pause or stop after actual exit.
    }

    @Override
    protected void destroyInternal() {
        context.detachDevice();
    }

    private RunState stepInternal() throws IOException {
        return engine.step(optimize);
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public AutoCloseable observe(Observer observer, Capture capture) { return engine.debugger.observe(observer, capture); }

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
    public void reset(int adr) {
        resetInternal(adr);
        publishState(RunState.STATE_STOPPED_BREAK);
    }

    @Override
    public boolean setInstructionLocation(int location) {
        boolean changed = setInstructionLocationInternal(location);
        if (changed) internalStateChanged();
        return changed;
    }
}
