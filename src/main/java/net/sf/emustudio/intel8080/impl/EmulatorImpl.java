/*
 * EmulatorImpl.java
 *
 * Implementation of CPU emulation
 * 
 * Created on Piatok, 2007, oktober 26, 10:45
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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
package net.sf.emustudio.intel8080.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
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
 * Main implementation class for CPU emulation
 * CPU works in a separate thread (parallel with other hardware)
 * 
 * @author vbmacher
 */
@PluginType(type=PLUGIN_TYPE.CPU,
        title="Intel 8080 CPU",
        copyright="\u00A9 Copyright 2007-2012, Peter Jakubčo",
        description="Emulator of Intel 8080 CPU")
public class EmulatorImpl extends AbstractCPU {
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

    private StatusPanel statusPanel;
    private MemoryContext<Short> memory;
    private ContextImpl context;
    private Disassembler disasm;

    // cpu speed
    private long executedCycles = 0; // count of executed cycles for frequency calculation
    private java.util.Timer frequencyScheduler;
    private FrequencyUpdater frequencyUpdater;
    private int checkTimeSlice = 100;

    // registers are public meant for only StatusPanel (didnt want make it thru getters)
    private int PC = 0; // program counter
    public int SP = 0; // stack pointer
    public short B = 0, C = 0, D = 0, E = 0, H = 0, L = 0, Flags = 2, A = 0; // registers
    public static final int flagS = 0x80, flagZ = 0x40, flagAC = 0x10, flagP = 0x4, flagC = 0x1;
    private boolean INTE = false; // enabling / disabling of interrupts
    private boolean isINT = false;
    private short b1 = 0; // the raw interrupt instruction
    private short b2 = 0;
    private short b3 = 0;

    /**
     * This class performs runtime frequency calculation and updating.
     * 
     * Given: time, executed cycles count
     * Frequency is defined as number of something by some period of time.
     * Hz = 1/s, kHz = 1000/s
     * time has to be in seconds
     * 
     * CC ..by.. time[s]
     * XX ..by.. 1 [s] ?
     * ---------------
     * XX:CC = 1:time
     * XX = CC / time [Hz]
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
        } catch (Exception e) {
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

            if (memory == null) {
                StaticDialogs.showErrorMessage("CPU must have access to memory");
                return false;
            }
            if (memory.getDataType() != Short.class) {
                StaticDialogs.showErrorMessage("Operating memory type is not supported for this kind of CPU.");
                return false;
            }

            // create disassembler and debug columns
            disasm = new DisassemblerImpl(memory, new DecoderImpl(memory));

            return true;
        } catch (InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not get memory context",
                    EmulatorImpl.class.getAnnotation(PluginType.class).title());
            return false;
        }
    }

    @Override
    public void destroy() {
        runState = RunState.STATE_STOPPED_NORMAL;
        stopFrequencyUpdater();
        context.clearDevices();
        context = null;
    }

    @Override
    public void reset(int startPos) {
        super.reset(startPos);
        SP = A = B = C = D = E = H = L = 0;
        Flags = 2; //0000 0010b
        PC = startPos;
        INTE = false;
        stopFrequencyUpdater();
        notifyCPURunState(runState);
        notifyCPUState();
    }

    /**
     * Force (external) breakpoint
     */
    @Override
    public void pause() {
        runState = RunState.STATE_STOPPED_BREAK;
        stopFrequencyUpdater();
        notifyCPURunState(runState);
    }

    /**
     *  Stops an emulation
     */
    @Override
    public void stop() {
        runState = RunState.STATE_STOPPED_NORMAL;
        stopFrequencyUpdater();
        notifyCPURunState(runState);
    }

    // vykona 1 krok - bez merania casov (bez real-time odozvy)
    @Override
    public void step() {
        if (runState == RunState.STATE_STOPPED_BREAK) {
            try {
                runState = RunState.STATE_RUNNING;
                evalStep();
                if (runState == RunState.STATE_RUNNING) {
                    runState = RunState.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException e) {
                runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            notifyCPURunState(runState);
            notifyCPUState();
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
        for (CPUListener listener : cpuListeners) {
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
            frequencyScheduler.scheduleAtFixedRate(frequencyUpdater, 0, checkTimeSlice);
        } catch (Exception e) {
        }
    }
    
    /**
     * Run a CPU execution (thread).
     * 
     * Real-time CPU frequency balancing
     * *********************************
     * 
     * 1 cycle is performed in 1 periode of CPU frequency.
     * CPU_PERIODE = 1 / CPU_FREQ [micros]
     * cycles_to_execute_per_second = 1000 / CPU_PERIODE
     * 
     * cycles_to_execute_per_second = 1000 / (1/CPU_FREQ)
     * cycles_to_execute_per_second = 1000 * CPU_FREQ
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
        notifyCPURunState(runState);
        notifyCPUState();
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
                        cycles = evalStep();
                    } catch (IndexOutOfBoundsException e) {
                        runState = RunState.STATE_STOPPED_ADDR_FALLOUT;
                        break;
                    } catch (Error er) {
                        runState = RunState.STATE_STOPPED_BREAK;
                        break;
                    }
                    cycles_executed += cycles;
                    executedCycles += cycles;
                    if (isBreakpointSet(PC) == true) {
                        throw new Error();
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
        notifyCPURunState(runState);
        notifyCPUState();
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
        switch (reg) {
            case 0:
                return B;
            case 1:
                return C;
            case 2:
                return D;
            case 3:
                return E;
            case 4:
                return H;
            case 5:
                return L;
            case 6:
                return ((Short) memory.read((H << 8) | L)).shortValue();
            case 7:
                return A;
        }
        return 0;
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, short val) {
        switch (reg) {
            case 0:
                B = val;
                break;
            case 1:
                C = val;
                break;
            case 2:
                D = val;
                break;
            case 3:
                E = val;
                break;
            case 4:
                H = val;
                break;
            case 5:
                L = val;
                break;
            case 6:
                memory.write((H << 8) | L, val);
                break;
            case 7:
                A = val;
        }
    }

    /* Put a value into an 8080 register pair */
    void putpair(int reg, int val) {
        short high, low;
        high = (short) ((val >>> 8) & 0xFF);
        low = (short) (val & 0xFF);
        switch (reg) {
            case 0:
                B = high;
                C = low;
                break;
            case 1:
                D = high;
                E = low;
                break;
            case 2:
                H = high;
                L = low;
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
                return (B << 8) | C;
            case 1:
                return (D << 8) | E;
            case 2:
                return (H << 8) | L;
            case 3:
                return SP;
        }
        return 0;
    }

    /* Return the value of a selected register pair, in PUSH
    format where 3 means A& flags, not SP
     */
    private int getpush(int reg) {
        int stat;
        switch (reg) {
            case 0:
                return (B << 8) | C;
            case 1:
                return (D << 8) | E;
            case 2:
                return (H << 8) | L;
            case 3:
                stat = (A << 8) | Flags;
                stat |= 0x02;
                return stat;
        }
        return 0;
    }

    /* Place data into the indicated register pair, in PUSH
    format where 3 means A& flags, not SP
     */
    private void putpush(int reg, int val) {
        short high, low;
        high = (short) ((val >>> 8) & 0xFF);
        low = (short) (val & 0xFF);
        switch (reg) {
            case 0:
                B = high;
                C = low;
                break;
            case 1:
                D = high;
                E = low;
                break;
            case 2:
                H = high;
                L = low;
                break;
            case 3:
                A = (short) ((val >>> 8) & 0xFF);
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

    private int evalStep() {
        short OP;
        int DAR;

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
        if (OP == 118) { // hlt?
            runState = RunState.STATE_STOPPED_NORMAL;
            return 7;
        }

        /* Handle below all operations which refer to registers or register pairs.
        After that, a large switch statement takes care of all other opcodes */
        if ((OP & 0xC0) == 0x40) {                             /* MOV */
            putreg((OP >>> 3) & 0x07, getreg(OP & 0x07));
            if (((OP & 0x07) == 6) || (((OP >>> 3) & 0x07) == 6)) {
                return 7;
            } else {
                return 5;
            }
        } else if ((OP & 0xC7) == 0x06) {                      /* MVI */
            putreg((OP >>> 3) & 0x07, ((Short) memory.read(PC++)).shortValue());
            if (((OP >>> 3) & 0x07) == 6) {
                return 10;
            } else {
                return 7;
            }
        } else if ((OP & 0xCF) == 0x01) {                      /* LXI */
            putpair((OP >>> 4) & 0x03, (Integer) memory.readWord(PC));
            PC += 2;
            return 10;
        } else if ((OP & 0xEF) == 0x0A) {                      /* LDAX */
            putreg(7, ((Short) memory.read(getpair((OP >>> 4) & 0x03))).shortValue());
            return 7;
        } else if ((OP & 0xEF) == 0x02) {                      /* STAX */
            memory.write(getpair((OP >>> 4) & 0x03), getreg(7));
            return 7;
        } else if ((OP & 0xF8) == 0xB8) {                      /* CMP */
            int X = A;
            DAR = A & 0xFF;
            DAR -= getreg(OP & 0x07);
            setarith(DAR, X);
            if ((OP & 0x07) == 6) {
                return 7;
            } else {
                return 4;
            }
        } else if ((OP & 0xC7) == 0xC2) {                      /* JMP <condition> */
            if (checkCondition((OP >>> 3) & 0x07)) {
                PC = (Integer) memory.readWord(PC);
            } else {
                PC += 2;
            }
            return 10;
        } else if ((OP & 0xC7) == 0xC4) {                      /* CALL <condition> */
            if (checkCondition((OP >>> 3) & 0x07)) {
                DAR = (Integer) memory.readWord(PC);
                PC += 2;
                memory.writeWord(SP - 2, PC);
                SP -= 2;
                PC = DAR;
                return 17;
            } else {
                PC += 2;
                return 11;
            }
        } else if ((OP & 0xC7) == 0xC0) {                      /* RET <condition> */
            if (checkCondition((OP >>> 3) & 0x07)) {
                PC = (Integer) memory.readWord(SP);
                SP += 2;
            }
            return 10;
        } else if ((OP & 0xC7) == 0xC7) {                      /* RST */
            memory.writeWord(SP - 2, PC);
            SP -= 2;
            PC = OP & 0x38;
            return 11;
        } else if ((OP & 0xCF) == 0xC5) {                      /* PUSH */
            DAR = getpush((OP >>> 4) & 0x03);
            memory.writeWord(SP - 2, DAR);
            SP -= 2;
            return 11;
        } else if ((OP & 0xCF) == 0xC1) {                      /*POP */
            DAR = (Integer) memory.readWord(SP);
            SP += 2;
            putpush((OP >>> 4) & 0x03, DAR);
            return 10;
        } else if ((OP & 0xF8) == 0x80) {                      /* ADD */
            int X = A;
            A += getreg(OP & 0x07);
            setarith(A, X);
            A = (short) (A & 0xFF);
            if ((OP & 0x07) == 6) {
                return 7;
            }
            return 4;
        } else if ((OP & 0xF8) == 0x88) {                      /* ADC */
            int X = A;
            A += getreg(OP & 0x07);
            if ((Flags & flagC) != 0) {
                A++;
            }
            setarith(A, X);
            A = (short) (A & 0xFF);
            if ((OP & 0x07) == 6) {
                return 7;
            }
            return 4;
        } else if ((OP & 0xF8) == 0x90) {                      /* SUB */
            int X = A;
            A -= getreg(OP & 0x07);
            setarith(A, X);
            A = (short) (A & 0xFF);
            if ((OP & 0x07) == 6) {
                return 7;
            }
            return 4;
        } else if ((OP & 0xF8) == 0x98) {                      /* SBB */
            int X = A;
            A -= (getreg(OP & 0x07));
            if ((Flags & flagC) != 0) {
                A--;
            }
            setarith(A, X);
            A = (short) (A & 0xFF);
            if ((OP & 0x07) == 6) {
                return 7;
            }
            return 4;
        } else if ((OP & 0xC7) == 0x04) {                      /* INR */
            DAR = getreg((OP >>> 3) & 0x07) + 1;
            setinc(DAR, DAR - 1);
            DAR = DAR & 0xFF;
            putreg((OP >>> 3) & 0x07, (short) DAR);
            return 5;
        } else if ((OP & 0xC7) == 0x05) {                      /* DCR */
            DAR = getreg((OP >>> 3) & 0x07) - 1;
            setinc(DAR, DAR + 1);
            DAR = DAR & 0xFF;
            putreg((OP >>> 3) & 0x07, (short) DAR);
            return 5;
        } else if ((OP & 0xCF) == 0x03) {                      /* INX */
            DAR = getpair((OP >>> 4) & 0x03) + 1;
            DAR = DAR & 0xFFFF;
            putpair((OP >>> 4) & 0x03, DAR);
            return 5;
        } else if ((OP & 0xCF) == 0x0B) {                      /* DCX */
            DAR = getpair((OP >>> 4) & 0x03) - 1;
            DAR = DAR & 0xFFFF;
            putpair((OP >>> 4) & 0x03, DAR);
            return 5;
        } else if ((OP & 0xCF) == 0x09) {                      /* DAD */
            DAR = getpair((OP >>> 4) & 0x03);
            DAR += getpair(2);
            if ((DAR & 0x10000) != 0) {
                Flags |= flagC;
            } else {
                Flags &= (~flagC);
            }
            DAR = DAR & 0xFFFF;
            putpair(2, DAR);
            return 10;
        } else if ((OP & 0xF8) == 0xA0) {                      /* ANA */
            A &= getreg(OP & 0x07);
            setlogical(A);
            A &= 0xFF;
            return 4;
        } else if ((OP & 0xF8) == 0xA8) {                      /* XRA */
            A ^= getreg(OP & 0x07);
            setlogical(A);
            A &= 0xFF;
            return 4;
        } else if ((OP & 0xF8) == 0xB0) {                      /* ORA */
            A |= getreg(OP & 0x07);
            setlogical(A);
            A &= 0xFF;
            return 4;
        }
        /* The Big Instruction Decode Switch */
        switch (OP) {
            /* Logical instructions */
            case 0376:                                     /* CPI */
                int X = A;
                DAR = A & 0xFF;
                DAR -= ((Short) memory.read(PC++)).shortValue();
                setarith(DAR, X);
                return 7;
            case 0346:                                     /* ANI */
                A &= ((Short) memory.read(PC++)).shortValue();
                Flags &= (~flagC);
                Flags &= (~flagAC);
                setlogical(A);
                A &= 0xFF;
                return 7;
            case 0356:                                     /* XRI */
                A ^= ((Short) memory.read(PC++)).shortValue();
                Flags &= (~flagC);
                Flags &= (~flagAC);
                setlogical(A);
                A &= 0xFF;
                return 7;
            case 0366:                                     /* ORI */
                A |= ((Short) memory.read(PC++)).shortValue();
                Flags &= (~flagC);
                Flags &= (~flagAC);
                setlogical(A);
                A &= 0xFF;
                return 7;
            /* Jump instructions */
            case 0303:                                     /* JMP */
                PC = (Integer) memory.readWord(PC);
                return 10;
            case 0351:                                     /* PCHL */
                PC = (H << 8) | L;
                return 5;
            case 0315:                                     /* CALL */
                memory.writeWord(SP - 2, PC + 2);
                SP -= 2;
                PC = (Integer) memory.readWord(PC);
                return 17;
            case 0311:                                     /* RET */
                PC = (Integer) memory.readWord(SP);
                SP += 2;
                return 10;
            /* Data Transfer Group */
            case 062:                                      /* STA */
                DAR = (Integer) memory.readWord(PC);
                PC += 2;
                memory.write(DAR, A);
                return 13;
            case 072:                                      /* LDA */
                DAR = (Integer) memory.readWord(PC);
                PC += 2;
                A = ((Short) memory.read(DAR)).shortValue();
                return 13;
            case 042:                                      /* SHLD */
                DAR = (Integer) memory.readWord(PC);
                PC += 2;
                memory.writeWord(DAR, (H << 8) | L);
                return 16;
            case 052:                                      /* LHLD BUG !*/
                DAR = (Integer) memory.readWord(PC);
                PC += 2;
                L = ((Short) memory.read(DAR)).shortValue();
                H = ((Short) memory.read(DAR + 1)).shortValue();
                return 16;
            case 0353:                                     /* XCHG */
                short x = H,
                 y = L;
                H = D;
                L = E;
                D = x;
                E = y;
                return 4;
            /* Arithmetic Group */
            case 0306:                                     /* ADI */
                DAR = A;
                A += ((Short) memory.read(PC++)).shortValue();
                setarith(A, DAR);
                A = (short) (A & 0xFF);
                return 7;
            case 0316:                                     /* ACI */
                DAR = A;
                A += ((Short) memory.read(PC++)).shortValue();
                if ((Flags & flagC) != 0) {
                    A++;
                }
                setarith(A, DAR);
                A = (short) (A & 0xFF);
                return 7;
            case 0326:                                     /* SUI */
                DAR = A;
                A -= ((Short) memory.read(PC++)).shortValue();
                setarith(A, DAR);
                A = (short) (A & 0xFF);
                return 7;
            case 0336:                                     /* SBI */
                DAR = A;
                A -= ((Short) memory.read(PC++)).shortValue();
                if ((Flags & flagC) != 0) {
                    A--;
                }
                setarith(A, DAR);
                A = (short) (A & 0xFF);
                return 7;
            case 047:                                      /* DAA */
                DAR = A;
                if (((DAR & 0x0F) > 9) || ((Flags & flagAC) != 0)) {
                    DAR += 6;
                    if ((DAR & 0x10) != (A & 0x10)) {
                        Flags |= flagAC;
                    } else {
                        Flags &= (~flagAC);
                    }
                    A = (short) (DAR & 0xFF);
                }
                DAR = (A >>> 4) & 0x0F;
                if ((DAR > 9) || ((Flags & flagC) != 0)) {
                    DAR += 6;
                    if ((DAR & 0x10) != 0) {
                        Flags |= flagC;
                    }
                    A &= 0x0F;
                    A |= ((DAR << 4) & 0xF0);
                }
                if ((A & 0x80) != 0) {
                    Flags |= flagS;
                } else {
                    Flags &= (~flagS);
                }
                if ((A & 0xff) == 0) {
                    Flags |= flagZ;
                } else {
                    Flags &= (~flagZ);
                }
                parity(A);
                A = (short) (A & 0xFF);
                return 4;
            case 07: {                                     /* RLC */
                int xx = (A << 9) & 0200000;
                if (xx != 0) {
                    Flags |= flagC;
                } else {
                    Flags &= (~flagC);
                }
                A = (short) ((A << 1) & 0xFF);
                if (xx != 0) {
                    A |= 0x01;
                }
                return 4;
            }
            case 017: {                                     /* RRC */
                if ((A & 0x01) == 1) {
                    Flags |= flagC;
                } else {
                    Flags &= (~flagC);
                }
                A = (short) ((A >>> 1) & 0xFF);
                if ((Flags & flagC) != 0) {
                    A |= 0x80;
                }
                return 4;
            }
            case 027: {                                    /* RAL */
                int xx = (A << 9) & 0200000;
                A = (short) ((A << 1) & 0xFF);
                if ((Flags & flagC) != 0) {
                    A |= 1;
                } else {
                    A &= 0xFE;
                }
                if (xx != 0) {
                    Flags |= flagC;
                } else {
                    Flags &= (~flagC);
                }
                return 4;
            }
            case 037: {                                    /* RAR */
                int xx = 0;
                if ((A & 0x01) == 1) {
                    xx |= 0200000;
                }
                A = (short) ((A >>> 1) & 0xFF);
                if ((Flags & flagC) != 0) {
                    A |= 0x80;
                } else {
                    A &= 0x7F;
                }
                if (xx != 0) {
                    Flags |= flagC;
                } else {
                    Flags &= (~flagC);
                }
                return 4;
            }
            case 057:                                      /* CMA */
                A = (short) (~A);
                A &= 0xFF;
                return 4;
            case 077:                                      /* CMC */
                if ((Flags & flagC) != 0) {
                    Flags &= (~flagC);
                } else {
                    Flags |= flagC;
                }
                return 4;
            case 067:                                      /* STC */
                Flags |= flagC;
                return 4;
            /* Stack, I/O & Machine Control Group */
            case 0:                                        /* NOP */
                return 4;
            case 0343:                                     /* XTHL */
                DAR = (Integer) memory.readWord(SP);
                memory.writeWord(SP, (H << 8) | L);
                H = (short) ((DAR >>> 8) & 0xFF);
                L = (short) (DAR & 0xFF);
                return 18;
            case 0371:                                     /* SPHL */
                SP = (H << 8) | L;
                return 5;
            case 0373:                                     /* EI */
                INTE = true;
                return 4;
            case 0363:                                     /* DI */
                INTE = false;
                return 4;
            case 0333:                                     /* IN */
                DAR = ((Short) memory.read(PC++)).shortValue();
                A = context.fireIO(DAR, true, (short) 0);
                return 10;
            case 0323:                                     /* OUT */
                DAR = ((Short) memory.read(PC++)).shortValue();
                context.fireIO(DAR, false, A);
                return 10;
        }
        runState = RunState.STATE_STOPPED_BAD_INSTR;
        return 0;
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }
}
