/*
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2014 Peter Jakubčo
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
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.braincpu.gui.DecoderImpl;
import net.sf.emustudio.braincpu.gui.DisassemblerImpl;
import net.sf.emustudio.brainduck.cpu.BrainCPUContext;
import net.sf.emustudio.brainduck.cpu.gui.StatusPanel;

import javax.swing.JPanel;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "BrainCPU",
copyright = "\u00A9 Copyright 2009-2014, Peter Jakubčo",
description = "Emulator of CPU for abstract BrainDuck architecture")
public class EmulatorImpl extends AbstractCPU {
    private final ContextPool contextPool;
    private final BrainCPUContextImpl context = new BrainCPUContextImpl();
    private final Deque<Integer> loopPointers = new LinkedList<>();

    private Disassembler disassembler;
    private MemoryContext<Short> memory;
    private volatile int IP, P; // registers of the CPU
    private int memorySize; // cached memory size

    // optimization
    private final Map<Integer, Integer> loopEndsCache = new HashMap<>();
    private volatile boolean optimize;
    private final Map<Integer, OperationCache> operationsCache = new HashMap<>();

    private static class OperationCache {
        public int argument;
        public int nextIP;
        public short operation;
    }

    public EmulatorImpl(Long pluginID, ContextPool contextPool) {
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
        try {
            memory = contextPool.getMemoryContext(getPluginID(), MemoryContext.class);

            if (memory.getDataType() != Short.class) {
                throw new PluginInitializationException(
                        this, "Selected operating memory is not supported."
                );
            }
            memorySize = memory.getSize();
            disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
        } catch (InvalidContextException | ContextNotFoundException e) {
            throw new PluginInitializationException(this, "Could not get memory context", e);
        }
    }

    @Override
    public int getInstructionPosition() {
        return IP;
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
    public JPanel getStatusPanel() {
        return new StatusPanel(memory, this);
    }

    @Override
    public void showSettings() {
        // We don't use any GUI for the settings
    }

    public int getP() {
        return P;
    }
    
    public int getLoopLevel() {
        return loopPointers.size();
    }

    @Override
    public void reset(int adr) {
        super.reset(adr);

        IP = adr; // initialize program counter
        loopPointers.clear();
        loopEndsCache.clear();
        operationsCache.clear();
        optimize = false;

        // find closest "free" address which does not contain a program
        try {
            while (memory.read(adr++) != 0) {
            }
        } catch (IndexOutOfBoundsException e) {
            // we get here if "adr" would point to nonexistant memory location,
            // ie. when we go through all memory to the end without a result
            adr = 0;
        }
        P = adr; // assign to the P register the address we have found
        analyzeForOptimization(adr);
    }

    private void analyzeForOptimization(int programSize) {
        int lastOperation = 0;
        short OP;

        for (int tmpIP = 0; tmpIP < programSize; tmpIP++) {
            OP = memory.read(tmpIP);
            if (OP != 0 && OP != 7 && OP != 8 && (lastOperation == OP)) {
                int previousIP = tmpIP - 1;
                OperationCache operation = new OperationCache();

                operation.operation = OP;
                operation.argument = 2;

                while ((tmpIP+1) < programSize && (memory.read(tmpIP+1) == lastOperation)) {
                    operation.argument++;
                    tmpIP++;
                }
                operation.nextIP = tmpIP + 1;
                operationsCache.put(previousIP, operation);
            }
            lastOperation = OP;
        }
    }


    @Override
    public RunState call() {
        optimize = true;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isBreakpointSet(IP)) {
                        throw new Breakpoint();
                    }
                    RunState tmpRunState = stepInternal();
                    if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                        return tmpRunState;
                    }
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
    protected RunState stepInternal() {
        short OP;

        // FETCH
        int argument = 1;

        if (optimize && operationsCache.containsKey(IP)) {
            OperationCache operation = operationsCache.get(IP);
            OP = operation.operation;
            IP = operation.nextIP;
            argument = operation.argument;
        } else {
            OP = memory.read(IP++);
        }

        // DECODE
        switch (OP) {
            case 0: /* ; */
                return RunState.STATE_STOPPED_NORMAL;
            case 1: /* >  */
                P += argument;
                if (P > memorySize) {
                    return RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                break;
            case 2: /* < */
                P -= argument;
                if (P < 0) {
                    return RunState.STATE_STOPPED_ADDR_FALLOUT;
                }
                break;
            case 3: /* + */
                memory.write(P, (short) (memory.read(P) + argument));
                break;
            case 4: /* - */
                memory.write(P, (short) (memory.read(P) - argument));
                break;
            case 5: /* . */
                while (argument > 0) {
                    context.writeToDevice(memory.read(P));
                    argument--;
                }
                break;
            case 6: /* , */
                while (argument > 0) {
                    memory.write(P, context.readFromDevice());
                    argument--;
                }
                break;
            case 7: /* [ */
                int startingBrace = IP - 1;
                if (memory.read(P) != 0) {
                    loopPointers.push(startingBrace);
                    break;
                }
                if (loopEndsCache.containsKey(startingBrace)) {
                    IP = loopEndsCache.get(startingBrace);
                } else {
                    int loop_count = 0; // loop nesting level counter

                    // we start to look for "]" instruction
                    // on the same nesting level (according to loop_count value)
                    // IP is pointing at following instruction
                    while ((OP = memory.read(IP++)) != 0) {
                        if (OP == 7) {
                            loop_count++;
                        }
                        if (OP == 8) {
                            if (loop_count == 0) {
                                loopEndsCache.put(startingBrace, IP);
                                break;
                            } else {
                                loop_count--;
                            }
                        }
                    }
                }
                break;
            case 8: /* ] */
                int tmpIP = loopPointers.pop();
                if (memory.read(P) != 0) {
                    IP = tmpIP;
                }
                break;
            default: /* invalid instruction */
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
