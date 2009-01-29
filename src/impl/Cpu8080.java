/*
 * Cpu8080.java
 *
 * Implementation of CPU emulation
 * 
 * Created on Piatok, 2007, oktober 26, 10:45
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package impl;

import gui.statusGUI;
import java.util.HashSet;
import java.util.TimerTask;
import javax.swing.JPanel;
import plugins.ISettingsHandler;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;


/**
 * Main implementation class for CPU emulation
 * CPU works in a separate thread (parallel with other hardware)
 * 
 * @author vbmacher
 */
public class Cpu8080 implements ICPU, Runnable {
	private long hash;
    private Thread cpuThread = null;
    private statusGUI status;

    private IMemoryContext mem;
    private CpuContext cpu;
    @SuppressWarnings("unused")
	private ISettingsHandler settings;
    private HashSet<Integer> breaks; // zoznam breakpointov (mnozina)

    // cpu speed
    private long long_cycles = 0; // count of executed cycles for runtime freq. computing
    private java.util.Timer freqScheduler;
    private RuntimeFrequencyCalculator rfc;
    private int sliceCheckTime = 100;

    // registers are public meant for only statusGUI (didnt want make it thru get() methods)
    private int PC=0; // program counter
    public int SP=0; // stack pointer
    public short B=0, C=0, D=0, E=0, H=0, L=0, Flags=2, A=0; // registre
    public static final int flagS = 0x80, flagZ = 0x40, flagAC = 0x10, flagP = 0x4, flagC = 0x1;
    
    private boolean INTE = false; // povolenie / zakazanie preruseni
    private boolean isINT = false;
    private short b1 = 0; // interrupt instruction
    private short b2 = 0;
    private short b3 = 0;
    
    private int run_state; // dovod zastavenia emulatora

    private byte parity_table[] = {
        1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,
        1,0,0,1,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,
        1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,
        1,0,0,1,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,
        1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,
        1,0,0,1,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,
        1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,
        1,0,0,1
    };
    
    
    /** Creates a new instance of Cpu8080 */
    public Cpu8080(Long hash) {
    	this.hash = hash;
        cpu = new CpuContext(this);
        run_state = ICPU.STATE_STOPPED_NORMAL;
        breaks = new HashSet<Integer>();
        status = new statusGUI(this);
        rfc = new RuntimeFrequencyCalculator();
        freqScheduler = new java.util.Timer();
    }

    @Override
    public String getDescription() {
    	return "Modified for use as CPU for MITS Altair 8800 computer";
    }
    @Override
    public String getVersion() { return "0.18b"; }
    @Override
    public String getTitle() { return "Intel 8080 CPU"; }
    @Override
    public String getCopyright() { return "\u00A9 Copyright 2006-2009, P. Jakubčo"; }

    @Override
    public long getHash() { return hash; }
    @Override
    public boolean initialize(IMemoryContext mem, ISettingsHandler sHandler) {
        if (mem == null)
            throw new java.lang.NullPointerException("CPU must have access to memory");
        if (!mem.getID().equals("byte_simple_variable")) {
            StaticDialogs.showErrorMessage("Operating memory type is not supported"
                    + " for this kind of CPU.");
            return false;
        }
        this.mem = mem;
        this.settings = sHandler;
        status.setMem(mem);
        return true;
    }

    @Override
    public ICPUContext getContext() { return cpu; }
    
    @Override
    public void destroy() {
        run_state = ICPU.STATE_STOPPED_NORMAL;
        setRuntimeFreqCounter(false);        
        cpu.clearDevices();
    }

    @Override
    public boolean isBreakpointSupported() { return true; }
    @Override
    public void setBreakpoint(int pos, boolean set) {
        if (set) breaks.add(pos);
        else breaks.remove(pos);
    }
    @Override
    public boolean getBreakpoint(int pos) { return breaks.contains(pos); }
    
    
    /**
     * Reset CPU (initialize before run)
     */    
    @Override
    public void reset() {
    	reset(0);
    }
    
    @Override
    public void reset(int startPos) {
        SP = A = B = C = D = E = H = L = 0;
        Flags = 2; //0000 0010b
        PC = startPos; 
        INTE = false;
        run_state = ICPU.STATE_STOPPED_BREAK;
        cpuThread = null;
        setRuntimeFreqCounter(false);
        cpu.fireCpuRun(status,run_state);
        cpu.fireCpuState();
    }
    
    public int getPC() { return PC; }
    
    /**
     * Create CPU Thread and start it until CPU halt (instruction hlt)
     * or until address fallout
     */ 
    @Override
    public void execute() {
        cpuThread = new Thread(this, "i8080");
        cpuThread.start();
    }
    /**
     * Force (external) breakpoint
     */
    @Override
    public void pause() {
        run_state = ICPU.STATE_STOPPED_BREAK;
        setRuntimeFreqCounter(false);
        cpu.fireCpuRun(status,run_state);
    }
    /**
     *  Stops an emulation
     */    
    @Override
    public void stop() {
        run_state = ICPU.STATE_STOPPED_NORMAL;
        setRuntimeFreqCounter(false);
        cpu.fireCpuRun(status,run_state);
    }
    // vykona 1 krok - bez merania casov (bez real-time odozvy)
    @Override
    public void step() {
        if (run_state == ICPU.STATE_STOPPED_BREAK) {
            try {
                run_state = ICPU.STATE_RUNNING;
                evalStep();
                if (run_state == ICPU.STATE_RUNNING)
                    run_state = ICPU.STATE_STOPPED_BREAK;
            }
            catch (IndexOutOfBoundsException e) {
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
            }
            cpu.fireCpuRun(status,run_state);
            cpu.fireCpuState();
        }
    }
    
    public void interrupt(short b1, short b2, short b3) {
        if (INTE == false) return;
        isINT = true;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }
    
    
    /* DOWN: GUI interaction */
    @Override
    public IDebugColumn[] getDebugColumns() { return status.getDebugColumns(); }
    @Override
    public void setDebugValue(int index, int col, Object value) {
        status.setDebugColVal(index, col, value);
    }
    @Override
    public Object getDebugValue(int index, int col) {
        return status.getDebugColVal(index, col);
    }
    @Override
    public JPanel getStatusGUI() { return status; }    
    
    /* DOWN: CPU Context */
//    public int getPC() { return PC; }
    
    public boolean setPC(int memPos) { 
        if (memPos < 0) return false;
        PC = memPos;
        return true;
    }
    
    /* DOWN: other */
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
        
        run_state = ICPU.STATE_RUNNING;
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
        while(run_state == ICPU.STATE_RUNNING) {
            i++;
            startTime = System.nanoTime();
            cycles_executed = 0;
            try { 
                while((cycles_executed < cycles_to_execute)
                        && (run_state == ICPU.STATE_RUNNING)) {
                    cycles = evalStep();
                    cycles_executed += cycles;
                    long_cycles += cycles;
                    if (getBreakpoint(PC) == true)
                        throw new Error();
                }
            }
            catch (IndexOutOfBoundsException e) {
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
                break;
            }
            catch (Error er) {
                run_state = ICPU.STATE_STOPPED_BREAK;
                break;
            }
            endTime = System.nanoTime() - startTime;
            if (endTime < slice) {
                // time correction
                try { Thread.sleep((slice - endTime)/1000000); }
                catch(java.lang.InterruptedException e) {}
            }
        }
        setRuntimeFreqCounter(false);
        cpu.fireCpuState();
        cpu.fireCpuRun(status,run_state);
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
    
    /* Get an 8080 register and return it */
    private short getreg(int reg) {
        switch (reg) {
            case 0: return B;  case 1: return C;  case 2: return D;
            case 3: return E;  case 4: return H;  case 5: return L;
            case 6: return ((Short)mem.read((H << 8) | L)).shortValue();
            case 7: return A;
        }
        return 0;
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, short val) {
        switch (reg) {
            case 0: B = val; break; case 1: C = val; break;
            case 2: D = val; break; case 3: E = val; break;
            case 4: H = val; break; case 5: L = val; break;
            case 6: mem.write((H << 8) | L, val); break;
            case 7: A = val;
        }
    }
    
    /* Put a value into an 8080 register pair */
    void putpair(int reg, int val) {
        short high, low;
        high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
        switch (reg) {
            case 0: B = high; C = low; break; case 1: D = high; E = low; break;
            case 2: H = high; L = low; break; case 3: SP = val; break;
        }
    }

    /* Return the value of a selected register pair */
    int getpair(int reg) {
        switch (reg) {
            case 0: return (B << 8) | C; case 1: return (D << 8) | E;
            case 2: return (H << 8) | L; case 3: return SP;
        }
        return 0;
    }

    /* Return the value of a selected register pair, in PUSH
       format where 3 means A& flags, not SP
     */
    private int getpush(int reg) {
        int stat;
        switch (reg) {
            case 0: return (B << 8) | C; case 1: return (D << 8) | E;
            case 2: return (H << 8) | L; case 3: stat = (A << 8) | Flags;
                stat |= 0x02; return stat;
        }
        return 0;
    }

    /* Place data into the indicated register pair, in PUSH
       format where 3 means A& flags, not SP
     */
    private void putpush(int reg, int val) {
        short high, low;
        high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
        switch (reg) {
            case 0: B = high; C = low; break; case 1: D = high; E = low; break;
            case 2: H = high; L = low; break;
            case 3: A = (short)((val >>> 8) & 0xFF);
                Flags = (short)(val & 0xFF);
                break;
        }
    }

    /* Set the <C>arry, <S>ign, <Z>ero and <P>arity flags following
       an arithmetic operation on 'reg'. 8080 changes AC flag
    */
    private void setarith(int reg, int before) {
        if ((reg & 0x100) != 0) Flags |= flagC; else Flags &= (~flagC);
        if ((reg & 0x80) != 0) Flags |= flagS;  else Flags &= (~flagS);
        if ((reg & 0xff) == 0) Flags |= flagZ;  else Flags &= (~flagZ);
        // carry from 3.bit to 4.
        if (((before & 8) == 8) && ((reg & 0x10) == 0x10)) Flags |= flagAC;
        else Flags &= (~flagAC);
        parity(reg);
    }

    /* Set the <S>ign, <Z>ero amd <P>arity flags following
       an INR/DCR operation on 'reg'.
    */
    private void setinc(int reg, int before) {
        if ((reg & 0x80) != 0) Flags |= flagS; else Flags &= (~flagS);
        if ((reg & 0xff) == 0) Flags |= flagZ; else Flags &= (~flagZ);
        if (((before & 8) == 8) && ((reg & 0x10) == 0x10)) Flags |= flagAC;
        else Flags &= (~flagAC);
        parity(reg);
    }

    /* Set the <C>arry, <S>ign, <Z>ero amd <P>arity flags following
       a logical (bitwise) operation on 'reg'.
    */
    private void setlogical(int reg) {
        Flags &= (~flagC);
        if ((reg & 0x80) != 0) Flags |= flagS; else Flags &= (~flagS);
        if ((reg & 0xff) == 0) Flags |= flagZ; else Flags &= (~flagZ);
        Flags &= (~flagAC); parity(reg);
    }

    /* Set the Parity (P) flag based on parity of 'reg', i.e., number
       of bits on even: P=0200000, else P=0
    */
    private void parity(int reg)
    {
        if (parity_table[reg & 0xFF] == 1) Flags |= flagP;
        else Flags &= (~flagP);
    }

    /* Test an 8080 flag condition and return 1 if true, 0 if false */
    private int cond(int con) {
        switch (con) {
            case 0: if ((Flags & flagZ) == 0) return 1; break;
            case 1: if ((Flags & flagZ) != 0) return 1; break;
            case 2: if ((Flags & flagC) == 0) return 1; break;
            case 3: if ((Flags & flagC) != 0) return 1; break;
            case 4: if ((Flags & flagP) == 0) return 1; break;
            case 5: if ((Flags & flagP) != 0) return 1; break;
            case 6: if ((Flags & flagS) == 0) return 1; break;
            case 7: if ((Flags & flagS) != 0) return 1; break;
        }
        return 0;
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
                    mem.writeWord(SP-2,PC); SP -= 2; PC = b1 & 0x38; return 11;
                } else if (b1 == 0315) {                        /* CALL */
                    mem.writeWord(SP-2, PC+2); SP -= 2; 
                    PC = (int)(((b3 & 0xFF) << 8) | (b2 & 0xFF));
                    return 17;
                }
            }
            isINT = false;
        }        
        OP = ((Short)mem.read(PC++)).shortValue();
        if (OP == 118) { // hlt?
            run_state = ICPU.STATE_STOPPED_NORMAL;
            return 7;
        }

       /* Handle below all operations which refer to registers or register pairs.
          After that, a large switch statement takes care of all other opcodes */
        if ((OP & 0xC0) == 0x40) {                             /* MOV */
            putreg((OP >>> 3) & 0x07, getreg(OP & 0x07)); 
            if (((OP & 0x07) == 6) || (((OP >>> 3) & 0x07) == 6)) return 7; else return 5;
        } else if ((OP & 0xC7) == 0x06) {                      /* MVI */
            putreg((OP >>> 3) & 0x07, ((Short)mem.read(PC++)).shortValue());
            if (((OP >>> 3) & 0x07) == 6) return 10; else return 7;
        } else if ((OP & 0xCF) == 0x01) {                      /* LXI */
            putpair((OP >>> 4) & 0x03, (Integer)mem.readWord(PC)); PC += 2; return 10;
        } else if ((OP & 0xEF) == 0x0A) {                      /* LDAX */
            putreg(7, ((Short)mem.read(getpair((OP >>> 4) & 0x03))).shortValue()); return 7;
        } else if ((OP & 0xEF) == 0x02) {                      /* STAX */
            mem.write(getpair((OP >>> 4) & 0x03), getreg(7)); return 7;
        } else if ((OP & 0xF8) == 0xB8) {                      /* CMP */
            int X = A; DAR = A & 0xFF; DAR -= getreg(OP & 0x07);
            setarith(DAR, X); if ((OP & 0x07) == 6) return 7; else return 4;
        } else if ((OP & 0xC7) == 0xC2) {                      /* JMP <condition> */
            if (cond((OP >>> 3) & 0x07) == 1) PC = (Integer)mem.readWord(PC);
            else PC += 2; return 10;
        } else if ((OP & 0xC7) == 0xC4) {                      /* CALL <condition> */
            if (cond((OP >>> 3) & 0x07) == 1) {
                DAR = (Integer)mem.readWord(PC); PC += 2; mem.writeWord(SP-2,PC);
                SP -= 2; PC = DAR; return 17;
            } else { PC += 2; return 11; }
        } else if ((OP & 0xC7) == 0xC0) {                      /* RET <condition> */
            if (cond((OP >>> 3) & 0x07) == 1) {
                PC = (Integer)mem.readWord(SP); SP += 2;
            } return 10;
        } else if ((OP & 0xC7) == 0xC7) {                      /* RST */
            mem.writeWord(SP-2,PC); SP -= 2; PC = OP & 0x38; return 11;
        } else if ((OP & 0xCF) == 0xC5) {                      /* PUSH */
            DAR = getpush((OP >>> 4) & 0x03); mem.writeWord(SP-2,DAR); SP -= 2;
            return 11;
        } else if ((OP & 0xCF) == 0xC1) {                      /*POP */
            DAR = (Integer)mem.readWord(SP); SP += 2; putpush((OP >>> 4) & 0x03, DAR);
            return 10;
        } else if ((OP & 0xF8) == 0x80) {                      /* ADD */
            int X = A; DAR = A & 0xF0; A += getreg(OP & 0x07); setarith(A,X);
            A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7; return 4;
        } else if ((OP & 0xF8) == 0x88) {                      /* ADC */
            int X = A; A += getreg(OP & 0x07); if ((Flags & flagC) != 0) A++; 
            setarith(A,X); A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7; 
            return 4;
        } else if ((OP & 0xF8) == 0x90) {                      /* SUB */
            int X = A; A -= getreg(OP & 0x07); setarith(A,X);
            A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7; return 4;
        } else if ((OP & 0xF8) == 0x98) {                      /* SBB */
            int X = A; A -= (getreg(OP & 0x07)); if ((Flags & flagC) != 0) A--; 
            setarith(A,X); A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7;
            return 4;
        } else if ((OP & 0xC7) == 0x04) {                      /* INR */
            DAR = getreg((OP >>> 3) & 0x07) + 1; setinc(DAR, DAR-1);
            DAR = DAR & 0xFF; putreg((OP >>> 3) & 0x07, (short)DAR); return 5;
        } else if ((OP & 0xC7) == 0x05) {                      /* DCR */
            DAR = getreg((OP >>> 3) & 0x07) - 1; setinc(DAR,DAR+1);
            DAR = DAR & 0xFF; putreg((OP >>> 3) & 0x07, (short)DAR); return 5;
        } else if ((OP & 0xCF) == 0x03) {                      /* INX */
            DAR = getpair((OP >>> 4) & 0x03) + 1; DAR = DAR & 0xFFFF;
            putpair((OP >>> 4) & 0x03, DAR); return 5;
        } else if ((OP & 0xCF) == 0x0B) {                      /* DCX */
            DAR = getpair((OP >>> 4) & 0x03) - 1; DAR = DAR & 0xFFFF;
            putpair((OP >>> 4) & 0x03, DAR); return 5;
        } else if ((OP & 0xCF) == 0x09) {                      /* DAD */
            DAR = getpair((OP >>> 4) & 0x03); DAR += getpair(2);
            if ((DAR & 0x10000) != 0) Flags |= flagC; else Flags &= (~flagC);
            DAR = DAR & 0xFFFF; putpair(2, DAR); return 10;
        } else if ((OP & 0xF8) == 0xA0) {                      /* ANA */
            A &= getreg(OP & 0x07); setlogical(A); A &= 0xFF; return 4;
        } else if ((OP & 0xF8) == 0xA8) {                      /* XRA */
            A ^= getreg(OP & 0x07); setlogical(A); A &= 0xFF; return 4;
        } else if ((OP & 0xF8) == 0xB0) {                      /* ORA */
            A |= getreg(OP & 0x07); setlogical(A); A &= 0xFF; return 4;
        }
        /* The Big Instruction Decode Switch */
        switch (OP) {
            /* Logical instructions */
            case 0376:                                     /* CPI */
                int X = A; DAR = A & 0xFF; DAR -= ((Short)mem.read(PC++)).shortValue();
                setarith(DAR,X); return 7;
            case 0346:                                     /* ANI */
                A &= ((Short)mem.read(PC++)).shortValue(); Flags &= (~flagC);
                Flags &= (~flagAC); setlogical(A); A &= 0xFF; return 7;
            case 0356:                                     /* XRI */
                A ^= ((Short)mem.read(PC++)).shortValue(); Flags &= (~flagC);
                Flags &= (~flagAC); setlogical(A); A &= 0xFF; return 7;
            case 0366:                                     /* ORI */
                A |= ((Short)mem.read(PC++)).shortValue(); Flags &= (~flagC);
                Flags &= (~flagAC); setlogical(A); A &= 0xFF; return 7;
            /* Jump instructions */
            case 0303:                                     /* JMP */
                PC = (Integer)mem.readWord(PC); return 10;
            case 0351:                                     /* PCHL */
                PC = (H << 8) | L; return 5;
            case 0315:                                     /* CALL */
                mem.writeWord(SP-2, PC+2); SP -= 2; PC = (Integer)mem.readWord(PC);
                return 17;
            case 0311:                                     /* RET */
                PC = (Integer)mem.readWord(SP); SP += 2; return 10;
            /* Data Transfer Group */
            case 062:                                      /* STA */
                DAR = (Integer)mem.readWord(PC); PC += 2; mem.write(DAR, A); return 13;
            case 072:                                      /* LDA */
                DAR = (Integer)mem.readWord(PC); PC += 2; 
                A = ((Short)mem.read(DAR)).shortValue(); return 13;
            case 042:                                      /* SHLD */
                DAR = (Integer)mem.readWord(PC); PC += 2;
                mem.writeWord(DAR, (H << 8) | L); return 16;
            case 052:                                      /* LHLD BUG !*/
                DAR = (Integer)mem.readWord(PC); PC += 2;
                L = ((Short)mem.read(DAR)).shortValue();
                H = ((Short)mem.read(DAR+1)).shortValue(); return 16;
            case 0353:                                     /* XCHG */
                short x = H, y = L; H = D; L = E; D = x; E = y; return 4;
            /* Arithmetic Group */
            case 0306:                                     /* ADI */
                DAR = A; A += ((Short)mem.read(PC++)).shortValue(); 
                setarith(A,DAR); A = (short)(A & 0xFF); return 7;
            case 0316:                                     /* ACI */
                DAR = A; A += ((Short)mem.read(PC++)).shortValue();
                if ((Flags & flagC) != 0) A++; setarith(A,DAR); A = (short)(A & 0xFF);
                return 7;
            case 0326:                                     /* SUI */
                DAR = A; A -= ((Short)mem.read(PC++)).shortValue(); setarith(A,DAR);
                A = (short)(A & 0xFF); return 7;
            case 0336:                                     /* SBI */
                DAR = A; A -= ((Short)mem.read(PC++)).shortValue(); if ((Flags & flagC) != 0) A--;
                setarith(A,DAR); A = (short)(A & 0xFF); return 7;
            case 047:                                      /* DAA */
                DAR = A;
                if (((DAR&0x0F) > 9) || ((Flags & flagAC) != 0)) {
                    DAR += 6; 
                    if ((DAR & 0x10) != (A & 0x10)) Flags |= flagAC;
                    else Flags &= (~flagAC);
                    A = (short)(DAR & 0xFF);
                }
                DAR = (A >>> 4)&0x0F;
                if ((DAR > 9) || ((Flags & flagC) != 0)) {
                    DAR += 6;
                    if ((DAR & 0x10) != 0) Flags |= flagC;
                    A &= 0x0F; A |= ((DAR << 4)&0xF0);
                }
                if ((A & 0x80) != 0) Flags |= flagS; else Flags &= (~flagS);
                if ((A & 0xff) == 0) Flags |= flagZ; else Flags &= (~flagZ);
                parity(A); A = (short)(A & 0xFF); return 4;
            case 07: {                                     /* RLC */
                int xx = (A << 9) & 0200000; if (xx != 0) Flags |= flagC;
                else Flags &= (~flagC); A = (short)((A << 1) & 0xFF);
                if (xx != 0) A |= 0x01; return 4; }
            case 017: {                                     /* RRC */
                if ((A & 0x01) == 1) Flags |= flagC; else Flags &= (~flagC);
                A = (short)((A >>> 1) & 0xFF); 
                if ((Flags & flagC) != 0) A |= 0x80; return 4; }
            case 027: {                                    /* RAL */
                int xx = (A << 9) & 0200000; A = (short)((A << 1) & 0xFF);
                if ((Flags & flagC) != 0) A |= 1; else A &= 0xFE;
                if (xx != 0) Flags |= flagC; else Flags &= (~flagC); return 4; }
            case 037: {                                    /* RAR */
                int xx = 0; if ((A & 0x01) == 1) xx |= 0200000;
                A = (short)((A >>> 1) & 0xFF);
                if ((Flags & flagC) != 0) A |= 0x80; else A &= 0x7F;
                if (xx != 0) Flags |= flagC; else Flags &= (~flagC); return 4; }
            case 057:                                      /* CMA */
                A = (short)(~A); A &= 0xFF; return 4;
            case 077:                                      /* CMC */
                if ((Flags & flagC) != 0) Flags &= (~flagC); else Flags |= flagC;
                return 4;
            case 067:                                      /* STC */
                Flags |= flagC; return 4;
            /* Stack, I/O & Machine Control Group */
            case 0:                                        /* NOP */
                return 4;
            case 0343:                                     /* XTHL */
                DAR = (Integer)mem.readWord(SP); mem.writeWord(SP, (H << 8) | L);
                H = (short)((DAR >>> 8) & 0xFF); L = (short)(DAR & 0xFF);
                return 18;
            case 0371:                                     /* SPHL */
                SP = (H << 8) | L; return 5;
            case 0373:                                     /* EI */
                INTE = true; return 4;
            case 0363:                                     /* DI */
                INTE = false; return 4;
            case 0333:                                     /* IN */
                DAR = ((Short)mem.read(PC++)).shortValue();
                A = cpu.fireIO(DAR, true, (short)0);
                return 10;
            case 0323:                                     /* OUT */
                DAR = ((Short)mem.read(PC++)).shortValue();
                cpu.fireIO(DAR, false, A); 
                return 10;
        }
        run_state = ICPU.STATE_STOPPED_BAD_INSTR;
        return 0;
    }
    
    @Override
    public int getInstrPosition() { return PC; }

    // get the address from next instruction
    // this method exist only from a view of effectivity
    @Override
    public int getInstrPosition(int pos) { return status.getNextPosition(pos); }
    @Override
    public boolean setInstrPosition(int pos) { return setPC(pos); }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
		
	}

}
