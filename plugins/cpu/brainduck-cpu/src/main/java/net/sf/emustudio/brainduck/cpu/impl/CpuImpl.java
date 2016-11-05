/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.braincpu.gui.DecoderImpl;
import net.sf.emustudio.braincpu.gui.DisassemblerImpl;
import net.sf.emustudio.brainduck.cpu.BrainCPUContext;
import net.sf.emustudio.brainduck.cpu.gui.StatusPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.CPU,
        title = "BrainCPU",
        copyright = "\u00A9 Copyright 2006-2016, Peter Jakubčo",
        description = "Emulator of CPU for abstract BrainDuck architecture"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final ContextPool contextPool;
    private final BrainCPUContextImpl context = new BrainCPUContextImpl();

    private Disassembler disassembler;
    private MemoryContext<Short> memory;
    private EmulatorEngine engine;
    private volatile boolean optimize;

    public CpuImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        try {
            contextPool.register(pluginID, context, BrainCPUContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context", getTitle());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.braincpu.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }
    
    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        memory = contextPool.getMemoryContext(getPluginID(), MemoryContext.class);

        if (memory.getDataType() != Short.class) {
            throw new PluginInitializationException(
                    this, "Selected operating memory is not supported."
            );
        }
        disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
        engine = new EmulatorEngine(memory, context, new Profiler(memory));
    }

    @Override
    public int getInstructionPosition() {
        return engine.IP;
    }
    
    @Override
    public boolean setInstructionPosition(int pos) {
        if (pos < 0) {
            return false;
        }
        engine.IP = pos;
        return true;
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    @Override
    public JPanel getStatusPanel() {
        return new StatusPanel(memory, this);
    }


    @Override
    public void resetInternal(int adr) {
        engine.reset(adr);
        LOGGER.debug("Register P was reset to " + engine.P);
    }

    @Override
    public RunState call() {
        optimize = true;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isBreakpointSet(engine.IP)) {
                        throw new Breakpoint();
                    }
                    RunState tmpRunState = stepInternal();
                    if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                        return tmpRunState;
                    }
                } catch (IOException e) {
                    LOGGER.error("Unexpected error", e);
                    return RunState.STATE_STOPPED_BAD_INSTR;
                } catch (Breakpoint er) {
                    return RunState.STATE_STOPPED_BREAK;
                }
            }
        } finally {
            optimize = false;
        }
        return RunState.STATE_STOPPED_NORMAL; // cannot be in finally block! it can rewrite breakpoint
    }

    @Override
    protected void destroyInternal() {
        context.detachDevice();
    }

    @Override
    protected RunState stepInternal() throws IOException {
        return engine.step(optimize);
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }
}
