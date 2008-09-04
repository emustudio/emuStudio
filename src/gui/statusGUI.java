/*
 * statusGUI.java
 *
 * Created on Nedeľa, 2008, august 24, 10:22
 */

package gui;

import impl.CpuContext;
import impl.CpuZ80;
import interfaces.IICpuListener;
import java.util.EventObject;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import plugins.cpu.ICPUContext.stateEnum;
import plugins.cpu.ICPUInstruction;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author  vbmacher
 */
public class statusGUI extends javax.swing.JPanel {
    private IDebugColumn[] columns;
    private stateEnum run_state;
    private CpuZ80 cpu;
    private CpuContext cpuC;
    private IMemoryContext mem = null;
    private AbstractTableModel flagModel1;
    private AbstractTableModel flagModel2;
    private String regs[] = new String[11];

    private class FlagsModel extends AbstractTableModel {
        private String[] flags = {"S","Z","H","P/V","N","C"};
        private int[] flagsI = {0,0,0,0,0,0};
        private int set;
        
        public FlagsModel(int set) { this.set = set; }
        public int getRowCount() { return 2; }
        public int getColumnCount() { return 6; }
        public String getColumnName(int columnIndex) { return flags[columnIndex]; }
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (rowIndex) {
                case 0: return flags[columnIndex];
                case 1: return flagsI[columnIndex];
            }
        }
        public void fireTableDataChanged() {
            flagsI[0] = Integer.valueOf(String.valueOf((cpu.getF(set)&CpuZ80.flagS)!=0));
            flagsI[1] = Integer.valueOf(String.valueOf((cpu.getF(set)&CpuZ80.flagZ)!=0));
            flagsI[2] = Integer.valueOf(String.valueOf((cpu.getF(set)&CpuZ80.flagH)!=0));
            flagsI[3] = Integer.valueOf(String.valueOf((cpu.getF(set)&CpuZ80.flagPV)!=0));
            flagsI[4] = Integer.valueOf(String.valueOf((cpu.getF(set)&CpuZ80.flagN)!=0));
            flagsI[5] = Integer.valueOf(String.valueOf((cpu.getF(set)&CpuZ80.flagC)!=0));
            super.fireTableDataChanged();
        }
    }
    
    /** Creates new form statusGUI */
    public statusGUI(final CpuZ80 cpu, IMemoryContext mem) {
        initComponents();
        
        this.cpu = cpu;
        this.mem = mem;
        this.cpuC = (CpuContext) cpu.getContext();
        run_state = stateEnum.stoppedNormal;
        columns = new IDebugColumn[4];
        columns[0] = new ColumnInfo("breakpoint", java.lang.Boolean.class,true);
        columns[1] = new ColumnInfo("address", java.lang.String.class,false);
        columns[2] = new ColumnInfo("mnemonics", java.lang.String.class,false);
        columns[3] = new ColumnInfo("opcode", java.lang.String.class,false);

        cpuC.addCPUListener(new IICpuListener() {
            public void runChanged(EventObject evt, stateEnum state) {
                run_state = state;
            }
            public void stateUpdated(EventObject evt) {
                updateGUI();   
            }
            public void frequencyChanged(EventObject evt, float frequency) {
                lblFrequency.setText(String.format("%.2f kHz", frequency));
            }
        });
        spnFrequency.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int i = (Integer)((SpinnerNumberModel)spnFrequency.getModel()).getValue();
                try { setCPUFreq(i); } catch(IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel)spnFrequency.getModel())
                    .setValue(cpuC.getFrequency());
                }
            }
        });
        spnTestPeriode.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int i = (Integer)((SpinnerNumberModel)spnTestPeriode.getModel()).getValue();
                try { cpu.setSliceTime(i); } catch(IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel)spnTestPeriode.getModel())
                    .setValue(cpu.getSliceTime());
                }
            }
        });
        flagModel1 = new FlagsModel(0);
        flagModel2 = new FlagsModel(1);
        tblFlags.setModel(flagModel1);
        tblFlags2.setModel(flagModel2);
    }
    
    // user set frequency (must be synchronized inside)
    private void setCPUFreq(int f) { cpuC.setFrequency(f); }

    public IDebugColumn[] getDebugColumns() { return columns; }
    
    public void setDebugColVal(int index, int col, Object value) {
        if (col != 0) return;
        if (value.getClass() != Boolean.class) return;
        
        boolean v = Boolean.valueOf(value.toString());
        cpu.setBreakpoint(index,v);
    }

    public Object getDebugColVal(int index, int col) {
        try {
            ICPUInstruction instr = cpuDecode(index);
            switch (col) {
                case 0: return cpu.getBreakpoint(index); // breakpoint
                case 1: return String.format("%04Xh", index); // adresa
                case 2: return instr.getMnemo(); // mnemonika
                case 3: return instr.getOperCode(); // operacny kod
                default: return "";
            }
        } catch(IndexOutOfBoundsException e) {
            // tu sa dostanem iba v pripade, ak pouzivatel manualne
            // zmenil hodnotu operacnej pamate tak, ze vyjadruje
            // instrukciu s viacerymi bytami, ktore sa uz nezmestili
            // do operacnej pamate
            switch (col) {
                case 0: return cpu.getBreakpoint(index);
                case 1: return String.format("%04Xh", index);
                case 2: return "incomplete";
                case 3: return String.format("%X", (Short)mem.read(index));
                default: return "";
            }
        }
    }
    
    public void updateGUI() {
        regs[0] = String.format("%02X", cpu.getA());
        regs[1] = String.format("%02X", cpu.getF());
        regs[2] = String.format("%02X", cpu.getB());
        regs[3] = String.format("%02X", cpu.getC());
        regs[4] = String.format("%04X",cpu.getBC());
        regs[5] = String.format("%02X", cpu.getD());
        regs[6] = String.format("%02X", cpu.getE());
        regs[7] = String.format("%04X",cpu.getDE());
        regs[8] = String.format("%02X", cpu.getH());
        regs[9] = String.format("%02X", cpu.getL());
        regs[10] = String.format("%04X",cpu.getHL());

        switch (cpu.getActiveRegSet()) {
            case 0:
                txtRegA.setText(regs[0]); txtRegF.setText(regs[1]);
                txtRegB.setText(regs[2]); txtRegC.setText(regs[3]);
                txtRegBC.setText(regs[4]); txtRegD.setText(regs[5]);
                txtRegE.setText(regs[6]); txtRegDE.setText(regs[7]);
                txtRegH.setText(regs[8]); txtRegL.setText(regs[9]);
                txtRegHL.setText(regs[10]);
                flagModel1.fireTableDataChanged();
                break;
            case 1:
                txtRegA1.setText(regs[0]); txtRegF1.setText(regs[1]);
                txtRegB1.setText(regs[2]); txtRegC1.setText(regs[3]);
                txtRegBC1.setText(regs[4]); txtRegD1.setText(regs[5]);
                txtRegE1.setText(regs[6]); txtRegDE1.setText(regs[7]);
                txtRegH1.setText(regs[8]); txtRegL1.setText(regs[9]);
                txtRegHL1.setText(regs[10]);
                flagModel2.fireTableDataChanged();
                break;
               
        }
        txtRegSP.setText(String.format("%04X", cpu.getSP()));
        txtRegPC.setText(String.format("%04X", cpu.getPC()));
        txtRegIX.setText(String.format("%04X", cpu.getIX()));
        txtRegIY.setText(String.format("%04X", cpu.getIY()));
        txtRegI.setText(String.format("%02X", cpu.getI()));
        txtRegR.setText(String.format("%02X", cpu.getR()));
        
        if (run_state == stateEnum.runned) {
            lblRun.setText("running");
            spnFrequency.setEnabled(false);
            spnTestPeriode.setEnabled(false);
        }
        else {
            spnFrequency.setEnabled(true);
            spnTestPeriode.setEnabled(true);
            switch (run_state.ordinal()) {
                case 0: lblRun.setText("stopped (normal)"); break;
                case 1: lblRun.setText("breakpoint"); break;
                case 2: lblRun.setText("stopped (address fallout)"); break;
                case 3: lblRun.setText("stopped (instruction fallout)"); break;
            }
        }
    }

    private String getRegMnemo(int reg) {
        switch (reg) {
            case 0: return "b";
            case 1: return "c";
            case 2: return "d";
            case 3: return "e";
            case 4: return "h";
            case 5: return "l";
            case 6: return "(hl)";
            case 7: return "a";
        }
        return "";
    }
    
    private String getRegPairMnemo(int reg) {
        switch (reg) {
            case 0: return "bc";
            case 1: return "de";
            case 2: return "hl";
            case 3: return "sp";
        }
        return "";
    }
    
    private String getOpMnemo(int op) {
        switch (op) {
            case 0: return "add a,";
            case 1: return "adc a,";
            case 2: return "sub ";
            case 3: return "sbc ";
            case 4: return "and ";
            case 5: return "xor ";
            case 6: return "or ";
            case 7: return "cp ";
        }
        return "";
    }

    public ICPUInstruction cpuDecode(int memPos) {
        short val;
        int actPos = memPos;
        ICPUInstruction instr;
        String mnemo, oper;
        
        if (this.mem == null) return null;
        val = ((Short)mem.read(actPos++)).shortValue();
        oper = String.format("%02X",val);
        
        switch (val & 0x3F) {
            case 0x40: mnemo = "ld " + getRegMnemo((val>>>3)&7) + ","
                        + getRegMnemo(val & 7); break;
            case 0x80: mnemo = getOpMnemo((val>>>3)&7) + getRegMnemo(val&7); break;
        }
        switch (val & 0xC7) {
            case 0x04: mnemo = "inc " + getRegMnemo((val>>>3)&7); break;
            case 0x05: mnemo = "dec " + getRegMnemo((val>>>3)&7); break;
        }
        switch (val & 0xCF) {
            case 0x03: mnemo = "inc " + getRegPairMnemo((val>>>4)&3); break;
            case 0x09: mnemo = "add hl, " + getRegPairMnemo((val>>>4)&3); break;
            case 0x0B: mnemo = "dec " + getRegPairMnemo((val>>>4)&3); break;
                
        }

//|ADC A,N      | 7  | 2 |      |CE XX       |                     |                      |
//|ADC A,(IX+N) | 19 | 3 |      |DD 8E XX    |                     |                      |
//|ADC A,(IY+N) | 19 | 3 |      |FD 8E XX    |                     |                      |
//|ADC HL,BC    | 15 | 2 |**?V0*|ED 4A       |Add with Carry       |HL=HL+ss+CY           |
//|ADC HL,DE    | 15 | 2 |      |ED 5A       |                     |                      |
//|ADC HL,HL    | 15 | 2 |      |ED 6A       |                     |                      |
//|ADC HL,SP    | 15 | 2 |      |ED 7A       |                     |                      |
//|ADD A,N      | 7  | 2 |      |C6 XX       |                     |                      |
//|ADD A,(IX+N) | 19 | 3 |      |DD 86 XX    |                     |                      |
//|ADD A,(IY+N) | 19 | 3 |      |FD 86 XX    |                     |                      |
//|ADD IX,BC    | 15 | 2 |--?-0*|DD 09       |Add (IX register)    |IX=IX+pp              |
//|ADD IX,DE    | 15 | 2 |      |DD 19       |                     |                      |
//|ADD IX,IX    | 15 | 2 |      |DD 29       |                     |                      |
//|ADD IX,SP    | 15 | 2 |      |DD 39       |                     |                      |
//|ADD IY,BC    | 15 | 2 |--?-0*|FD 09       |Add (IY register)    |IY=IY+rr              |
//|ADD IY,DE    | 15 | 2 |      |FD 19       |                     |                      |
//|ADD IY,IY    | 15 | 2 |      |FD 29       |                     |                      |
//|ADD IY,SP    | 15 | 2 |      |FD 39       |                     |                      |
//|AND N        | 7  | 2 |      |E6 XX       |                     |                      |
//|AND (IX+N)   | 19 | 3 |      |DD A6 XX    |                     |                      |
//|AND (IY+N)   | 19 | 3 |      |FD A6 XX    |                     |                      |
//|BIT b,r      | 8  | 2 |?*1?0-|CB 40+8*b+rb|Test Bit             |m&{2^b}               |
//|BIT b,(HL)   | 12 | 2 |      |CB 46+8*b   |                     |                      |
//|BIT b,(IX+N) | 20 | 4 |      |DD CB XX 46+8*b                   |                      |
//|BIT b,(IY+N) | 20 | 4 |      |FD CB XX 46+8*b                   |                      |
//|CALL NN      | 17 | 3 |------|CD XX XX    |Unconditional Call   |-(SP)=PC,PC=nn        |
//|CALL C,NN    |17/1| 3 |------|DC XX XX    |Conditional Call     |If Carry = 1          |
//|CALL NC,NN   |17/1| 3 |      |D4 XX XX    |                     |If carry = 0          |
//|CALL M,NN    |17/1| 3 |      |FC XX XX    |                     |If Sign = 1 (negative)|
//|CALL P,NN    |17/1| 3 |      |F4 XX XX    |                     |If Sign = 0 (positive)|
//|CALL Z,NN    |17/1| 3 |      |CC XX XX    |                     |If Zero = 1 (ans.=0)  |
//|CALL NZ,NN   |17/1| 3 |      |C4 XX XX    |                     |If Zero = 0 (non-zero)|
//|CALL PE,NN   |17/1| 3 |      |EC XX XX    |                     |If Parity = 1 (even)  |
//|CALL PO,NN   |17/1| 3 |      |E4 XX XX    |                     |If Parity = 0 (odd)   |
//|CP N         | 7  | 2 |      |FE XX       |                     |                      |
//|CP (IX+N)    | 19 | 3 |      |DD BE XX    |                     |                      |
//|CP (IY+N)    | 19 | 3 |      |FD BE XX    |                     |                      |
//|CPD          | 16 | 2 |****1-|ED A9       |Compare and Decrement|A-(HL),HL=HL-1,BC=BC-1|
//|CPDR         |21/1| 2 |****1-|ED B9       |Compare, Dec., Repeat|CPD till A=(HL)or BC=0|
//|CPI          | 16 | 2 |****1-|ED A1       |Compare and Increment|A-(HL),HL=HL+1,BC=BC-1|
//|CPIR         |21/1| 2 |****1-|ED B1       |Compare, Inc., Repeat|CPI till A=(HL)or BC=0|
//|DEC (IX+N)   | 23 | 3 |      |DD 35 XX    |                     |                      |
//|DEC (IY+N)   | 23 | 3 |      |FD 35 XX    |                     |                      |
//|DEC IX       | 10 | 2 |------|DD 2B       |Decrement            |xx=xx-1               |
//|DEC IY       | 10 | 2 |      |FD 2B       |                     |                      |
//|EX (SP),IX   | 23 | 2 |------|DD E3       |                     |(SP)<->xx             |
//|EX (SP),IY   | 23 | 2 |      |FD E3       |                     |                      |
//|IM 0         | 8  | 2 |------|ED 46       |Interrupt Mode       |             (n=0,1,2)|
//|IM 1         | 8  | 2 |      |ED 56       |                     |                      |
//|IM 2         | 8  | 2 |      |ED 5E       |                     |                      |
//|IN A,(N)     | 11 | 2 |------|DB XX       |Input                |A=(n)                 |
//|IN (C)       | 12 | 2 |***P0-|ED 70       |Input*               |         (Unsupported)|
//|IN A,(C)     | 12 | 2 |***P0-|ED 78       |Input                |r=(C)                 |
//|IN B,(C)     | 12 | 2 |      |ED 40       |                     |                      |
//|IN C,(C)     | 12 | 2 |      |ED 48       |                     |                      |
//|IN D,(C)     | 12 | 2 |      |ED 50       |                     |                      |
//|IN E,(C)     | 12 | 2 |      |ED 58       |                     |                      |
//|IN H,(C)     | 12 | 2 |      |ED 60       |                     |                      |
//|IN L,(C)     | 12 | 2 |      |ED 68       |                     |                      |
//|INC IX       | 10 | 2 |------|DD 23       |Increment            |xx=xx+1               |
//|INC IY       | 10 | 2 |      |FD 23       |                     |                      |
//|INC (IX+N)   | 23 | 3 |***V0-|DD 34 XX    |Increment            |(xx+d)=(xx+d)+1       |
//|INC (IY+N)   | 23 | 3 |      |FD 34 XX    |                     |                      |
//|IND          | 16 | 2 |?*??1-|ED AA       |Input and Decrement  |(HL)=(C),HL=HL-1,B=B-1|
//|INDR         |21/1| 2 |?1??1-|ED BA       |Input, Dec., Repeat  |IND till B=0          |
//|INI          | 16 | 2 |?*??1-|ED A2       |Input and Increment  |(HL)=(C),HL=HL+1,B=B-1|
//|INIR         |21/1| 2 |?1??1-|ED B2       |Input, Inc., Repeat  |INI till B=0          |
//|JP $NN       | 10 | 3 |------|C3 XX XX    |Unconditional Jump   |PC=nn                 |
//|JP (IX)      | 8  | 2 |------|DD E9       |Unconditional Jump   |PC=(xx)               |
//|JP (IY)      | 8  | 2 |      |FD E9       |                     |                      |
//|JP C,$NN     |10/1| 3 |------|DA XX XX    |Conditional Jump     |If Carry = 1          |
//|JP NC,$NN    |10/1| 3 |      |D2 XX XX    |                     |If Carry = 0          |
//|JP M,$NN     |10/1| 3 |      |FA XX XX    |                     |If Sign = 1 (negative)|
//|JP P,$NN     |10/1| 3 |      |F2 XX XX    |                     |If Sign = 0 (positive)|
//|JP Z,$NN     |10/1| 3 |      |CA XX XX    |                     |If Zero = 1 (ans.= 0) |
//|JP NZ,$NN    |10/1| 3 |      |C2 XX XX    |                     |If Zero = 0 (non-zero)|
//|JP PE,$NN    |10/1| 3 |      |EA XX XX    |                     |If Parity = 1 (even)  |
//|JP PO,$NN    |10/1| 3 |      |E2 XX XX    |                     |If Parity = 0 (odd)   |
//|JR $N+2      | 12 | 2 |------|18 XX       |Relative Jump        |PC=PC+e               |
//|JR C,$N+2    |12/7| 2 |------|38 XX       |Cond. Relative Jump  |If cc JR(cc=C,NC,NZ,Z)|
//|JR NC,$N+2   |12/7| 2 |      |30 XX       |                     |                      |
//|JR Z,$N+2    |12/7| 2 |      |28 XX       |                     |                      |
//|JR NZ,$N+2   |12/7| 2 |      |20 XX       |                     |                      |
//|LD I,A       | 9  | 2 |------|ED 47       |Load*                |dst=src               |
//|LD R,A       | 9  | 2 |      |ED 4F       |                     |                      |
//|LD A,I       | 9  | 2 |**0*0-|ED 57       |Load*                |dst=src               |
//|LD A,R       | 9  | 2 |      |ED 5F       |                     |                      |
//|LD A,N       | 7  | 2 |      |3E XX       |                     |                      |
//|LD A,(IX+N)  | 19 | 3 |      |DD 7E XX    |                     |                      |
//|LD A,(IY+N)  | 19 | 3 |      |FD 7E XX    |                     |                      |
//|LD A,(NN)    | 13 | 3 |      |3A XX XX    |                     |                      |
//|LD B,N       | 7  | 2 |      |06 XX       |                     |                      |
//|LD B,(IX+N)  | 19 | 3 |      |DD 46 XX    |                     |                      |
//|LD B,(IY+N)  | 19 | 3 |      |FD 46 XX    |                     |                      |
//|LD C,N       | 7  | 2 |      |0E XX       |                     |                      |
//|LD C,(IX+N)  | 19 | 3 |      |DD 4E XX    |                     |                      |
//|LD C,(IY+N)  | 19 | 3 |      |FD 4E XX    |                     |                      |
//|LD D,N       | 7  | 2 |      |16 XX       |                     |                      |
//|LD D,(IX+N)  | 19 | 3 |      |DD 56 XX    |                     |                      |
//|LD D,(IY+N)  | 19 | 3 |      |FD 56 XX    |                     |                      |
//|LD E,N       | 7  | 2 |      |1E XX       |                     |                      |
//|LD E,(IX+N)  | 19 | 3 |      |DD 5E XX    |                     |                      |
//|LD E,(IY+N)  | 19 | 3 |      |FD 5E XX    |                     |                      |
//|LD H,N       | 7  | 2 |      |26 XX       |                     |                      |
//|LD H,(IX+N)  | 19 | 3 |      |DD 66 XX    |                     |                      |
//|LD H,(IY+N)  | 19 | 3 |      |FD 66 XX    |                     |                      |
//|LD L,N       | 7  | 2 |      |2E XX       |                     |                      |
//|LD L,(IX+N)  | 19 | 3 |      |DD 6E XX    |                     |                      |
//|LD L,(IY+N)  | 19 | 3 |      |FD 6E XX    |                     |                      |
//|LD BC,(NN)   | 20 | 4 |------|ED 4B XX XX |Load (16-bit)        |dst=src               |
//|LD BC,NN     | 10 | 3 |      |01 XX XX    |                     |                      |
//|LD DE,(NN)   | 20 | 4 |      |ED 5B XX XX |                     |                      |
//|LD DE,NN     | 10 | 3 |      |11 XX XX    |                     |                      |
//|LD HL,(NN)   | 20 | 3 |      |2A XX XX    |                     |                      |
//|LD HL,NN     | 10 | 3 |      |21 XX XX    |                     |                      |
//|LD SP,(NN)   | 20 | 4 |      |ED 7B XX XX |                     |                      |
//|LD SP,IX     | 10 | 2 |      |DD F9       |                     |                      |
//|LD SP,IY     | 10 | 2 |      |FD F9       |                     |                      |
//|LD SP,NN     | 10 | 3 |      |31 XX XX    |                     |                      |
//|LD IX,(NN)   | 20 | 4 |      |DD 2A XX XX |                     |                      |
//|LD IX,NN     | 14 | 4 |      |DD 21 XX XX |                     |                      |
//|LD IY,(NN)   | 20 | 4 |      |FD 2A XX XX |                     |                      |
//|LD IY,NN     | 14 | 4 |      |FD 21 XX XX |                     |                      |
//|LD (HL),N    | 10 | 2 |      |36 XX       |                     |                      |
//|LD (NN),A    | 13 | 3 |      |32 XX XX    |                     |                      |
//|LD (NN),BC   | 20 | 4 |      |ED 43 XX XX |                     |                      |
//|LD (NN),DE   | 20 | 4 |      |ED 53 XX XX |                     |                      |
//|LD (NN),HL   | 16 | 3 |      |22 XX XX    |                     |                      |
//|LD (NN),IX   | 20 | 4 |      |DD 22 XX XX |                     |                      |
//|LD (NN),IY   | 20 | 4 |      |FD 22 XX XX |                     |                      |
//|LD (NN),SP   | 20 | 4 |      |ED 73 XX XX |                     |                      |
//|LD (IX+N),r  | 19 | 3 |      |DD 70+rb XX |                     |                      |
//|LD (IX+N),N  | 19 | 4 |      |DD 36 XX XX |                     |                      |
//|LD (IY+N),r  | 19 | 3 |      |FD 70+rb XX |                     |                      |
//|LD (IY+N),N  | 19 | 4 |      |FD 36 XX XX |                     |                      |
//|LDD          | 16 | 2 |--0*0-|ED A8       |Load and Decrement   |(DE)=(HL),HL=HL-1,#   |
//|LDDR         |21/1| 2 |--000-|ED B8       |Load, Dec., Repeat   |LDD till BC=0         |
//|LDI          | 16 | 2 |--0*0-|ED A0       |Load and Increment   |(DE)=(HL),HL=HL+1,#   |
//|LDIR         |21/1| 2 |--000-|ED B0       |Load, Inc., Repeat   |LDI till BC=0         |
//|NEG          | 8  | 2 |***V1*|ED 44       |Negate               |A=-A                  |
//|OR N         | 7  | 2 |      |F6 XX       |                     |                      |
//|OR (IX+N)    | 19 | 3 |      |DD B6 XX    |                     |                      |
//|OR (IY+N)    | 19 | 3 |      |FD B6 XX    |                     |                      |
//|OUT (N),A    | 11 | 2 |------|D3 XX       |Output               |(n)=A                 |
//|OUT (C),0    | 12 | 2 |------|ED 71       |Output*              |         (Unsupported)|
//|OUT (C),A    | 12 | 2 |------|ED 79       |Output               |(C)=r                 |
//|OUT (C),B    | 12 | 2 |      |ED 41       |                     |                      |
//|OUT (C),C    | 12 | 2 |      |ED 49       |                     |                      |
//|OUT (C),D    | 12 | 2 |      |ED 51       |                     |                      |
//|OUT (C),E    | 12 | 2 |      |ED 59       |                     |                      |
//|OUT (C),H    | 12 | 2 |      |ED 61       |                     |                      |
//|OUT (C),L    | 12 | 2 |      |ED 69       |                     |                      |
//|OUTD         | 16 | 2 |?*??1-|ED AB       |Output and Decrement |(C)=(HL),HL=HL-1,B=B-1|
//|OTDR         |21/1| 2 |?1??1-|ED BB       |Output, Dec., Repeat |OUTD till B=0         |
//|OUTI         | 16 | 2 |?*??1-|ED A3       |Output and Increment |(C)=(HL),HL=HL+1,B=B-1|
//|OTIR         |21/1| 2 |?1??1-|ED B3       |Output, Inc., Repeat |OUTI till B=0         |
//|POP IX       | 14 | 2 |------|DD E1       |Pop                  |xx=(SP)+              |
//|POP IY       | 14 | 2 |      |FD E1       |                     |                      |
//|PUSH IX      | 15 | 2 |------|DD E5       |Push                 |-(SP)=xx              |
//|PUSH IY      | 15 | 2 |      |FD E5       |                     |                      |
//|RES b,r      | 8  | 2 |------|CB 80+8*b+rb|Reset bit            |m=m&{~2^b}            |
//|RES b,(HL)   | 15 | 2 |------|CB 86+8*b   |                     |                      |
//|RES b,(IX+N) | 23 | 4 |------|DD CB XX 86+8*b                   |                      |
//|RES b,(IY+N) | 23 | 4 |------|FD CB XX 86+8*b                   |                      |
//|RETI         | 14 | 2 |------|ED 4D       |Return from Interrupt|PC=(SP)+              |
//|RETN         | 14 | 2 |------|ED 45       |Return from NMI      |PC=(SP)+              |
//|RL r         | 8  | 2 |**0P0*|CB 10+rb    |Rotate Left          |m={CY,m}<-            |
//|RL (HL)      | 15 | 2 |      |CB 16       |                     |                      |
//|RL (IX+N)    | 23 | 4 |      |DD CB XX 16 |                     |                      |
//|RL (IY+N)    | 23 | 4 |      |FD CB XX 16 |                     |                      |
//|RLC r        | 8  | 2 |**0P0*|CB 00+rb    |Rotate Left Circular |m=m<-                 |
//|RLC (HL)     | 15 | 2 |      |CB 06       |                     |                      |
//|RLC (IX+N)   | 23 | 4 |      |DD CB XX 06 |                     |                      |
//|RLC (IY+N)   | 23 | 4 |      |FD CB XX 06 |                     |                      |
//|RLD          | 18 | 2 |**0P0-|ED 6F       |Rotate Left 4 bits   |{A,(HL)}={A,(HL)}<- ##|
//|RR r         | 8  | 2 |**0P0*|CB 18+rb    |Rotate Right         |m=->{CY,m}            |
//|RR (HL)      | 15 | 2 |      |CB 1E       |                     |                      |
//|RR (IX+N)    | 23 | 4 |      |DD CB XX 1E |                     |                      |
//|RR (IY+N)    | 23 | 4 |      |FD CB XX 1E |                     |                      |
//|RRC r        | 8  | 2 |**0P0*|CB 08+rb    |Rotate Right Circular|m=->m                 |
//|RRC (HL)     | 15 | 2 |      |CB 0E       |                     |                      |
//|RRC (IX+N)   | 23 | 4 |      |DD CB XX 0E |                     |                      |
//|RRC (IY+N)   | 23 | 4 |      |FD CB XX 0E |                     |                      |
//|RRD          | 18 | 2 |**0P0-|ED 67       |Rotate Right 4 bits  |{A,(HL)}=->{A,(HL)} ##|
//|SBC A,N      | 7  | 2 |      |DE XX       |                     |                      |
//|SBC A,(IX+N) | 19 | 3 |      |DD 9E XX    |                     |                      |
//|SBC A,(IY+N) | 19 | 3 |      |FD 9E XX    |                     |                      |
//|SBC HL,BC    | 15 | 2 |**?V1*|ED 42       |Subtract with Carry  |HL=HL-ss-CY           |
//|SBC HL,DE    | 15 | 2 |      |ED 52       |                     |                      |
//|SBC HL,HL    | 15 | 2 |      |ED 62       |                     |                      |
//|SBC HL,SP    | 15 | 2 |      |ED 72       |                     |                      |
//|SET b,r      | 8  | 2 |------|CB C0+8*b+rb|Set bit              |m=mv{2^b}             |
//|SET b,(HL)   | 15 | 2 |      |CB C6+8*b   |                     |                      |
//|SET b,(IX+N) | 23 | 4 |      |DD CB XX C6+8*b                   |                      |
//|SET b,(IY+N) | 23 | 4 |      |FD CB XX C6+8*b                   |                      |
//|SLA r        | 8  | 2 |**0P0*|CB 20+rb    |Shift Left Arithmetic|m=m*2                 |
//|SLA (HL)     | 15 | 2 |      |CB 26       |                     |                      |
//|SLA (IX+N)   | 23 | 4 |      |DD CB XX 26 |                     |                      |
//|SLA (IY+N)   | 23 | 4 |      |FD CB XX 26 |                     |                      |
//|SRA r        | 8  | 2 |**0P0*|CB 28+rb    |Shift Right Arith.   |m=m/2                 |
//|SRA (HL)     | 15 | 2 |      |CB 2E       |                     |                      |
//|SRA (IX+N)   | 23 | 4 |      |DD CB XX 2E |                     |                      |
//|SRA (IY+N)   | 23 | 4 |      |FD CB XX 2E |                     |                      |
//|SLL r        | 8  | 2 |**0P0*|CB 30+rb    |Shift Left Logical*  |m={0,m,CY}<-          |
//|SLL (HL)     | 15 | 2 |      |CB 36       |                     |  (SLL instructions   |
//|SLL (IX+N)   | 23 | 4 |      |DD CB XX 36 |                     |     are Unsupported) |
//|SLL (IY+N)   | 23 | 4 |      |FD CB XX 36 |                     |                      |
//|SRL r        | 8  | 2 |**0P0*|CB 38+rb    |Shift Right Logical  |m=->{0,m,CY}          |
//|SRL (HL)     | 15 | 2 |      |CB 3E       |                     |                      |
//|SRL (IX+N)   | 23 | 4 |      |DD CB XX 3E |                     |                      |
//|SRL (IY+N)   | 23 | 4 |      |FD CB XX 3E |                     |                      |
//|SUB N        | 7  | 2 |      |D6 XX       |                     |                      |
//|SUB (IX+N)   | 19 | 3 |      |DD 96 XX    |                     |                      |
//|SUB (IY+N)   | 19 | 3 |      |FD 96 XX    |                     |                      |
//|XOR N        | 7  | 2 |      |EE XX       |                     |                      |
//|XOR (IX+N)   | 19 | 3 |      |DD AE XX    |                     |                      |
//|XOR (IY+N)   | 19 | 3 |      |FD AE XX    |                 
//        
        mnemo = null;
        switch (val) {
            case 0x00: mnemo = "nop"; break;
            case 0x02: mnemo = "ld (bc),a"; break;
            case 0x07: mnemo = "rlca"; break;
            case 0x08: mnemo = "ex af,af'"; break;
            case 0x0A: mnemo = "ld a,(bc)"; break;
            case 0x0B: case 0x0C: case 0x0D:
            case 0x0F: case 0x10: case 0x12: case 0x13: case 0x14: case 0x15:
            case 0x17: case 0x19:
            case 0x1A: mnemo = "ld a,(de)"; break;
            case 0x1B: case 0x1C: case 0x1D:
            case 0x1F: case 0x23: case 0x24: case 0x25:
            case 0x27: mnemo = "daa"; break;
            case 0x29:
            case 0x2B: case 0x2C: case 0x2D:
            case 0x2F: mnemo = "cpl"; break;
            case 0x33: case 0x34:
            case 0x35: case 0x37: case 0x39: case 0x3B: case 0x3C: case 0x3D:
            case 0x3F: mnemo = "ccf"; break;
            case 0xC0: case 0xC1: case 0xC5: case 0xC7: case 0xC8:
            case 0xC9: case 0xCF: case 0xD0: case 0xD1: case 0xD5: case 0xD7:
            case 0xD8: 
            case 0xD9: mnemo = "exx"; break;
            case 0xDF: case 0xE0: case 0xE1:
            case 0xE3: mnemo = "ex (sp),hl"; break;
            case 0xE5: case 0xE7: case 0xE8: case 0xE9:
            case 0xEB: mnemo = "ex de,hl"; break;
            case 0xEF:
            case 0xF0: case 0xF1:
            case 0xF3: mnemo = "di"; break;
            case 0xF5: case 0xF7: case 0xF8:
            case 0xF9:
            case 0xFB: mnemo = "ei"; break;
            case 0xFF:
//|DJNZ $+2     |13/8| 1 |------|10          |Dec., Jump Non-Zero  |B=B-1 till B=0        |
//|HALT         | 4  | 1 |------|76          |Halt                 |                      |
//|JP (HL)      | 4  | 1 |------|E9          |Unconditional Jump   |PC=(HL)               |
//|LD SP,HL     | 6  | 1 |      |F9          |                     |                      |
//|LD (DE),A    | 7  | 1 |      |12          |                     |                      |
//|POP AF       | 10 | 1 |------|F1          |Pop                  |qq=(SP)+              |
//|POP BC       | 10 | 1 |      |C1          |                     |                      |
//|POP DE       | 10 | 1 |      |D1          |                     |                      |
//|POP HL       | 10 | 1 |      |E1          |                     |                      |
//|PUSH AF      | 11 | 1 |------|F5          |Push                 |-(SP)=qq              |
//|PUSH BC      | 11 | 1 |      |C5          |                     |                      |
//|PUSH DE      | 11 | 1 |      |D5          |                     |                      |
//|PUSH HL      | 11 | 1 |      |E5          |                     |                      |
//|RET          | 10 | 1 |------|C9          |Return               |PC=(SP)+              |
//|RET C        |11/5| 1 |------|D8          |Conditional Return   |If Carry = 1          |
//|RET NC       |11/5| 1 |      |D0          |                     |If Carry = 0          |
//|RET M        |11/5| 1 |      |F8          |                     |If Sign = 1 (negative)|
//|RET P        |11/5| 1 |      |F0          |                     |If Sign = 0 (positive)|
//|RET Z        |11/5| 1 |      |C8          |                     |If Zero = 1 (ans.=0)  |
//|RET NZ       |11/5| 1 |      |C0          |                     |If Zero = 0 (non-zero)|
//|RET PE       |11/5| 1 |      |E8          |                     |If Parity = 1 (even)  |
//|RET PO       |11/5| 1 |      |E0          |                     |If Parity = 0 (odd)   |
//|RLA          | 4  | 1 |--0-0*|17          |Rotate Left Acc.     |A={CY,A}<-            |
//|RRA          | 4  | 1 |--0-0*|1F          |Rotate Right Acc.    |A=->{CY,A}            |
//|RRCA         | 4  | 1 |--0-0*|0F          |Rotate Right Cir.Acc.|A=->A                 |
//|RST 0        | 11 | 1 |------|C7          |Restart              | (p=0H,8H,10H,...,38H)|
//|RST 08H      | 11 | 1 |      |CF          |                     |                      |
//|RST 10H      | 11 | 1 |      |D7          |                     |                      |
//|RST 18H      | 11 | 1 |      |DF          |                     |                      |
//|RST 20H      | 11 | 1 |      |E7          |                     |                      |
//|RST 28H      | 11 | 1 |      |EF          |                     |                      |
//|RST 30H      | 11 | 1 |      |F7          |                     |                      |
//|RST 38H      | 11 | 1 |      |FF          |                     |                      |
//|SCF          | 4  | 1 |--0-01|37          |Set Carry Flag       |CY=1                  |
            
            case 0x06: case 0x0E: case 0x16: case 0x18: case 0x1E: case 0x20:
            case 0x26: case 0x28: case 0x2E: case 0x30: case 0x36: case 0x38:
            case 0x3E: case 0xC6: case 0xCE: case 0xD3: case 0xD6: case 0xDB:
            case 0xDE: case 0xE6: case 0xEE: case 0xF6: case 0xFE: case 0xCB:
                return (memPos+1);
            case 0xED:
                tmp = (Short)mem.read(memPos++);
                switch (tmp) {
                    case 0x40: case 0x41: case 0x42: case 0x44: case 0x45:
                    case 0x46: case 0x47: case 0x48: case 0x49: case 0x4A:
                    case 0x4D: case 0x4F: case 0x50: case 0x51: case 0x52:
                    case 0x56: case 0x57: case 0x58: case 0x59: case 0x5A:
                    case 0x5E: case 0x5F: case 0x60: case 0x61: case 0x62:
                    case 0x67: case 0x6A: case 0x6F: case 0x70: case 0x71:
                    case 0x72: case 0x78: case 0x79: case 0x7A: case 0xA0:
                    case 0xA1: case 0xA2: case 0xA3: case 0xA8: case 0xA9:
                    case 0xAA: case 0xAB: case 0xB0: case 0xB1: case 0xB2:
                    case 0xB3: case 0xB8: case 0xB9: case 0xBA: case 0xBB:
                        return memPos;
                    case 0x43: case 0x4B: case 0x53: case 0x5B: case 0x73:
                    case 0x7B:
                        return memPos+2;
                } memPos--;
                break;
            case 0xDD:
                tmp = (Short)mem.read(memPos++);
                switch (tmp) {
                    case 0x09: case 0x19: case 0x23: case 0x29: case 0x2B:
                    case 0x34: case 0x39: case 0xE1: case 0xE3: case 0xE5:
                    case 0xE9: case 0xF9:
                        return memPos;
                    case 0x35: case 0x46: case 0x4E: case 0x56: case 0x5E:
                    case 0x66: case 0x6E: case 0x7E: case 0x86: case 0x8E:
                    case 0x96: case 0x9E: case 0xA6: case 0xAE: case 0xB6:
                    case 0xBE: case 0x70: case 0x71: case 0x72: case 0x73:
                    case 0x74: case 0x75: case 0x77:
                        return (memPos+1);
                    case 0x21: case 0x22: case 0x2A: case 0x36: 
                        return (memPos+2);
                    case 0xCB:
                        memPos++;
                        tmp = (Short)mem.read(memPos++);
                        switch (tmp) {
                            case 0x06: case 0x0E: case 0x16: case 0x1E:
                            case 0x26: case 0x2E: case 0x36: case 0x3E:
                            case 0x46: case 0x4E: case 0x56: case 0x5E:
                            case 0x66: case 0x6E: case 0x76: case 0x7E:
                            case 0x86: case 0x8E: case 0x96: case 0x9E:
                            case 0xA6: case 0xAE: case 0xB6: case 0xBE:
                            case 0xC6: case 0xCE: case 0xD6: case 0xDE:
                            case 0xE6: case 0xEE: case 0xF6: case 0xFE:
                                return memPos;
                        } memPos -= 2;
                        
                } memPos--;
                break;
            case 0xFD:
                tmp = (Short)mem.read(memPos++);
                switch (tmp) {
                    case 0x09: case 0x19: case 0x23: case 0x29: case 0x2B:
                    case 0x34: case 0x39: case 0xE1: case 0xE3: case 0xE5:
                    case 0xE9: case 0xF9:
                        return memPos;
                    case 0x35: case 0x46: case 0x4E: case 0x56: case 0x5E:
                    case 0x66: case 0x6E: case 0x7E: case 0x86: case 0x8E:
                    case 0x96: case 0x9E: case 0xA6: case 0xAE: case 0xB6:
                    case 0xBE: case 0x70: case 0x71: case 0x72: case 0x73:
                    case 0x74: case 0x75: case 0x77:
                        return (memPos+1);
                    case 0x21: case 0x22: case 0x2A: case 0x36: 
                        return (memPos+2);
                    case 0xCB:
                        memPos++;
                        tmp = (Short)mem.read(memPos++);
                        switch (tmp) {
                            case 0x06: case 0x0E: case 0x16: case 0x1E:
                            case 0x26: case 0x2E: case 0x36: case 0x3E:
                            case 0x46: case 0x4E: case 0x56: case 0x5E:
                            case 0x66: case 0x6E: case 0x76: case 0x7E:
                            case 0x86: case 0x8E: case 0x96: case 0x9E:
                            case 0xA6: case 0xAE: case 0xB6: case 0xBE:
                            case 0xC6: case 0xCE: case 0xD6: case 0xDE:
                            case 0xE6: case 0xEE: case 0xF6: case 0xFE:
                                return memPos;
                        } memPos -= 2;
                        
                } memPos--;
                break;
//            case 0xCB:
  //              return memPos + 1;
//                tmp = (Short)mem.read(memPos++);
//                switch (tmp) {
//                    case 0x00: case 0x01: case 0x02: case 0x03: case 0x04:
//                    case 0x05: case 0x06: case 0x07: case 0x08: case 0x09:
//                    case 0x0A: case 0x0B: case 0x0C: case 0x0D: case 0x0E:
//                    case 0x0F: case 0x10: case 0x11: case 0x12: case 0x13:
//                    case 0x14: case 0x15: case 0x16: case 0x17: case 0x18:
//                    case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D:
//                    case 0x1E: case 0x1F: case 0x20: case 0x21: case 0x22:
//                    case 0x23: case 0x24: case 0x25: case 0x26: case 0x27:
//                    case 0x28: case 0x29: case 0x2A: case 0x2B: case 0x2C:
//                    case 0x2D: case 0x2E: case 0x2F: case 0x30: case 0x31:
//                    case 0x32: case 0x33: case 0x34: case 0x35: case 0x36:
//                    case 0x37: case 0x38: case 0x39: case 0x3A: case 0x3B:
//                    case 0x3C: case 0x3D: case 0x3E: case 0x3F: case 0x40:
//                    case 0x41: case 0x42: case 0x43: case 0x44: case 0x45:
//                    case 0x46: case 0x47: case 0x48: case 0x49: case 0x4A:
//                    case 0x4B: case 0x4C: case 0x4D: case 0x4E: case 0x4F:
//                    case 0x50: case 0x51: case 0x52: case 0x53: case 0x54:
//                    case 0x55: case 0x56: case 0x57: case 0x58: case 0x59:
//                    case 0x5A: case 0x5B: case 0x5C: case 0x5D: case 0x5E:
//                    case 0x5F: case 0x60: case 0x61: case 0x62: case 0x63:
//                    case 0x64: case 0x65: case 0x66: case 0x67: case 0x68:
//                    case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D:
//                    case 0x6E: case 0x6F: case 0x70: case 0x71: case 0x72:
//                    case 0x73: case 0x74: case 0x75: case 0x76: case 0x77:
//                    case 0x78: case 0x79: case 0x7A: case 0x7B: case 0x7C:
//                    case 0x7D: case 0x7E: case 0x7F: case 0x80: case 0x81:
//                    case 0x82: case 0x83: case 0x84: case 0x85: case 0x86:
//                    case 0x87: case 0x88: case 0x89: case 0x8A: case 0x8B:
//                    case 0x8C: case 0x8D: case 0x8E: case 0x8F: case 0x90:
//                    case 0x91: case 0x92: case 0x93: case 0x94: case 0x95:
//                    case 0x96: case 0x97: case 0x98: case 0x99: case 0x9A:
//                    case 0x9B: case 0x9C: case 0x9D: case 0x9E: case 0x9F:
//                    case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4:
//                    case 0xA5: case 0xA6: case 0xA7: case 0xA8: case 0xA9:
//                    case 0xAA: case 0xAB: case 0xAC: case 0xAD: case 0xAE:
//                    case 0xAF: case 0xB0: case 0xB1: case 0xB2: case 0xB3:
//                    case 0xB4: case 0xB5: case 0xB6: case 0xB7: case 0xB8:
//                    case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD:
//                    case 0xBE: case 0xBF:
//                    case 0xC6: case 0xCE: case 0xD6: case 0xDE: case 0xE6:
//                    case 0xEE: case 0xF6: case 0xFE: 
//                        return memPos;
//                } memPos--;
            case 0x01: case 0x11: case 0x21: case 0x22: case 0x2A: case 0x31:
            case 0x32: case 0x3A: case 0xC2: case 0xC3: case 0xC4: case 0xCA:
            case 0xCC: case 0xCD: case 0xD2: case 0xD4: case 0xDC: case 0xDA:
            case 0xE2: case 0xE4: case 0xEA: case 0xEC: case 0xF2: case 0xF4:
            case 0xFA: case 0xFC:
                return (memPos+2);
        }
        
        
        instr = new ICPUInstruction(mnemo,oper,actPos);
        return instr;
    }
    
    /**
     * Return memory location of next instruction (from memPos)
     * Instructions that are located at address memPos+1, are not included
     * because for not found opcodes this method returns memPos+1 implicitly.
     * @return memory location of next instruction
     */
    public int getNextPosition(int memPos) throws ArrayIndexOutOfBoundsException {
        short val,tmp;
        if (mem == null) return 0;
        val = (Short)mem.read(memPos++);
        switch (val) {
            case 0x06: case 0x0E: case 0x10: case 0x16: case 0x18: case 0x1E:
            case 0x20: case 0x26: case 0x28: case 0x2E: case 0x30: case 0x36:
            case 0x38: case 0x3E: case 0xC6: case 0xCE: case 0xD3: case 0xD6:
            case 0xDB: case 0xDE: case 0xE6: case 0xEE: case 0xF6: case 0xFE:
            case 0xCB:
                return (memPos+1);
            case 0xED:
                tmp = (Short)mem.read(memPos++);
                switch (tmp) {
                    case 0x40: case 0x41: case 0x42: case 0x44: case 0x45:
                    case 0x46: case 0x47: case 0x48: case 0x49: case 0x4A:
                    case 0x4D: case 0x4F: case 0x50: case 0x51: case 0x52:
                    case 0x56: case 0x57: case 0x58: case 0x59: case 0x5A:
                    case 0x5E: case 0x5F: case 0x60: case 0x61: case 0x62:
                    case 0x67: case 0x6A: case 0x6F: case 0x70: case 0x71:
                    case 0x72: case 0x78: case 0x79: case 0x7A: case 0xA0:
                    case 0xA1: case 0xA2: case 0xA3: case 0xA8: case 0xA9:
                    case 0xAA: case 0xAB: case 0xB0: case 0xB1: case 0xB2:
                    case 0xB3: case 0xB8: case 0xB9: case 0xBA: case 0xBB:
                        return memPos;
                    case 0x43: case 0x4B: case 0x53: case 0x5B: case 0x73:
                    case 0x7B:
                        return memPos+2;
                } memPos--;
                break;
            case 0xDD:
                tmp = (Short)mem.read(memPos++);
                switch (tmp) {
                    case 0x09: case 0x19: case 0x23: case 0x29: case 0x2B:
                    case 0x34: case 0x39: case 0xE1: case 0xE3: case 0xE5:
                    case 0xE9: case 0xF9:
                        return memPos;
                    case 0x35: case 0x46: case 0x4E: case 0x56: case 0x5E:
                    case 0x66: case 0x6E: case 0x7E: case 0x86: case 0x8E:
                    case 0x96: case 0x9E: case 0xA6: case 0xAE: case 0xB6:
                    case 0xBE: case 0x70: case 0x71: case 0x72: case 0x73:
                    case 0x74: case 0x75: case 0x77:
                        return (memPos+1);
                    case 0x21: case 0x22: case 0x2A: case 0x36: 
                        return (memPos+2);
                    case 0xCB:
                        memPos++;
                        tmp = (Short)mem.read(memPos++);
                        switch (tmp) {
                            case 0x06: case 0x0E: case 0x16: case 0x1E:
                            case 0x26: case 0x2E: case 0x36: case 0x3E:
                            case 0x46: case 0x4E: case 0x56: case 0x5E:
                            case 0x66: case 0x6E: case 0x76: case 0x7E:
                            case 0x86: case 0x8E: case 0x96: case 0x9E:
                            case 0xA6: case 0xAE: case 0xB6: case 0xBE:
                            case 0xC6: case 0xCE: case 0xD6: case 0xDE:
                            case 0xE6: case 0xEE: case 0xF6: case 0xFE:
                                return memPos;
                        } memPos -= 2;
                        
                } memPos--;
                break;
            case 0xFD:
                tmp = (Short)mem.read(memPos++);
                switch (tmp) {
                    case 0x09: case 0x19: case 0x23: case 0x29: case 0x2B:
                    case 0x34: case 0x39: case 0xE1: case 0xE3: case 0xE5:
                    case 0xE9: case 0xF9:
                        return memPos;
                    case 0x35: case 0x46: case 0x4E: case 0x56: case 0x5E:
                    case 0x66: case 0x6E: case 0x7E: case 0x86: case 0x8E:
                    case 0x96: case 0x9E: case 0xA6: case 0xAE: case 0xB6:
                    case 0xBE: case 0x70: case 0x71: case 0x72: case 0x73:
                    case 0x74: case 0x75: case 0x77:
                        return (memPos+1);
                    case 0x21: case 0x22: case 0x2A: case 0x36: 
                        return (memPos+2);
                    case 0xCB:
                        memPos++;
                        tmp = (Short)mem.read(memPos++);
                        switch (tmp) {
                            case 0x06: case 0x0E: case 0x16: case 0x1E:
                            case 0x26: case 0x2E: case 0x36: case 0x3E:
                            case 0x46: case 0x4E: case 0x56: case 0x5E:
                            case 0x66: case 0x6E: case 0x76: case 0x7E:
                            case 0x86: case 0x8E: case 0x96: case 0x9E:
                            case 0xA6: case 0xAE: case 0xB6: case 0xBE:
                            case 0xC6: case 0xCE: case 0xD6: case 0xDE:
                            case 0xE6: case 0xEE: case 0xF6: case 0xFE:
                                return memPos;
                        } memPos -= 2;
                        
                } memPos--;
                break;
            case 0x01: case 0x11: case 0x21: case 0x22: case 0x2A: case 0x31:
            case 0x32: case 0x3A: case 0xC2: case 0xC3: case 0xC4: case 0xCA:
            case 0xCC: case 0xCD: case 0xD2: case 0xD4: case 0xDC: case 0xDA:
            case 0xE2: case 0xE4: case 0xEA: case 0xEC: case 0xF2: case 0xF4:
            case 0xFA: case 0xFC:
                return (memPos+2);
        }
        return memPos;
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel paneRegisters = new javax.swing.JPanel();
        javax.swing.JTabbedPane tabbedGPR = new javax.swing.JTabbedPane();
        javax.swing.JPanel panelSET1 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        txtRegB = new javax.swing.JTextField();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        txtRegC = new javax.swing.JTextField();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        txtRegBC = new javax.swing.JTextField();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        txtRegD = new javax.swing.JTextField();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        txtRegE = new javax.swing.JTextField();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        txtRegDE = new javax.swing.JTextField();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        txtRegH = new javax.swing.JTextField();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        txtRegL = new javax.swing.JTextField();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        txtRegHL = new javax.swing.JTextField();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        txtRegA = new javax.swing.JTextField();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        txtRegF = new javax.swing.JTextField();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        tblFlags = new javax.swing.JTable();
        javax.swing.JPanel panelSET2 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        txtRegB1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel14 = new javax.swing.JLabel();
        txtRegC1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel15 = new javax.swing.JLabel();
        txtRegBC1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel16 = new javax.swing.JLabel();
        txtRegD1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel17 = new javax.swing.JLabel();
        txtRegE1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel18 = new javax.swing.JLabel();
        txtRegDE1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel19 = new javax.swing.JLabel();
        txtRegH1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel20 = new javax.swing.JLabel();
        txtRegL1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel21 = new javax.swing.JLabel();
        txtRegHL1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel22 = new javax.swing.JLabel();
        txtRegA1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel23 = new javax.swing.JLabel();
        txtRegF1 = new javax.swing.JTextField();
        javax.swing.JLabel jLabel24 = new javax.swing.JLabel();
        tblFlags2 = new javax.swing.JTable();
        javax.swing.JLabel jLabel25 = new javax.swing.JLabel();
        txtRegPC = new javax.swing.JTextField();
        javax.swing.JLabel jLabel26 = new javax.swing.JLabel();
        txtRegSP = new javax.swing.JTextField();
        javax.swing.JLabel jLabel27 = new javax.swing.JLabel();
        txtRegIX = new javax.swing.JTextField();
        javax.swing.JLabel jLabel28 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel29 = new javax.swing.JLabel();
        txtRegI = new javax.swing.JTextField();
        javax.swing.JLabel jLabel30 = new javax.swing.JLabel();
        txtRegIY = new javax.swing.JTextField();
        txtRegR = new javax.swing.JTextField();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        lblRun = new javax.swing.JLabel();
        javax.swing.JLabel jLabel31 = new javax.swing.JLabel();
        spnFrequency = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel32 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel33 = new javax.swing.JLabel();
        spnTestPeriode = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel34 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel35 = new javax.swing.JLabel();
        lblFrequency = new javax.swing.JLabel();

        setBorder(null);

        paneRegisters.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(java.awt.Color.gray, 1, true), "Registers", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 14), java.awt.Color.gray)); // NOI18N

        tabbedGPR.setBorder(null);

        panelSET1.setBorder(null);

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setText("B");

        txtRegB.setEditable(false);
        txtRegB.setText("0");
        txtRegB.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel2.setText("C");

        txtRegC.setEditable(false);
        txtRegC.setText("0");
        txtRegC.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel3.setText("BC");

        txtRegBC.setEditable(false);
        txtRegBC.setText("0");
        txtRegBC.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel4.setText("D");

        txtRegD.setEditable(false);
        txtRegD.setText("0");
        txtRegD.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel5.setText("E");

        txtRegE.setEditable(false);
        txtRegE.setText("0");
        txtRegE.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel6.setText("DE");

        txtRegDE.setEditable(false);
        txtRegDE.setText("0");
        txtRegDE.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel7.setText("H");

        txtRegH.setEditable(false);
        txtRegH.setText("0");
        txtRegH.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel8.setText("L");

        txtRegL.setEditable(false);
        txtRegL.setText("0");
        txtRegL.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel9.setText("HL");

        txtRegHL.setEditable(false);
        txtRegHL.setText("0");
        txtRegHL.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel10.setText("A");

        txtRegA.setEditable(false);
        txtRegA.setText("0");
        txtRegA.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel11.setFont(jLabel11.getFont().deriveFont(jLabel11.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel11.setText("F");

        txtRegF.setEditable(false);
        txtRegF.setText("0");
        txtRegF.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel12.setText("Flags (F):");

        tblFlags.setAutoCreateRowSorter(true);
        tblFlags.setBackground(java.awt.Color.white);
        tblFlags.setBorder(null);
        tblFlags.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"S", "Z", "H", "P/V", "N", "C"},
                {"0", "0", "0", "0", "0", "0"}
            },
            new String [] {
                "S", "Z", "H", "P/V", "N", "C"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblFlags.setRowSelectionAllowed(false);

        javax.swing.GroupLayout panelSET1Layout = new javax.swing.GroupLayout(panelSET1);
        panelSET1.setLayout(panelSET1Layout);
        panelSET1Layout.setHorizontalGroup(
            panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSET1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(tblFlags, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelSET1Layout.createSequentialGroup()
                        .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelSET1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegB, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegC, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3))
                            .addGroup(panelSET1Layout.createSequentialGroup()
                                .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panelSET1Layout.createSequentialGroup()
                                            .addComponent(jLabel4)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtRegD, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLabel5)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtRegE, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelSET1Layout.createSequentialGroup()
                                            .addComponent(jLabel7)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSET1Layout.createSequentialGroup()
                                                    .addComponent(txtRegA, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                                                .addGroup(panelSET1Layout.createSequentialGroup()
                                                    .addComponent(txtRegH, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(jLabel8)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtRegL)))))
                                    .addGroup(panelSET1Layout.createSequentialGroup()
                                        .addComponent(jLabel10)
                                        .addGap(54, 54, 54)
                                        .addComponent(jLabel11)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtRegF, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(12, 12, 12)
                                .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel6))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtRegDE, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRegBC, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRegHL, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(155, 155, 155))
        );
        panelSET1Layout.setVerticalGroup(
            panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSET1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtRegC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(txtRegB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(txtRegBC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtRegE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(txtRegD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(txtRegDE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelSET1Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jLabel11)
                            .addComponent(txtRegF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelSET1Layout.createSequentialGroup()
                        .addGroup(panelSET1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(txtRegH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8)
                            .addComponent(txtRegL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9)
                            .addComponent(txtRegHL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tblFlags, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedGPR.addTab("Set1", panelSET1);

        panelSET2.setBorder(null);

        jLabel13.setFont(jLabel13.getFont().deriveFont(jLabel13.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel13.setText("B");

        txtRegB1.setEditable(false);
        txtRegB1.setText("0");
        txtRegB1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel14.setFont(jLabel14.getFont().deriveFont(jLabel14.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel14.setText("C");

        txtRegC1.setEditable(false);
        txtRegC1.setText("0");
        txtRegC1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel15.setText("BC");

        txtRegBC1.setEditable(false);
        txtRegBC1.setText("0");
        txtRegBC1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel16.setFont(jLabel16.getFont().deriveFont(jLabel16.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel16.setText("D");

        txtRegD1.setEditable(false);
        txtRegD1.setText("0");
        txtRegD1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel17.setFont(jLabel17.getFont().deriveFont(jLabel17.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel17.setText("E");

        txtRegE1.setEditable(false);
        txtRegE1.setText("0");
        txtRegE1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel18.setFont(jLabel18.getFont().deriveFont(jLabel18.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel18.setText("DE");

        txtRegDE1.setEditable(false);
        txtRegDE1.setText("0");
        txtRegDE1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel19.setFont(jLabel19.getFont().deriveFont(jLabel19.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel19.setText("H");

        txtRegH1.setEditable(false);
        txtRegH1.setText("0");
        txtRegH1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel20.setFont(jLabel20.getFont().deriveFont(jLabel20.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel20.setText("L");

        txtRegL1.setEditable(false);
        txtRegL1.setText("0");
        txtRegL1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel21.setFont(jLabel21.getFont().deriveFont(jLabel21.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel21.setText("HL");

        txtRegHL1.setEditable(false);
        txtRegHL1.setText("0");
        txtRegHL1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel22.setFont(jLabel22.getFont().deriveFont(jLabel22.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel22.setText("A");

        txtRegA1.setEditable(false);
        txtRegA1.setText("0");
        txtRegA1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel23.setFont(jLabel23.getFont().deriveFont(jLabel23.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel23.setText("F");

        txtRegF1.setEditable(false);
        txtRegF1.setText("0");
        txtRegF1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel24.setText("Flags (F):");

        tblFlags2.setAutoCreateRowSorter(true);
        tblFlags2.setBackground(java.awt.Color.white);
        tblFlags2.setBorder(null);
        tblFlags2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"S", "Z", "H", "P/V", "N", "C"},
                {"0", "0", "0", "0", "0", "0"}
            },
            new String [] {
                "S", "Z", "H", "P/V", "N", "C"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblFlags2.setRowSelectionAllowed(false);

        javax.swing.GroupLayout panelSET2Layout = new javax.swing.GroupLayout(panelSET2);
        panelSET2.setLayout(panelSET2Layout);
        panelSET2Layout.setHorizontalGroup(
            panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSET2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(tblFlags2, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelSET2Layout.createSequentialGroup()
                        .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelSET2Layout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegB1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegC1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15))
                            .addGroup(panelSET2Layout.createSequentialGroup()
                                .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panelSET2Layout.createSequentialGroup()
                                            .addComponent(jLabel16)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtRegD1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLabel17)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtRegE1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelSET2Layout.createSequentialGroup()
                                            .addComponent(jLabel19)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSET2Layout.createSequentialGroup()
                                                    .addComponent(txtRegA1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                                                .addGroup(panelSET2Layout.createSequentialGroup()
                                                    .addComponent(txtRegH1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(jLabel20)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(txtRegL1)))))
                                    .addGroup(panelSET2Layout.createSequentialGroup()
                                        .addComponent(jLabel22)
                                        .addGap(54, 54, 54)
                                        .addComponent(jLabel23)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtRegF1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(12, 12, 12)
                                .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel21)
                                    .addComponent(jLabel18))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtRegDE1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRegBC1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRegHL1, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(155, 155, 155))
        );
        panelSET2Layout.setVerticalGroup(
            panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSET2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(txtRegC1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(txtRegB1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel15)
                    .addComponent(txtRegBC1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(txtRegE1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(txtRegD1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(txtRegDE1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelSET2Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(jLabel23)
                            .addComponent(txtRegF1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panelSET2Layout.createSequentialGroup()
                        .addGroup(panelSET2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(txtRegH1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20)
                            .addComponent(txtRegL1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel21)
                            .addComponent(txtRegHL1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegA1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel24)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tblFlags2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabbedGPR.addTab("Set 2", panelSET2);

        jLabel25.setFont(jLabel25.getFont().deriveFont(jLabel25.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel25.setText("PC");

        txtRegPC.setEditable(false);
        txtRegPC.setText("0");
        txtRegPC.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel26.setFont(jLabel26.getFont().deriveFont(jLabel26.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel26.setText("SP");

        txtRegSP.setEditable(false);
        txtRegSP.setText("0");
        txtRegSP.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel27.setFont(jLabel27.getFont().deriveFont(jLabel27.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel27.setText("IX");

        txtRegIX.setEditable(false);
        txtRegIX.setText("0");
        txtRegIX.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel28.setFont(jLabel28.getFont().deriveFont(jLabel28.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel28.setText("IY");

        jLabel29.setFont(jLabel29.getFont().deriveFont(jLabel29.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel29.setText("I");

        txtRegI.setEditable(false);
        txtRegI.setText("0");
        txtRegI.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        jLabel30.setFont(jLabel30.getFont().deriveFont(jLabel30.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel30.setText("R");

        txtRegIY.setEditable(false);
        txtRegIY.setText("0");
        txtRegIY.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        txtRegR.setEditable(false);
        txtRegR.setText("0");
        txtRegR.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 1, 1));

        javax.swing.GroupLayout paneRegistersLayout = new javax.swing.GroupLayout(paneRegisters);
        paneRegisters.setLayout(paneRegistersLayout);
        paneRegistersLayout.setHorizontalGroup(
            paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paneRegistersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel29)
                    .addGroup(paneRegistersLayout.createSequentialGroup()
                        .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(paneRegistersLayout.createSequentialGroup()
                                .addComponent(jLabel27)
                                .addGap(18, 18, 18)
                                .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(paneRegistersLayout.createSequentialGroup()
                                        .addComponent(txtRegIX, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel28))
                                    .addGroup(paneRegistersLayout.createSequentialGroup()
                                        .addComponent(txtRegI, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel30))))
                            .addGroup(paneRegistersLayout.createSequentialGroup()
                                .addComponent(jLabel25)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegPC, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel26)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtRegSP, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRegR, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtRegIY, javax.swing.GroupLayout.PREFERRED_SIZE, 55, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(tabbedGPR, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        paneRegistersLayout.setVerticalGroup(
            paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paneRegistersLayout.createSequentialGroup()
                .addComponent(tabbedGPR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel25)
                    .addComponent(txtRegPC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26)
                    .addComponent(txtRegSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(txtRegIX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28)
                    .addComponent(txtRegIY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(txtRegI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30)
                    .addComponent(txtRegR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(java.awt.Color.gray, 1, true), "Run control", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 14), java.awt.Color.gray)); // NOI18N

        lblRun.setFont(lblRun.getFont().deriveFont(lblRun.getFont().getStyle() | java.awt.Font.BOLD));
        lblRun.setForeground(new java.awt.Color(0, 102, 0));
        lblRun.setText("Stopped");

        jLabel31.setText("CPU frequency:");

        spnFrequency.setModel(new SpinnerNumberModel(20000, 1, 99999, 100));

        jLabel32.setFont(jLabel32.getFont().deriveFont(jLabel32.getFont().getStyle() | java.awt.Font.BOLD, jLabel32.getFont().getSize()-3));
        jLabel32.setText("kHz");

        jLabel33.setText("Test periode:");

        spnTestPeriode.setModel(new SpinnerNumberModel(50, 1, 10000, 10));

        jLabel34.setFont(jLabel34.getFont().deriveFont(jLabel34.getFont().getStyle() | java.awt.Font.BOLD, jLabel34.getFont().getSize()-3));
        jLabel34.setText("ms");

        jLabel35.setText("Runtime frequency:");

        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | java.awt.Font.BOLD));
        lblFrequency.setText("0,0 kHz");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblRun)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel35)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblFrequency))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel31)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel33)
                                .addGap(26, 26, 26)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(spnTestPeriode)
                            .addComponent(spnFrequency, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE))
                        .addGap(4, 4, 4)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel34)
                            .addComponent(jLabel32))))
                .addGap(140, 140, 140))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblRun)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(spnFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel32))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel33)
                    .addComponent(spnTestPeriode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel34))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel35)
                    .addComponent(lblFrequency))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneRegisters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel1, 0, 285, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(paneRegisters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JLabel lblFrequency;
    javax.swing.JLabel lblRun;
    javax.swing.JSpinner spnFrequency;
    javax.swing.JSpinner spnTestPeriode;
    javax.swing.JTable tblFlags;
    javax.swing.JTable tblFlags2;
    javax.swing.JTextField txtRegA;
    javax.swing.JTextField txtRegA1;
    javax.swing.JTextField txtRegB;
    javax.swing.JTextField txtRegB1;
    javax.swing.JTextField txtRegBC;
    javax.swing.JTextField txtRegBC1;
    javax.swing.JTextField txtRegC;
    javax.swing.JTextField txtRegC1;
    javax.swing.JTextField txtRegD;
    javax.swing.JTextField txtRegD1;
    javax.swing.JTextField txtRegDE;
    javax.swing.JTextField txtRegDE1;
    javax.swing.JTextField txtRegE;
    javax.swing.JTextField txtRegE1;
    javax.swing.JTextField txtRegF;
    javax.swing.JTextField txtRegF1;
    javax.swing.JTextField txtRegH;
    javax.swing.JTextField txtRegH1;
    javax.swing.JTextField txtRegHL;
    javax.swing.JTextField txtRegHL1;
    javax.swing.JTextField txtRegI;
    javax.swing.JTextField txtRegIX;
    javax.swing.JTextField txtRegIY;
    javax.swing.JTextField txtRegL;
    javax.swing.JTextField txtRegL1;
    javax.swing.JTextField txtRegPC;
    javax.swing.JTextField txtRegR;
    javax.swing.JTextField txtRegSP;
    // End of variables declaration//GEN-END:variables

}
