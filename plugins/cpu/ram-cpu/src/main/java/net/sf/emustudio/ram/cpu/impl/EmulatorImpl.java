/*
 * Copyright (C) 2009-2014 Peter Jakubčo
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
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.LoggerFactory;
import emulib.runtime.StaticDialogs;
import emulib.runtime.interfaces.Logger;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.JPanel;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;
import net.sf.emustudio.ram.cpu.gui.RAMDisassembler;
import net.sf.emustudio.ram.cpu.gui.RAMStatusPanel;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "Random Access Machine (RAM)",
copyright = "\u00A9 Copyright 2009-2014, Peter Jakubčo",
description = "Emulator of abstract RAM machine")
public class EmulatorImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorImpl.class);
    private RAMMemoryContext mem;
    private RAMContext context;
    private RAMDisassembler dis;     // disassembler
    private int IP; // instruction position

    public EmulatorImpl(Long pluginID) {
        super(pluginID);
        context = new RAMContext(this);
        try {
            ContextPool.getInstance().register(pluginID, context, CPUContext.class);
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
        super.initialize(settings);

        try {
            mem = (RAMMemoryContext) ContextPool.getInstance().getMemoryContext(pluginID,
                    RAMMemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            // Will be processed later on
            throw new PluginInitializationException(
                this, "Could not get memory context", e
            );
        }

        if (mem.getDataType() != RAMInstruction.class) {
            throw new PluginInitializationException(
                this, "The RAM machine doesn't support this kind of program memory!"
            );
        }

        dis = new RAMDisassembler(this.mem);
        context.init(pluginID);
    }

    // called from RAMContext after Input tape attachement
    public void loadTape(AbstractTapeContext tape) {
        List<String> data = mem.getInputs();
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
        return new RAMStatusPanel(this, mem);
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
    public void destroy() {
        runState = RunState.STATE_STOPPED_NORMAL;
        context.destroy();
        context = null;
        breaks.clear();
        breaks = null;
    }

    @Override
    public void reset(int pos) {
        super.reset(pos);
        IP = pos;
        notifyStateChanged(runState);

        if (context.checkTapes()) {
            loadTape(context.getInput());
        }
    }

    @Override
    public void pause() {
        runState = RunState.STATE_STOPPED_BREAK;
        notifyStateChanged(runState);
    }

    @Override
    public void stop() {
        runState = RunState.STATE_STOPPED_NORMAL;
        notifyStateChanged(runState);
    }

    @Override
    public void step() {
        if (runState == RunState.STATE_STOPPED_BREAK) {
            try {
                runState = RunState.STATE_RUNNING;
                emulateInstruction();
                if (runState == RunState.STATE_RUNNING) {
                    runState = RunState.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException e) {
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            notifyStateChanged(runState);
        }
    }

    @Override
    public void showSettings() {
        // no settings
    }

    @Override
    public void run() {
        runState = RunState.STATE_RUNNING;
        notifyStateChanged(runState);

        while (runState == RunState.STATE_RUNNING) {
            try {
                if (isBreakpointSet(IP)) {
                    throw new Error();
                }
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
                break;
            } catch (Error er) {
                runState = RunState.STATE_STOPPED_BREAK;
                break;
            }
        }
        notifyStateChanged(runState);
    }

    private void emulateInstruction() {
        if (!context.checkTapes()) {
            runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            return;
        }

        RAMInstruction in = (RAMInstruction) mem.read(IP++);
        if (in == null) {
            runState = RunState.STATE_STOPPED_BAD_INSTR;
            return;
        }
        switch (in.getCode()) {
            case RAMInstruction.READ:
                if (in.getDirection() == 0) {
                    String input = (String) context.getInput().read();
                    context.getInput().moveRight();
                    context.getStorage().setSymbolAt((Integer) in.getOperand(),
                            input);
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        String input = (String) context.getInput().read();
                        context.getInput().moveRight();
                        context.getStorage().setSymbolAt(M, input);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                }
                break;
            case RAMInstruction.WRITE:
                if (in.getDirection() == 0) {
                    context.getOutput().write(context.getStorage()
                            .getSymbolAt((Integer) in.getOperand()));
                    context.getOutput().moveRight();
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage()
                                .getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        context.getOutput().write(context.getStorage().getSymbolAt(M));
                        context.getOutput().moveRight();
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    context.getOutput().write(String.valueOf(in.getOperand()));
                    context.getOutput().moveRight();
                    return;
                }
                break;
            case RAMInstruction.LOAD:
                if (in.getDirection() == 0) {
                    context.getStorage().setSymbolAt(0, context.getStorage().getSymbolAt((Integer) in.getOperand()));
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, context.getStorage().getSymbolAt(M));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    context.getStorage().setSymbolAt(0, (String) in.getOperand());
                    return;
                }
                break;
            case RAMInstruction.STORE:
                if (in.getDirection() == 0) {
                    context.getStorage().setSymbolAt((Integer) in.getOperand(),
                            context.getStorage().getSymbolAt(0));
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(M, context.getStorage().getSymbolAt(0));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                }
                break;
            case RAMInstruction.ADD:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    // first try double values
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                        return;
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
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        // first try double values
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                            return;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }
                        // then integer (if double failed)
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    // first try double values
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                        return;
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
                        break;
                    }
                    return;
                }
                break;
            case RAMInstruction.SUB:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                        return;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                            return;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                        return;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                }
                break;
            case RAMInstruction.MUL:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                        return;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                            return;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                        return;
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
                }
                break;
            case RAMInstruction.DIV:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                        return;
                    } catch (NumberFormatException e) {
                        // This really works (tested) for double numbers
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
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
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
                            return;
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                        }

                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            break;
                        }

                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                    return;
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
                        return;
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
                        break;
                    }
                    return;
                }
                break;
            case RAMInstruction.JMP:
                IP = (Integer) in.getOperand();
                return;
            case RAMInstruction.JZ: {
                String r0 = context.getStorage().getSymbolAt(0);
                if (r0 == null || r0.equals("")) {
                    IP = (Integer) in.getOperand();
                    return;
                }
                int rr0 = 0;
                boolean t = false;
                try {
                    rr0 = Integer.decode(r0);
                    t = true;
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not parse number", e);
                }
                if (t == false) {
                    try {
                        rr0 = (int) Double.parseDouble(r0);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not parse number", e);
                        break;
                    }
                }
                if (rr0 == 0) {
                    IP = (Integer) in.getOperand();
                    return;
                }
                return;
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
                    if (t == false) {
                        try {
                            rr0 = (int) Double.parseDouble(r0);
                        } catch (NumberFormatException e) {
                            LOGGER.error("Could not parse number", e);
                            break;
                        }
                    }
                    if (rr0 > 0) {
                        IP = (Integer) in.getOperand();
                        return;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not parse number", e);
                    break;
                }
                return;
            case RAMInstruction.HALT:
                runState = RunState.STATE_STOPPED_NORMAL;
                return;
        }
        runState = RunState.STATE_STOPPED_BAD_INSTR;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public Disassembler getDisassembler() {
        return dis;
    }
}
