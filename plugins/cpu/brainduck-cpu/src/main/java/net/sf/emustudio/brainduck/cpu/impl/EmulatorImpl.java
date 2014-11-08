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
import net.sf.emustudio.brainduck.cpu.gui.BrainStatusPanel;

import javax.swing.*;
import java.util.Deque;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@PluginType(type = PLUGIN_TYPE.CPU,
title = "BrainCPU",
copyright = "\u00A9 Copyright 2009-2014, Peter Jakubčo",
description = "Emulator of CPU for abstract BrainDuck architecture")
public class EmulatorImpl extends AbstractCPU {
    private final BrainCPUContextImpl context;
    private final Deque<Integer> loopPointers = new LinkedList<>();

    private Disassembler disassembler;
    private MemoryContext<Short> memory;
    private volatile int IP, P; // registers of the CPU

    public EmulatorImpl(Long pluginID) {
        super(pluginID);
        context = new BrainCPUContextImpl();
        try {
            ContextPool.getInstance().register(pluginID, context, BrainCPUContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context",
                    EmulatorImpl.class.getAnnotation(PluginType.class).title());
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
        super.initialize(settings);
        try {
            memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);

            if (memory.getDataType() != Short.class) {
                throw new PluginInitializationException(
                        this, "Selected operating memory is not supported."
                );
            }
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
        return new BrainStatusPanel(this, memory);
    }

    @Override
    public void showSettings() {
        // We don't use any GUI for the settings
    }

    public int getP() {
        return P;
    }

    public int getIP() {
        return IP;
    }

    @Override
    public void reset(int adr) {
        super.reset(adr);
        IP = adr; // initialize program counter
        loopPointers.clear();

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
    }

    @Override
    public void run() {
        // here we are emulating in endless loop until an event stops it.
        // Externally, it might be the user, internally invalid instruction
        // or nonexistant memory location (address fallout)
        while (getRunState() == RunState.STATE_RUNNING) {
            try {
                // if a breakpoint is set on the address pointed by IP register
                // we throw new Breakpoint instance.
                if (isBreakpointSet(IP) == true) {
                    throw new Breakpoint();
                }
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
                // we get here if IP registers would point at nonexistant memory
                setRunState(RunState.STATE_STOPPED_ADDR_FALLOUT);
                break;
            } catch (Breakpoint er) {
                // we get here if a Breakpoint was thrown.
                setRunState(RunState.STATE_STOPPED_BREAK);
                break;
            }
        }
    }

    @Override
    protected void stepInternal() {
        emulateInstruction();
    }

    @Override
    public void destroy() {
        setRunState(RunState.STATE_STOPPED_NORMAL);
    }

    /**
     * This method emulates a single instruction.
     */
    private void emulateInstruction() {
        short OP, param;

        // FETCH
        OP = memory.read(IP++);

        // DECODE
        switch (OP) {
            case 0: /* ; */
                setRunState(RunState.STATE_STOPPED_NORMAL);
                return;
            case 1: /* >  */
                P++;
                return;
            case 2: /* < */
                P--;
                return;
            case 3: /* + */
                memory.write(P, (short) (memory.read(P) + 1));
                return;
            case 4: /* - */
                memory.write(P, (short) (memory.read(P) - 1));
                return;
            case 5: /* . */
                context.writeToDevice(memory.read(P));
                return;
            case 6: /* , */
                memory.write(P, context.readFromDevice());
                return;
            case 7: { /* [ */
                loopPointers.push(IP -1);
                if (memory.read(P) != 0) {
                    return;
                }
                int loop_count = 0; // loop nesting level counter

                // we start to look for "endl" instruction
                // on the same nesting level (according to loop_count value)
                // IP is pointing at following instruction
                while ((OP = memory.read(IP++)) != 0) {
                    if (OP == 7) {
                        loop_count++;
                    }
                    if (OP == 8) {
                        if (loop_count == 0) {
                            return;
                        } else {
                            loop_count--;
                        }
                    }
                }
                break;
            }
            case 8: /* ] */
                int tmpIP = loopPointers.pop();
                if (memory.read(P) != 0) {
                    IP = tmpIP;
                }
                return;
            default: /* invalid instruction */
                break;
        }
        setRunState(RunState.STATE_STOPPED_BAD_INSTR);
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
