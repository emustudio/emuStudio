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
            return null;
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
        int tmp;
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
        mnemo = null;
        switch (val) {
            case 0x00: mnemo = "nop"; break;
            case 0x02: mnemo = "ld (bc),a"; break;
            case 0x07: mnemo = "rlca"; break;
            case 0x08: mnemo = "ex af,af'"; break;
            case 0x0A: mnemo = "ld a,(bc)"; break;
            case 0x0F: mnemo = "rrca"; break;
            case 0x10: val = ((Short)mem.read(actPos++)).shortValue();
                mnemo = "djnz " + String.format("%02X", val);
                oper += String.format(" %02X", val);break;
            case 0x12: mnemo = "ld (de),a"; break;
            case 0x17: mnemo = "rla"; break;
            case 0x1A: mnemo = "ld a,(de)"; break;
            case 0x1F: mnemo = "rra"; break;
            case 0x27: mnemo = "daa"; break;
            case 0x2F: mnemo = "cpl"; break;
            case 0x37: mnemo = "scf"; break;
            case 0x3F: mnemo = "ccf"; break;
            case 0x76: mnemo = "halt"; break;
            case 0xC0: mnemo = "ret nz"; break;
            case 0xC1: mnemo = "pop bc"; break;
            case 0xC5: mnemo = "push bc"; break;
            case 0xC7: mnemo = "rst 0"; break;
            case 0xC8: mnemo = "ret z"; break;
            case 0xC9: mnemo = "ret"; break;
            case 0xCF: mnemo = "rst 08"; break;
            case 0xD0: mnemo = "ret nc"; break;
            case 0xD1: mnemo = "pop de"; break;
            case 0xD5: mnemo = "push de"; break;
            case 0xD7: mnemo = "rst 10"; break;
            case 0xD8: mnemo = "ret c"; break;
            case 0xD9: mnemo = "exx"; break;
            case 0xDF: mnemo = "rst 18"; break;
            case 0xE0: mnemo = "ret po"; break;
            case 0xE1: mnemo = "pop hl"; break;
            case 0xE3: mnemo = "ex (sp),hl"; break;
            case 0xE5: mnemo = "push hl"; break;
            case 0xE7: mnemo = "rst 20"; break;
            case 0xE8: mnemo = "ret pe"; break;
            case 0xE9: mnemo = "jp (hl)"; break;
            case 0xEB: mnemo = "ex de,hl"; break;
            case 0xEF: mnemo = "rst 28"; break;
            case 0xF0: mnemo = "ret p"; break;
            case 0xF1: mnemo = "pop af"; break;
            case 0xF3: mnemo = "di"; break;
            case 0xF5: mnemo = "push af"; break;
            case 0xF7: mnemo = "rst 30"; break;
            case 0xF8: mnemo = "ret m"; break;
            case 0xF9: mnemo = "ld sp,hl"; break;
            case 0xFB: mnemo = "ei"; break;
            case 0xFF: mnemo = "rst 38"; break;
            case 0xED:
                val = ((Short)mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val);
                switch(val) {
                    case 0x40: mnemo = "in b,(c)"; break;
                    case 0x41: mnemo = "out (c),b"; break;
                    case 0x42: mnemo = "sbc hl,bc"; break;
                    case 0x44: mnemo = "neg"; break;
                    case 0x45: mnemo = "retn"; break;
                    case 0x46: mnemo = "im 0"; break;
                    case 0x47: mnemo = "ld i,a"; break;
                    case 0x48: mnemo = "in c,(c)"; break;
                    case 0x49: mnemo = "out (c),c"; break;
                    case 0x4A: mnemo = "add hl,bc"; break;
                    case 0x4D: mnemo = "reti"; break;
                    case 0x4F: mnemo = "ld r,a"; break;
                    case 0x50: mnemo = "in d,(c)"; break;
                    case 0x51: mnemo = "out (c),d"; break;
                    case 0x52: mnemo = "sbc hl,de"; break;
                    case 0x56: mnemo = "im 1"; break;
                    case 0x57: mnemo = "ld a,i"; break;
                    case 0x58: mnemo = "in e,(c)"; break;
                    case 0x59: mnemo = "out (c),e"; break;
                    case 0x5A: mnemo = "add hl,de"; break;
                    case 0x5E: mnemo = "im 2"; break;
                    case 0x5F: mnemo = "ld a,r"; break;
                    case 0x60: mnemo = "in h,(c)"; break;
                    case 0x61: mnemo = "out (c),h"; break;
                    case 0x62: mnemo = "sbc hl,hl"; break;
                    case 0x67: mnemo = "rrd"; break;
                    case 0x68: mnemo = "in l,(c)"; break;
                    case 0x69: mnemo = "out (c),l"; break;
                    case 0x6A: mnemo = "add hl,hl"; break;
                    case 0x6F: mnemo = "rld"; break;
                    case 0x70: mnemo = "in (c)"; break;
                    case 0x71: mnemo = "out (c),0"; break;
                    case 0x72: mnemo = "sbc hl,sp"; break;
                    case 0x78: mnemo = "in a,(c)"; break;
                    case 0x79: mnemo = "out (c),a"; break;
                    case 0x7A: mnemo = "add hl,sp"; break;
                    case 0xA0: mnemo = "ldi"; break;
                    case 0xA1: mnemo = "cpi"; break;
                    case 0xA2: mnemo = "ini"; break;
                    case 0xA3: mnemo = "outi"; break;
                    case 0xA8: mnemo = "ldd"; break;
                    case 0xA9: mnemo = "cpd"; break;
                    case 0xAA: mnemo = "ind"; break;
                    case 0xAB: mnemo = "outd"; break;
                    case 0xB0: mnemo = "ldir"; break;
                    case 0xB1: mnemo = "cpir"; break;
                    case 0xB2: mnemo = "inir"; break;
                    case 0xB3: mnemo = "otir"; break;
                    case 0xB8: mnemo = "lddr"; break;
                    case 0xB9: mnemo = "cpdr"; break;
                    case 0xBA: mnemo = "indr"; break;
                    case 0xBB: mnemo = "otdr"; break;
                }
                if (mnemo == null) {
                    tmp = (Integer)mem.readWord(actPos);
                    actPos += 2;
                    oper += String.format(" %02X %02X", tmp&0xFF, (tmp>>>8)&0xff);
                    switch (val) {
                        case 0x43: mnemo = "ld (" + String.format("%04X", tmp)+"),bc"; break;
                        case 0x4B: mnemo = "ld bc,(" + String.format("%04X", tmp)+")"; break;
                        case 0x53: mnemo = "ld (" + String.format("%04X", tmp)+"),de"; break;
                        case 0x5B: mnemo = "ld de,(" + String.format("%04X", tmp)+")"; break;
                        case 0x73: mnemo = "ld (" + String.format("%04X", tmp)+"),sp"; break;
                        case 0x7B: mnemo = "ld sp,(" + String.format("%04X", tmp)+")"; break;
                    }
                }break;
            case 0xDD:
                val = ((Short)mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val); tmp = 0;
                switch (val) {
                    case 0x09: mnemo = "add ix,bc"; break;
                    case 0x19: mnemo = "add ix,de"; break;
                    case 0x23: mnemo = "inc ix"; break;
                    case 0x29: mnemo = "add ix,ix"; break;
                    case 0x2B: mnemo = "dec ix"; break;
                    case 0x39: mnemo = "add ix,sp"; break;
                    case 0xE1: mnemo = "pop ix"; break;
                    case 0xE3: mnemo = "ex (sp),ix"; break;
                    case 0xE5: mnemo = "push ix"; break;
                    case 0xE9: mnemo = "jp (ix)"; break;
                    case 0xF9: mnemo = "ld sp,ix"; break;
                }
                if (mnemo == null) {
                    tmp = ((Short)mem.read(actPos++)).shortValue();
                    oper += String.format(" %02X", tmp);
                    switch(val) {
                        case 0x34: mnemo = "inc (ix+" + String.format("%02X",tmp)+")";break;
                        case 0x35: mnemo = "dec (ix+" + String.format("%02X",tmp)+")";break;
                        case 0x46: mnemo = "ld b,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x4E: mnemo = "ld c,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x56: mnemo = "ld d,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x5E: mnemo = "ld e,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x66: mnemo = "ld h,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x6E: mnemo = "ld l,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x7E: mnemo = "ld a,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x86: mnemo = "add a,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x8E: mnemo = "adc a,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0x96: mnemo = "sub (ix+" + String.format("%02X",tmp)+")";break;
                        case 0x9E: mnemo = "sbc a,(ix+" + String.format("%02X",tmp)+")";break;
                        case 0xA6: mnemo = "and (ix+" + String.format("%02X",tmp)+")";break;
                        case 0xAE: mnemo = "xor (ix+" + String.format("%02X",tmp)+")";break;
                        case 0xB6: mnemo = "or (ix+" + String.format("%02X",tmp)+")";break;
                        case 0xBE: mnemo = "cp (ix+" + String.format("%02X",tmp)+")";break;
                        case 0x70: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),b";break;
                        case 0x71: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),c";break;
                        case 0x72: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),d";break;
                        case 0x73: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),e";break;
                        case 0x74: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),h";break;
                        case 0x75: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),l";break;
                        case 0x77: mnemo = "ld (ix+" + String.format("%02X", tmp)+"),a";break;
                    }
                }
                if (mnemo == null) {
                    tmp += (((Short)mem.read(actPos++)).shortValue()<<8);
                    oper += String.format(" %02X", (tmp>>>8)&0xff);
                    switch (val) {
                        case 0x21: mnemo = "ld ix," + String.format("%04X", tmp);break;
                        case 0x22: mnemo = "ld (" + String.format("%04X", tmp)+"),ix";break;
                        case 0x2A: mnemo = "ld ix,(" + String.format("%04X", tmp)+")";break;
                        case 0x36:
                            mnemo = "ld (ix+" + String.format("%02X", tmp&0xff)
                                    +")," + String.format("%02X", (tmp>>>8)&0xff);break;
                    }
                }
                if ((mnemo == null) && (val == 0xCB)) {
                    val = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
                    switch (val) {
                        case 0x06: mnemo = "rlc (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x0E: mnemo = "rrc (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x16: mnemo = "rl (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x1E: mnemo = "rr (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x26: mnemo = "sla (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x2E: mnemo = "sra (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x36: mnemo = "sll (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x3E: mnemo = "srl (ix+"+String.format("%02X",tmp)+")";break;
                        case 0x46: mnemo = "bit 0,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x4E: mnemo = "bit 1,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x56: mnemo = "bit 2,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x5E: mnemo = "bit 3,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x66: mnemo = "bit 4,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x6E: mnemo = "bit 5,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x76: mnemo = "bit 6,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x7E: mnemo = "bit 7,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x86: mnemo = "res 0,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x8E: mnemo = "res 1,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x96: mnemo = "res 2,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0x9E: mnemo = "res 3,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xA6: mnemo = "res 4,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xAE: mnemo = "res 5,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xB6: mnemo = "res 6,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xBE: mnemo = "res 7,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xC6: mnemo = "set 0,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xCE: mnemo = "set 1,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xD6: mnemo = "set 2,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xDE: mnemo = "set 3,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xE6: mnemo = "set 4,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xEE: mnemo = "set 5,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xF6: mnemo = "set 6,(ix+"+String.format("%02X",tmp)+")";break;
                        case 0xFE: mnemo = "set 7,(ix+"+String.format("%02X",tmp)+")";break;
                    }
                }break;
            case 0xFD:
                val = ((Short)mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val); tmp = 0;
                switch (val) {
                    case 0x09: mnemo = "add iy,bc"; break;
                    case 0x19: mnemo = "add iy,de"; break;
                    case 0x23: mnemo = "inc iy"; break;
                    case 0x29: mnemo = "add iy,iy"; break;
                    case 0x2B: mnemo = "dec iy"; break;
                    case 0x39: mnemo = "add iy,sp"; break;
                    case 0xE1: mnemo = "pop iy"; break;
                    case 0xE3: mnemo = "ex (sp),iy"; break;
                    case 0xE5: mnemo = "push iy"; break;
                    case 0xE9: mnemo = "jp (iy)"; break;
                    case 0xF9: mnemo = "ld sp,iy"; break;
                }
                if (mnemo == null) {
                    tmp = ((Short)mem.read(actPos++)).shortValue();
                    oper += String.format(" %02X", tmp);
                    switch(val) {
                        case 0x34: mnemo = "inc (iy+" + String.format("%02X",tmp)+")";break;
                        case 0x35: mnemo = "dec (iy+" + String.format("%02X",tmp)+")";break;
                        case 0x46: mnemo = "ld b,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x4E: mnemo = "ld c,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x56: mnemo = "ld d,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x5E: mnemo = "ld e,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x66: mnemo = "ld h,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x6E: mnemo = "ld l,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x7E: mnemo = "ld a,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x86: mnemo = "add a,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x8E: mnemo = "adc a,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0x96: mnemo = "sub (iy+" + String.format("%02X",tmp)+")";break;
                        case 0x9E: mnemo = "sbc a,(iy+" + String.format("%02X",tmp)+")";break;
                        case 0xA6: mnemo = "and (iy+" + String.format("%02X",tmp)+")";break;
                        case 0xAE: mnemo = "xor (iy+" + String.format("%02X",tmp)+")";break;
                        case 0xB6: mnemo = "or (iy+" + String.format("%02X",tmp)+")";break;
                        case 0xBE: mnemo = "cp (iy+" + String.format("%02X",tmp)+")";break;
                        case 0x70: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),b";break;
                        case 0x71: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),c";break;
                        case 0x72: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),d";break;
                        case 0x73: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),e";break;
                        case 0x74: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),h";break;
                        case 0x75: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),l";break;
                        case 0x77: mnemo = "ld (iy+" + String.format("%02X", tmp)+"),a";break;
                    }
                }
                if (mnemo == null) {
                    tmp += (((Short)mem.read(actPos++)).shortValue()<<8);
                    oper += String.format(" %02X", (tmp>>>8)&0xff);
                    switch (val) {
                        case 0x21: mnemo = "ld iy," + String.format("%04X", tmp);break;
                        case 0x22: mnemo = "ld (" + String.format("%04X", tmp)+"),iy";break;
                        case 0x2A: mnemo = "ld iy,(" + String.format("%04X", tmp)+")";break;
                        case 0x36:
                            mnemo = "ld (iy+" + String.format("%02X", tmp&0xff)
                                    +")," + String.format("%02X", (tmp>>>8)&0xff);break;
                    }
                }
                if ((mnemo == null) && (val == 0xCB)) {
                    val = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
                    switch (val) {
                        case 0x06: mnemo = "rlc (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x0E: mnemo = "rrc (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x16: mnemo = "rl (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x1E: mnemo = "rr (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x26: mnemo = "sla (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x2E: mnemo = "sra (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x36: mnemo = "sll (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x3E: mnemo = "srl (iy+"+String.format("%02X",tmp)+")";break;
                        case 0x46: mnemo = "bit 0,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x4E: mnemo = "bit 1,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x56: mnemo = "bit 2,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x5E: mnemo = "bit 3,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x66: mnemo = "bit 4,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x6E: mnemo = "bit 5,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x76: mnemo = "bit 6,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x7E: mnemo = "bit 7,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x86: mnemo = "res 0,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x8E: mnemo = "res 1,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x96: mnemo = "res 2,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0x9E: mnemo = "res 3,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xA6: mnemo = "res 4,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xAE: mnemo = "res 5,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xB6: mnemo = "res 6,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xBE: mnemo = "res 7,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xC6: mnemo = "set 0,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xCE: mnemo = "set 1,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xD6: mnemo = "set 2,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xDE: mnemo = "set 3,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xE6: mnemo = "set 4,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xEE: mnemo = "set 5,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xF6: mnemo = "set 6,(iy+"+String.format("%02X",tmp)+")";break;
                        case 0xFE: mnemo = "set 7,(iy+"+String.format("%02X",tmp)+")";break;
                    }
                }break;
            case 0xCB:
                val = (Short)mem.read(memPos++);
                oper += String.format(" %02X", val);
                switch (val & 0xF8) {
                    case 0x00: mnemo = "rlc " + getRegMnemo(val & 7); break;
                    case 0x08: mnemo = "rrc " + getRegMnemo(val & 7); break;
                    case 0x10: mnemo = "rl " + getRegMnemo(val & 0x7); break;
                    case 0x18: mnemo = "rr " + getRegMnemo(val & 7); break;
                    case 0x20: mnemo = "sla " + getRegMnemo(val & 7); break;
                    case 0x28: mnemo = "sra " + getRegMnemo(val & 7); break;
                    case 0x30: mnemo = "sll " + getRegMnemo(val & 7); break;
                    case 0x38: mnemo = "srl " + getRegMnemo(val & 7); break;
                    case 0x40: mnemo = "bit 0," + getRegMnemo(val & 7); break;
                    case 0x48: mnemo = "bit 1," + getRegMnemo(val & 7); break;
                    case 0x50: mnemo = "bit 2," + getRegMnemo(val & 7); break;
                    case 0x58: mnemo = "bit 3," + getRegMnemo(val & 7); break;
                    case 0x60: mnemo = "bit 4," + getRegMnemo(val & 7); break;
                    case 0x68: mnemo = "bit 5," + getRegMnemo(val & 7); break;
                    case 0x70: mnemo = "bit 6," + getRegMnemo(val & 7); break;
                    case 0x78: mnemo = "bit 7," + getRegMnemo(val & 7); break;
                    case 0x80: mnemo = "res 0," + getRegMnemo(val & 7); break;
                    case 0x88: mnemo = "res 1," + getRegMnemo(val & 7); break;
                    case 0x90: mnemo = "res 2," + getRegMnemo(val & 7); break;
                    case 0x98: mnemo = "res 3," + getRegMnemo(val & 7); break;
                    case 0xA0: mnemo = "res 4," + getRegMnemo(val & 7); break;
                    case 0xA8: mnemo = "res 5," + getRegMnemo(val & 7); break;
                    case 0xB0: mnemo = "res 6," + getRegMnemo(val & 7); break;
                    case 0xB8: mnemo = "res 7," + getRegMnemo(val & 7); break;
                    case 0xC0: mnemo = "set 0," + getRegMnemo(val & 7); break;
                    case 0xC8: mnemo = "set 1," + getRegMnemo(val & 7); break;
                    case 0xD0: mnemo = "set 2," + getRegMnemo(val & 7); break;
                    case 0xD8: mnemo = "set 3," + getRegMnemo(val & 7); break;
                    case 0xE0: mnemo = "set 4," + getRegMnemo(val & 7); break;
                    case 0xE8: mnemo = "set 5," + getRegMnemo(val & 7); break;
                    case 0xF0: mnemo = "set 6," + getRegMnemo(val & 7); break;
                    case 0xF8: mnemo = "set 7," + getRegMnemo(val & 7); break;
                    
                }
                if (mnemo != null) break;
                switch (val) {
                    case 0x06: mnemo = "rlc (hl)"; break;
                    case 0x0E: mnemo = "rrc (hl)"; break;
                    case 0x16: mnemo = "rl (hl)"; break;
                    case 0x1E: mnemo = "rr (hl)"; break;
                    case 0x26: mnemo = "sla (hl)"; break;
                    case 0x2E: mnemo = "sra (hl)"; break;
                    case 0x36: mnemo = "sll (hl)"; break;
                    case 0x3E: mnemo = "srl (hl)"; break;
                    case 0x46: mnemo = "bit 0,(hl)"; break;
                    case 0x4E: mnemo = "bit 1,(hl)"; break;
                    case 0x56: mnemo = "bit 2,(hl)"; break;
                    case 0x5E: mnemo = "bit 3,(hl)"; break;
                    case 0x66: mnemo = "bit 4,(hl)"; break;
                    case 0x6E: mnemo = "bit 5,(hl)"; break;
                    case 0x76: mnemo = "bit 6,(hl)"; break;
                    case 0x7E: mnemo = "bit 7,(hl)"; break;
                    case 0x86: mnemo = "res 0,(hl)"; break;
                    case 0x8E: mnemo = "res 1,(hl)"; break;
                    case 0x96: mnemo = "res 2,(hl)"; break;
                    case 0x9E: mnemo = "res 3,(hl)"; break;
                    case 0xA6: mnemo = "res 4,(hl)"; break;
                    case 0xAE: mnemo = "res 5,(hl)"; break;
                    case 0xB6: mnemo = "res 6,(hl)"; break;
                    case 0xBE: mnemo = "res 7,(hl)"; break;
                    case 0xC6: mnemo = "set 0,(hl)"; break;
                    case 0xCE: mnemo = "set 1,(hl)"; break;
                    case 0xD6: mnemo = "set 2,(hl)"; break;
                    case 0xDE: mnemo = "set 3,(hl)"; break;
                    case 0xE6: mnemo = "set 4,(hl)"; break;
                    case 0xEE: mnemo = "set 5,(hl)"; break;
                    case 0xF6: mnemo = "set 6,(hl)"; break;
                    case 0xFE: mnemo = "set 7,(hl)"; break;
                }
        }
        tmp = 0;
        if (mnemo == null) {
            tmp = ((Short)mem.read(actPos++)).shortValue();
            oper += String.format(" %02X", tmp&0xff);
            switch (val) {
                case 0x06: mnemo = "ld b," + String.format("%02X", tmp); break;
                case 0x0E: mnemo = "ld c," + String.format("%02X", tmp); break;
                case 0x16: mnemo = "ld d," + String.format("%02X", tmp); break;
                case 0x18: mnemo = "jr " + String.format("%02X", tmp); break;
                case 0x1E: mnemo = "ld e," + String.format("%02X", tmp); break;
                case 0x20: mnemo = "jr nz," + String.format("%02X", tmp); break;
                case 0x26: mnemo = "ld h," + String.format("%02X", tmp); break;
                case 0x28: mnemo = "jr z," + String.format("%02X", tmp); break;
                case 0x2E: mnemo = "ld l," + String.format("%02X", tmp); break;
                case 0x30: mnemo = "jr nc," + String.format("%02X", tmp); break;
                case 0x36: mnemo = "ld (hl)," + String.format("%02X", tmp); break;
                case 0x38: mnemo = "jr c," + String.format("%02X", tmp); break;
                case 0x3E: mnemo = "ld a," + String.format("%02X", tmp); break;
                case 0xC6: mnemo = "add a," + String.format("%02X", tmp); break;
                case 0xCE: mnemo = "adc a," + String.format("%02X", tmp); break;
                case 0xD3: mnemo = "out (" + String.format("%02X", tmp)+"),a"; break;
                case 0xD6: mnemo = "sub " + String.format("%02X", tmp); break;
                case 0xDB: mnemo = "in a,(" + String.format("%02X", tmp)+")"; break;
                case 0xDE: mnemo = "sbc a," + String.format("%02X", tmp); break;
                case 0xE6: mnemo = "and " + String.format("%02X", tmp); break;
                case 0xEE: mnemo = "xor " + String.format("%02X", tmp); break;
                case 0xF6: mnemo = "or " + String.format("%02X", tmp); break;
                case 0xFE: mnemo = "cp " + String.format("%02X", tmp); break;
            }
        }
        if (mnemo == null) {
            tmp += (((Short)mem.read(actPos++)).shortValue() << 8);
            oper += String.format(" %02X", (tmp>>>8)&0xff);
            switch (val) {
                case 0x01: mnemo = "ld bc," + String.format("%04X", tmp); break;
                case 0x11: mnemo = "ld de," + String.format("%04X", tmp); break;
                case 0x21: mnemo = "ld hl," + String.format("%04X", tmp); break;
                case 0x22: mnemo = "ld (" + String.format("%04X", tmp) + "),hl"; break;
                case 0x2A: mnemo = "ld hl,(" + String.format("%04X", tmp)+")"; break;
                case 0x31: mnemo = "ld sp," + String.format("%04X", tmp); break;
                case 0x32: mnemo = "ld (" + String.format("%04X", tmp) + "),a"; break;
                case 0x3A: mnemo = "ld a,(" + String.format("%04X", tmp) + ")"; break;
                case 0xC2: mnemo = "jp nz," + String.format("%04X", tmp); break;
                case 0xC3: mnemo = "jp " + String.format("%04X", tmp); break;
                case 0xC4: mnemo = "call nz," + String.format("%04X", tmp); break;
                case 0xCA: mnemo = "jp z," + String.format("%04X", tmp); break;
                case 0xCC: mnemo = "call z," + String.format("%04X", tmp); break;
                case 0xCD: mnemo = "call " + String.format("%04X", tmp); break;
                case 0xD2: mnemo = "jp nc," + String.format("%04X", tmp); break;
                case 0xD4: mnemo = "call nc," + String.format("%04X", tmp); break;
                case 0xDC: mnemo = "call c," + String.format("%04X", tmp); break;
                case 0xDA: mnemo = "jp c," + String.format("%04X", tmp); break;
                case 0xE2: mnemo = "jp po," + String.format("%04X", tmp); break;
                case 0xE4: mnemo = "call po," + String.format("%04X", tmp); break;
                case 0xEA: mnemo = "jp pe," + String.format("%04X", tmp); break;
                case 0xEC: mnemo = "call pe," + String.format("%04X", tmp); break;
                case 0xF2: mnemo = "jp p," + String.format("%04X", tmp); break;
                case 0xF4: mnemo = "call p," + String.format("%04X", tmp); break;
                case 0xFA: mnemo = "jp m," + String.format("%04X", tmp); break;
                case 0xFC: mnemo = "call m," + String.format("%04X", tmp); break;
            }
        }
        if (mnemo == null) mnemo = "unknown";
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
                    case 0x67: case 0x68: case 0x69: case 0x6A: case 0x6F:
                    case 0x70: case 0x71: case 0x72: case 0x78: case 0x79:
                    case 0x7A: case 0xA0: case 0xA1: case 0xA2: case 0xA3:
                    case 0xA8: case 0xA9: case 0xAA: case 0xAB: case 0xB0:
                    case 0xB1: case 0xB2: case 0xB3: case 0xB8: case 0xB9:
                    case 0xBA: case 0xBB:
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
                    case 0x39: case 0xE1: case 0xE3: case 0xE5: case 0xE9:
                    case 0xF9:
                        return memPos;
                    case 0x34: case 0x35: case 0x46: case 0x4E: case 0x56:
                    case 0x5E: case 0x66: case 0x6E: case 0x7E: case 0x86:
                    case 0x8E: case 0x96: case 0x9E: case 0xA6: case 0xAE:
                    case 0xB6: case 0xBE: case 0x70: case 0x71: case 0x72:
                    case 0x73: case 0x74: case 0x75: case 0x77:
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
                    case 0x39: case 0xE1: case 0xE3: case 0xE5: case 0xE9:
                    case 0xF9:
                        return memPos;
                    case 0x34: case 0x35: case 0x46: case 0x4E: case 0x56:
                    case 0x5E: case 0x66: case 0x6E: case 0x7E: case 0x86:
                    case 0x8E: case 0x96: case 0x9E: case 0xA6: case 0xAE:
                    case 0xB6: case 0xBE: case 0x70: case 0x71: case 0x72:
                    case 0x73: case 0x74: case 0x75: case 0x77:
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
