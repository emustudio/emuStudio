/*
 * statusGUI.java
 *
 * Created on Pondelok, 2007, december 31, 10:59
 * 
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package gui;

import impl.ColumnInfo;
import impl.Cpu8080;
import impl.CpuContext;
import interfaces.IICpuListener;
import interfaces.ICPUInstruction;

import java.awt.Color;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import plugins.cpu.ICPU;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class statusGUI extends JPanel {
    private Cpu8080 cpu;
    private CpuContext cpuC;
    private IMemoryContext mem = null;
    private IDebugColumn[] columns;
    private int run_state;
    private AbstractTableModel flagModel;
    
    private class FlagsModel extends AbstractTableModel {
        private String[] flags = {"S","Z","A","P","C"};
        private int[] flagsI = {0,0,0,0,0};
        
        public int getRowCount() { return 2; }
        public int getColumnCount() { return 5; }
        @Override
        public String getColumnName(int columnIndex) { return flags[columnIndex]; }
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (rowIndex) {
                case 0: return flags[columnIndex];
                case 1: return flagsI[columnIndex];
            }
            return null;
        }
        @Override
        public void fireTableDataChanged() {
            short F = cpu.Flags;
            flagsI[0] = ((F&Cpu8080.flagS)!=0) ? 1 : 0;
            flagsI[1] = ((F&Cpu8080.flagZ)!=0) ? 1 : 0;
            flagsI[2] = ((F&Cpu8080.flagAC)!=0) ? 1 : 0;
            flagsI[3] = ((F&Cpu8080.flagP)!=0) ? 1 : 0;
            flagsI[4] = ((F&Cpu8080.flagC)!=0) ? 1 : 0;
            super.fireTableDataChanged();
        }
    }
    
    
    public statusGUI(final Cpu8080 cpu) {
        this.cpu = cpu;
        this.cpuC = (CpuContext)cpu.getContext();
        columns = new IDebugColumn[4];
        IDebugColumn c1 = new ColumnInfo("breakpoint", java.lang.Boolean.class,true);
        IDebugColumn c2 = new ColumnInfo("address", java.lang.String.class,false);
        IDebugColumn c3 = new ColumnInfo("mnemonics", java.lang.String.class,false);
        IDebugColumn c4 = new ColumnInfo("opcode", java.lang.String.class,false);
        
        columns[0] = c1;columns[1] = c2;columns[2] = c3;columns[3] = c4;
        run_state = ICPU.STATE_STOPPED_NORMAL;

        initComponents();
        cpuC.addCPUListener(new IICpuListener() {
            public void runChanged(EventObject evt, int state) {
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
        flagModel = new FlagsModel();
        tblFlags.setModel(flagModel);
    }
    
    private void setCPUFreq(int f) { cpuC.setFrequency(f); }
    
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
                case 2: return "incomplete instruction";
                case 3: return String.format("%X", (Short)mem.read(index));
                default: return "";
            }
        }
    }

    public IDebugColumn[] getDebugColumns() { return columns; }

    
    public void setDebugColVal(int index, int col, Object value) {
        if (col != 0) return;
        if (value.getClass() != Boolean.class) return;
        
        boolean v = Boolean.valueOf(value.toString());
        cpu.setBreakpoint(index,v);
    }
    
    public void setMem(IMemoryContext mem) {
        this.mem = mem;
    }
    
    private String getRegMnemo(int reg) {
        switch (reg) {
            case 0: return "B";
            case 1: return "C";
            case 2: return "D";
            case 3: return "E";
            case 4: return "H";
            case 5: return "L";
            case 6: return "M";
            case 7: return "A";
        }
        return "";
    }

    
    public ICPUInstruction cpuDecode(int memPos) {
        short val;
        int addr;
        int actPos = memPos;
        ICPUInstruction instr;
        String mnemo, oper;
        
        if (this.mem == null) return null;
        val = ((Short)mem.read(actPos++)).shortValue();
        oper = String.format("%02X",val);
        if ((val >= 64) && (val <= 127) && (val != 118))
            mnemo = "mov " + getRegMnemo((val&56) >> 3) + ","
                + getRegMnemo((byte)(val&7));
        else if ((val >= 128) && (val <= 135)) mnemo = "add " + getRegMnemo(val&7);
        else if ((val >= 136) && (val <= 143)) mnemo = "adc " + getRegMnemo(val&7);
        else if ((val >= 144) && (val <= 151)) mnemo = "sub " + getRegMnemo(val&7);
        else if ((val >= 152) && (val <= 159)) mnemo = "sbb " + getRegMnemo(val&7);
        else if ((val >= 160) && (val <= 167)) mnemo = "ana " + getRegMnemo(val&7);
        else if ((val >= 168) && (val <= 175)) mnemo = "xra " + getRegMnemo(val&7);
        else if ((val >= 176) && (val <= 183)) mnemo = "ora " + getRegMnemo(val&7);
        else if ((val >= 184) && (val <= 191)) mnemo = "cmp " + getRegMnemo(val&7);
        else {
            switch (val) {
                case 58: // lda addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("lda %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 50: // sta addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("sta %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 10: mnemo = "ldax BC"; break;
                case 26: mnemo = "ldax DE"; break;
                case 2:  mnemo = "stax BC"; break;
                case 18: mnemo = "stax DE"; break;
                case 42: // lhld addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("lhld %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 34: // shld addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("shld %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 235: mnemo = "xchg"; break;
                case 6: // mvi b, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi B,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 14: // mvi c, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi C,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 22: // mvi d, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi D,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 30: // mvi e, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi E,%02Xh", val);
                     oper = String.format(" %02X", val);
                     break;
                case 38: // mvi h, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi H,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 46: // mvi l, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi L,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 54: // mvi m, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi M,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 62: // mvi a, byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("mvi A,%02Xh", val);
                     oper += String.format(" %02X", val);
                     break;
                case 1: // lxi bc, dble
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("lxi BC,%04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 17: // lxi de, dble
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("lxi DE,%04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 33: // lxi hl, dble
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("lxi HL,%04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 49: // lxi sp, dble
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("lxi SP,%04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 249: mnemo = "sphl"; break;
                case 227: mnemo = "xthl"; break;
                case 193: mnemo = "pop BC"; break;
                case 209: mnemo = "pop DE"; break;
                case 225: mnemo = "pop HL"; break;
                case 241: mnemo = "pop PSW"; break;
                case 197: mnemo = "push BC"; break;
                case 213: mnemo = "push DE"; break;
                case 229: mnemo = "push HL"; break;
                case 245: mnemo = "push PSW"; break;
                case 219: // in port
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("in %Xh", val);
                     oper += String.format(" %02X", val); break;
                case 211: // out port
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("out %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 198: // adi byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("adi %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 206: // aci byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("aci %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 9: mnemo = "dad BC"; break;
                case 25: mnemo = "dad DE"; break;
                case 41: mnemo = "dad HL"; break;
                case 57: mnemo = "dad SP"; break;
                case 214: // sui byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("sui %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 222: // sbi byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("sbi %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 4: mnemo = "inr B"; break;
                case 12: mnemo = "inr C"; break;
                case 20: mnemo = "inr D"; break;
                case 28: mnemo = "inr E"; break;
                case 36: mnemo = "inr H"; break;
                case 44: mnemo = "inr L"; break;
                case 52: mnemo = "inr M"; break;
                case 60: mnemo = "inr A"; break;
                case 3: mnemo = "inx BC"; break;
                case 19: mnemo = "inx DE"; break;
                case 35: mnemo = "inx HL"; break;
                case 51: mnemo = "inx SP"; break;
                case 5: mnemo = "dcr B"; break;
                case 13: mnemo = "dcr C"; break;
                case 21: mnemo = "dcr D"; break;
                case 29: mnemo = "dcr E"; break;
                case 37: mnemo = "dcr H"; break;
                case 45: mnemo = "dcr L"; break;
                case 53: mnemo = "dcr M"; break;
                case 61: mnemo = "dcr A"; break;
                case 11: mnemo = "dcx BC"; break;
                case 27: mnemo = "dcx DE"; break;
                case 43: mnemo = "dcx HL"; break;
                case 59: mnemo = "dcx SP"; break;
                case 254: // cpi byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("cpi %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 39: mnemo = "daa"; oper = "27"; break;
                case 230: // ani byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("ani %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 246: // ori byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("ori %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 238: // xri byte
                     val = ((Short)mem.read(actPos++)).shortValue();
                     mnemo = String.format("xri %02Xh", val);
                     oper += String.format(" %02X", val); break;
                case 47: mnemo = "cma"; break;
                case 7: mnemo = "rlc"; break;
                case 15: mnemo = "rrc"; break;
                case 23: mnemo = "ral"; break;
                case 31: mnemo = "rar"; break;
                case 55: mnemo = "stc"; break;
                case 63: mnemo = "cmc"; break;
                case 233: mnemo = "pchl"; break;
                case 195: // jmp addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jmp %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 194: // jnz addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jnz %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 202: // jz addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jz %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 210: // jnc addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jnc %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 218: // jc addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jc %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 226: // jpo addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jpo %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 234: // jpe addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jpe %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 242: // jp addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jp %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 250: // jm addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("jm %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 205: // call addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("call %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 196: // cnz addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cnz %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 204: // cz addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cz %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 212: // cnc addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cnc %04Xh", addr);
                     oper += String.format("%02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 220: // cc addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cc %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 228: // cpo addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cpo %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 236: // cpe addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cpe %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 244: // cp addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cp %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 252: // cm addr
                     addr = (Integer)mem.readWord(actPos);
                     mnemo = String.format("cm %04Xh", addr);
                     oper += String.format(" %02X %02X", addr&0xFF,(addr>>8)&0xFF);
                     actPos += 2; break;
                case 201: mnemo = "ret"; break;
                case 192: mnemo = "rnz"; break;
                case 200: mnemo = "rz"; break;
                case 208: mnemo = "rnc"; break;
                case 216: mnemo = "rc"; break;
                case 224: mnemo = "rpo"; break;
                case 232: mnemo = "rpe"; break;
                case 240: mnemo = "rp"; break;
                case 248: mnemo = "rm"; break;
                case 199: mnemo = "rst 0"; break;
                case 207: mnemo = "rst 1"; break;
                case 215: mnemo = "rst 2"; break;
                case 223: mnemo = "rst 3"; break;
                case 231: mnemo = "rst 4"; break;
                case 239: mnemo = "rst 5"; break;
                case 247: mnemo = "rst 6"; break;
                case 255: mnemo = "rst 7"; break;
                case 251: mnemo = "ei"; break;
                case 243: mnemo = "di"; break;
                case 118: mnemo = "hlt"; break;
                case 0: mnemo = "nop"; break;
                default: mnemo = "unknown instruction";
            }
        }
        instr = new ICPUInstruction(mnemo,oper);
        return instr;
    }
    
    public int getNextPosition(int memPos) throws ArrayIndexOutOfBoundsException {
        short val;
        if (mem == null) return 0;
        val = (Short)mem.read(memPos++);
        switch (val) {
            case 64: case 65: case 66: case 67: case 68: case 69: case 70: case 71: case 72: case 73: case 74: case 75: case 76:
            case 77: case 78: case 79: case 80: case 81: case 82: case 83: case 84: case 85: case 86: case 87: case 88: case 89:
            case 90: case 91: case 92: case 93: case 94: case 95: case 96: case 97: case 98: case 99: case 100: case 101: case 102:
            case 103: case 104: case 105: case 106: case 107: case 108: case 109: case 110: case 111: case 112: case 113: case 114:
            case 115: case 116: case 117: case 119: case 120: case 121: case 122: case 123: case 124: case 125: case 126: case 127:
            case 10: case 26: case 2: case 18: case 235: case 249: case 227: case 193: case 209: case 225: case 241: case 197: case 213:
            case 229: case 245: case 128: case 129: case 130: case 131: case 132: case 133: case 134: case 135: case 136: case 137:
            case 138: case 139: case 140: case 141: case 142: case 143: case 9: case 25: case 41: case 57: case 144: case 145: case 146:
            case 147: case 148: case 149: case 150: case 151: case 152: case 153: case 154: case 155: case 156: case 157: case 158:
            case 159: case 4: case 12: case 20: case 28: case 36: case 44: case 52: case 60: case 3: case 19: case 35: case 51: case 5:
            case 13: case 21: case 29: case 37: case 45: case 53: case 61: case 11: case 27: case 43: case 59: case 184: case 185:
            case 186: case 187: case 188: case 189: case 190: case 191: case 39: case 160: case 161: case 162: case 163: case 164:
            case 165: case 166: case 167: case 176: case 177: case 178: case 179: case 180: case 181: case 182: case 183: case 168:
            case 169: case 170: case 171: case 172: case 173: case 174: case 175: case 47: case 7: case 15: case 23: case 31: case 55:
            case 63: case 233: case 201: case 192: case 200: case 208: case 216: case 224: case 232: case 240: case 248: case 199: case 207:
            case 215: case 223: case 231: case 239: case 247: case 255: case 251: case 243: case 118: case 0: break;
            case 58: case 50: case 42: case 34: case 1: case 17: case 33: case 49: case 195: case 194: case 202: case 210: case 218:
            case 226: case 234: case 242: case 250: case 205: case 196: case 204: case 212: case 220: case 228: case 236: case 244: case 252:
                 return (memPos + 2);
            case 6: case 14: case 22: case 30: case 38: case 46: case 54: case 62: case 219: case 211: case 198: case 206: case 214:
            case 222: case 254: case 230: case 246: case 238: return (memPos+1);
        }
        return memPos;
    }

    public void updateGUI() {
        txtRegA.setText(String.format("%02X", cpu.A));
        txtRegB.setText(String.format("%02X", cpu.B));
        txtRegC.setText(String.format("%02X", cpu.C));
        txtRegBC.setText(String.format("%04X",((cpu.B << 8) | cpu.C)&0xFFFF));
        txtRegD.setText(String.format("%02X", cpu.D));
        txtRegE.setText(String.format("%02X", cpu.E));
        txtRegDE.setText(String.format("%04X", ((cpu.D << 8) | cpu.E)&0xFFFF));
        txtRegH.setText(String.format("%02X", cpu.H));
        txtRegL.setText(String.format("%02X", cpu.L));
        txtRegHL.setText(String.format("%04X", ((cpu.H << 8) | cpu.L)&0xFFFF));
        txtRegSP.setText(String.format("%04X", cpu.SP));
        txtRegPC.setText(String.format("%04X", cpu.getPC()));
        
        txtFlags.setText(String.format("%02X", cpu.Flags));
        flagModel.fireTableDataChanged();
        
        if (run_state == ICPU.STATE_RUNNING) {
            lblRun.setText("running");
            spnFrequency.setEnabled(false);
            spnTestPeriode.setEnabled(false);
        }
        else {
            spnFrequency.setEnabled(true);
            spnTestPeriode.setEnabled(true);
            switch (run_state) {
                case ICPU.STATE_STOPPED_NORMAL:
                	lblRun.setText("stopped (normal)"); break;
                case ICPU.STATE_STOPPED_BREAK:
                	lblRun.setText("breakpoint"); break;
                case ICPU.STATE_STOPPED_ADDR_FALLOUT:
                	lblRun.setText("stopped (address fallout)"); break;
                case ICPU.STATE_STOPPED_BAD_INSTR:
                	lblRun.setText("stopped (instruction fallout)"); break;
            }
        }
    }

    
    private void initComponents() {
        JPanel paneRegisters = new JPanel();
        JLabel lblRegB = new JLabel("B");
        txtRegB = new JTextField("00");
        JLabel lblRegC = new JLabel("C");
        txtRegC = new JTextField("00");
        JLabel lblRegBC = new JLabel("BC");
        txtRegBC = new JTextField("0000");
        JLabel lblRegD = new JLabel("D");
        txtRegD = new JTextField("00");
        JLabel lblRegE = new JLabel("E");
        txtRegE = new JTextField("00");
        JLabel lblRegDE = new JLabel("DE");
        txtRegDE = new JTextField("0000");
        JLabel lblRegH = new JLabel("H");
        txtRegH = new JTextField("00");
        JLabel lblRegL = new JLabel("L");
        txtRegL = new JTextField("00");
        JLabel lblRegHL = new JLabel("HL");
        txtRegHL = new JTextField("0000");
        JLabel lblRegA = new JLabel("A");
        txtRegA = new JTextField("00");
        JLabel lblRegF = new JLabel("F");
        txtFlags = new JTextField("00");
        JLabel lblRegPC = new JLabel("PC");
        txtRegPC = new JTextField("0000");
        JLabel lblRegSP = new JLabel("SP");
        txtRegSP = new JTextField("0000");
        JPanel panelRun = new JPanel();
        lblRun = new JLabel("Stopped");
        JLabel lblCPUFreq = new JLabel("CPU frequency:");
        spnFrequency = new JSpinner();
        JLabel lblKHZ = new JLabel("kHz");
        JLabel lblRuntimeFreq = new JLabel("Runtime frequency:");
        lblFrequency = new JLabel("0,0 kHz");
        JLabel lblTestPeriode = new JLabel("Test periode:");
        spnTestPeriode = new JSpinner();
        JLabel lblMS = new JLabel("ms");
        JLabel lblFlags = new JLabel("Flags (F): ");
        tblFlags = new JTable();

        setBorder(null);
        paneRegisters.setBorder(null); // NOI18N


        lblRegB.setFont(lblRegB.getFont().deriveFont(lblRegB.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegB.setEditable(false);
        txtRegB.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegC.setFont(lblRegC.getFont().deriveFont(lblRegC.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegC.setEditable(false);
        txtRegC.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegBC.setFont(lblRegBC.getFont().deriveFont(lblRegBC.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegBC.setEditable(false);
        txtRegBC.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegD.setFont(lblRegD.getFont().deriveFont(lblRegD.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegD.setEditable(false);
        txtRegD.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegE.setFont(lblRegE.getFont().deriveFont(lblRegE.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegE.setEditable(false);
        txtRegE.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegDE.setFont(lblRegDE.getFont().deriveFont(lblRegDE.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegDE.setEditable(false);
        txtRegDE.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegH.setFont(lblRegH.getFont().deriveFont(lblRegH.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegH.setEditable(false);
        txtRegH.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegL.setFont(lblRegL.getFont().deriveFont(lblRegL.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegL.setEditable(false);
        txtRegL.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegHL.setFont(lblRegHL.getFont().deriveFont(lblRegHL.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegHL.setEditable(false);
        txtRegHL.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegA.setFont(lblRegA.getFont().deriveFont(lblRegA.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegA.setEditable(false);
        txtRegA.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegF.setFont(lblRegF.getFont().deriveFont(lblRegF.getFont().getStyle() | java.awt.Font.BOLD));
        txtFlags.setEditable(false);
        txtFlags.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegPC.setFont(lblRegPC.getFont().deriveFont(lblRegPC.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegPC.setEditable(false);
        txtRegPC.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        lblRegSP.setFont(lblRegSP.getFont().deriveFont(lblRegSP.getFont().getStyle() | java.awt.Font.BOLD));
        txtRegSP.setEditable(false);
        txtRegSP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        tblFlags.setAutoCreateRowSorter(true);
        tblFlags.setBackground(Color.white);
        tblFlags.setBorder(null);
        tblFlags.setRowSelectionAllowed(false);

        GroupLayout paneRegistersLayout = new GroupLayout(paneRegisters);
        paneRegisters.setLayout(paneRegistersLayout);
        
        paneRegistersLayout.setHorizontalGroup(
        		paneRegistersLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addGroup(paneRegistersLayout.createSequentialGroup()
        						.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        								.addComponent(lblRegB)
        								.addComponent(lblRegD)
        								.addComponent(lblRegH)
        								.addComponent(lblRegA))
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        								.addComponent(txtRegB)
        								.addComponent(txtRegD)
        								.addComponent(txtRegH)
        								.addComponent(txtRegA))
        						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        						.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        								.addComponent(lblRegC)
        								.addComponent(lblRegE)
        								.addComponent(lblRegL)
        								.addComponent(lblRegF))
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        								.addComponent(txtRegC)
        								.addComponent(txtRegE)
        								.addComponent(txtRegL)
        								.addComponent(txtFlags))
        						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        						.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        								.addComponent(lblRegBC)
        								.addComponent(lblRegDE)
        								.addComponent(lblRegHL)
        								.addComponent(lblRegPC)
        								.addComponent(lblRegSP))
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        								.addComponent(txtRegBC)
        								.addComponent(txtRegDE)
        								.addComponent(txtRegHL)
        								.addComponent(txtRegPC)
        								.addComponent(txtRegSP)))
        				.addComponent(lblFlags)
        				.addComponent(tblFlags))
        		.addContainerGap());
        paneRegistersLayout.setVerticalGroup(
        		paneRegistersLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblRegB)
        				.addComponent(txtRegB)
        				.addComponent(lblRegC)
        				.addComponent(txtRegC)
        				.addComponent(lblRegBC)
        				.addComponent(txtRegBC))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblRegD)
        				.addComponent(txtRegD)
        				.addComponent(lblRegE)
        				.addComponent(txtRegE)
        				.addComponent(lblRegDE)
        				.addComponent(txtRegDE))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblRegH)
        				.addComponent(txtRegH)
        				.addComponent(lblRegL)
        				.addComponent(txtRegL)
        				.addComponent(lblRegHL)
        				.addComponent(txtRegHL))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblRegA)
        				.addComponent(txtRegA)
        				.addComponent(lblRegF)
        				.addComponent(txtFlags)
        				.addComponent(lblRegPC)
        				.addComponent(txtRegPC))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblRegSP)
        				.addComponent(txtRegSP))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addComponent(lblFlags)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(tblFlags));

        panelRun.setBorder(BorderFactory.createTitledBorder(
        		new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, true),
        		"Run control", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
        		javax.swing.border.TitledBorder.DEFAULT_POSITION,
        		new java.awt.Font("DejaVu Sans", 1, 14),
        		new java.awt.Color(102, 102, 102))); // NOI18N

        lblRun.setFont(lblRun.getFont().deriveFont(lblRun.getFont().getStyle() | java.awt.Font.BOLD));
        lblRun.setForeground(new java.awt.Color(0, 102, 0));

        spnFrequency.setModel(new SpinnerNumberModel(2000, 1, 99999, 100));
        lblKHZ.setFont(lblKHZ.getFont().deriveFont(lblKHZ.getFont().getStyle() | java.awt.Font.BOLD));
        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | java.awt.Font.BOLD));

        spnTestPeriode.setModel(new SpinnerNumberModel(50, 1, 10000, 50));
        lblMS.setFont(lblMS.getFont().deriveFont(lblMS.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout panelRunLayout = new GroupLayout(panelRun);
        panelRun.setLayout(panelRunLayout);
        panelRunLayout.setHorizontalGroup(
        		panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		.addGroup(panelRunLayout.createSequentialGroup()
        				.addContainerGap()
        				.addComponent(lblRun)
        				.addContainerGap())
        		.addGroup(panelRunLayout.createSequentialGroup()
        				.addContainerGap()
        				.addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        						.addComponent(lblCPUFreq)
        						.addComponent(lblTestPeriode))
        				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        				.addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        						.addComponent(spnFrequency)
        						.addComponent(spnTestPeriode))
        				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        				.addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        						.addComponent(lblKHZ)
        						.addComponent(lblMS))
        				.addContainerGap())
        		.addGroup(panelRunLayout.createSequentialGroup()
        				.addContainerGap()
        				.addComponent(lblRuntimeFreq)
        				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        				.addComponent(lblFrequency)
        				.addContainerGap()));
        panelRunLayout.setVerticalGroup(
        		panelRunLayout.createSequentialGroup()
        		.addContainerGap()
        		.addComponent(lblRun)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.BASELINE,false)
        				.addComponent(lblCPUFreq)
        				.addComponent(spnFrequency,GroupLayout.DEFAULT_SIZE,25,Short.MAX_VALUE)
        				.addComponent(lblKHZ))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.BASELINE,false)
        				.addComponent(lblTestPeriode)
        				.addComponent(spnTestPeriode,GroupLayout.DEFAULT_SIZE,25,Short.MAX_VALUE)
        				.addComponent(lblMS))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblRuntimeFreq)
        				.addComponent(lblFrequency))
        		.addContainerGap());
        
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(paneRegisters, 10, 290, Short.MAX_VALUE)
            .addComponent(panelRun, 10, 290, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
            .addComponent(paneRegisters, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(panelRun, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addContainerGap()
        );
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JLabel lblFrequency;
    JLabel lblRun;
    JSpinner spnFrequency;
    JSpinner spnTestPeriode;
    JTable tblFlags;
    JTextField txtFlags;
    JTextField txtRegA;
    JTextField txtRegB;
    JTextField txtRegBC;
    JTextField txtRegC;
    JTextField txtRegD;
    JTextField txtRegDE;
    JTextField txtRegE;
    JTextField txtRegH;
    JTextField txtRegHL;
    JTextField txtRegL;
    JTextField txtRegPC;
    JTextField txtRegSP;
    // End of variables declaration//GEN-END:variables
    
}
