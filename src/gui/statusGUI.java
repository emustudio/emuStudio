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
import java.util.EventObject;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import plugins.cpu.ICPUContext.stateEnum;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author  vbmacher
 */
public class statusGUI extends javax.swing.JPanel {
    private Cpu8080 cpu;
    private CpuContext cpuC;
    private IMemoryContext mem = null;
    private IDebugColumn[] columns;
    private stateEnum run_state;
    
    public statusGUI(final Cpu8080 cpu) {
        this.cpu = cpu;
        this.cpuC = (CpuContext)cpu.getContext();
        columns = new IDebugColumn[4];
        IDebugColumn c1 = new ColumnInfo("breakpoint", java.lang.Boolean.class,true);
        IDebugColumn c2 = new ColumnInfo("address", java.lang.String.class,false);
        IDebugColumn c3 = new ColumnInfo("mnemonics", java.lang.String.class,false);
        IDebugColumn c4 = new ColumnInfo("opcode", java.lang.String.class,false);
        
        columns[0] = c1;columns[1] = c2;columns[2] = c3;columns[3] = c4;
        run_state = stateEnum.stoppedNormal;

        initComponents();
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
        instr = new ICPUInstruction(mnemo,oper,actPos);
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
        if ((cpu.Flags & cpu.flagS) != 0) txtFlagS.setText("1");
        else txtFlagS.setText("0");
        if ((cpu.Flags & cpu.flagZ) != 0) txtFlagZ.setText("1");
        else txtFlagZ.setText("0");
        if ((cpu.Flags & cpu.flagAC) != 0) txtFlagAC.setText("1");
        else txtFlagAC.setText("0");
        if ((cpu.Flags & cpu.flagP) != 0) txtFlagP.setText("1");
        else txtFlagP.setText("0");
        if ((cpu.Flags & cpu.flagC) != 0) txtFlagC.setText("1");
        else txtFlagC.setText("0");
        
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

    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel paneInfoRegisters = new javax.swing.JPanel();
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
        javax.swing.JLabel jLabel16 = new javax.swing.JLabel();
        txtFlags = new javax.swing.JTextField();
        javax.swing.JLabel jLabel17 = new javax.swing.JLabel();
        txtRegPC = new javax.swing.JTextField();
        javax.swing.JLabel jLabel18 = new javax.swing.JLabel();
        txtRegSP = new javax.swing.JTextField();
        javax.swing.JLabel jLabel19 = new javax.swing.JLabel();
        txtFlagS = new javax.swing.JTextField();
        javax.swing.JLabel jLabel20 = new javax.swing.JLabel();
        txtFlagZ = new javax.swing.JTextField();
        javax.swing.JLabel jLabel21 = new javax.swing.JLabel();
        txtFlagAC = new javax.swing.JTextField();
        javax.swing.JLabel jLabel22 = new javax.swing.JLabel();
        txtFlagP = new javax.swing.JTextField();
        javax.swing.JLabel jLabel23 = new javax.swing.JLabel();
        txtFlagC = new javax.swing.JTextField();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        lblRun = new javax.swing.JLabel();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        spnFrequency = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        lblFrequency = new javax.swing.JLabel();
        javax.swing.JLabel jLabel14 = new javax.swing.JLabel();
        spnTestPeriode = new javax.swing.JSpinner();
        javax.swing.JLabel jLabel15 = new javax.swing.JLabel();

        paneInfoRegisters.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setText("B");

        txtRegB.setBackground(java.awt.Color.white);
        txtRegB.setEditable(false);
        txtRegB.setFont(txtRegB.getFont());
        txtRegB.setText("00");
        txtRegB.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegB.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel2.setText("C");

        txtRegC.setBackground(java.awt.Color.white);
        txtRegC.setEditable(false);
        txtRegC.setText("00");
        txtRegC.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegC.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel3.setText("BC");

        txtRegBC.setBackground(java.awt.Color.white);
        txtRegBC.setEditable(false);
        txtRegBC.setText("0000");
        txtRegBC.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegBC.setPreferredSize(new java.awt.Dimension(50, 21));

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel4.setText("D");

        txtRegD.setBackground(java.awt.Color.white);
        txtRegD.setEditable(false);
        txtRegD.setFont(txtRegD.getFont());
        txtRegD.setText("00");
        txtRegD.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegD.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel5.setText("E");

        txtRegE.setBackground(java.awt.Color.white);
        txtRegE.setEditable(false);
        txtRegE.setText("00");
        txtRegE.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegE.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel6.setText("DE");

        txtRegDE.setBackground(java.awt.Color.white);
        txtRegDE.setEditable(false);
        txtRegDE.setText("0000");
        txtRegDE.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegDE.setPreferredSize(new java.awt.Dimension(50, 21));

        jLabel7.setFont(jLabel7.getFont().deriveFont(jLabel7.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel7.setText("H");

        txtRegH.setBackground(java.awt.Color.white);
        txtRegH.setEditable(false);
        txtRegH.setFont(txtRegH.getFont());
        txtRegH.setText("00");
        txtRegH.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegH.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel8.setFont(jLabel8.getFont().deriveFont(jLabel8.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel8.setText("L");

        txtRegL.setBackground(java.awt.Color.white);
        txtRegL.setEditable(false);
        txtRegL.setText("00");
        txtRegL.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegL.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel9.setFont(jLabel9.getFont().deriveFont(jLabel9.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel9.setText("HL");

        txtRegHL.setBackground(java.awt.Color.white);
        txtRegHL.setEditable(false);
        txtRegHL.setText("0000");
        txtRegHL.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegHL.setPreferredSize(new java.awt.Dimension(50, 21));

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel10.setText("A");

        txtRegA.setBackground(java.awt.Color.white);
        txtRegA.setEditable(false);
        txtRegA.setFont(txtRegA.getFont());
        txtRegA.setText("00");
        txtRegA.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegA.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel16.setFont(jLabel16.getFont().deriveFont(jLabel16.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel16.setText("F");

        txtFlags.setBackground(java.awt.Color.white);
        txtFlags.setEditable(false);
        txtFlags.setText("00");
        txtFlags.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtFlags.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel17.setFont(jLabel17.getFont().deriveFont(jLabel17.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel17.setText("PC");

        txtRegPC.setBackground(java.awt.Color.white);
        txtRegPC.setEditable(false);
        txtRegPC.setText("0000");
        txtRegPC.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegPC.setPreferredSize(new java.awt.Dimension(50, 21));

        jLabel18.setFont(jLabel18.getFont().deriveFont(jLabel18.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel18.setText("SP");

        txtRegSP.setBackground(java.awt.Color.white);
        txtRegSP.setEditable(false);
        txtRegSP.setText("0000");
        txtRegSP.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtRegSP.setPreferredSize(new java.awt.Dimension(50, 21));

        jLabel19.setFont(jLabel19.getFont().deriveFont(jLabel19.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel19.setText("S");

        txtFlagS.setBackground(java.awt.Color.white);
        txtFlagS.setEditable(false);
        txtFlagS.setFont(txtFlagS.getFont());
        txtFlagS.setText("00");
        txtFlagS.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtFlagS.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel20.setFont(jLabel20.getFont().deriveFont(jLabel20.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel20.setText("Z");

        txtFlagZ.setBackground(java.awt.Color.white);
        txtFlagZ.setEditable(false);
        txtFlagZ.setText("00");
        txtFlagZ.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtFlagZ.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel21.setFont(jLabel21.getFont().deriveFont(jLabel21.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel21.setText("AC");

        txtFlagAC.setBackground(java.awt.Color.white);
        txtFlagAC.setEditable(false);
        txtFlagAC.setText("00");
        txtFlagAC.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtFlagAC.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel22.setFont(jLabel22.getFont().deriveFont(jLabel22.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel22.setText("P");

        txtFlagP.setBackground(java.awt.Color.white);
        txtFlagP.setEditable(false);
        txtFlagP.setFont(txtFlagP.getFont());
        txtFlagP.setText("00");
        txtFlagP.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtFlagP.setPreferredSize(new java.awt.Dimension(27, 21));

        jLabel23.setFont(jLabel23.getFont().deriveFont(jLabel23.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel23.setText("C");

        txtFlagC.setBackground(java.awt.Color.white);
        txtFlagC.setEditable(false);
        txtFlagC.setText("00");
        txtFlagC.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 3, 2, 3));
        txtFlagC.setPreferredSize(new java.awt.Dimension(27, 21));

        javax.swing.GroupLayout paneInfoRegistersLayout = new javax.swing.GroupLayout(paneInfoRegisters);
        paneInfoRegisters.setLayout(paneInfoRegistersLayout);
        paneInfoRegistersLayout.setHorizontalGroup(
            paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegBC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegDE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegHL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRegA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel16)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFlags, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                                .addComponent(jLabel18)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRegPC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                        .addComponent(jLabel19)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFlagS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFlagZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel21)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFlagAC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFlagP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel23)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFlagC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(37, Short.MAX_VALUE))
        );
        paneInfoRegistersLayout.setVerticalGroup(
            paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(paneInfoRegistersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtRegB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(txtRegC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(txtRegBC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtRegD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(txtRegE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(txtRegDE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtRegH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(txtRegL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(txtRegHL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(txtRegA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(txtFlags, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(txtRegPC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(txtRegSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(txtFlagS, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20)
                    .addComponent(txtFlagZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21)
                    .addComponent(txtFlagAC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(paneInfoRegistersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(txtFlagP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23)
                    .addComponent(txtFlagC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(new java.awt.Color(153, 153, 153), 1, true), "Run status", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("DejaVu Sans", 1, 14), new java.awt.Color(102, 102, 102))); // NOI18N

        lblRun.setFont(lblRun.getFont().deriveFont(lblRun.getFont().getStyle() | java.awt.Font.BOLD));
        lblRun.setForeground(new java.awt.Color(0, 102, 0));
        lblRun.setText("Stopped");

        jLabel11.setText("CPU frequency:");

        spnFrequency.setModel(new SpinnerNumberModel(2000, 1, 99999, 100));

        jLabel12.setFont(jLabel12.getFont().deriveFont(jLabel12.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel12.setText("kHz");

        jLabel13.setText("Runtime frequency:");

        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | java.awt.Font.BOLD));
        lblFrequency.setText("0,0 kHz");

        jLabel14.setText("Test periode:");

        spnTestPeriode.setModel(new SpinnerNumberModel(50, 1, 10000, 50));

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel15.setText("ms");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblRun)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblFrequency))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel14)
                                .addGap(26, 26, 26)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spnTestPeriode, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
                            .addComponent(spnFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(4, 4, 4)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15)
                            .addComponent(jLabel12))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblRun)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(spnFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jLabel15)
                    .addComponent(spnTestPeriode, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(lblFrequency))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneInfoRegisters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(paneInfoRegisters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JLabel lblFrequency;
    javax.swing.JLabel lblRun;
    javax.swing.JSpinner spnFrequency;
    javax.swing.JSpinner spnTestPeriode;
    javax.swing.JTextField txtFlagAC;
    javax.swing.JTextField txtFlagC;
    javax.swing.JTextField txtFlagP;
    javax.swing.JTextField txtFlagS;
    javax.swing.JTextField txtFlagZ;
    javax.swing.JTextField txtFlags;
    javax.swing.JTextField txtRegA;
    javax.swing.JTextField txtRegB;
    javax.swing.JTextField txtRegBC;
    javax.swing.JTextField txtRegC;
    javax.swing.JTextField txtRegD;
    javax.swing.JTextField txtRegDE;
    javax.swing.JTextField txtRegE;
    javax.swing.JTextField txtRegH;
    javax.swing.JTextField txtRegHL;
    javax.swing.JTextField txtRegL;
    javax.swing.JTextField txtRegPC;
    javax.swing.JTextField txtRegSP;
    // End of variables declaration//GEN-END:variables
    
}
