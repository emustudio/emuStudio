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
import runtime.StaticDialogs;

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
    
    // sychronization locks for PC
    private Object PClock = new Object();

    // 2 sets of 6 GPR
    private volatile short B; private short B_S;
    private volatile short C; private short C_S;
    private volatile short D; private short D_S;
    private volatile short E; private short E_S;
    private volatile short H; private short H_S;
    private volatile short L; private short L_S;
    
    // accumulator and flags
    private volatile short A; private short A_S;
    private volatile short F; private volatile short F_S;
    
    // special registers
    private volatile int PC = 0; private volatile int SP = 0;
    private volatile int IX = 0; private volatile int IY = 0;
    private volatile short I = 0; private volatile short R = 0; // interrupt r., refresh r.
    private boolean[] IFF; // interrupt enable flip-flops
    
    private volatile boolean INTE = false; // interrupts
    private boolean isInt = false;
    private byte intMode = 0; // interrupt mode (0,1,2)
    
    public static final int flagS = 0x80, flagZ = 0x40, flagH = 0x10, flagPV = 0x4,flagN = 0x2, flagC = 0x1;
    
    // cpu speed
    private long long_cycles = 0; // count of executed cycles for runtime freq. computing
    private java.util.Timer freqScheduler;
    private RuntimeFrequencyCalculator rfc;
    private int sliceCheckTime = 100;
    private stateEnum run_state; // state of emulation run

    /* parityTable[i] = (number of 1's in i is odd) ? 0 : 4, i = 0..255 */
    private final static short parityTable[] = {
        4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,
        0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,
        0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,
        4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4
    };
        
    // incTable[i] = (i & 0x28) | ((!i) << 6) | ((!(i&0xf))<<4) | (((i&0x80) > 0)<<7) | ((i==0x80)<<2);
    private final static short incTable[] = {
         80,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8, 16,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8,
         48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40, 48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40,
         16,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8, 16,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8,
         48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40, 48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40,
        144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,
        176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168,176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168,
        144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,
        176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168,176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168, 80
    };

    // decTable[i] = (i & 0x28) | ((!i) << 6) | (((i&0xf) == 0xf)<<4) | (((i&0x80) > 0)<<7) | ((i==0x7f)<<2) | 2;
    private final static short decTable[] = {
         66,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,  2,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,
         34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58, 34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58,
          2,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,  2,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,
         34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58, 34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58,
        130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,
        162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186,162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186,
        130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,
        162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186,162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186
    };
    
    // used for add - determines carry and half-carry
    // cbitsTable[i] = (i & 0x10) | ((i >> 8) & 1), i = 0..511
    private final static short cbitsTable[] = {
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,
    };
    
    // rrcaTable[i] = ((i & 1) << 7)|(i>>1);
    private final static short rrcaTable[] = {
        0,128,1,129,2,130,3,131,4,132,5,133,6,134,7,135,8,136,9,137,10,138,11,139,12,140,13,141,14,142,15,143,16,144,17,145,18,146,19,147,20,148,21,149,22,150,
        23,151,24,152,25,153,26,154,27,155,28,156,29,157,30,158,31,159,32,160,33,161,34,162,35,163,36,164,37,165,38,166,39,167,40,168,41,169,42,170,43,171,44,172,45,
        173,46,174,47,175,48,176,49,177,50,178,51,179,52,180,53,181,54,182,55,183,56,184,57,185,58,186,59,187,60,188,61,189,62,190,63,191,64,192,65,193,66,194,67,195,
        68,196,69,197,70,198,71,199,72,200,73,201,74,202,75,203,76,204,77,205,78,206,79,207,80,208,81,209,82,210,83,211,84,212,85,213,86,214,87,215,88,216,89,217,90,
        218,91,219,92,220,93,221,94,222,95,223,96,224,97,225,98,226,99,227,100,228,101,229,102,230,103,231,104,232,105,233,106,234,107,235,108,236,109,237,110,238,111,239,112,240,
        113,241,114,242,115,243,116,244,117,245,118,246,119,247,120,248,121,249,122,250,123,251,124,252,125,253,126,254,127,255
    };
    
    // daaTable[i] = (i & 0x80)|((i==0) << 6)|parityTable[i]
    private final static short daaTable[] = {
        68,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,4,0,0,4,0,4,4,0,0,4,4,0,4,
        0,0,4,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,4,0,0,4,0,4,4,0,0,4,4,0,4,0,0,4,0,4,4,0,4,0,0,4,4,
        0,0,4,0,4,4,0,128,132,132,128,132,128,128,132,132,128,128,132,128,132,132,128,132,128,128,132,128,132,132,128,128,132,132,128,132,128,128,132,132,128,128,132,128,132,
        132,128,128,132,132,128,132,128,128,132,128,132,132,128,132,128,128,132,132,128,128,132,128,132,132,128,132,128,128,132,
        128,132,132,128,128,132,132,128,132,128,128,132,128,132,132,128,132,128,128,132,132,128,128,132,128,132,132,128,128,132,
        132,128,132,128,128,132,132,128,128,132,128,132,132,128,132,128,128,132,128,132,132,128,128,132,132,128,132,128,128,132,
    };
    
    // cbits2Z80Table[i]       0..511  (i & 0x10) | (((i >> 6) ^ (i >> 5)) & 4) | ((i >> 8) & 1) | 2
    private static final short cbits2Z80Table[] = {
         2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18,
         2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18,18, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22,
         6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22,22, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23,
         7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23,23,
         3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,
         3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19
    };

    // negTable[i] = (i&0x80)|((i==0)<<6)|2|((i==0x80)<<2)|(i!=0)|(((i&0x0f)!=0)<<4)
    private static final short negTable[] = {
        66,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,3,19,19,19,19,19,19,19,19,19,19,19,19,19,
        19,19,3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,3,19,19,19,19,19,19,19,19,19,19,
        19,19,19,19,19,3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,3,19,19,19,19,19,19,19,19,19,19,19,19,19,19,19,135,147,147,147,147,147,147,147,
        147,147,147,147,147,147,147,147,131,147,147,147,147,147,147,147,147,147,147,147,147,147,147,147,131,147,147,147,147,147,147,147,147,147,147,147,147,147,147,147,131,147,147,147,147,
        147,147,147,147,147,147,147,147,147,147,147,131,147,147,147,147,147,147,147,147,147,147,147,147,147,147,147,131,147,147,147,147,147,147,147,147,147,147,147,147,147,147,147,131,147,
        147,147,147,147,147,147,147,147,147,147,147,147,147,147,131,147,147,147,147,147,147,147,147,147,147,147,147,147,147,147,
    }; 
    
    // cbitsZ80Table[i] = (i & 0x10) | (((i >> 6) ^ (i >> 5)) & 4) | ((i >> 8) & 1), i = 0..511
    private static final short cbitsZ80Table[] = {
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,
         0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,
         4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,
         4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,
         5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,
         5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,21,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,
         1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17,17
    };

    /* incZ80Table[i] = (i & 0xa8) | (((i & 0xff) == 0) << 6) |(((i & 0xf) == 0) << 4) | ((i == 0x80) << 2), i = 0..256 */
    private static final short incZ80Table[] = {
         80,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8, 16,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8,
         48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40, 48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40,
         16,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8, 16,  0,  0,  0,  0,  0,  0,  0,  8,  8,  8,  8,  8,  8,  8,  8,
         48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40, 48, 32, 32, 32, 32, 32, 32, 32, 40, 40, 40, 40, 40, 40, 40, 40,
        148,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,
        176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168,176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168,
        144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,144,128,128,128,128,128,128,128,136,136,136,136,136,136,136,136,
        176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168,176,160,160,160,160,160,160,160,168,168,168,168,168,168,168,168, 80,
    };
    
    /* decZ80Table[i] = (i & 0xa8) | (((i & 0xff) == 0) << 6)|(((i & 0xf) == 0xf) << 4) | ((i == 0x7f) << 2) | 2, i = 0..255 */
    private static final short decZ80Table[] = {
         66,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,  2,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26, 34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58, 34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58,
          2,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,  2,  2,  2,  2,  2,  2,  2,  2, 10, 10, 10, 10, 10, 10, 10, 26,34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 58,
         34, 34, 34, 34, 34, 34, 34, 34, 42, 42, 42, 42, 42, 42, 42, 62,130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,
        162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186,162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186,130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,
        130,130,130,130,130,130,130,130,138,138,138,138,138,138,138,154,162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186,162,162,162,162,162,162,162,162,170,170,170,170,170,170,170,186
    };
 
    // andTable[i] = (i&0x80)|((i==0)<<6)|0x10|parityTable[i];
    private static final short andTable[] = {
        84,16,16,20,16,20,20,16,16,20,20,16,20,16,16,20,16,20,20,16,20,16,16,20,20,16,16,20,16,20,20,16,16,20,20,16,20,16,16,20,20,16,16,20,16,20,20,16,20,16,16,20,16,20,20,16,16,20,20,16,20,16,16,20,16,20,20,16,20,16,16,20,20,16,16,20,
        16,20,20,16,20,16,16,20,16,20,20,16,16,20,20,16,20,16,16,20,20,16,16,20,16,20,20,16,16,20,20,16,20,16,16,20,16,20,20,16,20,16,16,20,20,
        16,16,20,16,20,20,16,144,148,148,144,148,144,144,148,148,144,144,148,144,148,148,144,148,144,144,148,144,148,148,144,144,148,148,144,148,144,144,148,148,144,144,148,144,148,
        148,144,144,148,148,144,148,144,144,148,144,148,148,144,148,144,144,148,148,144,144,148,144,148,148,144,148,144,144,148,144,148,148,144,144,148,148,144,148,144,144,148,144,148,148,
        144,148,144,144,148,148,144,144,148,144,148,148,144,144,148,148,144,148,144,144,148,148,144,144,148,144,148,148,144,148,144,144,148,144,148,148,144,144,148,148,144,148,144,144,148,
    };

    // cpTable[i] = (i&0x80)|((i==0)<<6)|2
    private static final short cpTable[] = {
        66,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
        2,2,2,2,2,2,2,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,
        130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,
        130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,130,
    };
    
    /**
     * Constructor
     */
    public CpuZ80() {
        IFF = new boolean[2];
        cpu = new CpuContext(this);
        run_state = stateEnum.stoppedNormal;
        breaks = new HashSet();
        rfc = new RuntimeFrequencyCalculator();
        freqScheduler = new java.util.Timer();
    }
    
    public String getName() { return "Zilog Z80"; }
    public String getCopyright() { return "\u00A9 Copyright 2008, Peter Jakubčo"; }
    public String getDescription() {
        return "Implementation of Zilog Z80 8bit CPU. With its architecture"
               + " it is similar to Intel's 8080 but something is modified and"
               + " extended.";
    }
    public String getVersion() { return "0.1b"; }

    /**
     * Should be called only once
     */
    public boolean initialize(IMemoryContext mem, ISettingsHandler sHandler) {
        if (mem == null)
            throw new NullPointerException("CPU must have access to memory");
        if (!mem.getID().equals("byte_simple_variable")) {
            StaticDialogs.showErrorMessage("Operating memory type is not supported"
                    + " for this kind of CPU.");
            return false;
        }
        this.mem = mem;
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
        if (cpuThread != null && cpuThread.isAlive()) return;
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

    // get the address from next instruction
    // this method exist only from a view of effectivity
    public int getNextPC(int memPos) {
        return status.getNextPosition(memPos);
    }
    public int getPC() { synchronized(PClock) { return PC; } }
    public int getSP() { return SP; }
    public short getA() { return A; }
    public short getF() { return F; }

    public short getB() { return B; }
    public short getC() { return C; }
    public int getBC() { return (((B<<8)|C)&0xFFFF); }
    public short getD() { return D; }
    public short getE() { return E; }
    public int getDE() { return (((D<<8)|E)&0xFFFF); }
    public short getH() { return H; }
    public short getL() { return L; }
    public int getHL() { return (((H<<8)|L)&0xFFFF); }
    public int getIX() { return IX; }
    public int getIY() { return IY; }
    public short getI() { return I; }
    public short getR() { return R; }

    public short getA_S() { return A_S; }
    public short getF_S() { return F_S; }
    public short getB_S() { return B_S; }
    public short getC_S() { return C_S; }
    public int getBC_S() { return (((B_S<<8)|C_S)&0xFFFF); }
    public short getD_S() { return D_S; }
    public short getE_S() { return E_S; }
    public int getDE_S() { return (((D_S<<8)|E_S)&0xFFFF); }
    public short getH_S() { return H_S; }
    public short getL_S() { return L_S; }
    public int getHL_S() { return (((H_S<<8)|L_S)&0xFFFF); }
    
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
        SP = IX = IY = 0;
        I = R = 0;
        A = B = C = D = E = H = L = 0;
        A_S = B_S = C_S = D_S = E_S = H_S = L_S = F = F_S = 0;
        IFF[0] = false;
        IFF[1] = false;
        synchronized(PClock) { PC = mem.getProgramStart(); }
        run_state = stateEnum.stoppedBreak;
        cpuThread = null;
        setRuntimeFreqCounter(false);
        cpu.fireCpuRun(status,run_state);
        cpu.fireCpuState();
    }

    public void destroy() {}

    public int getSliceTime() { return sliceCheckTime; }
    public void setSliceTime(int t) { sliceCheckTime = t; }

    
    /* Get an GPR register and return it */
    private short getreg(int reg) {
        switch (reg) {
            case 0: return B; case 1: return C;
            case 2: return D; case 3: return E;
            case 4: return H; case 5: return L;
            case 6: return ((Short)mem.read((H << 8) | L)).shortValue();
            case 7: return A;
        }
        return 0;
    }
    
    /* Get an GPR register and return it */
    private short getreg2(int reg) {
        switch (reg) {
            case 0: return B; case 1: return C;
            case 2: return D; case 3: return E;
            case 4: return H; case 5: return L; case 7: return A;
        }
        return 0;
    }

    /* Put a value into an GPR register from memory */
    private void putreg(int reg, short val) {
        val &= 0xff;
        switch (reg) {
            case 0: B = val; break;
            case 1: C = val; break;
            case 2: D = val; break;
            case 3: E = val; break;
            case 4: H = val; break;
            case 5: L = val; break;
            case 6: mem.write((H << 8) | L, val); break;
            case 7: A = val;
        }
    }

    /* Put a value into an GPR register from memory */
    private void putreg2(int reg, short val) {
        val &= 0xff;
        switch (reg) {
            case 0: B = val; break;
            case 1: C = val; break;
            case 2: D = val; break;
            case 3: E = val; break;
            case 4: H = val; break;
            case 5: L = val; break;
            case 7: A = val;
        }
    }
    
    /* Put a value into an register pair */
    private void putpair(int reg, int val) {
        short high, low;
        val &= 0xFFFF;
        switch (reg) {
            case 0: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    B = high; C = low; break;
            case 1: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    D = high; E = low; break;
            case 2: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    H = high; L = low; break;
            case 3: SP = val; break;
        }
    }

    /* Put a value into an register pair (POP) */
    private void putpair2(int reg, int val) {
        short high, low;
        val &= 0xFFFF;
        switch (reg) {
            case 0: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    B = high; C = low; break;
            case 1: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    D = high; E = low; break;
            case 2: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    H = high; L = low; break;
            case 3: high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
                    A = high; F = low; break;
        }
    }
    
    /* Return the value of a selected register pair */
    private int getpair(int reg) {
        switch (reg) {
            case 0: return (B << 8) | C;
            case 1: return (D << 8) | E;
            case 2: return (H << 8) | L;
            case 3: return SP;
        }
        return 0;
    }
    
    /* Return the value of a selected register pair */
    private int getpair2(int reg) {
        switch (reg) {
            case 0: return (B << 8) | C;
            case 1: return (D << 8) | E;
            case 2: return (H << 8) | L;
            case 3: return (A << 8) | F;
        }
        return 0;
    }

    /* Return the value of a selected register pair */
    private int getpair(short special,int reg) {
        switch (reg) {
            case 0: return (B << 8) | C;
            case 1: return (D << 8) | E;
            case 2: return (special == 0xDD)?IX:IY;
            case 3: return SP;
        }
        return 0;
    }
    
    private boolean getCC(int cc) {
        switch (cc) {
            case 0: return ((F & flagZ) == 0); // NZ
            case 1: return ((F & flagZ) != 0); // Z
            case 2: return ((F & flagC) == 0); // NC
            case 3: return ((F & flagC) != 0); // C
            case 4: return ((F & flagPV) == 0);// PO
            case 5: return ((F & flagPV) != 0);// PE
            case 6: return ((F & flagS) == 0); // P
            case 7: return ((F & flagS) != 0); // M
        }
        return false;
    }
    
    private boolean getCC1(int cc) {
        switch (cc) {
            case 0: return ((F & flagZ) == 0); // NZ
            case 1: return ((F & flagZ) != 0); // Z
            case 2: return ((F & flagC) == 0); // NC
            case 3: return ((F & flagC) != 0); // C
        }
        return false;
    }
    
    /**
     * Put a value into IX/IY register
     */
    private void putspecial(short spec, int val) {
        val &= 0xFFFF;
        switch (spec) {
            case 0xDD: IX = val; return;
            case 0xFD: IY = val; return;
        }
    }

    /**
     * Get value from IX/IY register
     */
    private int getspecial(short spec) {
        switch (spec) {
            case 0xDD: return IX;
            case 0xFD: return IY;
        }
        return 0;
    }
    
    /**
     * This method evaluates one instruction. It provides all phases:
     * Fetch, Decode, Execute, Store.
     */
    private int evalStep() {
        short OP; int tmp, tmp1, tmp2, tmp3;
        short special = 0; // prefix if available = 0xDD or 0xFD
        byte b;

        /* if interrupt is waiting, instruction won't be read from memory
         * but from one or all of 3 bytes (b1,b2,b3) which represents either
         * rst or call instruction incomed from external peripheral device
         */
//        if (isINT == true) {
//            if (INTE == true) {
//                if ((b1 & 0xC7) == 0xC7) {                      /* RST */
//                    mem.writeWord(SP-2,PC); SP -= 2; PC = b1 & 0x38; return 11;
//                } else if (b1 == 0315) {                        /* CALL */
//                    mem.writeWord(SP-2, PC+2); SP -= 2; 
//                    PC = (int)(((b3 & 0xFF) << 8) | (b2 & 0xFF));
//                    return 17;
//                }
//            }
//            isINT = false;
//        }        
        synchronized(PClock) { OP = ((Short)mem.read(PC++)).shortValue(); }
        
        /* Handle below all operations which refer to registers or register pairs.
          After that, a large switch statement takes care of all other opcodes */
        switch (OP & 0xC0) {
            case 0x40: /* LD r,r' */
                tmp = (OP >>> 3) & 0x07; tmp1 = OP & 0x07; putreg(tmp, getreg(tmp1)); 
                if ((tmp1 == 6) || (tmp == 6)) return 7; else return 4;
        }
        switch (OP & 0xC7) {
            case 0x06: /* LD r,n */
                tmp = (OP >>> 3) & 0x07;
                synchronized(PClock) {putreg(tmp, ((Short)mem.read(PC++)).shortValue());}
                if (tmp == 6) return 10; else return 7;
            case 0x04: /* INC r */
                tmp = (OP >>> 3) & 0x07; tmp1 = (getreg(tmp)+1) & 0xff;
                putreg(tmp,(short)tmp1); F = (short)((F & 1)|incTable[tmp1]);
                if (tmp == 6) return 11; else return 4;
            case 0x05: /* DEC r */
                tmp = (OP >>> 3) & 0x07; tmp1 = (getreg(tmp)-1) & 0xff;
                putreg(tmp,(short)tmp1); F = (short)((F & 1)|decTable[tmp1]);
                if (tmp == 6) return 11; else return 4;
            case 0xC0: /* RET cc */
                tmp = (OP >>> 3) & 7;
                if (getCC(tmp)) {
                    synchronized(PClock) { PC = (Integer)mem.readWord(SP); SP += 2; }
                    return 11;
                } return 5;
            case 0xC7: /* RST p */
                synchronized(PClock) { mem.writeWord(SP-2,PC); SP -= 2; PC = OP & 0x38; }
                return 11;
        }
        switch (OP & 0xCF) {
            case 0x03: /* INC ss */
                tmp = (OP >>> 4) & 0x03; tmp1 = getpair(tmp) + 1;
                putpair(tmp,tmp1); return 6;
            case 0x09: /* ADD HL, ss*/
                tmp = (OP >>> 4) & 0x03; tmp1 = getpair(tmp);
                tmp2 = getpair(2); tmp3 = tmp1 + tmp2; putpair(2,tmp3);
                F = (short)((F & 0xEC) |cbitsTable[((tmp1 ^ tmp2 ^ tmp3) >>> 8)&0x1ff]);
                return 11;
            case 0x0B: /* DEC ss*/
                tmp = (OP >>> 4) & 0x03; tmp1 = getpair(tmp) - 1;
                putpair(tmp,tmp1); return 6;
            case 0xC1: /* POP qq */
                tmp = (OP >>> 4) & 0x03; tmp1 = (Integer)mem.readWord(SP); SP += 2;
                putpair2(tmp, tmp1); return 10;
            case 0xC5: /* PUSH qq */
                tmp = (OP >>> 4) & 0x03; tmp1 = getpair2(tmp);
                SP -= 2; mem.writeWord(SP,tmp1); return 11;
        }
        switch (OP & 0xF8) {
            case 0x80: /* ADD A,r */
                tmp = getreg(OP&7); tmp1 = A + tmp; tmp2 = A ^ tmp1 ^ tmp;
                F = (short)((tmp1&0x80)|((tmp1 == 0)?flagZ:0)|cbitsTable[tmp2] 
                        |(((tmp2>>6)^(tmp2>>5))&4)); A = (short)(tmp1&0xff); return 4;
            case 0x88: /* ADC A,r */
                tmp3 = getreg(OP&7); tmp1 = A + tmp3 + (F&1); tmp2 = A ^ tmp1 ^ tmp3;
                F = (short)((tmp1&0x80)|((tmp1 == 0)?flagZ:0)|cbitsTable[tmp2] 
                        |(((tmp2>>6)^(tmp2>>5))&4)); A = (short)(tmp1&0xff);return 4;
            case 0x90: /* SUB r */
                tmp3 = getreg(OP&7); tmp1 = A - tmp3; tmp2 = A ^ tmp1 ^ tmp3;
                F = (short)((tmp1&0x80)|((tmp1 == 0)?flagZ:0)|cbitsTable[tmp2&0x1ff] 
                        |(((tmp2>>6)^(tmp2>>5))&4)|flagN); A = (short)(tmp1&0xff);return 4;
            case 0x98: /* SBC A,r */
                tmp = getreg(OP&7); tmp2 = A - tmp - (F&1);
                F = (short)(cbits2Z80Table[(A ^ tmp ^ tmp2) & 0x1ff] | (tmp2 & 0x80)
                  | (((tmp2&0xff)==0)?flagZ:0) | flagN); A = (short)(tmp2&0xff); return 4;
            case 0xA0: /* AND r */
                A &= getreg(OP&7); F = andTable[A]; return 4;
            case 0xA8: /* XOR r */
                A ^= getreg(OP&7); F = daaTable[A]; return 4;
            case 0xB0: /* OR r */
                A |= getreg(OP&7); F = daaTable[A]; return 4;
            case 0xB8: /* CP r */
                tmp3 = getreg(OP&7); tmp2 = A - tmp3; F = (short)(cpTable[tmp2&0xff]
                        |cbits2Z80Table[(A ^ tmp3 ^ tmp2) & 0x1ff]); return 4;
        }
        switch (OP) {
            case 0x00: /* NOP */
                return 4;
            case 0x02: /* LD (BC),A */
                mem.write(getpair(0), A); return 7;
            case 0x07: /* RLCA */
                tmp = A >>> 7; A = (short)(((A << 1)&0xFF) | tmp);
                F = (short)((F & 0xEC) | tmp);
                return 4;
            case 0x08: /* EX AF,AF' */
                tmp = A; A = A_S; A_S = (short)tmp;
                tmp = F; F = F_S; F_S = (short)tmp;
                return 4;
            case 0x0A: /* LD A,(BC) */
                tmp = (Short)mem.read(getpair(0)); A = (short)tmp;
                return 7;
            case 0x0F: /* RRCA */
                F = (short)((F & 0xFE) | (A & 1)); A = rrcaTable[A];
                return 4;
            case 0x10: /* DJNZ e */
                synchronized(PClock) { tmp = (Short)mem.read(PC++); }
                B--; B &= 0xFF;
                if (B != 0) {
                    synchronized(PClock) { PC += tmp + 1; } return 13;
                } return 8;
            case 0x12: /* LD (DE), A */
                mem.write(getpair(1), A);return 7;
            case 0x17: /* RLA */
                tmp = A >>> 7; A = (short)((A << 1) | (F & 1));
                F = (short)((F & 0xEC) | tmp); return 4;
            case 0x1A: /* LD A,(DE) */
                tmp = (Short)mem.read(getpair(1)); A = (short)tmp;
                return 7;
            case 0x1F: /* RRA */
                tmp = (F & 1) << 7; F = (short)((F & 0xEC) | (A & 1));
                A = (short)(A >>> 1 | tmp);
                return 4;
            case 0x27: /* DAA */
                // code taken from:
                // http://www.msx.org/forumtopic7029.htmlhttp://www.msx.org/forumtopic7029.html
                tmp = A; tmp1 = F & flagH; tmp2 = F & flagC;
                if ((F & flagN) != 0) {
                    if ((tmp1 != 0) || ((A & 0xf) > 9)) tmp -= 6;
                    if ((tmp2 != 0) || (A > 0x99)) tmp -= 0x60;
                } else {
                    if ((tmp1 != 0) || ((A & 0xf) > 9)) tmp += 6;
                    if ((tmp2 != 0) || (A > 0x99)) tmp += 0x60;
                }
                F = (short)((F & 3)|daaTable[tmp&0xff]|((A > 0x99)?1:0)|((A^tmp) & 0x10));
                A = (short)tmp;
                return 4;
            case 0x2F: /* CPL */
                A = (short)~A; F |= 0x0A; return 4;
            case 0x37: /* SCF */
                F = (short)((F & 0xED) | 1); return 4;
            case 0x3F: /* CCF */
                tmp = F & 1; //flagC
                F = (short)((F & 0xEC) | (tmp << 4) | ((~tmp)&1));
                return 4;
            case 0x76: /* HALT */
                run_state = stateEnum.stoppedNormal; return 4;
            case 0xC9: /* RET */
                synchronized(PClock) { PC = (Integer)mem.readWord(SP); SP += 2; }
                return 10;
            case 0xD9: /* EXX */
                tmp = B; B = B_S; B_S = (short)tmp; tmp = C; C = C_S; C_S = (short)tmp;
                tmp = D; D = D_S; D_S = (short)tmp; tmp = E; E = E_S; E_S = (short)tmp;
                tmp = H; H = H_S; H_S = (short)tmp; tmp = L; L = L_S; L_S = (short)tmp;
                return 4;
            case 0xE3: /* EX (SP),HL */
                tmp = (Short)mem.read(SP); tmp1 = (Short)mem.read(SP+1);
                mem.write(SP, L); mem.write(SP+1, H); L = (short)tmp; H = (short)tmp1;
                return 19;
            case 0xE9: /* JP (HL) */
                synchronized(PClock) { PC = ((H<<8)|L); } return 4;
            case 0xEB: /* EX DE,HL */
                tmp = D; D = H; H = (short)tmp;tmp = E; E = L; L = (short)tmp; return 4;
            case 0xF3: /* DI */
                IFF[0] = IFF[1] = false; return 4;
            case 0xF9: /* LD SP,HL */
                SP = ((H<<8)|L); return 6;
            case 0xFB:
                IFF[0] = IFF[1] = true; return 4;
            case 0xED:
                synchronized(PClock) { OP = ((Short)mem.read(PC++)).shortValue(); }
                switch (OP & 0xC7) {
                    case 0x70: /* IN (C) - unsupported */
                        tmp = (cpu.fireIO(C, true, (short)0) & 0xFF);
                        F = (short)((F & 1) | daaTable[tmp]); return 12;
                    case 0x40: /* IN r,(C) */
                        tmp = (OP >>> 3) & 0x7;
                        putreg(tmp, cpu.fireIO(C, true, (short)0));
                        F = (short)((F & 1) | daaTable[tmp]); return 12;
                    case 0x71: /* OUT (C),0 - unsupported */
                        cpu.fireIO(C, false, (short)0); return 12;
                    case 0x41: /* OUT (C),r */
                        tmp = (OP >>> 3) & 0x7; cpu.fireIO(C, false, getreg(tmp));
                        return 12;
                }
                switch (OP & 0xCF) {
                    case 0x42: /* SBC HL, ss */
                        tmp = (OP >>> 4) & 3; tmp2 = (H<<8)|L; tmp3 = getpair(tmp);
                        tmp1 = (tmp2 - tmp3 - (F & 1)) & 0xFFFF;
                        // this code taken from: simh
                        F = (short)(((tmp1 == 0) ? flagZ : 0) |
                            cbits2Z80Table[((tmp2 ^ tmp3 ^ tmp1) >>> 8) & 0x1ff]);
                        H = (short)(tmp1 >>> 8); L = (short)(tmp1 & 0xFF);
                        return 15;
                    case 0x4A: /* ADC HL,ss */
                        tmp = (OP >>> 4) & 3; tmp2 = (H<<8)|L;
                        tmp3 = getpair(tmp);
                        tmp1 = (tmp2 + tmp3 + (F & 1)) & 0xFFFF;
                        F = (short)(((tmp1 >>> 8) & 0x80) | ((tmp1 == 0)? flagZ:0) |
                            cbitsZ80Table[(tmp2 ^ tmp3 ^ tmp1) >>> 8]);
                        H = (short)(tmp1 >>> 8); L = (short)(tmp1 & 0xFF);
                        return 11;
                }
                switch(OP) {
                    case 0x44: /* NEG */
                        A = (short)((0 - A) & 0xFF); F = negTable[A]; return 8;
                    case 0x45: /* RETN */
                        IFF[0] = IFF[1];
                        synchronized(PClock) { PC = (Integer)mem.readWord(SP); SP += 2; }
                        return 14;
                    case 0x46: /* IM 0 */
                        intMode = 0; return 8;
                    case 0x47: /* LD I,A */
                        I = A; return 9;
                    case 0x4D: /* RETI - weird.. */ 
                        IFF[0] = IFF[1];
                        synchronized(PClock) { PC = (Integer)mem.readWord(SP); SP += 2; }
                        return 14;
                    case 0x4F: /* LD R,A */
                        R = A; return 9;
                    case 0x56: /* IM 1 */
                        intMode = 1; return 8;
                    case 0x57: /* LD A,I */
                        A = I; F = (short)((I&0x80)|((I==0)?flagZ:0)|(IFF[1]?flagPV:0)|(F&1));
                        return 9;
                    case 0x5E: /* IM 2 */
                        intMode = 2; return 8;
                    case 0x5F: /* LD A,R */
                        A = R; F = (short)((R&0x80)|((R==0)?flagZ:0)|(IFF[1]?flagPV:0)|(F&1));
                        return 9;
                    case 0x67: /* RRD */
                        tmp = A & 0x0F; tmp1 = (Short)mem.read((H<<8)|L);
                        A = (short)((A&0xF0)|(tmp1&0x0F));tmp1 = ((tmp1>>>4)&0x0F)|(tmp<<4);
                        mem.write(((H<<8)|L),(short)tmp1&0xff); F = (short)(daaTable[A]|(F&flagC));
                        return 18;
                    case 0x6F: /* RLD */
                        tmp = (Short)mem.read((H<<8)|L);tmp1 = (tmp >>> 4) & 0x0F;
                        tmp = ((tmp<<4)&0xF0)|(A&0x0F);A = (short)((A&0xF0)|tmp1);
                        mem.write((H<<8)|L,(short)tmp&0xff); F = (short)(daaTable[A]|(F&flagC));
                        return 18;
                    case 0xA0: /* LDI */
                        tmp1 = (H<<8)|L; tmp2 = (D<<8)|E;
                        mem.writeWord(tmp2, (Integer)mem.readWord(tmp1));
                        tmp1 = (tmp1 + 1) & 0xFFFF; tmp2 = (tmp2 + 1) & 0xFFFF;
                        tmp = (((B<<8)|C) - 1) & 0xFFFF;
                        H = (short)(tmp1 >>> 8); L = (short)(tmp1 & 0xFF);
                        B = (short)(tmp >>> 8); C = (short)(tmp & 0xFF);
                        D = (short)(tmp2 >>> 8); E = (short)(tmp2 & 0xFF);
                        F = (short)((F & 0xE9) | ((tmp != 0) ? flagPV : 0));
                        return 16;
                    case 0xA1: /* CPI */
                        tmp2 = (Short)mem.read((H<<8)|L);tmp = (A - tmp2)&0xFF;
                        tmp1 = (((B<<8)|C)-1)&0xFFFF;tmp3 = tmp ^ tmp2 ^ A;
                        B = (short)(tmp1 >>> 8); C = (short)(tmp1 & 0xFF);
                        // flags borrowed from simh
                        F = (short)((F & flagC)|flagN|((tmp1 != 0)?flagPV:0)
                                |((tmp == 0)?flagZ:0)|(tmp & 0x80)
                                |(((tmp - ((tmp3 & 16) >>> 4)) & 2) << 4) | (tmp3 & 16)
                                |((tmp - ((tmp3 >>> 4) & 1)) & 8));
                        if (((tmp & 15) == 8) && ((tmp3 & 16) != 0)) F &= 0xF7;
                        tmp = (((H<<8)|L)+1)&0xFFFF;H=(short)(tmp>>>8);L=(short)(tmp&0xFF);
                        return 16;
                    case 0xA2: /* INI */
                        tmp = (cpu.fireIO(C, true, (short)0) & 0xFF);
                        tmp1 = (H<<8)|L; mem.write(tmp1, (short)tmp);
                        tmp1 = (tmp1 + 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F = (short)((F & 0xBF)|flagN|((B == 0)?flagZ:0));
                        return 16;
                    case 0xA3: /* OUTI */
                        tmp1 = (H<<8)|L;
                        cpu.fireIO(C, false, (Short)mem.read(tmp1));
                        tmp1 = (tmp1 + 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F = (short)((F & 0xBF)|flagN|((B == 0)?flagZ:0));
                        return 16;
                    case 0xA8: /* LDD */
                        tmp1 = (H<<8)|L; tmp2 = (D<<8)|E;
                        mem.writeWord(tmp2, (Integer)mem.readWord(tmp1));
                        tmp1 = (tmp1 - 1) & 0xFFFF; tmp2 = (tmp2 - 1) & 0xFFFF;
                        tmp = (((B<<8)|C) - 1) & 0xFFFF;
                        H = (short)(tmp1 >>> 8); L = (short)(tmp1 & 0xFF);
                        B = (short)(tmp >>> 8); C = (short)(tmp & 0xFF);
                        D = (short)(tmp2 >>> 8); E = (short)(tmp2 & 0xFF);
                        F = (short)((F & 0xE9) | ((tmp != 0) ? flagPV : 0));
                        return 16;
                    case 0xA9: /* CPD */
                        tmp2 = (Short)mem.read((H<<8)|L); tmp = (A - tmp2)&0xFF;
                        tmp1 = (((B<<8)|C)-1)&0xFFFF; tmp3 = tmp ^ tmp2 ^ A;
                        B = (short)(tmp1 >>> 8); C = (short)(tmp1 & 0xFF);
                        // flags borrowed from simh
                        F = (short)((F & 1) | (tmp & 0x80) | (((tmp & 0xff)==0)?flagZ:0)
                                | (((tmp - ((tmp3 & 16) >> 4)) & 2) << 4)
                                | (tmp3 & 16) | ((tmp - ((tmp3 >> 4) & 1)) & 8)
                                | ((tmp1 != 0)?flagPV:0) | flagN);
                        if (((tmp & 15) == 8) && ((tmp3 & 16) != 0)) F &= 0xF7;
                        tmp = (((H<<8)|L)-1)&0xFFFF;H=(short)(tmp>>>8);L=(short)(tmp&0xFF);
                        return 16;
                    case 0xAA: /* IND */
                        tmp = (cpu.fireIO(C, true, (short)0) & 0xFF);
                        tmp1 = (H<<8)|L; mem.write(tmp1, (short)tmp);
                        tmp1 = (tmp1 - 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F = (short)((F & 0xBF)|flagN|((B == 0)?flagZ:0));
                        return 16;
                    case 0xAB: /* OUTD */
                        tmp1 = (H<<8)|L;
                        cpu.fireIO(C, false, (Short)mem.read((short)tmp1));
                        tmp1 = (tmp1 - 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F = (short)((F & 0xBF)|flagN|((B == 0)?flagZ:0));
                        return 16;
                    case 0xB0: /* LDIR */
                        tmp1 = (H<<8)|L; tmp2 = (D<<8)|E;
                        mem.writeWord(tmp2, (Integer)mem.readWord(tmp1));
                        tmp1 = (tmp1 + 1) & 0xFFFF; tmp2 = (tmp2 + 1) & 0xFFFF;
                        H = (short)(tmp1 >>> 8); L = (short)(tmp1 & 0xFF);
                        D = (short)(tmp2 >>> 8); E = (short)(tmp2 & 0xFF);
                        tmp = (((B<<8)|C) - 1) & 0xFFFF;
                        B = (short)(tmp >>> 8); C = (short)(tmp & 0xFF); F &= 0xE9;
                        if (tmp == 0) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xB1: /* CPIR */
                        tmp2 = (Short)mem.read((H<<8)|L);tmp = (A - tmp2)&0xFF;
                        tmp1 = (((B<<8)|C)-1)&0xFFFF;tmp3 = tmp ^ tmp2 ^ A;
                        B = (short)(tmp1 >>> 8); C = (short)(tmp1 & 0xFF);
                        if (tmp == 0) {
                            // flags borrowed from simh
                            F = (short)((F & flagC)|flagN|((tmp1 != 0)?flagPV:0)
                                    |((tmp == 0)?flagZ:0)|(tmp & 0x80)
                                    |(((tmp - ((tmp3 & 16) >>> 4)) & 2) << 4) | (tmp3 & 16)
                                    |((tmp - ((tmp3 >>> 4) & 1)) & 8));
                            if (((tmp & 15) == 8) && ((tmp3 & 16) != 0)) F &= 0xF7;
                        }
                        tmp2 = (((H<<8)|L)+1)&0xFFFF;H=(short)(tmp2>>>8);L=(short)(tmp2&0xFF);
                        if ((tmp1 == 0) || (tmp == 0)) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xB2: /* INIR */
                        tmp = (cpu.fireIO(C, true, (short)0) & 0xFF);
                        tmp1 = (H<<8)|L; mem.write(tmp1, (short)tmp);
                        tmp1 = (tmp1 + 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F |= (short)(flagN|flagZ); if (B == 0) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xB3: /* OTIR */
                        tmp1 = (H<<8)|L;
                        cpu.fireIO(C, false, (Short)mem.read(tmp1));
                        tmp1 = (tmp1 + 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F |= (short)(flagN|flagZ); if (B == 0) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xB8: /* LDDR */
                        tmp1 = (H<<8)|L; tmp2 = (D<<8)|E;
                        mem.writeWord(tmp2, (Integer)mem.readWord(tmp1));
                        tmp1 = (tmp1 - 1) & 0xFFFF; tmp2 = (tmp2 - 1) & 0xFFFF;
                        tmp = (((B<<8)|C) - 1) & 0xFFFF;
                        H = (short)(tmp1 >>> 8); L = (short)(tmp1 & 0xFF);
                        B = (short)(tmp >>> 8); C = (short)(tmp & 0xFF);
                        D = (short)(tmp2 >>> 8); E = (short)(tmp2 & 0xFF);
                        F &= 0xE9; if (tmp == 0) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xB9: /* CPDR */
                        tmp2 = (Short)mem.read((H<<8)|L); tmp = (A - tmp2)&0xFF;
                        tmp1 = (((B<<8)|C)-1)&0xFFFF; tmp3 = tmp ^ tmp2 ^ A;
                        B = (short)(tmp1 >>> 8); C = (short)(tmp1 & 0xFF);
                        // flags borrowed from simh
                        if (tmp == 0) {
                            F = (short)((F & 1) | (tmp & 0x80) | (((tmp & 0xff)==0)?flagZ:0)
                                    | (((tmp - ((tmp3 & 16) >> 4)) & 2) << 4)
                                    | (tmp3 & 16) | ((tmp - ((tmp3 >> 4) & 1)) & 8)
                                    | ((tmp1 != 0)?flagPV:0) | flagN);
                            if (((tmp & 15) == 8) && ((tmp3 & 16) != 0)) F &= 0xF7;
                        }
                        tmp2 = (((H<<8)|L)-1)&0xFFFF;H=(short)(tmp2>>>8);L=(short)(tmp2&0xFF);
                        if ((tmp == 0) || (tmp1 == 0)) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xBA: /* INDR */
                        tmp = (cpu.fireIO(C, true, (short)0) & 0xFF);
                        tmp1 = (H<<8)|L; mem.write(tmp1, (short)tmp);
                        tmp1 = (tmp1 - 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F |= (short)(flagN|flagZ); if (B == 0) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                    case 0xBB: /* OTDR */
                        tmp1 = (H<<8)|L;
                        cpu.fireIO(C, false, (Short)mem.read((short)tmp1));
                        tmp1 = (tmp1 - 1) & 0xFFFF; H = (short)(tmp1 >>> 8);
                        L = (short)(tmp1 & 0xFF); B = (short)((B - 1) & 0xFF);
                        F |= (short)(flagN|flagZ); if (B == 0) return 16;
                        synchronized(PClock) { PC -= 2; } return 21;
                }
                synchronized(PClock) { tmp = (Integer)mem.readWord(PC); PC += 2; }
                switch (OP & 0xCF) {
                    case 0x43: /* LD (nn), ss */
                        tmp1 = getpair((OP>>>4)&3); mem.writeWord(tmp, tmp1); return 20;
                    case 0x4B: /* LD ss,(nn) */
                        tmp1 = (Integer)mem.readWord(tmp); putpair((OP>>>4)&3, tmp1); return 20;                        
                }
            case 0xDD: special = 0xDD; case 0xFD: if (OP == 0xFD) special = 0xFD;
                synchronized(PClock) { OP = ((Short)mem.read(PC++)).shortValue(); }
                switch (OP & 0xCF) {
                    case 0x09: /* ADD ii,pp */
                        tmp1 = getpair(special,(OP>>>4)&3); tmp = getspecial(special)+tmp1;
                        F = (short)((F&0xEC)|cbitsTable[(IX ^ tmp1 ^ tmp) >> 8]);
                        putspecial(special, tmp); return 15;
                }
                switch (OP) {
                    case 0x23: /* INC ii */
                        if (special == 0xDD) IX++; else IY++; return 10;
                    case 0x2B: /* DEC ii */
                        if (special == 0xDD) IX--; else IY--; return 10;
                    case 0xE1: /* POP ii */
                        if (special == 0xDD) IX = (Integer)mem.readWord(SP);
                        else IY = (Integer)mem.readWord(SP); SP += 2; return 14;
                    case 0xE3: /* EX (SP),ii */
                        tmp = (Integer)mem.readWord(SP);
                        if (special == 0xDD) { tmp1 = IX; IX = tmp; }
                        else { tmp1 = IY; IY = tmp; } mem.writeWord(SP, tmp1);
                        return 23;
                    case 0xE5: /* PUSH ii */
                        SP -= 2; if (special == 0xDD) mem.writeWord(SP, IX);
                        else mem.writeWord(SP, IY); return 15;
                    case 0xE9: /* JP (ii) */
                        synchronized(PClock) {
                            if (special == 0xDD) PC = IX; else PC = IY;
                        } return 8;
                    case 0xF9: /* LD SP,ii */
                        SP = (special == 0xDD)?IX:IY; return 10;
                }
                synchronized(PClock) { tmp = ((Short)mem.read(PC++)).shortValue(); }
                switch (OP & 0xC7) {
                    case 0x76: break;
                    case 0x46: /* LD r,(ii+d) */
                        tmp1 = (OP>>>3)&7; putreg2(tmp1,(Short)mem.read((getspecial(special)+tmp)&0xffff));
                        return 19;
                }
                switch (OP & 0xF8) {
                    case 0x76: break;
                    case 0x70: /* LD (ii+d) */
                        tmp1 = (OP & 7); tmp2 = (getspecial(special)+tmp)&0xffff;
                        mem.write(tmp2, getreg2(tmp1)); return 19;
                }
                switch(OP) {
                    case 0x34: /* INC (ii+d) */
                        tmp1 = (getspecial(special)+tmp)&0xffff; tmp2 = ((Short)mem.read(tmp1)+1)&0xff;
                        mem.write(tmp1, tmp2); F = (short)((F&1)|incZ80Table[tmp2]); return 23;
                    case 0x35: /* DEC (ii+d) */
                        tmp1 = (getspecial(special)+tmp)&0xffff; tmp2 = ((Short)mem.read(tmp1)-1)&0xff;
                        F = (short)((F & 1)|decZ80Table[tmp2]); return 23;
                    case 0x86: /* ADD A,(ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF; tmp2 = A + tmp1;
                        F = (short)(cbitsZ80Table[tmp1^tmp2^A] | (tmp2 & 0x80)
                          | (((tmp2&0xff)==0)?flagZ:0)); A = (short)(tmp2&0xff); return 19;
                    case 0x8E: /* ADC A,(ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF; tmp2 = A + tmp1 + (F&1);
                        F = (short)(cbitsZ80Table[tmp1^tmp2^A] | (tmp2 & 0x80)
                          | (((tmp2&0xff)==0)?flagZ:0)); A = (short)(tmp2&0xff); return 19;
                    case 0x96: /* SUB (ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF; tmp2 = A - tmp1;
                        F = (short)(cbits2Z80Table[(A ^ tmp1 ^ tmp2) & 0x1ff] | (tmp2 & 0x80)
                          | (((tmp2&0xff)==0)?flagZ:0) | flagN); A = (short)(tmp2&0xff); return 19;
                    case 0x9E: /* SBC A,(ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF; tmp2 = A - tmp1 - (F&1);
                        F = (short)(cbits2Z80Table[(A ^ tmp1 ^ tmp2) & 0x1ff] | (tmp2 & 0x80)
                          | (((tmp2&0xff)==0)?flagZ:0) | flagN); A = (short)(tmp2&0xff); return 19;
                    case 0xA6: /* AND (ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF;
                        A &= tmp1; F = andTable[A]; return 19;
                    case 0xAE: /* XOR (ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF;
                        A ^= tmp1; F = daaTable[A]; return 19;
                    case 0xB6: /* OR (ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF;
                        A |= tmp1; F = daaTable[A]; return 19;
                    case 0xBE: /* CP (ii+d) */
                        tmp1 = (Short)mem.read(getspecial(special)+tmp)&0xFF;
                        tmp2 = A - tmp1; F = (short)(cpTable[tmp2&0xff]|cbits2Z80Table[(A ^ tmp1 ^ tmp2) & 0x1ff]);
                        return 19;
                }
                synchronized(PClock) { tmp |= (((Short)mem.read(PC++)).shortValue()<<8); }
                switch (OP) {
                    case 0x21: /* LD ii,nn */
                        putspecial(special, tmp); return 14;
                    case 0x22: /* LD (nn),ii */
                        mem.writeWord(tmp, getspecial(special)); return 16;
                    case 0x2A: /* LD ii,(nn) */
                        tmp1 = (Integer)mem.readWord(tmp); putspecial(special,tmp1); return 20;
                    case 0x36: /* LD (ii+d),d */
                        mem.write(getspecial(special)+(tmp&0xff), (tmp>>>8)&0xff); return 19;
                    case 0xCB:
                        OP = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
                        switch(OP & 0xC7) {
                            case 0x46: /* BIT b,(ii+d) */
                                tmp2 = (OP>>>3)&7;tmp1 = (Short)mem.read((getspecial(special) + tmp)&0xffff);
                                F = (short)((F&0x95)|flagH|(((tmp1&(1<<tmp2)) == 0)?flagZ:0)); return 20;
                            case 0x86: /* RES b,(ii+d) */
                                tmp2 = (OP>>>3)&7; tmp3 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp3); tmp1 = (tmp1 & (~(1<<tmp2)));
                                mem.write(tmp3, tmp1&0xff); return 23;
                            case 0xC6: /* SET b,(ii+d) */
                                tmp2 = (OP>>>3)&7; tmp3 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp3); tmp1 = (tmp1 | (1<<tmp2));
                                mem.write(tmp3, tmp1&0xff); return 23;
                        }
                        switch (OP) {
                            case 0x06: /* RLC (ii+d) */
                                tmp2 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp2); F = (short)(tmp1>>>7);
                                tmp1 <<= 1; tmp1 |= (F&1); mem.write(tmp2, tmp1&0xff);
                                F |= daaTable[tmp1]; return 23;
                            case 0x0E: /* RRC (ii+d) */
                                tmp2 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp2); F = (short)(tmp1&1);
                                tmp1 >>>= 1; tmp1 |= ((F&1)<<7); mem.write(tmp2, tmp1&0xff);
                                F |= daaTable[tmp1]; return 23;
                            case 0x16: /* RL (ii+d) */
                                tmp2 = (getspecial(special) + tmp)&0xffff; tmp3 = F&1;
                                tmp1 = (Short)mem.read(tmp2); F = (short)(tmp1>>>7);
                                tmp1 <<= 1; tmp1 |= tmp3; mem.write(tmp2, tmp1&0xff);
                                F |= daaTable[tmp1]; return 23;
                            case 0x1E: /* RR (ii+d) */
                                tmp2 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp2); tmp3 = F&1;F = (short)(tmp1&1);
                                tmp1 >>>= 1; tmp1 |= (tmp3<<7); mem.write(tmp2, tmp1&0xff);
                                F |= daaTable[tmp1]; return 23;
                            case 0x26: /* SLA (ii+d) */
                                tmp2 = (getspecial(special)+tmp)&0xffff; 
                                tmp1 = (Short)mem.read(tmp2); F = (short)(tmp1>>>7);
                                tmp1 <<= 1; mem.write(tmp2, tmp1&0xff); F|=daaTable[tmp1]; return 23;
                            case 0x2E: /* SRA (ii+d) */
                                tmp2 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp2); tmp3 = tmp1&0x80;
                                F = (short)(tmp1&1); tmp1 >>>= 1; tmp1 |= tmp3;
                                mem.write(tmp2, tmp1&0xff); F |= daaTable[tmp1]; return 23;
                            case 0x36: /* SLL (ii+d) unsupported */
                                tmp2 = (getspecial(special)+tmp)&0xffff; 
                                tmp1 = (Short)mem.read(tmp2); F = (short)(tmp1>>>7);tmp3=tmp1&1;
                                tmp1 <<= 1; tmp1 |= tmp3; mem.write(tmp2, tmp1&0xff);
                                F|=daaTable[tmp1]; return 23;
                            case 0x3E: /* SRL (ii+d) */
                                tmp2 = (getspecial(special) + tmp)&0xffff;
                                tmp1 = (Short)mem.read(tmp2); F = (short)(tmp1&1); tmp1 >>>= 1;
                                mem.write(tmp2, tmp1&0xff); F |= ((tmp1 == 0)?flagZ:0)|parityTable[tmp1]; return 23;
                        }
                }
            case 0xCB:
                synchronized (PClock) { OP = (Short)mem.read(PC++); }
                switch (OP & 0xF8) {
                    case 0x00: /* RLC r */
                        tmp = OP&7; tmp1 = getreg(tmp); F = (short)(tmp1>>>7);
                        tmp1 <<= 1; tmp1 |= (F&1); F |= daaTable[tmp1]; putreg(tmp,(short)tmp1);
                        if (tmp==6) return 15; else return 8;
                    case 0x08: /* RRC r */
                        tmp = OP&7;tmp1 = getreg(tmp); F = (short)(tmp1&1);
                        tmp1 >>>= 1; tmp1 |= ((F&1)<<7); putreg(tmp, (short)tmp1);
                        F |= daaTable[tmp1]; if (tmp==6) return 15; else return 8;
                    case 0x10: /* RL r */
                        tmp = OP&7;tmp1 = getreg(tmp);tmp2 = F&1;
                        F = (short)(tmp1>>>7);
                        tmp1 <<= 1; tmp1 |= tmp2; putreg(tmp,(short)tmp1);
                        F |= daaTable[tmp1]; if (tmp==6) return 15; else return 8;
                    case 0x18: /* RR r */
                        tmp = OP&7; tmp1=getreg(tmp); tmp2 = F&1;F = (short)(tmp1&1);
                        tmp1 >>>= 1; tmp1 |= (tmp2<<7); putreg(tmp,(short)tmp1);
                        F |= daaTable[tmp1]; if (tmp==6) return 15; else return 8;
                    case 0x20: /* SLA r */
                        tmp = OP&7; tmp1 = getreg(tmp); F = (short)(tmp1>>>7);
                        tmp1 <<= 1; putreg(tmp,(short)tmp1); F|=daaTable[tmp1];
                        if (tmp==6) return 15; else return 8;
                    case 0x28: /* SRA r */
                        tmp = OP&7; tmp1 = getreg(tmp); tmp2 = tmp1&0x80;
                        F = (short)(tmp1&1); tmp1 >>>= 1; tmp1 |= tmp2;
                        putreg(tmp,(short)tmp1); F |= daaTable[tmp1];
                        if (tmp==6) return 15; else return 8;
                    case 0x30: /* SLL r - unsupported */
                        tmp = OP&7; tmp1 = getreg(tmp); F = (short)(tmp1>>>7);tmp2=tmp1&1;
                        tmp1 <<= 1; tmp1 |= tmp2; putreg(tmp,(short)tmp1);
                        F|=daaTable[tmp1]; if (tmp==6) return 15; else return 8;
                    case 0x38: /* SRL r */
                        tmp = OP&7; tmp1 = getreg(tmp); F = (short)(tmp1&1); tmp1 >>>= 1;
                        putreg(tmp,(short)tmp1); F |= ((tmp1 == 0)?flagZ:0)|parityTable[tmp1];
                        if (tmp==6) return 15; else return 8;
                    case 0x40: /* BIT b,r */
                        tmp = (OP>>>3)&7; tmp2 = OP&7; tmp1 = getreg(tmp2);
                        F = (short)((F&0x95)|flagH|(((tmp1&(1<<tmp)) == 0)?flagZ:0));
                        if (tmp2 == 6) return 12; else return 8;
                    case 0x80: /* RES b,r */
                        tmp = (OP>>>3)&7; tmp2 = OP&7; tmp1 = getreg(tmp2);
                        tmp1 = (tmp1 & (~(1<<tmp))); putreg(tmp2,(short)tmp1);
                        if (tmp2 == 6) return 15; else return 8;
                    case 0xC0: /* SET b,r */
                        tmp = (OP>>>3)&7; tmp2 = OP&7; tmp1 = getreg(tmp2);
                        tmp1 = (tmp1 | (1<<tmp)); putreg(tmp2,(short)tmp1);
                        if (tmp2 == 6) return 15; else return 8;
                }
        }
        synchronized(PClock) { tmp = (Short)mem.read(PC++); }
        switch (OP & 0xE7) {
            case 0x20: /* JR cc,d */
                if (getCC1((OP>>>3)&3)) {
                    b = (byte)tmp;
                    synchronized(PClock) { PC += b; PC &= 0xFFFF;} return 12;
                } return 7;
        }
        switch (OP) {
            case 0x18: /* JR e */
                b = (byte)tmp;
                synchronized(PClock) { PC += b; PC &= 0xFFFF; } return 12;
            case 0xC6: /* ADD A,d */
                tmp1 = A + tmp; tmp2 = A ^ tmp1 ^ tmp;
                F = (short)((tmp1&0x80)|((tmp1 == 0)?flagZ:0)|cbitsTable[tmp2] 
                        |(((tmp2>>6)^(tmp2>>5))&4)); A = (short)(tmp1&0xff); return 7;
            case 0xCE: /* ADC A,d */
                tmp1 = A + tmp + (F&1); tmp2 = A ^ tmp1 ^ tmp;
                F = (short)((tmp1&0x80)|((tmp1 == 0)?flagZ:0)|cbitsTable[tmp2] 
                        |(((tmp2>>6)^(tmp2>>5))&4)); A = (short)(tmp1&0xff); return 7;
            case 0xD3: /* OUT (d),A */
                cpu.fireIO(tmp, false, A); return 11;
            case 0xD6: /* SUB d */
                tmp1 = A - tmp; tmp2 = A ^ tmp1 ^ tmp;
                F = (short)((tmp1&0x80)|((tmp1 == 0)?flagZ:0)|cbitsTable[tmp2&0x1ff] 
                        |(((tmp2>>6)^(tmp2>>5))&4)|flagN); A = (short)(tmp1&0xff); return 7;
            case 0xDB: /* IN A,(d) */
                A = (short)(cpu.fireIO(tmp, true, (short)0) & 0xFF); return 11;
            case 0xDE: /* SBC A,d */
                tmp2 = A - tmp - (F&1);
                F = (short)(cbits2Z80Table[(A ^ tmp ^ tmp2) & 0x1ff] | (tmp2 & 0x80)
                  | (((tmp2&0xff)==0)?flagZ:0) | flagN); A = (short)(tmp2&0xff); return 7;
            case 0xE6: /* AND d */
                A &= tmp; F = andTable[A]; return 7;
            case 0xEE: /* XOR d */
                A ^= tmp; F = daaTable[A]; return 7;
            case 0xF6: /* OR d */
                A |= tmp; F = daaTable[A]; return 7;
            case 0xFE: /* CP d */
                tmp2 = A - tmp; F = (short)(cpTable[tmp2&0xff]|cbits2Z80Table[(A ^ tmp ^ tmp2) & 0x1ff]);
                return 7;
        }
        synchronized(PClock) { tmp += ((Short)mem.read(PC++) << 8); }
        switch (OP & 0xC7) {
            case 0xC2: /* JP cc,nn */
                if (getCC((OP>>>3)&7)) synchronized(PClock) { PC = tmp; } return 10;
            case 0xC4: /* CALL cc,nn */
                if (getCC((OP>>>3)&7)) {
                    SP -= 2; synchronized(PClock) { mem.writeWord(SP, PC); PC = tmp; }
                    return 17;
                } return 10;
        }
        switch (OP & 0xCF) {
            case 0x01: /* LD ss, nn */
                putpair((OP>>>4)&3,tmp); return 10;
        }
        switch (OP) {
            case 0x22: /* LD (nn),HL */
                tmp1 = getpair(2); mem.writeWord(tmp, tmp1); return 16;
            case 0x2A: /* LD HL,(nn) */
                tmp1 = (Integer)mem.readWord(tmp); putpair(2, tmp1); return 16;
            case 0x32: /* LD (nn),A */
                mem.write(tmp, A); return 13;
            case 0x3A: /* LD A,(nn) */
                A = (Short)mem.read(tmp); return 13;
            case 0xC3: /* JP nn */
                synchronized(PClock) { PC = tmp; } return 10;
            case 0xCD: /* CALL nn */
                SP -= 2; synchronized(PClock) { mem.writeWord(SP, PC); PC = tmp; }
                return 17;
        }
        run_state = stateEnum.stoppedBadInstr;
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
                    e.printStackTrace();
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
