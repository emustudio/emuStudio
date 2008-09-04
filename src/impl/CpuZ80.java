/*
 * CpuZ80.java
 *
 * Created on 23.8.2008, 12:53:21
 * hold to: KISS, YAGNI
 *
 */

package impl;

import gui.statusGUI;
import java.util.HashSet;
import java.util.TimerTask;
import javax.swing.JPanel;
import plugins.ISettingsHandler;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext;
import plugins.cpu.ICPUContext.stateEnum;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author vbmacher
 */
public class CpuZ80 implements ICPU, Runnable {
    private Thread cpuThread = null;
    private statusGUI status;

    private IMemoryContext mem;
    private CpuContext cpu;
    private ISettingsHandler settings;
    private HashSet breaks; // zoznam breakpointov (mnozina)
    
    // sychronization locks for registers
    private Object BClock = new Object();
    private Object DElock = new Object();
    private Object HLlock = new Object();
    private Object AFlock = new Object();
    private Object PClock = new Object();
    private Object SPlock = new Object();
    private Object IXlock = new Object();
    private Object IYlock = new Object();
    private Object Ilock = new Object();
    private Object Rlock = new Object();

    // 2 sets of 6 GPR
    private volatile short[] B; private volatile short[] C;
    private volatile short[] D; private volatile short[] E;
    private volatile short[] H; private volatile short[] L;
    
    // accumulator and flags
    private volatile short[] A; private volatile short[] F;
    
    // special registers
    private volatile int PC = 0; private volatile int SP = 0;
    private volatile int IX = 0; private volatile int IY = 0;
    private volatile short I = 0; private volatile short R = 0; // interrupt r., refresh r.
    private boolean[] IFF; // interrupt enable flip-flops
    
    private int activeSet; // active set of registers

    public static final int flagS = 0x80, flagZ = 0x40, flagH = 0x10, flagPV = 0x4,
            flagN = 0x2, flagC = 0x1;
    
    // cpu speed
    private long long_cycles = 0; // count of executed cycles for runtime freq. computing
    private java.util.Timer freqScheduler;
    private RuntimeFrequencyCalculator rfc;
    private int sliceCheckTime = 100;
    private stateEnum run_state; // state of emulation run
    
    public CpuZ80() {
        B = new short[2]; C = new short[2]; D = new short[2]; E = new short[2];
        H = new short[2]; L = new short[2]; A = new short[2]; F = new short[2];
        IFF = new boolean[2];

        cpu = new CpuContext(this);
        run_state = stateEnum.stoppedNormal;
        breaks = new HashSet();
        rfc = new RuntimeFrequencyCalculator();
        freqScheduler = new java.util.Timer();
        activeSet = 0;
    }
    
    public String getName() { return "Zilog Z80"; }
    public String getCopyright() { return "\u00A9 Copyright 2008, Peter Jakubčo"; }
    public String getDescription() {
        return "Implementation of Zilog Z80 8bit CPU. With its architecture"
               + " it is similar to Intel's 8080 but something is modified and"
               + " extended.";
    }
    public String getVersion() { return "0.1a1"; }

    /**
     * Should be called only once
     */
    public boolean initialize(IMemoryContext mem, ISettingsHandler sHandler) {
        this.mem = mem;
        activeSet = 0;
        this.settings = sHandler;
        status = new statusGUI(this,mem);
        return true;
    }

    public void step() {
        if (run_state == stateEnum.stoppedBreak) {
            try {
                synchronized(run_state) {
                    run_state = stateEnum.runned;
                    evalStep();
                    if (run_state == stateEnum.runned)
                        run_state = stateEnum.stoppedBreak;
                }
            }
            catch (IndexOutOfBoundsException e) {
                run_state = stateEnum.stoppedAddrFallout;
            }
            cpu.fireCpuRun(status,run_state);
            cpu.fireCpuState();
        }
    }

    /**
     * Create CPU Thread and start it until CPU halt (instruction hlt)
     * or until address fallout
     */ 
    public void execute() {
        cpuThread = new Thread(this, "Z80");
        cpuThread.start();
    }

    /**
     * Force (external) breakpoint
     */
    public void pause() {
        run_state = stateEnum.stoppedBreak;
        setRuntimeFreqCounter(false);
        cpu.fireCpuRun(status,run_state);
    }

    public void stop() {
        run_state = stateEnum.stoppedNormal;
        setRuntimeFreqCounter(false);
        cpu.fireCpuRun(status,run_state);
    }

    /* CPU Context */

    public ICPUContext getContext() { return cpu; }

    public int getActiveRegSet() { return activeSet; }
    
    // get the address from next instruction
    // this method exist only from a view of effectivity
    public int getNextPC(int memPos) {
        return status.getNextPosition(memPos);
    }
    public int getPC() { synchronized(PClock) { return PC; } }
    public int getSP() { synchronized(SPlock) { return SP; } }
    public short getA() { synchronized(AFlock) { return A[activeSet]; } }
    public short getF() { synchronized(AFlock) { return F[activeSet]; } }
    public short getF(int set) { synchronized(AFlock) { return F[set]; } }
    public short getB() { synchronized(BClock) { return B[activeSet]; } }
    public short getC() { synchronized(BClock) { return C[activeSet]; } }
    public int getBC() { synchronized(BClock) { return (((B[activeSet]<<8)|C[activeSet])&0xFFFF); } }
    public short getD() { synchronized(DElock) { return D[activeSet]; } }
    public short getE() { synchronized(DElock) { return E[activeSet]; } }
    public int getDE() { synchronized(DElock) { return (((D[activeSet]<<8)|E[activeSet])&0xFFFF); } }
    public short getH() { synchronized(HLlock) { return H[activeSet]; } }
    public short getL() { synchronized(HLlock) { return L[activeSet]; } }
    public int getHL() { synchronized(HLlock) { return (((H[activeSet]<<8)|L[activeSet])&0xFFFF); } }
    public int getIX() { synchronized(IXlock) { return IX; } }
    public int getIY() { synchronized(IYlock) { return IY; } }
    public short getI() { synchronized(Ilock) { return I; } }
    public short getR() { synchronized(Rlock) { return R; } }

    /**
     * Sets program counter to specific value
     */
    public boolean setPC(int memPos) {
        if (memPos < 0 || memPos > mem.getSize()) return false;
        synchronized(PClock) {
            PC = memPos;
        }
        return true;
    }

    /* GUI interaction */
    public IDebugColumn[] getDebugColumns() { return status.getDebugColumns(); }
    public void setDebugValue(int index, int col, Object value) {
        status.setDebugColVal(index, col, value);
    }
    public Object getDebugValue(int index, int col) {
        return status.getDebugColVal(index, col);
    }
    public JPanel getStatusGUI() { return status; }

    // breakpoints
    public boolean isBreakpointSupported() { return true; }
    public void setBreakpoint(int pos, boolean set) {
        if (set) breaks.add(pos);
        else breaks.remove(pos);
    }
    public boolean getBreakpoint(int pos) { return breaks.contains(pos); }

    public void reset() {
        synchronized(PClock) {
        synchronized(SPlock) {
        synchronized(IXlock) {
        synchronized(IYlock) {
        synchronized(Ilock) {
        synchronized(Rlock) {
        synchronized(AFlock) {
        synchronized(BClock) {
        synchronized(DElock) {
        synchronized(HLlock) {
            PC = SP = IX = IY = 0;
            I = R = 0;
            for (int i = 0; i < 2; i++) {
                A[i] = B[i] = C[i] = D[i] = E[i] = H[i] = L[i] = 0;
                IFF[i] = false;
            }
        }}}}}}}}}}
    }

    public void destroy() {}

    public int getSliceTime() { return sliceCheckTime; }
    public void setSliceTime(int t) { sliceCheckTime = t; }
    
    private void setRuntimeFreqCounter(boolean run) {
        if (run) {
            try { 
                freqScheduler.purge();
                freqScheduler.scheduleAtFixedRate(rfc, 0, sliceCheckTime);
            } catch(Exception e) {}
        } else {
            try {
                rfc.cancel();
                rfc = new RuntimeFrequencyCalculator();
            } catch (Exception e) {}
        }
    }
    
    /**
     * This class perform runtime frequency calculation
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
     * 
     * @author vbmacher
     */
    private class RuntimeFrequencyCalculator extends TimerTask {
        private long startTimeSaved = 0;

        public void run() {
            double endTime = System.nanoTime();
            double time = endTime - startTimeSaved;

            if (long_cycles == 0) return;
            double freq = (double)long_cycles / (time / 1000000.0);
            startTimeSaved = (long)endTime;
            long_cycles = 0;
            cpu.fireFrequencyChanged((float)freq);
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
    public void run() {
        long startTime, endTime;
        int cycles_executed;
        int cycles_to_execute; // per second
        int cycles;
        long slice;
        
        run_state = stateEnum.runned;
        cpu.fireCpuRun(status,run_state);
        cpu.fireCpuState();
        setRuntimeFreqCounter(true);
        /* 1 Hz  .... 1 tState per second
         * 1 kHz .... 1000 tStates per second
         * clockFrequency is in kHz it have to be multiplied with 1000
         */
        cycles_to_execute = sliceCheckTime * cpu.getFrequency();
        long i = 0;
        slice = sliceCheckTime * 1000000;
        synchronized(run_state) {
            while(run_state == stateEnum.runned) {
                i++;
                startTime = System.nanoTime();
                cycles_executed = 0;
                try { 
                    while((cycles_executed < cycles_to_execute)
                            && (run_state == stateEnum.runned)) {
                        cycles = evalStep();
                        cycles_executed += cycles;
                        long_cycles += cycles;
                        if (getBreakpoint(PC) == true)
                            throw new Error();
                    }
                }
                catch (IndexOutOfBoundsException e) {
                    run_state = stateEnum.stoppedAddrFallout;
                    break;
                }
                catch (Error er) {
                    run_state = stateEnum.stoppedBreak;
                    break;
                }
                endTime = System.nanoTime() - startTime;
                if (endTime < slice) {
                    // time correction
                    try { Thread.sleep((slice - endTime)/1000000); }
                    catch(java.lang.InterruptedException e) {}
                }
            }
        }
        setRuntimeFreqCounter(false);
        cpu.fireCpuState();
        cpu.fireCpuRun(status,run_state);
    }
    
}
