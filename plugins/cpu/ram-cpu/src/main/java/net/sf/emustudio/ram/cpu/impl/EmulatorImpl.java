/*
 * Copyright (C) 2009-2015 Peter Jakubčo
 * KISS, YAGNI, DRY
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
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;
import net.sf.emustudio.ram.cpu.gui.LabelDebugColumn;
import net.sf.emustudio.ram.cpu.gui.RAMDisassembler;
import net.sf.emustudio.ram.cpu.gui.RAMStatusPanel;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.CPU,
        title = "Random Access Machine (RAM)",
        copyright = "\u00A9 Copyright 2009-2015, Peter Jakubčo",
        description = "Emulator of abstract RAM machine"
)
public class EmulatorImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorImpl.class);

    private final RAMContext context;
    private final ContextPool contextPool;

    private volatile SettingsManager settings;

    private RAMMemoryContext memory;
    private RAMDisassembler disassembler;
    private boolean debugTableInitialized = false;
    private int IP;

    public EmulatorImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);

        context = new RAMContext(this, contextPool);
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
        this.settings = settings;

        try {
            memory = (RAMMemoryContext) contextPool.getMemoryContext(getPluginID(), RAMMemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            // Will be processed later on
            throw new PluginInitializationException(this, "Could not get memory context", e);
        }

        if (memory.getDataType() != RAMInstruction.class) {
            throw new PluginInitializationException(
                this, "The RAM machine doesn't support this kind of program memory!"
            );
        }

        disassembler = new RAMDisassembler(this.memory);
        context.init(getPluginID());
    }

    // called from RAMContext after Input tape attachement
    public void loadTape(AbstractTapeContext tape) {
        List<String> data = memory.getInputs();
        if (data == null) {
            return;
        }

        int j = data.size();
        for (int i = 0; i < j; i++) {
            tape.setSymbolAt(i, data.get(i));
        }
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
        if (pos < 0) {
            return false;
        }
        IP = pos;
        return true;
    }

    @Override
    public int getInstructionPosition() {
        return IP;
    }

    public String getR0() {
        if (!context.checkTapes()) {
            return "<empty>";
        }
        return context.getStorage().getSymbolAt(0);
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    public void reset(int pos) {
        IP = pos;
        super.reset(pos);
        if (context.checkTapes()) {
            loadTape(context.getInput());
        }
    }

    @Override
    public void showSettings() {
        // no settings
    }

    @Override
    public RunState call() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (isBreakpointSet(IP)) {
                    throw new Breakpoint();
                }
                RunState tmpRunState = stepInternal();
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException e) {
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Breakpoint er) {
                return RunState.STATE_STOPPED_BREAK;
            }
        }
        return RunState.STATE_STOPPED_NORMAL;
    }

    public RunState stepInternal() {
        if (!context.checkTapes()) {
            return RunState.STATE_STOPPED_ADDR_FALLOUT;
        }

        RAMInstruction in = memory.read(IP++);
        if (in == null) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        switch (in.getCode()) {
            case RAMInstruction.READ:
                if (in.getDirection() == 0) {
                    String input = context.getInput().read();
                    context.getInput().moveRight();
                    context.getStorage().setSymbolAt((Integer) in.getOperand(),
                            input);
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        String input = context.getInput().read();
                        context.getInput().moveRight();
                        context.getStorage().setSymbolAt(M, input);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.WRITE:
                if (in.getDirection() == 0) {
                    context.getOutput().write(context.getStorage()
                            .getSymbolAt((Integer) in.getOperand()));
                    context.getOutput().moveRight();
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage()
                                .getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        context.getOutput().write(context.getStorage().getSymbolAt(M));
                        context.getOutput().moveRight();
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '=') {
                    context.getOutput().write(String.valueOf(in.getOperand()));
                    context.getOutput().moveRight();
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.LOAD:
                if (in.getDirection() == 0) {
                    context.getStorage().setSymbolAt(0, context.getStorage().getSymbolAt((Integer) in.getOperand()));
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        context.getStorage().setSymbolAt(0, context.getStorage().getSymbolAt(M));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '=') {
                    context.getStorage().setSymbolAt(0, (String) in.getOperand());
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.STORE:
                if (in.getDirection() == 0) {
                    context.getStorage().setSymbolAt((Integer) in.getOperand(),
                            context.getStorage().getSymbolAt(0));
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        context.getStorage().setSymbolAt(M, context.getStorage().getSymbolAt(0));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.ADD:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    // first try double values
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    // then integer (if double failed)
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        // first try double values
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                            break;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }
                        // then integer (if double failed)
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    // first try double values
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    // then integer (if double failed)
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.SUB:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                            break;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.MUL:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                            break;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.DIV:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        if (ri == 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                        break;
                    } catch (NumberFormatException e) {
                        // This really works (tested) for double numbers
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);

                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            if (ri == 0) {
                                break;
                            }
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                            break;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }

                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }

                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                        break;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                    break;
                }
                return RunState.STATE_STOPPED_BAD_INSTR;
            case RAMInstruction.JMP:
                IP = (Integer) in.getOperand();
                break;
            case RAMInstruction.JZ: {
                String r0 = context.getStorage().getSymbolAt(0);
                if (r0 == null || r0.equals("")) {
                    IP = (Integer) in.getOperand();
                    break;
                }
                int rr0 = 0;
                boolean t = false;
                try {
                    rr0 = Integer.decode(r0);
                    t = true;
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not parse number", e);
                }
                if (!t) {
                    try {
                        rr0 = (int) Double.parseDouble(r0);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        return RunState.STATE_STOPPED_BAD_INSTR;
                    }
                }
                if (rr0 == 0) {
                    IP = (Integer) in.getOperand();
                }
                break;
            }
            case RAMInstruction.JGTZ:
                try {
                    String r0 = context.getStorage().getSymbolAt(0);
                    int rr0 = 0;
                    boolean t = false;
                    try {
                        rr0 = Integer.decode(r0);
                        t = true;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    if (!t) {
                        try {
                            rr0 = (int) Double.parseDouble(r0);
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                            return RunState.STATE_STOPPED_BAD_INSTR;
                        }
                    }
                    if (rr0 > 0) {
                        IP = (Integer) in.getOperand();
                        break;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not parse number", e);
                    return RunState.STATE_STOPPED_BAD_INSTR;
                }
                break;
            case RAMInstruction.HALT:
                return RunState.STATE_STOPPED_NORMAL;
            default:
                return RunState.STATE_STOPPED_BAD_INSTR;
        }
        return RunState.STATE_STOPPED_BREAK;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }
}
