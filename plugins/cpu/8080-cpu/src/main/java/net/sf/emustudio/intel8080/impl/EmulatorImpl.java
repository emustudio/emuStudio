/*
 * EmulatorImpl.java
 *
 * Implementation of CPU emulation
 *
 * Created on Piatok, 2007, oktober 26, 10:45
 *
 * Copyright (C) 2007-2013 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR regs[REG_A] PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.LoggerFactory;
import emulib.runtime.StaticDialogs;
import emulib.runtime.interfaces.Logger;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JPanel;
import net.sf.emustudio.intel8080.ExtendedContext;
import net.sf.emustudio.intel8080.FrequencyChangedListener;
import net.sf.emustudio.intel8080.gui.DecoderImpl;
import net.sf.emustudio.intel8080.gui.DisassemblerImpl;
import net.sf.emustudio.intel8080.gui.StatusPanel;

/**
 * Main implementation class for CPU emulation CPU works in a separate thread (parallel with other hardware)
 *
 * @author Peter Jakubčo
 */
@PluginType(type = PLUGIN_TYPE.CPU,
title = "Intel 8080 CPU",
copyright = "\u00A9 Copyright 2007-2013, Peter Jakubčo",
description = "Emulator of Intel 8080 CPU")
public class EmulatorImpl extends AbstractCPU {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorImpl.class);

    public static final int REG_A = 7;
    public static final int REG_B = 0;
    public static final int REG_C = 1;
    public static final int REG_D = 2;
    public static final int REG_E = 3;
    public static final int REG_H = 4;
    public static final int REG_L = 5;

    private static final byte PARITY[] = {
        1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1,
        1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0,
        1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0,
        1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1,
        1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 1, 1, 0,
        1, 0, 0, 1
    };

    private final StatusPanel statusPanel;
    private MemoryContext<Short> memory;
    private ContextImpl context;
    private Disassembler disasm;
    // cpu speed
    private long executedCycles = 0; // count of executed cycles for frequency calculation
    private final java.util.Timer frequencyScheduler;
    private FrequencyUpdater frequencyUpdater;
    private int checkTimeSlice = 100;
    // registers are public meant for only StatusPanel (didnt want make it thru getters)
    private int PC = 0; // program counter
    public int SP = 0; // stack pointer
    public short[] regs = new short[8];
    public short Flags = 2; // registers
    public static final int flagS = 0x80, flagZ = 0x40, flagAC = 0x10, flagP = 0x4, flagC = 0x1;
    private boolean INTE = false; // enabling / disabling of interrupts
    private boolean isINT = false;
    private short b1 = 0; // the raw interrupt instruction
    private short b2 = 0;
    private short b3 = 0;

    /**
     * This class performs runtime frequency calculation and updating.
     *
     * Given: time, executed cycles count Frequency is defined as number of something by some period of time. Hz = 1/s,
     * kHz = 1000/s time has to be in seconds
     *
     * CC ..by.. time[s] XX ..by.. 1 [s] ? --------------- XX:CC = 1:time XX = CC / time [Hz]
     */
    private class FrequencyUpdater extends TimerTask {

        private long startTimeSaved = 0;
        private float frequency;

        @Override
        public void run() {
            double endTime = System.nanoTime();
            double time = endTime - startTimeSaved;

            if (executedCycles == 0) {
                return;
            }
            frequency = (float) (executedCycles / (time / 1000000.0));
            startTimeSaved = (long) endTime;
            executedCycles = 0;
            fireFrequencyChanged(frequency);
        }
    }

    /**
     * Creates a new instance of EmulatorImpl.
     *
     * @param pluginID plugin unique ID
     */
    public EmulatorImpl(Long pluginID) {
        super(pluginID);
        context = new ContextImpl(this);
        try {
            ContextPool.getInstance().register(pluginID, context, ExtendedContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register CPU Context",
                    EmulatorImpl.class.getAnnotation(PluginType.class).title());
        }
        statusPanel = new StatusPanel(this, context);
        frequencyUpdater = new FrequencyUpdater();
        frequencyScheduler = new Timer();
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.intel8080.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);
        try {
            this.memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);

            if (memory.getDataType() != Short.class) {
                StaticDialogs.showErrorMessage("Operating memory type is not supported for this kind of CPU.");
                return false;
            }

            // create disassembler and debug columns
            disasm = new DisassemblerImpl(memory, new DecoderImpl(memory));

            return true;
        } catch (InvalidContextException | ContextNotFoundException e) {
            StaticDialogs.showErrorMessage("Could not get memory context",
                    EmulatorImpl.class.getAnnotation(PluginType.class).title());
            return false;
        }
    }

    @Override
    public void destroy() {
        runState = RunState.STATE_STOPPED_NORMAL;
        stopFrequencyUpdater();
        if (context != null) {
            context.clearDevices();
            context = null;
        }
    }

    @Override
    public void reset(int startPos) {
        super.reset(startPos);
        Arrays.fill(regs, (short)0);
        SP = 0;
        Flags = 2; //0000 0010b
        PC = startPos;
        INTE = false;
        stopFrequencyUpdater();
        notifyStateChanged(runState);
    }

    /**
     * Force (external) breakpoint
     */
    @Override
    public void pause() {
        runState = RunState.STATE_STOPPED_BREAK;
        stopFrequencyUpdater();
        notifyStateChanged(runState);
    }

    /**
     * Stops an emulation
     */
    @Override
    public void stop() {
        runState = RunState.STATE_STOPPED_NORMAL;
        stopFrequencyUpdater();
        notifyStateChanged(runState);
    }

    // vykona 1 krok - bez merania casov (bez real-time odozvy)
    @Override
    public void step() {
        if (runState == RunState.STATE_STOPPED_BREAK) {
            try {
                runState = RunState.STATE_RUNNING;
                dispatch();
                if (runState == RunState.STATE_RUNNING) {
                    runState = RunState.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException e) {
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                runState = RunState.STATE_STOPPED_BAD_INSTR;
            }
            notifyStateChanged(runState);
        }
    }

    public void interrupt(short b1, short b2, short b3) {
        if (INTE == false) {
            return;
        }
        isINT = true;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }

    private void fireFrequencyChanged(float newFrequency) {
        for (CPUListener listener : stateObservers) {
            if (listener instanceof FrequencyChangedListener) {
                ((FrequencyChangedListener) listener).frequencyChanged(newFrequency);
            }
        }
    }

    /* DOWN: GUI interaction */
    @Override
    public JPanel getStatusPanel() {
        return statusPanel;
    }

    /* DOWN: other */
    public int getSliceTime() {
        return checkTimeSlice;
    }

    public void setSliceTime(int t) {
        checkTimeSlice = t;
    }

    private void stopFrequencyUpdater() {
        try {
            frequencyUpdater.cancel();
            frequencyUpdater = new FrequencyUpdater();
        } catch (Exception e) {
        }
    }

    private void runFrequencyUpdater() {
        try {
            frequencyScheduler.purge();
            frequencyScheduler.scheduleAtFixedRate(frequencyUpdater, 0, 2*checkTimeSlice);
        } catch (Exception e) {
        }
    }

    /**
     * Run a CPU execution (thread).
     *
     * Real-time CPU frequency balancing *********************************
     *
     * 1 cycle is performed in 1 periode of CPU frequency. CPU_PERIODE = 1 / CPU_FREQ [micros]
     * cycles_to_execute_per_second = 1000 / CPU_PERIODE
     *
     * cycles_to_execute_per_second = 1000 / (1/CPU_FREQ) cycles_to_execute_per_second = 1000 * CPU_FREQ
     *
     * 1000 s = 1 micros => slice_length (can vary)
     *
     */
    @Override
    public void run() {
        long startTime, endTime;
        int cycles_executed;
        int cycles_to_execute; // per second
        int cycles;
        long slice;

        runState = RunState.STATE_RUNNING;
        notifyStateChanged(runState);
        runFrequencyUpdater();
        /* 1 Hz  .... 1 tState per second
         * 1 kHz .... 1000 tStates per second
         * clockFrequency is in kHz it have to be multiplied with 1000
         */
        cycles_to_execute = checkTimeSlice * context.getCPUFrequency();
        long i = 0;
        slice = checkTimeSlice * 1000000;
        while (runState == RunState.STATE_RUNNING) {
            i++;
            startTime = System.nanoTime();
            cycles_executed = 0;
            while ((cycles_executed < cycles_to_execute)
                    && (runState == RunState.STATE_RUNNING)) {
                try {
                    cycles = dispatch();
                    cycles_executed += cycles;
                    executedCycles += cycles;
                    if (isBreakpointSet(PC) == true) {
                        throw new Error();
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    runState = RunState.STATE_STOPPED_BAD_INSTR;
                } catch (IndexOutOfBoundsException e) {
                    runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
                    break;
                } catch (Error er) {
                    runState = RunState.STATE_STOPPED_BREAK;
                    break;
                }
            }
            endTime = System.nanoTime() - startTime;
            if (endTime < slice) {
                // time correction
                try {
                    Thread.sleep((slice - endTime) / 1000000);
                } catch (java.lang.InterruptedException e) {
                }
            }
        }
        stopFrequencyUpdater();
        notifyStateChanged(runState);
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public Disassembler getDisassembler() {
        return disasm;
    }

    @Override
    public int getInstructionPosition() {
        return PC;
    }

    @Override
    public boolean setInstructionPosition(int position) {
        if (position < 0) {
            return false;
        }
        PC = position;
        return true;
    }

    /* Get an 8080 register and return it */
    private short getreg(int reg) {
        if (reg == 6) {
            return ((Short) memory.read((regs[REG_H] << 8) | regs[REG_L])).shortValue();
        }
        return regs[reg];
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, short val) {
        if (reg == 6) {
            memory.write((regs[REG_H] << 8) | regs[REG_L], val);
        } else {
            regs[reg] = val;
        }
    }

    /* Put a value into an 8080 register pair */
    void putpair(int reg, int val) {
        short high, low;
        high = (short) ((val >>> 8) & 0xFF);
        low = (short) (val & 0xFF);
        switch (reg) {
            case 0:
                regs[REG_B] = high;
                regs[REG_C] = low;
                break;
            case 1:
                regs[REG_D] = high;
                regs[REG_E] = low;
                break;
            case 2:
                regs[REG_H] = high;
                regs[REG_L] = low;
                break;
            case 3:
                SP = val;
                break;
        }
    }

    /* Return the value of a selected register pair */
    int getpair(int reg) {
        switch (reg) {
            case 0:
                return (regs[REG_B] << 8) | regs[REG_C];
            case 1:
                return (regs[REG_D] << 8) | regs[REG_E];
            case 2:
                return (regs[REG_H] << 8) | regs[REG_L];
            case 3:
                return SP;
        }
        return 0;
    }

    /* Return the value of a selected register pair, in PUSH
     format where 3 means regs[REG_A]& flags, not SP
     */
    private int getpush(int reg) {
        int stat;
        switch (reg) {
            case 0:
                return (regs[REG_B] << 8) | regs[REG_C];
            case 1:
                return (regs[REG_D] << 8) | regs[REG_E];
            case 2:
                return (regs[REG_H] << 8) | regs[REG_L];
            case 3:
                stat = (regs[REG_A] << 8) | Flags;
                stat |= 0x02;
                return stat;
        }
        return 0;
    }

    /* Place data into the indicated register pair, in PUSH
     format where 3 means regs[REG_A]& flags, not SP
     */
    private void putpush(int reg, int val) {
        short high, low;
        high = (short) ((val >>> 8) & 0xFF);
        low = (short) (val & 0xFF);
        switch (reg) {
            case 0:
                regs[REG_B] = high;
                regs[REG_C] = low;
                break;
            case 1:
                regs[REG_D] = high;
                regs[REG_E] = low;
                break;
            case 2:
                regs[REG_H] = high;
                regs[REG_L] = low;
                break;
            case 3:
                regs[REG_A] = (short) ((val >>> 8) & 0xFF);
                Flags = (short) (val & 0xFF);
                break;
        }
    }

    /* Set the <C>arry, <S>ign, <Z>ero and <P>arity flags following
     an arithmetic operation on 'reg'. 8080 changes AC flag
     */
    private void setarith(int reg, int before) {
        if ((reg & 0x100) != 0) {
            Flags |= flagC;
        } else {
            Flags &= (~flagC);
        }
        if ((reg & 0x80) != 0) {
            Flags |= flagS;
        } else {
            Flags &= (~flagS);
        }
        if ((reg & 0xff) == 0) {
            Flags |= flagZ;
        } else {
            Flags &= (~flagZ);
        }
        // carry from 3.bit to 4.
        if (((before & 8) == 8) && ((reg & 0x10) == 0x10)) {
            Flags |= flagAC;
        } else {
            Flags &= (~flagAC);
        }
        parity(reg);
    }

    /* Set the <S>ign, <Z>ero amd <P>arity flags following
     an INR/DCR operation on 'reg'.
     */
    private void setinc(int reg, int before) {
        if ((reg & 0x80) != 0) {
            Flags |= flagS;
        } else {
            Flags &= (~flagS);
        }
        if ((reg & 0xff) == 0) {
            Flags |= flagZ;
        } else {
            Flags &= (~flagZ);
        }
        if (((before & 8) == 8) && ((reg & 0x10) == 0x10)) {
            Flags |= flagAC;
        } else {
            Flags &= (~flagAC);
        }
        parity(reg);
    }

    /* Set the <C>arry, <S>ign, <Z>ero amd <P>arity flags following
     a logical (bitwise) operation on 'reg'.
     */
    private void setlogical(int reg) {
        Flags &= (~flagC);
        if ((reg & 0x80) != 0) {
            Flags |= flagS;
        } else {
            Flags &= (~flagS);
        }
        if ((reg & 0xff) == 0) {
            Flags |= flagZ;
        } else {
            Flags &= (~flagZ);
        }
        Flags &= (~flagAC);
        parity(reg);
    }

    /* Set the Parity (P) flag based on parity of 'reg', i.e., number
     of bits on even: P=0200000, else P=0
     */
    private void parity(int reg) {
        if (PARITY[reg & 0xFF] == 1) {
            Flags |= flagP;
        } else {
            Flags &= (~flagP);
        }
    }

    /* Test an 8080 flag condition and return 1 if true, 0 if false */
    private boolean checkCondition(int con) {
        switch (con) {
            case 0:
                return ((Flags & flagZ) == 0);
            case 1:
                return ((Flags & flagZ) != 0);
            case 2:
                return ((Flags & flagC) == 0);
            case 3:
                return ((Flags & flagC) != 0);
            case 4:
                return ((Flags & flagP) == 0);
            case 5:
                return ((Flags & flagP) != 0);
            case 6:
                return ((Flags & flagS) == 0);
            case 7:
                return ((Flags & flagS) != 0);
        }
        return false;
    }

    private static final Method[] dispatchTable = new Method[256];

    static {
        try {
            dispatchTable[0] = EmulatorImpl.class.getDeclaredMethod("O0_NOP", new Class[]{short.class});
            dispatchTable[6] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[7] = EmulatorImpl.class.getDeclaredMethod("O7_RLC", new Class[]{short.class});
            dispatchTable[14] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[15] = EmulatorImpl.class.getDeclaredMethod("O15_RRC", new Class[]{short.class});
            dispatchTable[22] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[23] = EmulatorImpl.class.getDeclaredMethod("O23_RAL", new Class[]{short.class});
            dispatchTable[30] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[31] = EmulatorImpl.class.getDeclaredMethod("O31_RAR", new Class[]{short.class});
            dispatchTable[34] = EmulatorImpl.class.getDeclaredMethod("O34_SHLD", new Class[]{short.class});
            dispatchTable[38] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[39] = EmulatorImpl.class.getDeclaredMethod("O39_DAA", new Class[]{short.class});
            dispatchTable[42] = EmulatorImpl.class.getDeclaredMethod("O42_LHLD", new Class[]{short.class});
            dispatchTable[46] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[47] = EmulatorImpl.class.getDeclaredMethod("O47_CMA", new Class[]{short.class});
            dispatchTable[50] = EmulatorImpl.class.getDeclaredMethod("O50_STA", new Class[]{short.class});
            dispatchTable[54] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[55] = EmulatorImpl.class.getDeclaredMethod("O55_STC", new Class[]{short.class});
            dispatchTable[58] = EmulatorImpl.class.getDeclaredMethod("O58_LDA", new Class[]{short.class});
            dispatchTable[62] = EmulatorImpl.class.getDeclaredMethod("MC7_O6_MVI", new Class[]{short.class});
            dispatchTable[63] = EmulatorImpl.class.getDeclaredMethod("O63_CMC", new Class[]{short.class});
            dispatchTable[64] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[65] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[66] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[67] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[68] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[69] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[70] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[71] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[72] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[73] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[74] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[75] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[76] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[77] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[78] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[79] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[80] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[81] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[82] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[83] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[84] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[85] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[86] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[87] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[88] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[89] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[90] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[91] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[92] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[93] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[94] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[95] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[96] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[97] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[98] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[99] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[100] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[101] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[102] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[103] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[104] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[105] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[106] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[107] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[108] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[109] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[110] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[111] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[112] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[113] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[114] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[115] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[116] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[117] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[118] = EmulatorImpl.class.getDeclaredMethod("O118_HLT", new Class[]{short.class});
            dispatchTable[119] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[120] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[121] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[122] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[123] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[124] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[125] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[126] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[127] = EmulatorImpl.class.getDeclaredMethod("MC0_O40_MOV", new Class[]{short.class});
            dispatchTable[195] = EmulatorImpl.class.getDeclaredMethod("O195_JMP", new Class[]{short.class});
            dispatchTable[198] = EmulatorImpl.class.getDeclaredMethod("O198_ADI", new Class[]{short.class});
            dispatchTable[201] = EmulatorImpl.class.getDeclaredMethod("O201_RET", new Class[]{short.class});
            dispatchTable[205] = EmulatorImpl.class.getDeclaredMethod("O205_CALL", new Class[]{short.class});
            dispatchTable[206] = EmulatorImpl.class.getDeclaredMethod("O206_ACI", new Class[]{short.class});
            dispatchTable[211] = EmulatorImpl.class.getDeclaredMethod("O211_OUT", new Class[]{short.class});
            dispatchTable[214] = EmulatorImpl.class.getDeclaredMethod("O214_SUI", new Class[]{short.class});
            dispatchTable[219] = EmulatorImpl.class.getDeclaredMethod("O219_IN", new Class[]{short.class});
            dispatchTable[222] = EmulatorImpl.class.getDeclaredMethod("O222_SBI", new Class[]{short.class});
            dispatchTable[227] = EmulatorImpl.class.getDeclaredMethod("O227_XTHL", new Class[]{short.class});
            dispatchTable[230] = EmulatorImpl.class.getDeclaredMethod("O230_ANI", new Class[]{short.class});
            dispatchTable[233] = EmulatorImpl.class.getDeclaredMethod("O233_PCHL", new Class[]{short.class});
            dispatchTable[235] = EmulatorImpl.class.getDeclaredMethod("O235_XCHG", new Class[]{short.class});
            dispatchTable[238] = EmulatorImpl.class.getDeclaredMethod("O238_XRI", new Class[]{short.class});
            dispatchTable[243] = EmulatorImpl.class.getDeclaredMethod("O243_DI", new Class[]{short.class});
            dispatchTable[246] = EmulatorImpl.class.getDeclaredMethod("O246_ORI", new Class[]{short.class});
            dispatchTable[249] = EmulatorImpl.class.getDeclaredMethod("O249_SPHL", new Class[]{short.class});
            dispatchTable[251] = EmulatorImpl.class.getDeclaredMethod("O251_EI", new Class[]{short.class});
            dispatchTable[254] = EmulatorImpl.class.getDeclaredMethod("O254_CPI", new Class[]{short.class});
        } catch (NoSuchMethodException e) {
            LOGGER.error("Could not set up dispatch table. The emulator won't work correctly", e);
        }
    }

    private int O0_NOP(short OP) {
        return 4;
    }

    private int O7_RLC(short OP) {
        int xx = (regs[REG_A] << 9) & 0200000;
        if (xx != 0) {
            Flags |= flagC;
        } else {
            Flags &= (~flagC);
        }
        regs[REG_A] = (short) ((regs[REG_A] << 1) & 0xFF);
        if (xx != 0) {
            regs[REG_A] |= 0x01;
        }
        return 4;
    }

    private int O15_RRC(short OP) {
        if ((regs[REG_A] & 0x01) == 1) {
            Flags |= flagC;
        } else {
            Flags &= (~flagC);
        }
        regs[REG_A] = (short) ((regs[REG_A] >>> 1) & 0xFF);
        if ((Flags & flagC) != 0) {
            regs[REG_A] |= 0x80;
        }
        return 4;
    }

    private int O23_RAL(short OP) {
        int xx = (regs[REG_A] << 9) & 0200000;
        regs[REG_A] = (short) ((regs[REG_A] << 1) & 0xFF);
        if ((Flags & flagC) != 0) {
            regs[REG_A] |= 1;
        } else {
            regs[REG_A] &= 0xFE;
        }
        if (xx != 0) {
            Flags |= flagC;
        } else {
            Flags &= (~flagC);
        }
        return 4;
    }

    private int O31_RAR(short OP) {
        int xx = 0;
        if ((regs[REG_A] & 0x01) == 1) {
            xx |= 0200000;
        }
        regs[REG_A] = (short) ((regs[REG_A] >>> 1) & 0xFF);
        if ((Flags & flagC) != 0) {
            regs[REG_A] |= 0x80;
        } else {
            regs[REG_A] &= 0x7F;
        }
        if (xx != 0) {
            Flags |= flagC;
        } else {
            Flags &= (~flagC);
        }
        return 4;
    }

    private int O34_SHLD(short OP) {
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        memory.writeWord(DAR, (regs[REG_H] << 8) | regs[REG_L]);
        return 16;
    }

    private int O39_DAA(short OP) {
        int DAR = regs[REG_A];
        if (((DAR & 0x0F) > 9) || ((Flags & flagAC) != 0)) {
            DAR += 6;
            if ((DAR & 0x10) != (regs[REG_A] & 0x10)) {
                Flags |= flagAC;
            } else {
                Flags &= (~flagAC);
            }
            regs[REG_A] = (short) (DAR & 0xFF);
        }
        DAR = (regs[REG_A] >>> 4) & 0x0F;
        if ((DAR > 9) || ((Flags & flagC) != 0)) {
            DAR += 6;
            if ((DAR & 0x10) != 0) {
                Flags |= flagC;
            }
            regs[REG_A] &= 0x0F;
            regs[REG_A] |= ((DAR << 4) & 0xF0);
        }
        if ((regs[REG_A] & 0x80) != 0) {
            Flags |= flagS;
        } else {
            Flags &= (~flagS);
        }
        if ((regs[REG_A] & 0xff) == 0) {
            Flags |= flagZ;
        } else {
            Flags &= (~flagZ);
        }
        parity(regs[REG_A]);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 4;
    }

    private int O42_LHLD(short OP) {
        // TODO: test!
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        regs[REG_L] = ((Short) memory.read(DAR)).shortValue();
        regs[REG_H] = ((Short) memory.read(DAR + 1)).shortValue();
        return 16;
    }

    private int O47_CMA(short OP) {
        regs[REG_A] = (short) (~regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int O50_STA(short OP) {
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        memory.write(DAR, regs[REG_A]);
        return 13;
    }

    private int O55_STC(short OP) {
        Flags |= flagC;
        return 4;
    }

    private int O58_LDA(short OP) {
        int DAR = (Integer) memory.readWord(PC);
        PC += 2;
        regs[REG_A] = ((Short) memory.read(DAR)).shortValue();
        return 13;
    }

    private int O63_CMC(short OP) {
        if ((Flags & flagC) != 0) {
            Flags &= (~flagC);
        } else {
            Flags |= flagC;
        }
        return 4;
    }

    private int O118_HLT(short OP) {
        runState = RunState.STATE_STOPPED_NORMAL;
        return 7;
    }

    private int O195_JMP(short OP) {
        PC = (Integer) memory.readWord(PC);
        return 10;
    }

    private int O198_ADI(short OP) {
        int DAR = regs[REG_A];
        regs[REG_A] += ((Short) memory.read(PC++)).shortValue();
        setarith(regs[REG_A], DAR);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O201_RET(short OP) {
        PC = (Integer) memory.readWord(SP);
        SP += 2;
        return 10;
    }

    private int O205_CALL(short OP) {
        memory.writeWord(SP - 2, PC + 2);
        SP -= 2;
        PC = (Integer) memory.readWord(PC);
        return 17;
    }

    private int O206_ACI(short OP) {
        int DAR = regs[REG_A];
        regs[REG_A] += ((Short) memory.read(PC++)).shortValue();
        if ((Flags & flagC) != 0) {
            regs[REG_A]++;
        }
        setarith(regs[REG_A], DAR);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O211_OUT(short OP) {
        int DAR = ((Short) memory.read(PC++)).shortValue();
        context.fireIO(DAR, false, regs[REG_A]);
        return 10;
    }

    private int O214_SUI(short OP) {
        int DAR = regs[REG_A];
        regs[REG_A] -= ((Short) memory.read(PC++)).shortValue();
        setarith(regs[REG_A], DAR);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O219_IN(short OP) {
        int DAR = ((Short) memory.read(PC++)).shortValue();
        regs[REG_A] = context.fireIO(DAR, true, (short) 0);
        return 10;
    }

    private int O222_SBI(short OP) {
        int DAR = regs[REG_A];
        regs[REG_A] -= ((Short) memory.read(PC++)).shortValue();
        if ((Flags & flagC) != 0) {
            regs[REG_A]--;
        }
        setarith(regs[REG_A], DAR);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        return 7;
    }

    private int O227_XTHL(short OP) {
        int DAR = (Integer) memory.readWord(SP);
        memory.writeWord(SP, (regs[REG_H] << 8) | regs[REG_L]);
        regs[REG_H] = (short) ((DAR >>> 8) & 0xFF);
        regs[REG_L] = (short) (DAR & 0xFF);
        return 18;
    }

    private int O230_ANI(short OP) {
        regs[REG_A] &= ((Short) memory.read(PC++)).shortValue();
        Flags &= (~flagC);
        Flags &= (~flagAC);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int O233_PCHL(short OP) {
        PC = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    private int O235_XCHG(short OP) {
        short x = regs[REG_H];
        short y = regs[REG_L];
        regs[REG_H] = regs[REG_D];
        regs[REG_L] = regs[REG_E];
        regs[REG_D] = x;
        regs[REG_E] = y;
        return 4;
    }

    private int O238_XRI(short OP) {
        regs[REG_A] ^= ((Short) memory.read(PC++)).shortValue();
        Flags &= (~flagC);
        Flags &= (~flagAC);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int O243_DI(short OP) {
        INTE = false;
        return 4;
    }

    private int O246_ORI(short OP) {
        regs[REG_A] |= ((Short) memory.read(PC++)).shortValue();
        Flags &= (~flagC);
        Flags &= (~flagAC);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 7;
    }

    private int O249_SPHL(short OP) {
        SP = (regs[REG_H] << 8) | regs[REG_L];
        return 5;
    }

    private int O251_EI(short OP) {
        INTE = true;
        return 4;
    }

    private int O254_CPI(short OP) {
        int X = regs[REG_A];
        int DAR = regs[REG_A] & 0xFF;
        DAR -= ((Short) memory.read(PC++)).shortValue();
        setarith(DAR, X);
        return 7;
    }

    private int MC0_O40_MOV(short OP) {
        putreg((OP >>> 3) & 0x07, getreg(OP & 0x07));
        if (((OP & 0x07) == 6) || (((OP >>> 3) & 0x07) == 6)) {
            return 7;
        } else {
            return 5;
        }
    }

    private int MC7_O6_MVI(short OP) {
        putreg((OP >>> 3) & 0x07, ((Short) memory.read(PC++)).shortValue());
        if (((OP >>> 3) & 0x07) == 6) {
            return 10;
        } else {
            return 7;
        }
    }

    private int MCF_01_LXI(short OP) {
        putpair((OP >>> 4) & 0x03, (Integer) memory.readWord(PC));
        PC += 2;
        return 10;
    }

    private int MEF_0A_LDAX(short OP) {
        putreg(7, ((Short) memory.read(getpair((OP >>> 4) & 0x03))).shortValue());
        return 7;
    }

    private int MEF_02_STAX(short OP) {
        memory.write(getpair((OP >>> 4) & 0x03), getreg(7));
        return 7;
    }

    private int MF8_B8_CMP(short OP) {
        int X = regs[REG_A];
        int DAR = regs[REG_A] & 0xFF;
        DAR -= getreg(OP & 0x07);
        setarith(DAR, X);
        if ((OP & 0x07) == 6) {
            return 7;
        } else {
            return 4;
        }
    }

    private int MC7_C2_JMP(short OP) {
        if (checkCondition((OP >>> 3) & 0x07)) {
            PC = (Integer) memory.readWord(PC);
        } else {
            PC += 2;
        }
        return 10;
    }

    private int MC7_C4_CALL(short OP) {
        if (checkCondition((OP >>> 3) & 0x07)) {
            int DAR = (Integer) memory.readWord(PC);
            PC += 2;
            memory.writeWord(SP - 2, PC);
            SP -= 2;
            PC = DAR;
            return 17;
        } else {
            PC += 2;
            return 11;
        }
    }

    private int MC7_C0_RET(short OP) {
        if (checkCondition((OP >>> 3) & 0x07)) {
            PC = (Integer) memory.readWord(SP);
            SP += 2;
        }
        return 10;
    }

    private int MC7_C7_RST(short OP) {
        memory.writeWord(SP - 2, PC);
        SP -= 2;
        PC = OP & 0x38;
        return 11;
    }

    private int MCF_C5_PUSH(short OP) {
        int DAR = getpush((OP >>> 4) & 0x03);
        memory.writeWord(SP - 2, DAR);
        SP -= 2;
        return 11;
    }

    private int MCF_C1_POP(short OP) {
        int DAR = (Integer) memory.readWord(SP);
        SP += 2;
        putpush((OP >>> 4) & 0x03, DAR);
        return 10;
    }

    private int MF8_80_ADD(short OP) {
        int X = regs[REG_A];
        regs[REG_A] += getreg(OP & 0x07);
        setarith(regs[REG_A], X);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MF8_88_ADC(short OP) {
        int X = regs[REG_A];
        regs[REG_A] += getreg(OP & 0x07);
        if ((Flags & flagC) != 0) {
            regs[REG_A]++;
        }
        setarith(regs[REG_A], X);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MF8_90_SUB(short OP) {
        int X = regs[REG_A];
        regs[REG_A] -= getreg(OP & 0x07);
        setarith(regs[REG_A], X);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MF8_98_SBB(short OP) {
        int X = regs[REG_A];
        regs[REG_A] -= (getreg(OP & 0x07));
        if ((Flags & flagC) != 0) {
            regs[REG_A]--;
        }
        setarith(regs[REG_A], X);
        regs[REG_A] = (short) (regs[REG_A] & 0xFF);
        if ((OP & 0x07) == 6) {
            return 7;
        }
        return 4;
    }

    private int MC7_04_INR(short OP) {
        int DAR = getreg((OP >>> 3) & 0x07) + 1;
        setinc(DAR, DAR - 1);
        DAR = DAR & 0xFF;
        putreg((OP >>> 3) & 0x07, (short) DAR);
        return 5;
    }

    private int MC7_05_DCR(short OP) {
        int DAR = getreg((OP >>> 3) & 0x07) - 1;
        setinc(DAR, DAR + 1);
        DAR = DAR & 0xFF;
        putreg((OP >>> 3) & 0x07, (short) DAR);
        return 5;
    }

    private int MCF_03_INX(short OP) {
        int DAR = getpair((OP >>> 4) & 0x03) + 1;
        DAR = DAR & 0xFFFF;
        putpair((OP >>> 4) & 0x03, DAR);
        return 5;
    }

    private int MCF_0B_DCX(short OP) {
        int DAR = getpair((OP >>> 4) & 0x03) - 1;
        DAR = DAR & 0xFFFF;
        putpair((OP >>> 4) & 0x03, DAR);
        return 5;
    }

    private int MCF_09_DAD(short OP) {
        int DAR = getpair((OP >>> 4) & 0x03);
        DAR += getpair(2);
        if ((DAR & 0x10000) != 0) {
            Flags |= flagC;
        } else {
            Flags &= (~flagC);
        }
        DAR = DAR & 0xFFFF;
        putpair(2, DAR);
        return 10;
    }

    private int MF8_A0_ANA(short OP) {
        regs[REG_A] &= getreg(OP & 0x07);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int MF8_A8_XRA(short OP) {
        regs[REG_A] ^= getreg(OP & 0x07);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }

    private int MF8_B0_ORA(short OP) {
        regs[REG_A] |= getreg(OP & 0x07);
        setlogical(regs[REG_A]);
        regs[REG_A] &= 0xFF;
        return 4;
    }


    private int dispatch() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        short OP;

        /* if interrupt is waiting, instruction won't be read from memory
         * but from one or all of 3 bytes (b1,b2,b3) which represents either
         * rst or call instruction incomed from external peripheral device
         */
        if (isINT == true) {
            if (INTE == true) {
                if ((b1 & 0xC7) == 0xC7) {                      /* RST */
                    memory.writeWord(SP - 2, PC);
                    SP -= 2;
                    PC = b1 & 0x38;
                    return 11;
                } else if (b1 == 0315) {                        /* CALL */
                    memory.writeWord(SP - 2, PC + 2);
                    SP -= 2;
                    PC = (int) (((b3 & 0xFF) << 8) | (b2 & 0xFF));
                    return 17;
                }
            }
            isINT = false;
        }
        OP = ((Short) memory.read(PC++)).shortValue();

        /* Handle below all operations which refer to registers or register pairs.
         After that, a large switch statement takes care of all other opcodes */
        if ((OP & 0xCF) == 0x01) {                      /* LXI */
            return MCF_01_LXI(OP);
        } else if ((OP & 0xEF) == 0x0A) {                      /* LDAX */
            return MEF_0A_LDAX(OP);
        } else if ((OP & 0xEF) == 0x02) {                      /* STAX */
            return MEF_02_STAX(OP);
        } else if ((OP & 0xF8) == 0xB8) {                      /* CMP */
            return MF8_B8_CMP(OP);
        } else if ((OP & 0xC7) == 0xC2) {                      /* JMP <condition> */
            return MC7_C2_JMP(OP);
        } else if ((OP & 0xC7) == 0xC4) {                      /* CALL <condition> */
            return MC7_C4_CALL(OP);
        } else if ((OP & 0xC7) == 0xC0) {                      /* RET <condition> */
            return MC7_C0_RET(OP);
        } else if ((OP & 0xC7) == 0xC7) {                      /* RST */
            return MC7_C7_RST(OP);
        } else if ((OP & 0xCF) == 0xC5) {                      /* PUSH */
            return MCF_C5_PUSH(OP);
        } else if ((OP & 0xCF) == 0xC1) {                      /*POP */
            return MCF_C1_POP(OP);
        } else if ((OP & 0xF8) == 0x80) {                      /* ADD */
            return MF8_80_ADD(OP);
        } else if ((OP & 0xF8) == 0x88) {                      /* ADC */
            return MF8_88_ADC(OP);
        } else if ((OP & 0xF8) == 0x90) {                      /* SUB */
            return MF8_90_SUB(OP);
        } else if ((OP & 0xF8) == 0x98) {                      /* SBB */
            return MF8_98_SBB(OP);
        } else if ((OP & 0xC7) == 0x04) {                      /* INR */
            return MC7_04_INR(OP);
        } else if ((OP & 0xC7) == 0x05) {                      /* DCR */
            return MC7_05_DCR(OP);
        } else if ((OP & 0xCF) == 0x03) {                      /* INX */
            return MCF_03_INX(OP);
        } else if ((OP & 0xCF) == 0x0B) {                      /* DCX */
            return MCF_0B_DCX(OP);
        } else if ((OP & 0xCF) == 0x09) {                      /* DAD */
            return MCF_09_DAD(OP);
        } else if ((OP & 0xF8) == 0xA0) {                      /* ANA */
            return MF8_A0_ANA(OP);
        } else if ((OP & 0xF8) == 0xA8) {                      /* XRA */
            return MF8_A8_XRA(OP);
        } else if ((OP & 0xF8) == 0xB0) {                      /* ORA */
            return MF8_B0_ORA(OP);
        }
        /* Dispatch Instruction */
        Method instr = dispatchTable[OP];
        if (instr == null) {
            runState = RunState.STATE_STOPPED_BAD_INSTR;
            return 0;
        }
        return (Integer)instr.invoke(this, OP);
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }
}
