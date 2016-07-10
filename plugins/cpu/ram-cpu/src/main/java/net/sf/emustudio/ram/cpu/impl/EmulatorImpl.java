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
package net.sf.emustudio.ram.cpu.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.API;
import emulib.emustudio.SettingsManager;
import emulib.emustudio.debugtable.BreakpointColumn;
import emulib.emustudio.debugtable.DebugTable;
import emulib.emustudio.debugtable.MnemoColumn;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.ContextNotFoundException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;
import net.sf.emustudio.ram.cpu.gui.LabelDebugColumn;
import net.sf.emustudio.ram.cpu.gui.RAMDisassembler;
import net.sf.emustudio.ram.cpu.gui.RAMStatusPanel;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.CPU,
        title = "Random Access Machine (RAM)",
        copyright = "\u00A9 Copyright 2006-2016, Peter Jakubčo",
        description = "Emulator of abstract RAM machine"
)
@SuppressWarnings("unused")
public class EmulatorImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorImpl.class);

    private final RAMContext context;
    private final ContextPool contextPool;

    private EmulatorEngine engine;
    private RAMMemoryContext memory;
    private RAMDisassembler disassembler;
    private boolean debugTableInitialized = false;

    public EmulatorImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);

        context = new RAMContext(contextPool);
        try {
            contextPool.register(pluginID, context, CPUContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register RAM CPU Context",
                    EmulatorImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ram.cpu.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        try {
            memory = contextPool.getMemoryContext(getPluginID(), RAMMemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            // Will be processed later on
            throw new PluginInitializationException(this, "Could not get memory context", e);
        }

        if (memory.getDataType() != RAMInstruction.class) {
            throw new PluginInitializationException(
                this, "The RAM machine doesn't support this kind of program memory!"
            );
        }
        engine = new EmulatorEngine(context, memory);

        disassembler = new RAMDisassembler(memory);
        context.init(getPluginID(), engine);
    }

    @Override
    public JPanel getStatusPanel() {
        if (!debugTableInitialized) {
            DebugTable debugTable = API.getInstance().getDebugTable();
            if (debugTable != null) {
                debugTable.setCustomColumns(Arrays.asList(
                        new BreakpointColumn(this), new LabelDebugColumn(memory), new MnemoColumn(disassembler)
                ));
            }
            debugTableInitialized = true;
        }
        return new RAMStatusPanel(this, memory);
    }

    @Override
    public boolean setInstructionPosition(int pos) {
        return engine.setInstructionPosition(pos);
    }

    @Override
    public int getInstructionPosition() {
        return engine.IP;
    }

    public String getR0() {
        AbstractTapeContext storage = context.getStorage();
        if (storage == null) {
            return "<empty>";
        }
        return storage.getSymbolAt(0);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    public void resetInternal(int pos) {
        engine.reset(pos);
    }

    @Override
    public RunState call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isBreakpointSet(engine.IP)) {
                    throw new Breakpoint();
                }
                RunState tmpRunState = stepInternal();
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException e) {
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (IOException e) {
                LOGGER.error("Unexpected error while reading/writing to the tape", e);
                return RunState.STATE_STOPPED_BAD_INSTR;
            } catch (Breakpoint er) {
                return RunState.STATE_STOPPED_BREAK;
            }
        }
        return RunState.STATE_STOPPED_NORMAL;
    }

    @Override
    public RunState stepInternal() throws IOException {
        return engine.step();
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }
}
