/*
 * statusGUI.java
 *
 * Created on Nedeľa, 2008, august 24, 10:22
 *
 * KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package cpu_z80.gui;

import cpu_z80.impl.CpuZ80;
import interfaces.IICpuListener;

import java.awt.Color;
import java.awt.Font;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import interfaces.ICPUInstruction;
import plugins.cpu.ICPU;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class StatusGUI extends javax.swing.JPanel {

    private IDebugColumn[] columns;
    private int run_state;
    private CpuZ80 cpu;
    private IMemoryContext mem = null;
    private AbstractTableModel flagModel1;
    private AbstractTableModel flagModel2;
    // syntax = prefix,size,postfix
    private String[][] mnemoTab = {
        {"nop", "0", ""}, {"ld bc,", "2", ""}, {"ld (bc),a", "0", ""}, {"inc bc", "0", ""},
        {"inc b", "0", ""}, {"dec b", "0", ""}, {"ld b,", "1", ""}, {"rlca", "0", ""},
        {"ex af,af", "0", ""}, {"add hl,bc", "0", ""}, {"ld a,(bc)", "0", ""}, {"dec bc", "0", ""},
        {"inc c", "0", ""}, {"dec c", "0", ""}, {"ld c,", "1", ""}, {"rrca", "0", ""}, // 0f
        {"djnz", "1", ""}, {"ld de,", "2", ""}, {"ld (de),a", "0", ""}, {"inc de", "0", ""},
        {"inc d", "0", ""}, {"dec d", "0", ""}, {"ld d,", "1", ""}, {"rla", "0", ""},
        {"jr ", "1", ""}, {"add hl,de", "0", ""}, {"ld a,(de)", "0", ""}, {"dec de", "0", ""},
        {"inc e", "0", ""}, {"dec e", "0", ""}, {"ld e,", "1", ""}, {"rra", "0", ""}, //1f
        {"jr nz,", "1", ""}, {"ld hl,", "2", ""}, {"ld (", "2", "),hl"}, {"inc hl", "0", ""},
        {"inc h", "0", ""}, {"dec h", "0", ""}, {"ld h,", "1", ""}, {"daa", "0", ""},
        {"jr z,", "1", ""}, {"add hl,hl", "0", ""}, {"ld hl,(", "2", ")"}, {"dec hl", "0", ""},
        {"inc l", "0", ""}, {"dec l", "0", ""}, {"ld l,", "1", ""}, {"cpl", "0", ""}, //2f
        {"jr nc,", "1", ""}, {"ld sp,", "2", ""}, {"ld (", "2", "),a"}, {"inc sp", "0", ""},
        {"inc (hl)", "0", ""}, {"dec (hl)", "0", ""}, {"ld (hl),", "1", ""}, {"scf", "0", ""},
        {"jr c,", "1", ""}, {"add hl,sp", "0", ""}, {"ld a,(", "2", ")"}, {"dec sp", "0", ""},
        {"inc a", "0", ""}, {"dec a", "0", ""}, {"ld a,", "1", ""}, {"ccf", "0", ""}, //3f
        {"ld b,b", "0", ""}, {"ld b,c", "0", ""}, {"ld b,d", "0", ""}, {"ld b,e", "0", ""},
        {"ld b,h", "0", ""}, {"ld b,l", "0", ""}, {"ld b,(hl)", "0", ""}, {"ld b,a", "0", ""},
        {"ld c,b", "0", ""}, {"ld c,c", "0", ""}, {"ld c,d", "0", ""}, {"ld c,e", "0", ""},
        {"ld c,h", "0", ""}, {"ld c,l", "0", ""}, {"ld c,(hl)", "0", ""}, {"ld c,a", "0", ""}, //4f
        {"ld d,b", "0", ""}, {"ld d,c", "0", ""}, {"ld d,d", "0", ""}, {"ld d,e", "0", ""},
        {"ld d,h", "0", ""}, {"ld d,l", "0", ""}, {"ld d,(hl)", "0", ""}, {"ld d,a", "0", ""},
        {"ld e,b", "0", ""}, {"ld e,c", "0", ""}, {"ld e,d", "0", ""}, {"ld e,e", "0", ""},
        {"ld e,h", "0", ""}, {"ld e,l", "0", ""}, {"ld e,(hl)", "0", ""}, {"ld e,a", "0", ""}, //5f
        {"ld h,b", "0", ""}, {"ld h,c", "0", ""}, {"ld h,d", "0", ""}, {"ld h,e", "0", ""},
        {"ld h,h", "0", ""}, {"ld h,l", "0", ""}, {"ld h,(hl)", "0", ""}, {"ld h,a", "0", ""},
        {"ld l,b", "0", ""}, {"ld l,c", "0", ""}, {"ld l,d", "0", ""}, {"ld l,e", "0", ""},
        {"ld l,h", "0", ""}, {"ld l,l", "0", ""}, {"ld l,(hl)", "0", ""}, {"ld l,a", "0", ""}, //6f
        {"ld (hl),b", "0", ""}, {"ld (hl),c", "0", ""}, {"ld (hl),d", "0", ""}, {"ld (hl),e", "0", ""},
        {"ld (hl),h", "0", ""}, {"ld (hl),l", "0", ""}, {"halt", "0", ""}, {"ld (hl),a", "0", ""},
        {"ld a,b", "0", ""}, {"ld a,c", "0", ""}, {"ld a,d", "0", ""}, {"ld a,e", "0", ""},
        {"ld a,h", "0", ""}, {"ld a,l", "0", ""}, {"ld a,(hl)", "0", ""}, {"ld a,a", "0", ""}, //7f
        {"add a,b", "0", ""}, {"add a,c", "0", ""}, {"add a,d", "0", ""}, {"add a,e", "0", ""},
        {"add a,h", "0", ""}, {"add a,l", "0", ""}, {"add a,(hl)", "0", ""}, {"add a,a", "0", ""},
        {"adc a,b", "0", ""}, {"adc a,c", "0", ""}, {"adc a,d", "0", ""}, {"adc a,e", "0", ""},
        {"adc a,h", "0", ""}, {"adc a,l", "0", ""}, {"adc a,(hl)", "0", ""}, {"adc a,a", "0", ""}, //8f
        {"sub b", "0", ""}, {"sub c", "0", ""}, {"sub d", "0", ""}, {"sub e", "0", ""},
        {"sub h", "0", ""}, {"sub l", "0", ""}, {"sub (hl)", "0", ""}, {"sub a", "0", ""},
        {"sbc b", "0", ""}, {"sbc c", "0", ""}, {"sbc d", "0", ""}, {"sbc e", "0", ""},
        {"sbc h", "0", ""}, {"sbc l", "0", ""}, {"sbc (hl)", "0", ""}, {"sbc a", "0", ""}, //9f
        {"and b", "0", ""}, {"and c", "0", ""}, {"and d", "0", ""}, {"and e", "0", ""},
        {"and h", "0", ""}, {"and l", "0", ""}, {"and (hl)", "0", ""}, {"and a", "0", ""},
        {"xor b", "0", ""}, {"xor c", "0", ""}, {"xor d", "0", ""}, {"xor e", "0", ""},
        {"xor h", "0", ""}, {"xor l", "0", ""}, {"xor (hl)", "0", ""}, {"xor a", "0", ""}, //af
        {"or b", "0", ""}, {"or c", "0", ""}, {"or d", "0", ""}, {"or e", "0", ""},
        {"or h", "0", ""}, {"or l", "0", ""}, {"or (hl)", "0", ""}, {"or a", "0", ""},
        {"cp b", "0", ""}, {"cp c", "0", ""}, {"cp d", "0", ""}, {"cp e", "0", ""},
        {"cp h", "0", ""}, {"cp l", "0", ""}, {"cp (hl)", "0", ""}, {"cp a", "0", ""}, //bf
        {"ret nz", "0", ""}, {"pop bc", "0", ""}, {"jp nz,", "2", ""}, {"jp ", "2", ""},
        {"call nz,", "2", ""}, {"push bc", "0", ""}, {"add a,", "1", ""}, {"rst 0", "0", ""},
        {"ret z", "0", ""}, {"ret", "0", ""}, {"jp z,", "2", ""},
        null, // cb
        {"call z,", "2", ""}, {"call ", "2", ""}, {"adc a,", "1", ""}, {"rst 08", "0", ""}, //cf
        {"ret nc", "0", ""}, {"pop de", "0", ""}, {"jp nc,", "2", ""}, {"out (", "1", "),a"},
        {"call nc,", "2", ""}, {"push de", "0", ""}, {"sub ", "1", ""}, {"rst 10", "0", ""},
        {"ret c", "0", ""}, {"exx", "0", ""}, {"jp c,", "2", ""}, {"in a,(", "1", ")"},
        {"call c,", "2", ""},
        null, // dd
        {"sbc a,", "1", ""}, {"rst 18", "0", ""}, //df
        {"ret po", "0", ""}, {"pop hl", "0", ""}, {"jp po,", "2", ""}, {"ex (sp),hl", "0", ""},
        {"call po,", "2", ""}, {"push hl", "0", ""}, {"and ", "1", ""}, {"rst 20", "0", ""},
        {"ret pe", "0", ""}, {"jp (hl)", "0", ""}, {"jp pe,", "2", ""}, {"ex de,hl", "0", ""},
        {"call pe,", "2", ""},
        null, // ed
        {"xor ", "1", ""}, {"rst 28", "0", ""}, // ef
        {"ret p", "0", ""}, {"pop af", "0", ""}, {"jp p,", "2", ""}, {"di", "0", ""},
        {"call p,", "2", ""}, {"push af", "0", ""}, {"or ", "1", ""}, {"rst 30", "0", ""},
        {"ret m", "0", ""}, {"ld sp,hl", "0", ""}, {"jp m,", "2", ""}, {"ei", "0", ""},
        {"call m,", "2", ""},
        null, // fd
        {"cp ", "1", ""}, {"rst 38", "0", ""} // ff
    };
    private String[] mnemoTabCB = {
        "rlc b", "rlc c", "rlc d", "rlc e", "rlc h", "rlc l", "rlc (hl)", "rlc a",
        "rrc b", "rrc c", "rrc d", "rrc e", "rrc h", "rrc l", "rrc (hl)", "rrc a", // 0f
        "rl b", "rl c", "rl d", "rl e", "rl h", "rl l", "rl (hl)", "rl a",
        "rr b", "rr c", "rr d", "rr e", "rr h", "rr l", "rr (hl)", "rr a", // 1f
        "sla b", "sla c", "sla d", "sla e", "sla h", "sla l", "sla (hl)", "sla a",
        "sra b", "sra c", "sra d", "sra e", "sra h", "sra l", "sra (hl)", "sra a", // 2f
        "sll b", "sll c", "sll d", "sll e", "sll h", "sll l", "sll (hl)", "sll a",
        "srl b", "srl c", "srl d", "srl e", "srl h", "srl l", "srl (hl)", "srl a", // 3f
        "bit 0,b", "bit 0,c", "bit 0,d", "bit 0,e", "bit 0,h", "bit 0,l", "bin 0,(hl)", "bit 0,a",
        "bit 1,b", "bit 1,c", "bit 1,d", "bit 1,e", "bit 1,h", "bit 1,l", "bit 1,(hl)", "bit 1,a", // 4f
        "bit 2,b", "bit 2,c", "bit 2,d", "bit 2,e", "bit 2,h", "bit 2,l", "bit 2,(hl)", "bit 2,a",
        "bit 3,b", "bit 3,c", "bit 3,d", "bit 3,e", "bit 3,h", "bit 3,l", "bit 3,(hl)", "bit 3,a", // 5f
        "bit 4,b", "bit 4,c", "bit 4,d", "bit 4,e", "bit 4,h", "bit 4,l", "bit 4,(hl)", "bit 4,a",
        "bit 5,b", "bit 5,c", "bit 5,d", "bit 5,e", "bit 5,h", "bit 5,l", "bit 5,(hl)", "bit 5,a", // 6f
        "bit 6,b", "bit 6,c", "bit 6,d", "bit 6,e", "bit 6,h", "bit 6,l", "bit 6,(hl)", "bit 6,a",
        "bit 7,b", "bit 7,c", "bit 7,d", "bit 7,e", "bit 7,h", "bit 7,l", "bit 7,(hl)", "bit 7,a", // 7f
        "res 0,b", "res 0,c", "res 0,d", "res 0,e", "res 0,h", "res 0,l", "res 0,(hl)", "res 0,a",
        "res 1,b", "res 1,c", "res 1,d", "res 1,e", "res 1,h", "res 1,l", "res 1,(hl)", "res 1,a", // 8f
        "res 2,b", "res 2,c", "res 2,d", "res 2,e", "res 2,h", "res 2,l", "res 2,(hl)", "res 2,a",
        "res 3,b", "res 3,c", "res 3,d", "res 3,e", "res 3,h", "res 3,l", "res 3,(hl)", "res 3,a", // 9f
        "res 4,b", "res 4,c", "res 4,d", "res 4,e", "res 4,h", "res 4,l", "res 4,(hl)", "res 4,a",
        "res 5,b", "res 5,c", "res 5,d", "res 5,e", "res 5,h", "res 5,l", "res 5,(hl)", "res 5,a", // af
        "res 6,b", "res 6,c", "res 6,d", "res 6,e", "res 6,h", "res 6,l", "res 6,(hl)", "res 6,a",
        "res 7,b", "res 7,c", "res 7,d", "res 7,e", "res 7,h", "res 7,l", "res 7,(hl)", "res 7,a", // bf
        "set 0,b", "set 0,c", "set 0,d", "set 0,e", "set 0,h", "set 0,l", "set 0,(hl)", "set 0,a",
        "set 1,b", "set 1,c", "set 1,d", "set 1,e", "set 1,h", "set 1,l", "set 1,(hl)", "set 1,a", // cf
        "set 2,b", "set 2,c", "set 2,d", "set 2,e", "set 2,h", "set 2,l", "set 2,(hl)", "set 2,a",
        "set 3,b", "set 3,c", "set 3,d", "set 3,e", "set 3,h", "set 3,l", "set 3,(hl)", "set 3,a", // df
        "set 4,b", "set 4,c", "set 4,d", "set 4,e", "set 4,h", "set 4,l", "set 4,(hl)", "set 4,a",
        "set 5,b", "set 5,c", "set 5,d", "set 5,e", "set 5,h", "set 5,l", "set 5,(hl)", "set 5,a", // ef
        "set 6,b", "set 6,c", "set 6,d", "set 6,e", "set 6,h", "set 6,l", "set 6,(hl)", "set 6,a",
        "set 7,b", "set 7,c", "set 7,d", "set 7,e", "set 7,h", "set 7,l", "set 7,(hl)", "set 7,a" // ff
    };
    private String mnemoTabDD[][] = {
        null, null, null, null, null, null, null, null, null, {"add ix,bc", "0", ""}, null,
        null, null, null, null, null, // 0f
        null, null, null, null, null, null, null, null, null, {"add ix,de", "0", ""}, null,
        null, null, null, null, null, // 1f
        null, {"ld ix,", "2", ""}, {"ld (", "2", "),ix"}, {"inc ix", "0", ""}, null, null,
        null, null, null, {"add ix,ix", "0", ""}, {"ld ix,(", "2", ")"}, {"dec ix", "0", ""},
        null, null, null, null, // 2f
        null, null, null, null, {"inc (ix+", "1", ")"}, {"dec (ix+", "1", ")"}, null, //36
        null, null, {"add ix,sp", "0", ""}, null, null, null, null, null, null, // 3f
        null, null, null, null, null, null, {"ld b,(ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"ld c,(ix+", "1", ")"}, null, // 4f
        null, null, null, null, null, null, {"ld d,(ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"ld e,(ix+", "1", ")"}, null, // 5f
        null, null, null, null, null, null, {"ld h,(ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"ld l,(ix+", "1", ")"}, null, // 6f
        {"ld (ix+", "1", "),b"}, {"ld (ix+", "1", "),c"}, {"ld (ix+", "1", "),d"}, {"ld (ix+", "1", "),e"},
        {"ld (ix+", "1", "),h"}, {"ld (ix+", "1", "),l"}, null, {"ld (ix+", "1", "),a"},
        null, null, null, null, null, null, {"ld a,(ix+", "1", ")"}, null, // 7f
        null, null, null, null, null, null, {"add a,(ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"adc a,(ix+", "1", ")"}, null, // 8f
        null, null, null, null, null, null, {"sub (ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"sbc a,(ix+", "1", ")"}, null, // 9f
        null, null, null, null, null, null, {"and (ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"xor (ix+", "1", ")"}, null, // af
        null, null, null, null, null, null, {"or (ix+", "1", ")"}, null, null, null, null,
        null, null, null, {"cp (ix+", "1", ")"}, null, // bf
        null, null, null, null, null, null, null, null, null, null, null, null, // cb
        null, null, null, null, // cf
        null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, // df
        null, {"pop ix", "0", ""}, null, {"ex (sp),ix", "0", ""}, null, {"push ix", "0", ""},
        null, null, null, {"jp (ix)", "0", ""}, null, null, null, null, null, null, // ef
        null, null, null, null, null, null, null, null, null, {"ld sp,ix", "0", ""}, null,
        null, null, null, null, null // ff
    };
    private String mnemoTabFD[][] = {
        null, null, null, null, null, null, null, null, null, {"add iy,bc", "0", ""}, null,
        null, null, null, null, null, // 0f
        null, null, null, null, null, null, null, null, null, {"add iy,de", "0", ""}, null,
        null, null, null, null, null, // 1f
        null, {"ld iy,", "2", ""}, {"ld (", "2", "),iy"}, {"inc iy", "0", ""}, null, null,
        null, null, null,
        {"add iy,iy", "0", ""}, {"ld iy,(", "2", ")"}, {"dec iy", "0", ""}, null, null, null,
        null, // 2f
        null, null, null, null, {"inc (iy+", "1", ")"}, {"dec (iy+", "1", ")"}, null, //36
        null, null, {"add iy,sp", "0", ""}, null, null, null, null, null, null, // 3f
        null, null, null, null, null, null, {"ld b,(iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"ld c,(iy+", "1", ")"}, null, // 4f
        null, null, null, null, null, null, {"ld d,(iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"ld e,(iy+", "1", ")"}, null, // 5f
        null, null, null, null, null, null, {"ld h,(iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"ld l,(iy+", "1", ")"}, null, // 6f
        {"ld (iy+", "1", "),b"}, {"ld (iy+", "1", "),c"}, {"ld (iy+", "1", "),d"}, {"ld (iy+", "1", "),e"},
        {"ld (iy+", "1", "),h"}, {"ld (iy+", "1", "),l"}, null, {"ld (iy+", "1", "),a"},
        null, null, null, null, null, null, {"ld a,(iy+", "1", ")"}, null, // 7f
        null, null, null, null, null, null, {"add a,(iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"adc a,(iy+", "1", ")"}, null, // 8f
        null, null, null, null, null, null, {"sub (iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"sbc a,(iy+", "1", ")"}, null, // 9f
        null, null, null, null, null, null, {"and (iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"xor (iy+", "1", ")"}, null, // af
        null, null, null, null, null, null, {"or (iy+", "1", ")"}, null, null, null, null,
        null, null, null, {"cp (iy+", "1", ")"}, null, // bf
        null, null, null, null, null, null, null, null, null, null, null, null, // cb
        null, null, null, null, // cf
        null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, // df
        null, {"pop iy", "0", ""}, null, {"ex (sp),iy", "0", ""}, null, {"push iy", "0", ""},
        null, null, null, {"jp (iy)", "0", ""}, null, null, null, null, null, null, // ef
        null, null, null, null, null, null, null, null, null, {"ld sp,iy", "0", ""}, null,
        null, null, null, null, null // ff
    };

    private class FlagsModel extends AbstractTableModel {

        private String[] flags = {"S", "Z", "H", "P/V", "N", "C"};
        private int[] flagsI = {0, 0, 0, 0, 0, 0};
        private int set;

        public FlagsModel(int set) {
            this.set = set;
        }

        @Override
        public int getRowCount() {
            return 2;
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return flags[columnIndex];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (rowIndex) {
                case 0:
                    return flags[columnIndex];
                case 1:
                    return flagsI[columnIndex];
            }
            return null;
        }

        @Override
        public void fireTableDataChanged() {
            short F = 0;
            if (set == 0) {
                F = cpu.F;
            } else {
                F = cpu.F1;
            }
            flagsI[0] = ((F & CpuZ80.flagS) != 0) ? 1 : 0;
            flagsI[1] = ((F & CpuZ80.flagZ) != 0) ? 1 : 0;
            flagsI[2] = ((F & CpuZ80.flagH) != 0) ? 1 : 0;
            flagsI[3] = ((F & CpuZ80.flagPV) != 0) ? 1 : 0;
            flagsI[4] = ((F & CpuZ80.flagN) != 0) ? 1 : 0;
            flagsI[5] = ((F & CpuZ80.flagC) != 0) ? 1 : 0;
            super.fireTableDataChanged();
        }
    }

    /** Creates new form statusGUI */
    public StatusGUI(final CpuZ80 cpu, IMemoryContext mem) {
        initComponents();

        this.cpu = cpu;
        this.mem = mem;
//        this.cpuC = (CpuContext) cpu.getContext();
        run_state = ICPU.STATE_STOPPED_NORMAL;
        columns = new IDebugColumn[4];
        columns[0] = new ColumnInfo("breakpoint", java.lang.Boolean.class, true);
        columns[1] = new ColumnInfo("address", java.lang.String.class, false);
        columns[2] = new ColumnInfo("mnemonics", java.lang.String.class, false);
        columns[3] = new ColumnInfo("opcode", java.lang.String.class, false);

        cpu.addCPUListener(new IICpuListener() {

            @Override
            public void runChanged(EventObject evt, int state) {
                run_state = state;
            }

            @Override
            public void stateUpdated(EventObject evt) {
                updateGUI();
            }

            @Override
            public void frequencyChanged(EventObject evt, float frequency) {
                lblFrequency.setText(String.format("%.2f kHz", frequency));
            }
        });
        spnFrequency.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int i = (Integer) ((SpinnerNumberModel) spnFrequency.getModel()).getValue();
                try {
                    setCPUFreq(i);
                } catch (IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel) spnFrequency.getModel()).setValue(cpu.getFrequency());
                }
            }
        });
        spnTestPeriode.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int i = (Integer) ((SpinnerNumberModel) spnTestPeriode.getModel()).getValue();
                try {
                    cpu.setSliceTime(i);
                } catch (IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel) spnTestPeriode.getModel()).setValue(cpu.getSliceTime());
                }
            }
        });
        flagModel1 = new FlagsModel(0);
        flagModel2 = new FlagsModel(1);
        tblFlags.setModel(flagModel1);
        tblFlags2.setModel(flagModel2);
    }

    // user set frequency (must be synchronized inside)
    private void setCPUFreq(int f) {
        cpu.setFrequency(f);
    }

    public IDebugColumn[] getDebugColumns() {
        return columns;
    }

    public void setDebugColVal(int index, int col, Object value) {
        if (col != 0) {
            return;
        }
        if (value.getClass() != Boolean.class) {
            return;
        }

        boolean v = Boolean.valueOf(value.toString());
        cpu.setBreakpoint(index, v);
    }

    public Object getDebugColVal(int index, int col) {
        try {
            ICPUInstruction instr = cpuDecode(index);
            switch (col) {
                case 0:
                    return cpu.getBreakpoint(index); // breakpoint
                case 1:
                    return String.format("%04Xh", index); // adresa
                case 2:
                    return instr.getMnemo(); // mnemonika
                case 3:
                    return instr.getOperCode(); // operacny kod
                default:
                    return "";
            }
        } catch (IndexOutOfBoundsException e) {
            // tu sa dostanem iba v pripade, ak pouzivatel manualne
            // zmenil hodnotu operacnej pamate tak, ze vyjadruje
            // instrukciu s viacerymi bytami, ktore sa uz nezmestili
            // do operacnej pamate
            switch (col) {
                case 0:
                    return cpu.getBreakpoint(index);
                case 1:
                    return String.format("%04Xh", index);
                case 2:
                    return "incomplete";
                case 3:
                    return String.format("%X", (Short) mem.read(index));
                default:
                    return "";
            }
        }
    }

    private String f(int what) {
        return String.format("%04X", what);
    }

    private String f(short what) {
        return String.format("%02X", what);
    }

    public void updateGUI() {
        txtRegA.setText(f(cpu.A));
        txtRegF.setText(f(cpu.F));
        txtRegB.setText(f(cpu.B));
        txtRegC.setText(f(cpu.C));
        txtRegBC.setText(f(((cpu.B << 8) | cpu.C) & 0xFFFF));
        txtRegD.setText(f(cpu.D));
        txtRegE.setText(f(cpu.E));
        txtRegDE.setText(f(((cpu.D << 8) | cpu.E) & 0xFFFF));
        txtRegH.setText(f(cpu.H));
        txtRegL.setText(f(cpu.L));
        txtRegHL.setText(f(((cpu.H << 8) | cpu.L) & 0xFFFF));
        flagModel1.fireTableDataChanged();
        txtRegA1.setText(f(cpu.A1));
        txtRegF1.setText(f(cpu.F1));
        txtRegB1.setText(f(cpu.B1));
        txtRegC1.setText(f(cpu.C1));
        txtRegBC1.setText(f(((cpu.B1 << 8) | cpu.C1) & 0xFFFF));
        txtRegD1.setText(f(cpu.D1));
        txtRegE1.setText(f(cpu.E1));
        txtRegDE1.setText(f(((cpu.D1 << 8) | cpu.E1) & 0xFFFF));
        txtRegH1.setText(f(cpu.H1));
        txtRegL1.setText(f(cpu.L1));
        txtRegHL1.setText(f(((cpu.H1 << 8) | cpu.L1) & 0xFFFF));
        flagModel2.fireTableDataChanged();

        txtRegSP.setText(String.format("%04X", cpu.SP));
        txtRegPC.setText(String.format("%04X", cpu.PC));
        txtRegIX.setText(String.format("%04X", cpu.IX));
        txtRegIY.setText(String.format("%04X", cpu.IY));
        txtRegI.setText(String.format("%02X", cpu.I));
        txtRegR.setText(String.format("%02X", cpu.R));

        if (run_state == ICPU.STATE_RUNNING) {
            lblRun.setText("running");
            spnFrequency.setEnabled(false);
            spnTestPeriode.setEnabled(false);
        } else {
            spnFrequency.setEnabled(true);
            spnTestPeriode.setEnabled(true);
            switch (run_state) {
                case ICPU.STATE_STOPPED_NORMAL:
                    lblRun.setText("stopped (normal)");
                    break;
                case ICPU.STATE_STOPPED_BREAK:
                    lblRun.setText("breakpoint");
                    break;
                case ICPU.STATE_STOPPED_ADDR_FALLOUT:
                    lblRun.setText("stopped (address fallout)");
                    break;
                case ICPU.STATE_STOPPED_BAD_INSTR:
                    lblRun.setText("stopped (instruction fallout)");
                    break;
            }
        }
    }

    public ICPUInstruction cpuDecode(int memPos) {
        short val;
        int tmp;
        int actPos = memPos;
        ICPUInstruction instr;
        String mnemo, oper;

        if (this.mem == null) {
            return null;
        }
        val = ((Short) mem.read(actPos++)).shortValue();
        oper = String.format("%02X", val);

        mnemo = null;
        switch (val) {
            case 0xCB:
                val = (Short) mem.read(actPos++);
                oper += String.format(" %02X", val);
                mnemo = mnemoTabCB[val];
                break;
            case 0xDD:
                val = ((Short) mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val);
                tmp = 0;
                switch (val) {
                    case 0xCB:
                        tmp = (Integer) mem.readWord(actPos);
                        actPos += 2;
                        val = (short) ((tmp >>> 8) & 0xff);
                        tmp &= 0xff;
                        oper += String.format(" %02X %02X", tmp, val);
                        switch (val) {
                            case 0x06:
                                mnemo = "rlc (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x0E:
                                mnemo = "rrc (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x16:
                                mnemo = "rl (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x1E:
                                mnemo = "rr (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x26:
                                mnemo = "sla (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x2E:
                                mnemo = "sra (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x36:
                                mnemo = "sll (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x3E:
                                mnemo = "srl (ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x46:
                                mnemo = "bit 0,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x4E:
                                mnemo = "bit 1,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x56:
                                mnemo = "bit 2,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x5E:
                                mnemo = "bit 3,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x66:
                                mnemo = "bit 4,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x6E:
                                mnemo = "bit 5,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x76:
                                mnemo = "bit 6,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x7E:
                                mnemo = "bit 7,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x86:
                                mnemo = "res 0,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x8E:
                                mnemo = "res 1,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x96:
                                mnemo = "res 2,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x9E:
                                mnemo = "res 3,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xA6:
                                mnemo = "res 4,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xAE:
                                mnemo = "res 5,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xB6:
                                mnemo = "res 6,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xBE:
                                mnemo = "res 7,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xC6:
                                mnemo = "set 0,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xCE:
                                mnemo = "set 1,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xD6:
                                mnemo = "set 2,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xDE:
                                mnemo = "set 3,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xE6:
                                mnemo = "set 4,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xEE:
                                mnemo = "set 5,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xF6:
                                mnemo = "set 6,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xFE:
                                mnemo = "set 7,(ix+" + String.format("%02X", tmp) + ")";
                                break;
                        }
                        break;
                    case 0x36:
                        tmp = (Integer) mem.readWord(actPos);
                        actPos += 2;
                        oper += String.format(" %02X %02X", (tmp >>> 8) & 0xff, tmp & 0xff);
                        mnemo = "ld (ix+" + String.format("%02X", tmp & 0xff)
                                + ")," + String.format("%02X", (tmp >>> 8) & 0xff);
                        break;
                    default:
                        if (mnemoTabDD[val] == null) {
                            break;
                        } else if (mnemoTabDD[val][1].equals("0")) {
                            mnemo = mnemoTabDD[val][0];
                            break;
                        } else if (mnemoTabDD[val][1].equals("1")) {
                            tmp = (Short) mem.read(actPos++);
                            oper += String.format(" %02X", tmp);
                            mnemo = mnemoTabDD[val][0] + String.format("%02X", tmp)
                                    + mnemoTabDD[val][2];
                            break;
                        } else if (mnemoTabDD[val][1].equals("2")) {
                            tmp = (Integer) mem.readWord(actPos);
                            actPos += 2;
                            oper += String.format(" %02X %02X", tmp & 0xff, (tmp >>> 8) & 0xff);
                            mnemo = mnemoTabDD[val][0] + String.format("%04X", tmp)
                                    + mnemoTabDD[val][2];
                            break;
                        }
                }
                break;
            case 0xED:
                val = ((Short) mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val);
                switch (val) {
                    case 0x40:
                        mnemo = "in b,(c)";
                        break;
                    case 0x41:
                        mnemo = "out (c),b";
                        break;
                    case 0x42:
                        mnemo = "sbc hl,bc";
                        break;
                    case 0x44:
                        mnemo = "neg";
                        break;
                    case 0x45:
                        mnemo = "retn";
                        break;
                    case 0x46:
                        mnemo = "im 0";
                        break;
                    case 0x47:
                        mnemo = "ld i,a";
                        break;
                    case 0x48:
                        mnemo = "in c,(c)";
                        break;
                    case 0x49:
                        mnemo = "out (c),c";
                        break;
                    case 0x4A:
                        mnemo = "add hl,bc";
                        break;
                    case 0x4D:
                        mnemo = "reti";
                        break;
                    case 0x4F:
                        mnemo = "ld r,a";
                        break;
                    case 0x50:
                        mnemo = "in d,(c)";
                        break;
                    case 0x51:
                        mnemo = "out (c),d";
                        break;
                    case 0x52:
                        mnemo = "sbc hl,de";
                        break;
                    case 0x56:
                        mnemo = "im 1";
                        break;
                    case 0x57:
                        mnemo = "ld a,i";
                        break;
                    case 0x58:
                        mnemo = "in e,(c)";
                        break;
                    case 0x59:
                        mnemo = "out (c),e";
                        break;
                    case 0x5A:
                        mnemo = "add hl,de";
                        break;
                    case 0x5E:
                        mnemo = "im 2";
                        break;
                    case 0x5F:
                        mnemo = "ld a,r";
                        break;
                    case 0x60:
                        mnemo = "in h,(c)";
                        break;
                    case 0x61:
                        mnemo = "out (c),h";
                        break;
                    case 0x62:
                        mnemo = "sbc hl,hl";
                        break;
                    case 0x67:
                        mnemo = "rrd";
                        break;
                    case 0x68:
                        mnemo = "in l,(c)";
                        break;
                    case 0x69:
                        mnemo = "out (c),l";
                        break;
                    case 0x6A:
                        mnemo = "add hl,hl";
                        break;
                    case 0x6F:
                        mnemo = "rld";
                        break;
                    case 0x70:
                        mnemo = "in (c)";
                        break;
                    case 0x71:
                        mnemo = "out (c),0";
                        break;
                    case 0x72:
                        mnemo = "sbc hl,sp";
                        break;
                    case 0x78:
                        mnemo = "in a,(c)";
                        break;
                    case 0x79:
                        mnemo = "out (c),a";
                        break;
                    case 0x7A:
                        mnemo = "add hl,sp";
                        break;
                    case 0xA0:
                        mnemo = "ldi";
                        break;
                    case 0xA1:
                        mnemo = "cpi";
                        break;
                    case 0xA2:
                        mnemo = "ini";
                        break;
                    case 0xA3:
                        mnemo = "outi";
                        break;
                    case 0xA8:
                        mnemo = "ldd";
                        break;
                    case 0xA9:
                        mnemo = "cpd";
                        break;
                    case 0xAA:
                        mnemo = "ind";
                        break;
                    case 0xAB:
                        mnemo = "outd";
                        break;
                    case 0xB0:
                        mnemo = "ldir";
                        break;
                    case 0xB1:
                        mnemo = "cpir";
                        break;
                    case 0xB2:
                        mnemo = "inir";
                        break;
                    case 0xB3:
                        mnemo = "otir";
                        break;
                    case 0xB8:
                        mnemo = "lddr";
                        break;
                    case 0xB9:
                        mnemo = "cpdr";
                        break;
                    case 0xBA:
                        mnemo = "indr";
                        break;
                    case 0xBB:
                        mnemo = "otdr";
                        break;
                }
                if (mnemo == null) {
                    tmp = (Integer) mem.readWord(actPos);
                    actPos += 2;
                    oper += String.format(" %02X %02X", tmp & 0xFF, (tmp >>> 8) & 0xff);
                    switch (val) {
                        case 0x43:
                            mnemo = "ld (" + String.format("%04X", tmp) + "),bc";
                            break;
                        case 0x4B:
                            mnemo = "ld bc,(" + String.format("%04X", tmp) + ")";
                            break;
                        case 0x53:
                            mnemo = "ld (" + String.format("%04X", tmp) + "),de";
                            break;
                        case 0x5B:
                            mnemo = "ld de,(" + String.format("%04X", tmp) + ")";
                            break;
                        case 0x63:
                            mnemo = "ld (" + String.format("%04X", tmp) + "),hl";
                            break;
                        case 0x6B:
                            mnemo = "ld hl,(" + String.format("%04X", tmp) + ")";
                            break;
                        case 0x73:
                            mnemo = "ld (" + String.format("%04X", tmp) + "),sp";
                            break;
                        case 0x7B:
                            mnemo = "ld sp,(" + String.format("%04X", tmp) + ")";
                            break;
                    }
                }
                break;
            case 0xFD:
                val = ((Short) mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val);
                tmp = 0;
                switch (val) {
                    case 0xCB:
                        tmp = (Integer) mem.readWord(actPos);
                        actPos += 2;
                        val = (short) ((tmp >>> 8) & 0xff);
                        tmp &= 0xff;
                        oper += String.format(" %02X %02X", tmp, val);
                        switch (val) {
                            case 0x06:
                                mnemo = "rlc (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x0E:
                                mnemo = "rrc (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x16:
                                mnemo = "rl (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x1E:
                                mnemo = "rr (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x26:
                                mnemo = "sla (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x2E:
                                mnemo = "sra (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x36:
                                mnemo = "sll (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x3E:
                                mnemo = "srl (iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x46:
                                mnemo = "bit 0,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x4E:
                                mnemo = "bit 1,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x56:
                                mnemo = "bit 2,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x5E:
                                mnemo = "bit 3,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x66:
                                mnemo = "bit 4,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x6E:
                                mnemo = "bit 5,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x76:
                                mnemo = "bit 6,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x7E:
                                mnemo = "bit 7,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x86:
                                mnemo = "res 0,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x8E:
                                mnemo = "res 1,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x96:
                                mnemo = "res 2,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0x9E:
                                mnemo = "res 3,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xA6:
                                mnemo = "res 4,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xAE:
                                mnemo = "res 5,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xB6:
                                mnemo = "res 6,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xBE:
                                mnemo = "res 7,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xC6:
                                mnemo = "set 0,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xCE:
                                mnemo = "set 1,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xD6:
                                mnemo = "set 2,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xDE:
                                mnemo = "set 3,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xE6:
                                mnemo = "set 4,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xEE:
                                mnemo = "set 5,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xF6:
                                mnemo = "set 6,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                            case 0xFE:
                                mnemo = "set 7,(iy+" + String.format("%02X", tmp) + ")";
                                break;
                        }
                        break;
                    case 0x36:
                        tmp = (Integer) mem.readWord(actPos);
                        actPos += 2;
                        oper += String.format(" %02X %02X", (tmp >>> 8) & 0xff, tmp & 0xff);
                        mnemo = "ld (iy+" + String.format("%02X", tmp & 0xff)
                                + ")," + String.format("%02X", (tmp >>> 8) & 0xff);
                        break;
                    default:
                        if (mnemoTabFD[val] == null) {
                            break;
                        } else if (mnemoTabFD[val][1].equals("0")) {
                            mnemo = mnemoTabFD[val][0];
                            break;
                        } else if (mnemoTabFD[val][1].equals("1")) {
                            tmp = (Short) mem.read(actPos++);
                            oper += String.format(" %02X", tmp);
                            mnemo = mnemoTabFD[val][0] + String.format("%02X", tmp)
                                    + mnemoTabFD[val][2];
                            break;
                        } else if (mnemoTabFD[val][1].equals("2")) {
                            tmp = (Integer) mem.readWord(actPos);
                            actPos += 2;
                            oper += String.format(" %02X %02X", tmp & 0xff, (tmp >>> 8) & 0xff);
                            mnemo = mnemoTabFD[val][0] + String.format("%04X", tmp)
                                    + mnemoTabFD[val][2];
                            break;
                        }
                }
                break;
            default:
                if (mnemoTab[val][1].equals("0")) {
                    mnemo = mnemoTab[val][0];
                    break;
                } else if (mnemoTab[val][1].equals("1")) {
                    tmp = (Short) mem.read(actPos++);
                    oper += String.format(" %02X", tmp);
                    mnemo = mnemoTab[val][0] + String.format("%02X", tmp)
                            + mnemoTab[val][2];
                    break;
                } else if (mnemoTab[val][1].equals("2")) {
                    tmp = (Integer) mem.readWord(actPos++);
                    oper += String.format(" %02X %02X", tmp & 0xff, (tmp >>> 8) & 0xff);
                    mnemo = mnemoTab[val][0] + String.format("%04X", tmp)
                            + mnemoTab[val][2];
                    break;
                }
        }
        tmp = 0;
        if (mnemo == null) {
            mnemo = "unknown";
        }
        instr = new ICPUInstruction(mnemo, oper, actPos);
        return instr;
    }

    /**
     * Return memory location of next instruction (from memPos)
     * Instructions that are located at address memPos+1, are not included
     * because for not found opcodes this method returns memPos+1 implicitly.
     * @return memory location of next instruction
     */
    public int getNextPosition(int memPos) throws ArrayIndexOutOfBoundsException {
        short val;
        if (mem == null) {
            return 0;
        }
        val = (Short) mem.read(memPos++);
        switch (val) {
            case 0xCB:
                return memPos + 1;
            case 0xDD:
            case 0xFD:
                val = ((Short) mem.read(memPos++)).shortValue();
                switch (val) {
                    case 0xCB:
                        val = (Short) mem.read(memPos + 1);
                        memPos += 2;
                        switch (val) {
                            case 0x06:
                            case 0x0E:
                            case 0x16:
                            case 0x1E:
                            case 0x26:
                            case 0x2E:
                            case 0x36:
                            case 0x3E:
                            case 0x46:
                            case 0x4E:
                            case 0x56:
                            case 0x5E:
                            case 0x66:
                            case 0x6E:
                            case 0x76:
                            case 0x7E:
                            case 0x86:
                            case 0x8E:
                            case 0x96:
                            case 0x9E:
                            case 0xA6:
                            case 0xAE:
                            case 0xB6:
                            case 0xBE:
                            case 0xC6:
                            case 0xCE:
                            case 0xD6:
                            case 0xDE:
                            case 0xE6:
                            case 0xEE:
                            case 0xF6:
                            case 0xFE:
                                return memPos;
                            default:
                                memPos -= 2;
                        }
                        break;
                    case 0x36:
                        return memPos + 2;
                    default:
                        if (mnemoTabDD[val] == null) {
                            return memPos;
                        } else if (mnemoTabDD[val][1].equals("0")) {
                            return memPos;
                        } else if (mnemoTabDD[val][1].equals("1")) {
                            return memPos + 1;
                        } else if (mnemoTabDD[val][1].equals("2")) {
                            return memPos + 2;
                        }
                }
                memPos--;
                break;
            case 0xED:
                val = ((Short) mem.read(memPos++)).shortValue();
                switch (val) {
                    case 0x40:
                    case 0x41:
                    case 0x42:
                    case 0x44:
                    case 0x45:
                    case 0x46:
                    case 0x47:
                    case 0x48:
                    case 0x49:
                    case 0x4A:
                    case 0x4D:
                    case 0x4F:
                    case 0x50:
                    case 0x51:
                    case 0x52:
                    case 0x56:
                    case 0x57:
                    case 0x58:
                    case 0x59:
                    case 0x5A:
                    case 0x5E:
                    case 0x5F:
                    case 0x60:
                    case 0x61:
                    case 0x62:
                    case 0x67:
                    case 0x68:
                    case 0x69:
                    case 0x6A:
                    case 0x6F:
                    case 0x70:
                    case 0x71:
                    case 0x72:
                    case 0x78:
                    case 0x79:
                    case 0x7A:
                    case 0xA0:
                    case 0xA1:
                    case 0xA2:
                    case 0xA3:
                    case 0xA8:
                    case 0xA9:
                    case 0xAA:
                    case 0xAB:
                    case 0xB0:
                    case 0xB1:
                    case 0xB2:
                    case 0xB3:
                    case 0xB8:
                    case 0xB9:
                    case 0xBA:
                    case 0xBB:
                        return memPos;
                    case 0x43:
                    case 0x4B:
                    case 0x53:
                    case 0x5B:
                    case 0x63:
                    case 0x6B:
                    case 0x73:
                    case 0x7B:
                        return memPos + 2;
                    default:
                        memPos--;
                }
            default:
                if (mnemoTab[val][1].equals("0")) {
                    return memPos;
                } else if (mnemoTab[val][1].equals("1")) {
                    return memPos + 1;
                } else if (mnemoTab[val][1].equals("2")) {
                    return memPos + 2;
                }
        }
        return memPos;
    }

    private void initComponents() {
        JPanel paneRegisters = new JPanel();
        JTabbedPane tabbedGPR = new JTabbedPane();
        JPanel panelSET1 = new JPanel();
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
        txtRegF = new JTextField("00");
        JLabel lblFlagsLBL = new JLabel("Flags (F): ");
        tblFlags = new JTable();
        JPanel panelSET2 = new JPanel();
        JLabel lblRegB1 = new JLabel("B");
        txtRegB1 = new JTextField("00");
        JLabel lblRegC1 = new JLabel("C");
        txtRegC1 = new JTextField("00");
        JLabel lblRegBC1 = new JLabel("BC");
        txtRegBC1 = new JTextField("0000");
        JLabel lblRegD1 = new JLabel("D");
        txtRegD1 = new JTextField("00");
        JLabel lblRegE1 = new JLabel("E");
        txtRegE1 = new JTextField("00");
        JLabel lblRegDE1 = new JLabel("DE");
        txtRegDE1 = new JTextField("0000");
        JLabel lblRegH1 = new JLabel("H");
        txtRegH1 = new JTextField("00");
        JLabel lblRegL1 = new JLabel("L");
        txtRegL1 = new JTextField("00");
        JLabel lblRegHL1 = new JLabel("HL");
        txtRegHL1 = new JTextField("0000");
        JLabel lblRegA1 = new JLabel("A");
        txtRegA1 = new JTextField("00");
        JLabel lblRegF1 = new JLabel("F");
        txtRegF1 = new JTextField("00");
        JLabel lblFlags1LBL = new JLabel("Flags (F): ");
        tblFlags2 = new JTable();
        JLabel lblRegPC = new JLabel("PC");
        txtRegPC = new JTextField("0000");
        JLabel lblRegSP = new JLabel("SP");
        txtRegSP = new JTextField("0000");
        JLabel lblRegIX = new JLabel("IX");
        txtRegIX = new JTextField("0000");
        JLabel lblRegIY = new JLabel("IY");
        JLabel lblRegI = new JLabel("I");
        txtRegI = new JTextField("00");
        JLabel lblRegR = new JLabel("R");
        txtRegIY = new JTextField("0000");
        txtRegR = new JTextField("00");
        JPanel panelRun = new JPanel();
        lblRun = new JLabel("Stopped");
        JLabel lblCPUFreq = new JLabel("CPU frequency:");
        spnFrequency = new JSpinner();
        JLabel lblKHZ = new JLabel("kHz");
        JLabel lblTestPeriode = new JLabel("Test periode:");
        spnTestPeriode = new JSpinner();
        JLabel lblMS = new JLabel("ms");
        JLabel lblRuntimeFreq = new JLabel("Runtime frequency:");
        lblFrequency = new JLabel();

        setBorder(null);
        paneRegisters.setBorder(null);

        tabbedGPR.setBorder(null);
        panelSET1.setBorder(null);

        lblRegB.setFont(lblRegB.getFont().deriveFont(lblRegB.getFont().getStyle() | Font.BOLD));
        txtRegB.setEditable(false);
        txtRegB.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegC.setFont(lblRegC.getFont().deriveFont(lblRegC.getFont().getStyle() | Font.BOLD));
        txtRegC.setEditable(false);
        txtRegC.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegBC.setFont(lblRegBC.getFont().deriveFont(lblRegBC.getFont().getStyle() | Font.BOLD));
        txtRegBC.setEditable(false);
        txtRegBC.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegD.setFont(lblRegD.getFont().deriveFont(lblRegD.getFont().getStyle() | Font.BOLD));
        txtRegD.setEditable(false);
        txtRegD.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegE.setFont(lblRegE.getFont().deriveFont(lblRegE.getFont().getStyle() | Font.BOLD));
        txtRegE.setEditable(false);
        txtRegE.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegDE.setFont(lblRegDE.getFont().deriveFont(lblRegDE.getFont().getStyle() | Font.BOLD));
        txtRegDE.setEditable(false);
        txtRegDE.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegH.setFont(lblRegH.getFont().deriveFont(lblRegH.getFont().getStyle() | Font.BOLD));
        txtRegH.setEditable(false);
        txtRegH.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegL.setFont(lblRegL.getFont().deriveFont(lblRegL.getFont().getStyle() | Font.BOLD));
        txtRegL.setEditable(false);
        txtRegL.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegHL.setFont(lblRegHL.getFont().deriveFont(lblRegHL.getFont().getStyle() | Font.BOLD));
        txtRegHL.setEditable(false);
        txtRegHL.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegA.setFont(lblRegA.getFont().deriveFont(lblRegA.getFont().getStyle() | Font.BOLD));
        txtRegA.setEditable(false);
        txtRegA.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegF.setFont(lblRegF.getFont().deriveFont(lblRegF.getFont().getStyle() | Font.BOLD));
        txtRegF.setEditable(false);
        txtRegF.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        tblFlags.setAutoCreateRowSorter(true);
        tblFlags.setBackground(Color.white);
        tblFlags.setBorder(null);
        tblFlags.setRowSelectionAllowed(false);

        GroupLayout panelSET1Layout = new GroupLayout(panelSET1);
        panelSET1.setLayout(panelSET1Layout);
        panelSET1Layout.setHorizontalGroup(
                panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(panelSET1Layout.createSequentialGroup().addContainerGap().addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegB).addComponent(lblRegD).addComponent(lblRegH).addComponent(lblRegA)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegB).addComponent(txtRegD).addComponent(txtRegH).addComponent(txtRegA)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegC).addComponent(lblRegE).addComponent(lblRegL).addComponent(lblRegF)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegC).addComponent(txtRegE).addComponent(txtRegL).addComponent(txtRegF)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegBC).addComponent(lblRegDE).addComponent(lblRegHL)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegBC).addComponent(txtRegDE).addComponent(txtRegHL)).addContainerGap()).addGroup(panelSET1Layout.createSequentialGroup().addContainerGap().addComponent(lblFlagsLBL)).addGroup(panelSET1Layout.createSequentialGroup().addContainerGap().addComponent(tblFlags).addContainerGap()));
        panelSET1Layout.setVerticalGroup(panelSET1Layout.createSequentialGroup().addContainerGap().addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegB).addComponent(txtRegB).addComponent(lblRegC).addComponent(txtRegC).addComponent(lblRegBC).addComponent(txtRegBC)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegD).addComponent(txtRegD).addComponent(lblRegE).addComponent(txtRegE).addComponent(lblRegDE).addComponent(txtRegDE)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegH).addComponent(txtRegH).addComponent(lblRegL).addComponent(txtRegL).addComponent(lblRegHL).addComponent(txtRegHL)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegA).addComponent(txtRegA).addComponent(lblRegF).addComponent(txtRegF)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(lblFlagsLBL).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(tblFlags).addContainerGap());
        tabbedGPR.addTab("Set1", panelSET1);

        panelSET2.setBorder(null);

        lblRegB1.setFont(lblRegB1.getFont().deriveFont(lblRegB1.getFont().getStyle() | Font.BOLD));
        txtRegB1.setEditable(false);
        txtRegB1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegC1.setFont(lblRegC1.getFont().deriveFont(lblRegC1.getFont().getStyle() | Font.BOLD));
        txtRegC1.setEditable(false);
        txtRegC1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegBC1.setFont(lblRegBC1.getFont().deriveFont(lblRegBC1.getFont().getStyle() | Font.BOLD));
        txtRegBC1.setEditable(false);
        txtRegBC1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegD1.setFont(lblRegD1.getFont().deriveFont(lblRegD1.getFont().getStyle() | Font.BOLD));
        txtRegD1.setEditable(false);
        txtRegD1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegE1.setFont(lblRegE1.getFont().deriveFont(lblRegE1.getFont().getStyle() | Font.BOLD));
        txtRegE1.setEditable(false);
        txtRegE1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegDE1.setFont(lblRegDE1.getFont().deriveFont(lblRegDE1.getFont().getStyle() | Font.BOLD));
        txtRegDE1.setEditable(false);
        txtRegDE1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegH1.setFont(lblRegH1.getFont().deriveFont(lblRegH1.getFont().getStyle() | Font.BOLD));
        txtRegH1.setEditable(false);
        txtRegH1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegL1.setFont(lblRegL1.getFont().deriveFont(lblRegL1.getFont().getStyle() | Font.BOLD));
        txtRegL1.setEditable(false);
        txtRegL1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegHL1.setFont(lblRegHL1.getFont().deriveFont(lblRegHL1.getFont().getStyle() | Font.BOLD));
        txtRegHL1.setEditable(false);
        txtRegHL1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegA1.setFont(lblRegA1.getFont().deriveFont(lblRegA1.getFont().getStyle() | Font.BOLD));
        txtRegA1.setEditable(false);
        txtRegA1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegF1.setFont(lblRegF1.getFont().deriveFont(lblRegF1.getFont().getStyle() | Font.BOLD));
        txtRegF1.setEditable(false);
        txtRegF1.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        tblFlags2.setAutoCreateRowSorter(true);
        tblFlags2.setBackground(java.awt.Color.white);
        tblFlags2.setBorder(null);
        tblFlags2.setRowSelectionAllowed(false);

        GroupLayout panelSET2Layout = new GroupLayout(panelSET2);
        panelSET2.setLayout(panelSET2Layout);

        panelSET2Layout.setHorizontalGroup(
                panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(panelSET2Layout.createSequentialGroup().addContainerGap().addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegB1).addComponent(lblRegD1).addComponent(lblRegH1).addComponent(lblRegA1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegB1).addComponent(txtRegD1).addComponent(txtRegH1).addComponent(txtRegA1)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegC1).addComponent(lblRegE1).addComponent(lblRegL1).addComponent(lblRegF1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegC1).addComponent(txtRegE1).addComponent(txtRegL1).addComponent(txtRegF1)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegBC1).addComponent(lblRegDE1).addComponent(lblRegHL1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegBC1).addComponent(txtRegDE1).addComponent(txtRegHL1)).addContainerGap()).addGroup(panelSET2Layout.createSequentialGroup().addContainerGap().addComponent(lblFlags1LBL)).addGroup(panelSET2Layout.createSequentialGroup().addContainerGap().addComponent(tblFlags2).addContainerGap()));
        panelSET2Layout.setVerticalGroup(panelSET2Layout.createSequentialGroup().addContainerGap().addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegB1).addComponent(txtRegB1).addComponent(lblRegC1).addComponent(txtRegC1).addComponent(lblRegBC1).addComponent(txtRegBC1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegD1).addComponent(txtRegD1).addComponent(lblRegE1).addComponent(txtRegE1).addComponent(lblRegDE1).addComponent(txtRegDE1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegH1).addComponent(txtRegH1).addComponent(lblRegL1).addComponent(txtRegL1).addComponent(lblRegHL1).addComponent(txtRegHL1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelSET2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegA1).addComponent(txtRegA1).addComponent(lblRegF1).addComponent(txtRegF1)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(lblFlags1LBL).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(tblFlags2).addContainerGap());

        tabbedGPR.addTab("Set 2", panelSET2);

        lblRegPC.setFont(lblRegPC.getFont().deriveFont(lblRegPC.getFont().getStyle() | Font.BOLD));
        txtRegPC.setEditable(false);
        txtRegPC.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegSP.setFont(lblRegSP.getFont().deriveFont(lblRegSP.getFont().getStyle() | Font.BOLD));
        txtRegSP.setEditable(false);
        txtRegSP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegIX.setFont(lblRegIX.getFont().deriveFont(lblRegIX.getFont().getStyle() | Font.BOLD));
        txtRegIX.setEditable(false);
        txtRegIX.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegI.setFont(lblRegI.getFont().deriveFont(lblRegI.getFont().getStyle() | Font.BOLD));
        txtRegI.setEditable(false);
        txtRegI.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegIY.setFont(lblRegIY.getFont().deriveFont(lblRegIY.getFont().getStyle() | Font.BOLD));
        txtRegIY.setEditable(false);
        txtRegIY.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        lblRegR.setFont(lblRegR.getFont().deriveFont(lblRegR.getFont().getStyle() | Font.BOLD));
        txtRegR.setEditable(false);
        txtRegR.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));

        GroupLayout paneRegistersLayout = new GroupLayout(paneRegisters);
        paneRegisters.setLayout(paneRegistersLayout);
        paneRegistersLayout.setHorizontalGroup(
                paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(paneRegistersLayout.createSequentialGroup().addContainerGap().addComponent(tabbedGPR, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()).addGroup(paneRegistersLayout.createSequentialGroup().addContainerGap().addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegPC).addComponent(lblRegIX).addComponent(lblRegI)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegPC).addComponent(txtRegIX).addComponent(txtRegI)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblRegSP).addComponent(lblRegIY).addComponent(lblRegR)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtRegSP).addComponent(txtRegIY).addComponent(txtRegR)).addContainerGap()));
        paneRegistersLayout.setVerticalGroup(
                paneRegistersLayout.createSequentialGroup().addContainerGap().addComponent(tabbedGPR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegPC).addComponent(txtRegPC).addComponent(lblRegSP).addComponent(txtRegSP)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegIX).addComponent(txtRegIX).addComponent(lblRegIY).addComponent(txtRegIY)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(paneRegistersLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRegI).addComponent(txtRegI).addComponent(lblRegR).addComponent(txtRegR)).addContainerGap());
        panelRun.setBorder(BorderFactory.createTitledBorder(new javax.swing.border.LineBorder(
                Color.gray, 1, true), "Run control", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                panelRun.getFont().deriveFont(Font.BOLD), Color.gray)); // NOI18N

        lblRun.setFont(lblRun.getFont().deriveFont(lblRun.getFont().getStyle() | Font.BOLD));
        lblRun.setForeground(new java.awt.Color(0, 102, 0));
        spnFrequency.setModel(new SpinnerNumberModel(20000, 1, 99999, 100));
        lblKHZ.setFont(lblKHZ.getFont().deriveFont(lblKHZ.getFont().getStyle() | Font.BOLD));
        spnTestPeriode.setModel(new SpinnerNumberModel(50, 1, 10000, 10));
        lblMS.setFont(lblMS.getFont().deriveFont(lblMS.getFont().getStyle() | Font.BOLD));
        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | Font.BOLD));
        lblFrequency.setText("0,0 kHz");

        GroupLayout panelRunLayout = new GroupLayout(panelRun);
        panelRun.setLayout(panelRunLayout);

        panelRunLayout.setHorizontalGroup(
                panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(panelRunLayout.createSequentialGroup().addContainerGap().addComponent(lblRun).addContainerGap()).addGroup(panelRunLayout.createSequentialGroup().addContainerGap().addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblCPUFreq).addComponent(lblTestPeriode)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(spnFrequency).addComponent(spnTestPeriode)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblKHZ).addComponent(lblMS)).addContainerGap()).addGroup(panelRunLayout.createSequentialGroup().addContainerGap().addComponent(lblRuntimeFreq).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(lblFrequency).addContainerGap()));
        panelRunLayout.setVerticalGroup(
                panelRunLayout.createSequentialGroup().addContainerGap().addComponent(lblRun).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.BASELINE, false).addComponent(lblCPUFreq).addComponent(spnFrequency, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE).addComponent(lblKHZ)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.BASELINE, false).addComponent(lblTestPeriode).addComponent(spnTestPeriode, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE).addComponent(lblMS)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelRunLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblRuntimeFreq).addComponent(lblFrequency)).addContainerGap());

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(paneRegisters, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(panelRun, 10, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(paneRegisters, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelRun)));
    }

    JLabel lblFrequency;
    JLabel lblRun;
    JSpinner spnFrequency;
    JSpinner spnTestPeriode;
    JTable tblFlags;
    JTable tblFlags2;
    JTextField txtRegA;
    JTextField txtRegA1;
    JTextField txtRegB;
    JTextField txtRegB1;
    JTextField txtRegBC;
    JTextField txtRegBC1;
    JTextField txtRegC;
    JTextField txtRegC1;
    JTextField txtRegD;
    JTextField txtRegD1;
    JTextField txtRegDE;
    JTextField txtRegDE1;
    JTextField txtRegE;
    JTextField txtRegE1;
    JTextField txtRegF;
    JTextField txtRegF1;
    JTextField txtRegH;
    JTextField txtRegH1;
    JTextField txtRegHL;
    JTextField txtRegHL1;
    JTextField txtRegI;
    JTextField txtRegIX;
    JTextField txtRegIY;
    JTextField txtRegL;
    JTextField txtRegL1;
    JTextField txtRegPC;
    JTextField txtRegR;
    JTextField txtRegSP;
}
