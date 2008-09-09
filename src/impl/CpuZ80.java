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

    private int evalStep() {
//        switch (val & 0x3F) {
//            case 0x40: mnemo = "ld " + getRegMnemo((val>>>3)&7) + ","
//                        + getRegMnemo(val & 7); break;
//            case 0x80: mnemo = getOpMnemo((val>>>3)&7) + getRegMnemo(val&7); break;
//        }
//        switch (val & 0xC7) {
//            case 0x04: mnemo = "inc " + getRegMnemo((val>>>3)&7); break;
//            case 0x05: mnemo = "dec " + getRegMnemo((val>>>3)&7); break;
//        }
//        switch (val & 0xCF) {
//            case 0x03: mnemo = "inc " + getRegPairMnemo((val>>>4)&3); break;
//            case 0x09: mnemo = "add hl, " + getRegPairMnemo((val>>>4)&3); break;
//            case 0x0B: mnemo = "dec " + getRegPairMnemo((val>>>4)&3); break;
//        }
//        mnemo = null;
//        switch (val) {
//            case 0x00: mnemo = "nop"; break;
//            case 0x02: mnemo = "ld (bc),a"; break;
//            case 0x07: mnemo = "rlca"; break;
//            case 0x08: mnemo = "ex af,af'"; break;
//            case 0x0A: mnemo = "ld a,(bc)"; break;
//            case 0x0F: mnemo = "rrca"; break;
//            case 0x10: val = ((Short)mem.read(actPos++)).shortValue();
//                mnemo = "djnz " + String.format("%02X", val);
//                oper += String.format(" %02X", val);break;
//            case 0x12: mnemo = "ld (de),a"; break;
//            case 0x17: mnemo = "rla"; break;
//            case 0x1A: mnemo = "ld a,(de)"; break;
//            case 0x1F: mnemo = "rra"; break;
//            case 0x27: mnemo = "daa"; break;
//            case 0x2F: mnemo = "cpl"; break;
//            case 0x37: mnemo = "scf"; break;
//            case 0x3F: mnemo = "ccf"; break;
//            case 0x76: mnemo = "halt"; break;
//            case 0xC0: mnemo = "ret nz"; break;
//            case 0xC1: mnemo = "pop bc"; break;
//            case 0xC5: mnemo = "push bc"; break;
//            case 0xC7: mnemo = "rst 0"; break;
//            case 0xC8: mnemo = "ret z"; break;
//            case 0xC9: mnemo = "ret"; break;
//            case 0xCF: mnemo = "rst 08"; break;
//            case 0xD0: mnemo = "ret nc"; break;
//            case 0xD1: mnemo = "pop de"; break;
//            case 0xD5: mnemo = "push de"; break;
//            case 0xD7: mnemo = "rst 10"; break;
//            case 0xD8: mnemo = "ret c"; break;
//            case 0xD9: mnemo = "exx"; break;
//            case 0xDF: mnemo = "rst 18"; break;
//            case 0xE0: mnemo = "ret po"; break;
//            case 0xE1: mnemo = "pop hl"; break;
//            case 0xE3: mnemo = "ex (sp),hl"; break;
//            case 0xE5: mnemo = "push hl"; break;
//            case 0xE7: mnemo = "rst 20"; break;
//            case 0xE8: mnemo = "ret pe"; break;
//            case 0xE9: mnemo = "jp (hl)"; break;
//            case 0xEB: mnemo = "ex de,hl"; break;
//            case 0xEF: mnemo = "rst 28"; break;
//            case 0xF0: mnemo = "ret p"; break;
//            case 0xF1: mnemo = "pop af"; break;
//            case 0xF3: mnemo = "di"; break;
//            case 0xF5: mnemo = "push af"; break;
//            case 0xF7: mnemo = "rst 30"; break;
//            case 0xF8: mnemo = "ret m"; break;
//            case 0xF9: mnemo = "ld sp,hl"; break;
//            case 0xFB: mnemo = "ei"; break;
//            case 0xFF: mnemo = "rst 38"; break;
//            case 0xED:
//                val = ((Short)mem.read(actPos++)).shortValue();
//                oper += String.format(" %02X", val);
//                switch(val) {
//                    case 0x40: mnemo = "in b,(c)"; break;
//                    case 0x41: mnemo = "out (c),b"; break;
//                    case 0x42: mnemo = "sbc hl,bc"; break;
//                    case 0x44: mnemo = "neg"; break;
//                    case 0x45: mnemo = "retn"; break;
//                    case 0x46: mnemo = "im 0"; break;
//                    case 0x47: mnemo = "ld i,a"; break;
//                    case 0x48: mnemo = "in c,(c)"; break;
//                    case 0x49: mnemo = "out (c),c"; break;
//                    case 0x4A: mnemo = "add hl,bc"; break;
//                    case 0x4D: mnemo = "reti"; break;
//                    case 0x4F: mnemo = "ld r,a"; break;
//                    case 0x50: mnemo = "in d,(c)"; break;
//                    case 0x51: mnemo = "out (c),d"; break;
//                    case 0x52: mnemo = "sbc hl,de"; break;
//                    case 0x56: mnemo = "im 1"; break;
//                    case 0x57: mnemo = "ld a,i"; break;
//                    case 0x58: mnemo = "in e,(c)"; break;
//                    case 0x59: mnemo = "out (c),e"; break;
//                    case 0x5A: mnemo = "add hl,de"; break;
//                    case 0x5E: mnemo = "im 2"; break;
//                    case 0x5F: mnemo = "ld a,r"; break;
//                    case 0x60: mnemo = "in h,(c)"; break;
//                    case 0x61: mnemo = "out (c),h"; break;
//                    case 0x62: mnemo = "sbc hl,hl"; break;
//                    case 0x67: mnemo = "rrd"; break;
//                    case 0x68: mnemo = "in l,(c)"; break;
//                    case 0x69: mnemo = "out (c),l"; break;
//                    case 0x6A: mnemo = "add hl,hl"; break;
//                    case 0x6F: mnemo = "rld"; break;
//                    case 0x70: mnemo = "in (c)"; break;
//                    case 0x71: mnemo = "out (c),0"; break;
//                    case 0x72: mnemo = "sbc hl,sp"; break;
//                    case 0x78: mnemo = "in a,(c)"; break;
//                    case 0x79: mnemo = "out (c),a"; break;
//                    case 0x7A: mnemo = "add hl,sp"; break;
//                    case 0xA0: mnemo = "ldi"; break;
//                    case 0xA1: mnemo = "cpi"; break;
//                    case 0xA2: mnemo = "ini"; break;
//                    case 0xA3: mnemo = "outi"; break;
//                    case 0xA8: mnemo = "ldd"; break;
//                    case 0xA9: mnemo = "cpd"; break;
//                    case 0xAA: mnemo = "ind"; break;
//                    case 0xAB: mnemo = "outd"; break;
//                    case 0xB0: mnemo = "ldir"; break;
//                    case 0xB1: mnemo = "cpir"; break;
//                    case 0xB2: mnemo = "inir"; break;
//                    case 0xB3: mnemo = "otir"; break;
//                    case 0xB8: mnemo = "lddr"; break;
//                    case 0xB9: mnemo = "cpdr"; break;
//                    case 0xBA: mnemo = "indr"; break;
//                    case 0xBB: mnemo = "otdr"; break;
//                }
//                if (mnemo == null) {
//                    tmp = (Integer)mem.readWord(actPos);
//                    actPos += 2;
//                    oper += String.format(" %02X %02X", tmp&0xFF, (tmp>>>8)&0xff);
//                    switch (val) {
//                        case 0x43: mnemo = "ld (" + String.format("%04X", tmp)+"),bc"; break;
//                        case 0x4B: mnemo = "ld bc,(" + String.format("%04X", tmp)+")"; break;
//                        case 0x53: mnemo = "ld (" + String.format("%04X", tmp)+"),de"; break;
//                        case 0x5B: mnemo = "ld de,(" + String.format("%04X", tmp)+")"; break;
//                        case 0x73: mnemo = "ld (" + String.format("%04X", tmp)+"),sp"; break;
//                        case 0x7B: mnemo = "ld sp,(" + String.format("%04X", tmp)+")"; break;
//                    }
//                }break;
//            case 0xDD:
//                val = ((Short)mem.read(actPos++)).shortValue();
//                oper += String.format(" %02X", val); tmp = 0;
//                switch (val) {
//                    case 0x09: mnemo = "add ix,bc"; break;
//                    case 0x19: mnemo = "add ix,de"; break;
//                    case 0x23: mnemo = "inc ix"; break;
//                    case 0x29: mnemo = "add ix,ix"; break;
//                    case 0x2B: mnemo = "dec ix"; break;
//                    case 0x39: mnemo = "add ix,sp"; break;
//                    case 0xE1: mnemo = "pop ix"; break;
//                    case 0xE3: mnemo = "ex (sp),ix"; break;
//                    case 0xE5: mnemo = "push ix"; break;
//                    case 0xE9: mnemo = "jp (ix)"; break;
//                    case 0xF9: mnemo = "ld sp,ix"; break;
//                }
//                if (mnemo == null) {
//                    tmp = ((Short)mem.read(actPos++)).shortValue();
//                    oper += String.format(" %02X", tmp);
//                    switch(val) {
//                        case 0x34: mnemo = "inc (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x35: mnemo = "dec (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x46: mnemo = "ld b,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x4E: mnemo = "ld c,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x56: mnemo = "ld d,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x5E: mnemo = "ld e,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x66: mnemo = "ld h,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x6E: mnemo = "ld l,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x7E: mnemo = "ld a,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x86: mnemo = "add a,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x8E: mnemo = "adc a,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x96: mnemo = "sub (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x9E: mnemo = "sbc a,(ix+" + String.format("%02X",tmp)+")";break;
//                        case 0xA6: mnemo = "and (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0xAE: mnemo = "xor (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0xB6: mnemo = "or (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0xBE: mnemo = "cp (ix+" + String.format("%02X",tmp)+")";break;
//                        case 0x70: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),b";break;
//                        case 0x71: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),c";break;
//                        case 0x72: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),d";break;
//                        case 0x73: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),e";break;
//                        case 0x74: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),h";break;
//                        case 0x75: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),l";break;
//                        case 0x77: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),a";break;
//                    }
//                }
//                if (mnemo == null) {
//                    tmp += (((Short)mem.read(actPos++)).shortValue()<<8);
//                    oper += String.format(" %02X", (tmp>>>8)&0xff);
//                    switch (val) {
//                        case 0x21: mnemo = "ld ix," + String.format("%04X", tmp);break;
//                        case 0x22: mnemo = "ld (" + String.format("%04X", tmp)+"),ix";break;
//                        case 0x2A: mnemo = "ld ix,(" + String.format("%04X", tmp)+")";break;
//                        case 0x36:
//                            mnemo = "ld (ix+" + String.format("%02X", tmp&0xff)
//                                    +")," + String.format("%02X", (tmp>>>8)&0xff);break;
//                    }
//                }
//                if ((mnemo == null) && (val == 0xCB)) {
//                    val = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
//                    switch (val) {
//                        case 0x06: mnemo = "rlc (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x0E: mnemo = "rrc (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x16: mnemo = "rl (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x1E: mnemo = "rr (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x26: mnemo = "sla (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x2E: mnemo = "sra (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x36: mnemo = "sll (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x3E: mnemo = "srl (ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x46: mnemo = "bit 0,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x4E: mnemo = "bit 1,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x56: mnemo = "bit 2,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x5E: mnemo = "bit 3,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x66: mnemo = "bit 4,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x6E: mnemo = "bit 5,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x76: mnemo = "bit 6,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x7E: mnemo = "bit 7,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x86: mnemo = "res 0,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x8E: mnemo = "res 1,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x96: mnemo = "res 2,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0x9E: mnemo = "res 3,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xA6: mnemo = "res 4,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xAE: mnemo = "res 5,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xB6: mnemo = "res 6,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xBE: mnemo = "res 7,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xC6: mnemo = "set 0,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xCE: mnemo = "set 1,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xD6: mnemo = "set 2,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xDE: mnemo = "set 3,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xE6: mnemo = "set 4,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xEE: mnemo = "set 5,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xF6: mnemo = "set 6,(ix+"+String.format("%02X",tmp)+")";break;
//                        case 0xFE: mnemo = "set 7,(ix+"+String.format("%02X",tmp)+")";break;
//                    }
//                }break;
//            case 0xFD:
//                val = ((Short)mem.read(actPos++)).shortValue();
//                oper += String.format(" %02X", val); tmp = 0;
//                switch (val) {
//                    case 0x09: mnemo = "add iy,bc"; break;
//                    case 0x19: mnemo = "add iy,de"; break;
//                    case 0x23: mnemo = "inc iy"; break;
//                    case 0x29: mnemo = "add iy,iy"; break;
//                    case 0x2B: mnemo = "dec iy"; break;
//                    case 0x39: mnemo = "add iy,sp"; break;
//                    case 0xE1: mnemo = "pop iy"; break;
//                    case 0xE3: mnemo = "ex (sp),iy"; break;
//                    case 0xE5: mnemo = "push iy"; break;
//                    case 0xE9: mnemo = "jp (iy)"; break;
//                    case 0xF9: mnemo = "ld sp,iy"; break;
//                }
//                if (mnemo == null) {
//                    tmp = ((Short)mem.read(actPos++)).shortValue();
//                    oper += String.format(" %02X", tmp);
//                    switch(val) {
//                        case 0x34: mnemo = "inc (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x35: mnemo = "dec (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x46: mnemo = "ld b,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x4E: mnemo = "ld c,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x56: mnemo = "ld d,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x5E: mnemo = "ld e,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x66: mnemo = "ld h,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x6E: mnemo = "ld l,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x7E: mnemo = "ld a,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x86: mnemo = "add a,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x8E: mnemo = "adc a,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x96: mnemo = "sub (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x9E: mnemo = "sbc a,(iy+" + String.format("%02X",tmp)+")";break;
//                        case 0xA6: mnemo = "and (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0xAE: mnemo = "xor (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0xB6: mnemo = "or (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0xBE: mnemo = "cp (iy+" + String.format("%02X",tmp)+")";break;
//                        case 0x70: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),b";break;
//                        case 0x71: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),c";break;
//                        case 0x72: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),d";break;
//                        case 0x73: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),e";break;
//                        case 0x74: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),h";break;
//                        case 0x75: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),l";break;
//                        case 0x77: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),a";break;
//                    }
//                }
//                if (mnemo == null) {
//                    tmp += (((Short)mem.read(actPos++)).shortValue()<<8);
//                    oper += String.format(" %02X", (tmp>>>8)&0xff);
//                    switch (val) {
//                        case 0x21: mnemo = "ld iy," + String.format("%04X", tmp);break;
//                        case 0x22: mnemo = "ld (" + String.format("%04X", tmp)+"),iy";break;
//                        case 0x2A: mnemo = "ld iy,(" + String.format("%04X", tmp)+")";break;
//                        case 0x36:
//                            mnemo = "ld (iy+" + String.format("%02X", tmp&0xff)
//                                    +")," + String.format("%02X", (tmp>>>8)&0xff);break;
//                    }
//                }
//                if ((mnemo == null) && (val == 0xCB)) {
//                    val = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
//                    switch (val) {
//                        case 0x06: mnemo = "rlc (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x0E: mnemo = "rrc (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x16: mnemo = "rl (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x1E: mnemo = "rr (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x26: mnemo = "sla (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x2E: mnemo = "sra (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x36: mnemo = "sll (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x3E: mnemo = "srl (iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x46: mnemo = "bit 0,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x4E: mnemo = "bit 1,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x56: mnemo = "bit 2,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x5E: mnemo = "bit 3,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x66: mnemo = "bit 4,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x6E: mnemo = "bit 5,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x76: mnemo = "bit 6,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x7E: mnemo = "bit 7,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x86: mnemo = "res 0,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x8E: mnemo = "res 1,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x96: mnemo = "res 2,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0x9E: mnemo = "res 3,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xA6: mnemo = "res 4,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xAE: mnemo = "res 5,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xB6: mnemo = "res 6,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xBE: mnemo = "res 7,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xC6: mnemo = "set 0,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xCE: mnemo = "set 1,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xD6: mnemo = "set 2,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xDE: mnemo = "set 3,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xE6: mnemo = "set 4,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xEE: mnemo = "set 5,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xF6: mnemo = "set 6,(iy+"+String.format("%02X",tmp)+")";break;
//                        case 0xFE: mnemo = "set 7,(iy+"+String.format("%02X",tmp)+")";break;
//                    }
//                }break;
//            case 0xCB:
//                val = (Short)mem.read(memPos++);
//                oper += String.format(" %02X", val);
//                switch (val & 0xF8) {
//                    case 0x00: mnemo = "rlc " + getRegMnemo(val & 7); break;
//                    case 0x08: mnemo = "rrc " + getRegMnemo(val & 7); break;
//                    case 0x10: mnemo = "rl " + getRegMnemo(val & 0x7); break;
//                    case 0x18: mnemo = "rr " + getRegMnemo(val & 7); break;
//                    case 0x20: mnemo = "sla " + getRegMnemo(val & 7); break;
//                    case 0x28: mnemo = "sra " + getRegMnemo(val & 7); break;
//                    case 0x30: mnemo = "sll " + getRegMnemo(val & 7); break;
//                    case 0x38: mnemo = "srl " + getRegMnemo(val & 7); break;
//                    case 0x40: mnemo = "bit 0," + getRegMnemo(val & 7); break;
//                    case 0x48: mnemo = "bit 1," + getRegMnemo(val & 7); break;
//                    case 0x50: mnemo = "bit 2," + getRegMnemo(val & 7); break;
//                    case 0x58: mnemo = "bit 3," + getRegMnemo(val & 7); break;
//                    case 0x60: mnemo = "bit 4," + getRegMnemo(val & 7); break;
//                    case 0x68: mnemo = "bit 5," + getRegMnemo(val & 7); break;
//                    case 0x70: mnemo = "bit 6," + getRegMnemo(val & 7); break;
//                    case 0x78: mnemo = "bit 7," + getRegMnemo(val & 7); break;
//                    case 0x80: mnemo = "res 0," + getRegMnemo(val & 7); break;
//                    case 0x88: mnemo = "res 1," + getRegMnemo(val & 7); break;
//                    case 0x90: mnemo = "res 2," + getRegMnemo(val & 7); break;
//                    case 0x98: mnemo = "res 3," + getRegMnemo(val & 7); break;
//                    case 0xA0: mnemo = "res 4," + getRegMnemo(val & 7); break;
//                    case 0xA8: mnemo = "res 5," + getRegMnemo(val & 7); break;
//                    case 0xB0: mnemo = "res 6," + getRegMnemo(val & 7); break;
//                    case 0xB8: mnemo = "res 7," + getRegMnemo(val & 7); break;
//                    case 0xC0: mnemo = "set 0," + getRegMnemo(val & 7); break;
//                    case 0xC8: mnemo = "set 1," + getRegMnemo(val & 7); break;
//                    case 0xD0: mnemo = "set 2," + getRegMnemo(val & 7); break;
//                    case 0xD8: mnemo = "set 3," + getRegMnemo(val & 7); break;
//                    case 0xE0: mnemo = "set 4," + getRegMnemo(val & 7); break;
//                    case 0xE8: mnemo = "set 5," + getRegMnemo(val & 7); break;
//                    case 0xF0: mnemo = "set 6," + getRegMnemo(val & 7); break;
//                    case 0xF8: mnemo = "set 7," + getRegMnemo(val & 7); break;
//                    
//                }
//                if (mnemo != null) break;
//                switch (val) {
//                    case 0x06: mnemo = "rlc (hl)"; break;
//                    case 0x0E: mnemo = "rrc (hl)"; break;
//                    case 0x16: mnemo = "rl (hl)"; break;
//                    case 0x1E: mnemo = "rr (hl)"; break;
//                    case 0x26: mnemo = "sla (hl)"; break;
//                    case 0x2E: mnemo = "sra (hl)"; break;
//                    case 0x36: mnemo = "sll (hl)"; break;
//                    case 0x3E: mnemo = "srl (hl)"; break;
//                    case 0x46: mnemo = "bit 0,(hl)"; break;
//                    case 0x4E: mnemo = "bit 1,(hl)"; break;
//                    case 0x56: mnemo = "bit 2,(hl)"; break;
//                    case 0x5E: mnemo = "bit 3,(hl)"; break;
//                    case 0x66: mnemo = "bit 4,(hl)"; break;
//                    case 0x6E: mnemo = "bit 5,(hl)"; break;
//                    case 0x76: mnemo = "bit 6,(hl)"; break;
//                    case 0x7E: mnemo = "bit 7,(hl)"; break;
//                    case 0x86: mnemo = "res 0,(hl)"; break;
//                    case 0x8E: mnemo = "res 1,(hl)"; break;
//                    case 0x96: mnemo = "res 2,(hl)"; break;
//                    case 0x9E: mnemo = "res 3,(hl)"; break;
//                    case 0xA6: mnemo = "res 4,(hl)"; break;
//                    case 0xAE: mnemo = "res 5,(hl)"; break;
//                    case 0xB6: mnemo = "res 6,(hl)"; break;
//                    case 0xBE: mnemo = "res 7,(hl)"; break;
//                    case 0xC6: mnemo = "set 0,(hl)"; break;
//                    case 0xCE: mnemo = "set 1,(hl)"; break;
//                    case 0xD6: mnemo = "set 2,(hl)"; break;
//                    case 0xDE: mnemo = "set 3,(hl)"; break;
//                    case 0xE6: mnemo = "set 4,(hl)"; break;
//                    case 0xEE: mnemo = "set 5,(hl)"; break;
//                    case 0xF6: mnemo = "set 6,(hl)"; break;
//                    case 0xFE: mnemo = "set 7,(hl)"; break;
//                }
//        }
//        tmp = 0;
//        if (mnemo == null) {
//            tmp = ((Short)mem.read(actPos++)).shortValue();
//            oper += String.format(" %02X", tmp&0xff);
//            switch (val) {
//                case 0x06: mnemo = "ld b," + String.format("%02X", tmp); break;
//                case 0x0E: mnemo = "ld c," + String.format("%02X", tmp); break;
//                case 0x16: mnemo = "ld d," + String.format("%02X", tmp); break;
//                case 0x18: mnemo = "jr " + String.format("%02X", tmp); break;
//                case 0x1E: mnemo = "ld e," + String.format("%02X", tmp); break;
//                case 0x20: mnemo = "jr nz," + String.format("%02X", tmp); break;
//                case 0x26: mnemo = "ld h," + String.format("%02X", tmp); break;
//                case 0x28: mnemo = "jr z," + String.format("%02X", tmp); break;
//                case 0x2E: mnemo = "ld l," + String.format("%02X", tmp); break;
//                case 0x30: mnemo = "jr nc," + String.format("%02X", tmp); break;
//                case 0x36: mnemo = "ld (hl)," + String.format("%02X", tmp); break;
//                case 0x38: mnemo = "jr c," + String.format("%02X", tmp); break;
//                case 0x3E: mnemo = "ld a," + String.format("%02X", tmp); break;
//                case 0xC6: mnemo = "add a," + String.format("%02X", tmp); break;
//                case 0xCE: mnemo = "adc a," + String.format("%02X", tmp); break;
//                case 0xD3: mnemo = "out (" + String.format("%02X", tmp)+"),a"; break;
//                case 0xD6: mnemo = "sub " + String.format("%02X", tmp); break;
//                case 0xDB: mnemo = "in a,(" + String.format("%02X", tmp)+")"; break;
//                case 0xDE: mnemo = "sbc a," + String.format("%02X", tmp); break;
//                case 0xE6: mnemo = "and " + String.format("%02X", tmp); break;
//                case 0xEE: mnemo = "xor " + String.format("%02X", tmp); break;
//                case 0xF6: mnemo = "or " + String.format("%02X", tmp); break;
//                case 0xFE: mnemo = "cp " + String.format("%02X", tmp); break;
//            }
//        }
//        if (mnemo == null) {
//            tmp += (((Short)mem.read(actPos++)).shortValue() << 8);
//            oper += String.format(" %02X", (tmp>>>8)&0xff);
//            switch (val) {
//                case 0x01: mnemo = "ld bc," + String.format("%04X", tmp); break;
//                case 0x11: mnemo = "ld de," + String.format("%04X", tmp); break;
//                case 0x21: mnemo = "ld hl," + String.format("%04X", tmp); break;
//                case 0x22: mnemo = "ld (" + String.format("%04X", tmp) + "),hl"; break;
//                case 0x2A: mnemo = "ld hl,(" + String.format("%04X", tmp)+")"; break;
//                case 0x31: mnemo = "ld sp," + String.format("%04X", tmp); break;
//                case 0x32: mnemo = "ld (" + String.format("%04X", tmp) + "),a"; break;
//                case 0x3A: mnemo = "ld a,(" + String.format("%04X", tmp) + ")"; break;
//                case 0xC2: mnemo = "jp nz," + String.format("%04X", tmp); break;
//                case 0xC3: mnemo = "jp " + String.format("%04X", tmp); break;
//                case 0xC4: mnemo = "call nz," + String.format("%04X", tmp); break;
//                case 0xCA: mnemo = "jp z," + String.format("%04X", tmp); break;
//                case 0xCC: mnemo = "call z," + String.format("%04X", tmp); break;
//                case 0xCD: mnemo = "call " + String.format("%04X", tmp); break;
//                case 0xD2: mnemo = "jp nc," + String.format("%04X", tmp); break;
//                case 0xD4: mnemo = "call nc," + String.format("%04X", tmp); break;
//                case 0xDC: mnemo = "call c," + String.format("%04X", tmp); break;
//                case 0xDA: mnemo = "jp c," + String.format("%04X", tmp); break;
//                case 0xE2: mnemo = "jp po," + String.format("%04X", tmp); break;
//                case 0xE4: mnemo = "call po," + String.format("%04X", tmp); break;
//                case 0xEA: mnemo = "jp pe," + String.format("%04X", tmp); break;
//                case 0xEC: mnemo = "call pe," + String.format("%04X", tmp); break;
//                case 0xF2: mnemo = "jp p," + String.format("%04X", tmp); break;
//                case 0xF4: mnemo = "call p," + String.format("%04X", tmp); break;
//                case 0xFA: mnemo = "jp m," + String.format("%04X", tmp); break;
//                case 0xFC: mnemo = "call m," + String.format("%04X", tmp); break;
//            }
//        }
        return 0;
    }
    
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
