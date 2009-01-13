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
import interfaces.ICPUInstruction;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class statusGUI extends javax.swing.JPanel {
    private IDebugColumn[] columns;
    private stateEnum run_state;
    private CpuZ80 cpu;
    private CpuContext cpuC;
    private IMemoryContext mem = null;
    private AbstractTableModel flagModel1;
    private AbstractTableModel flagModel2;
    //private String regs[] = new String[11];

    // syntax = prefix,size,postfix
    private String[][] mnemoTab = {
        {"nop","0",""      }, {"ld bc,","2",""   }, {"ld (bc),a","0","" }, {"inc bc","0",""    },
        {"inc b","0",""    }, {"dec b","0",""    }, {"ld b,","1",""     }, {"rlca","0",""      },
        {"ex af,af","0","" }, {"add hl,bc","0",""}, {"ld a,(bc)","0","" }, {"dec bc","0",""    },
        {"inc c","0",""    }, {"dec c","0",""    }, {"ld c,","1",""     }, {"rrca","0",""      }, // 0f
        {"djnz","1",""     }, {"ld de,","2",""   }, {"ld (de),a","0","" }, {"inc de","0",""    },
        {"inc d","0",""    }, {"dec d","0",""    }, {"ld d,","1",""     }, {"rla","0",""       },
        {"jr ","1",""      }, {"add hl,de","0",""}, {"ld a,(de)","0","" }, {"dec de","0",""    },
        {"inc e","0",""    }, {"dec e","0",""    }, {"ld e,","1",""     }, {"rra","0",""       }, //1f
        {"jr nz,","1",""   }, {"ld hl,","2",""   }, {"ld (","2","),hl"  }, {"inc hl","0",""    },
        {"inc h","0",""    }, {"dec h","0",""    }, {"ld h,","1",""     }, {"daa","0",""       },
        {"jr z,","1",""    }, {"add hl,hl","0",""}, {"ld hl,(","2",")"  }, {"dec hl","0",""    },
        {"inc l","0",""    }, {"dec l","0",""    }, {"ld l,","1",""     }, {"cpl","0",""       }, //2f
        {"jr nc,","1",""   }, {"ld sp,","2",""   }, {"ld (","2","),a"   }, {"inc sp","0",""    },
        {"inc (hl)","0","" }, {"dec (hl)","0","" }, {"ld (hl),","1",""  }, {"scf","0",""       },
        {"jr c,","1",""    }, {"add hl,sp","0",""}, {"ld a,(","2",")"   }, {"dec sp","0",""    },
        {"inc a","0",""    }, {"dec a","0",""    }, {"ld a,","1",""     }, {"ccf","0",""       }, //3f
        {"ld b,b","0",""   }, {"ld b,c","0",""   }, {"ld b,d","0",""    }, {"ld b,e","0",""    },
        {"ld b,h","0",""   }, {"ld b,l","0",""   }, {"ld b,(hl)","0","" }, {"ld b,a","0",""    },
        {"ld c,b","0",""   }, {"ld c,c","0",""   }, {"ld c,d","0",""    }, {"ld c,e","0",""    },
        {"ld c,h","0",""   }, {"ld c,l","0",""   }, {"ld c,(hl)","0","" }, {"ld c,a","0",""    }, //4f
        {"ld d,b","0",""   }, {"ld d,c","0",""   }, {"ld d,d","0",""    }, {"ld d,e","0",""    },
        {"ld d,h","0",""   }, {"ld d,l","0",""   }, {"ld d,(hl)","0","" }, {"ld d,a","0",""    },
        {"ld e,b","0",""   }, {"ld e,c","0",""   }, {"ld e,d","0",""    }, {"ld e,e","0",""    },
        {"ld e,h","0",""   }, {"ld e,l","0",""   }, {"ld e,(hl)","0","" }, {"ld e,a","0",""    }, //5f
        {"ld h,b","0",""   }, {"ld h,c","0",""   }, {"ld h,d","0",""    }, {"ld h,e","0",""    },
        {"ld h,h","0",""   }, {"ld h,l","0",""   }, {"ld h,(hl)","0","" }, {"ld h,a","0",""    },
        {"ld l,b","0",""   }, {"ld l,c","0",""   }, {"ld l,d","0",""    }, {"ld l,e","0",""    },
        {"ld l,h","0",""   }, {"ld l,l","0",""   }, {"ld l,(hl)","0","" }, {"ld l,a","0",""    }, //6f
        {"ld (hl),b","0",""}, {"ld (hl),c","0",""}, {"ld (hl),d","0","" }, {"ld (hl),e","0","" },
        {"ld (hl),h","0",""}, {"ld (hl),l","0",""}, {"halt","0",""      }, {"ld (hl),a","0","" },
        {"ld a,b","0",""   }, {"ld a,c","0",""   }, {"ld a,d","0",""    }, {"ld a,e","0",""    },
        {"ld a,h","0",""   }, {"ld a,l","0",""   }, {"ld a,(hl)","0","" }, {"ld a,a","0",""    }, //7f
        {"add a,b","0",""  }, {"add a,c","0",""  }, {"add a,d","0",""   }, {"add a,e","0",""   },
        {"add a,h","0",""  }, {"add a,l","0",""  }, {"add a,(hl)","0",""}, {"add a,a","0",""   },
        {"adc a,b","0",""  }, {"adc a,c","0",""  }, {"adc a,d","0",""   }, {"adc a,e","0",""   },
        {"adc a,h","0",""  }, {"adc a,l","0",""  }, {"adc a,(hl)","0",""}, {"adc a,a","0",""   }, //8f
        {"sub b","0",""    }, {"sub c","0",""    }, {"sub d","0",""     }, {"sub e","0",""     },
        {"sub h","0",""    }, {"sub l","0",""    }, {"sub (hl)","0",""  }, {"sub a","0",""     },
        {"sbc b","0",""    }, {"sbc c","0",""    }, {"sbc d","0",""     }, {"sbc e","0",""     },
        {"sbc h","0",""    }, {"sbc l","0",""    }, {"sbc (hl)","0",""  }, {"sbc a","0",""     }, //9f
        {"and b","0",""    }, {"and c","0",""    }, {"and d","0",""     }, {"and e","0",""     },
        {"and h","0",""    }, {"and l","0",""    }, {"and (hl)","0",""  }, {"and a","0",""     },
        {"xor b","0",""    }, {"xor c","0",""    }, {"xor d","0",""     }, {"xor e","0",""     },
        {"xor h","0",""    }, {"xor l","0",""    }, {"xor (hl)","0",""  }, {"xor a","0",""     }, //af
        {"or b","0",""     }, {"or c","0",""     }, {"or d","0",""      }, {"or e","0",""      },
        {"or h","0",""     }, {"or l","0",""     }, {"or (hl)","0",""   }, {"or a","0",""      },
        {"cp b","0",""     }, {"cp c","0",""     }, {"cp d","0",""      }, {"cp e","0",""      },
        {"cp h","0",""     }, {"cp l","0",""     }, {"cp (hl)","0",""   }, {"cp a","0",""      }, //bf
        {"ret nz","0",""   }, {"pop bc","0",""   }, {"jp nz,","2",""    }, {"jp ","2",""       },
        {"call nz,","2","" }, {"push bc","0",""  }, {"add a,","1",""    }, {"rst 0","0",""     },
        {"ret z","0",""    }, {"ret","0",""      }, {"jp z,","2",""     },
        null, // cb
        {"call z,","2",""  }, {"call ","2",""    }, {"adc a,","1",""    }, {"rst 08","0",""    }, //cf
        {"ret nc","0",""   }, {"pop de","0",""   }, {"jp nc,","2",""    }, {"out (","1","),a"  },
        {"call nc,","2","" }, {"push de","0",""  }, {"sub ","1",""      }, {"rst 10","0",""    },
        {"ret c","0",""    }, {"exx","0",""      }, {"jp c,","2",""     }, {"in a,(","1",")"   },
        {"call c,","2",""  },
        null, // dd
        {"sbc a,","1",""   }, {"rst 18","0",""   }, //df
        {"ret po","0",""   }, {"pop hl","0",""   }, {"jp po,","2",""    }, {"ex (sp),hl","0",""},
        {"call po,","2","" }, {"push hl","0",""  }, {"and ","1",""      }, {"rst 20","0",""    },
        {"ret pe","0",""   }, {"jp (hl)","0",""  }, {"jp pe,","2",""    }, {"ex de,hl","0",""  },
        {"call pe,","2","" },
        null, // ed
        {"xor ","1",""     }, {"rst 28","0",""   }, // ef
        {"ret p","0",""    }, {"pop af","0",""   }, {"jp p,","2",""     }, {"di","0",""        },
        {"call p,","2",""  }, {"push af","0",""  }, {"or ","1",""       }, {"rst 30","0",""    },
        {"ret m","0",""    }, {"ld sp,hl","0","" }, {"jp m,","2",""     }, {"ei","0",""        },
        {"call m,","2",""  },
        null, // fd
        {"cp ","1",""      }, {"rst 38","0",""} // ff
    };

    private String[] mnemoTabCB = {
        "rlc b", "rlc c", "rlc d", "rlc e","rlc h", "rlc l", "rlc (hl)", "rlc a",
        "rrc b", "rrc c", "rrc d", "rrc e","rrc h", "rrc l", "rrc (hl)", "rrc a", // 0f
        "rl b", "rl c", "rl d", "rl e","rl h", "rl l", "rl (hl)", "rl a",
        "rr b", "rr c", "rr d", "rr e","rr h", "rr l", "rr (hl)", "rr a", // 1f
        "sla b", "sla c", "sla d", "sla e","sla h", "sla l", "sla (hl)", "sla a",
        "sra b", "sra c", "sra d", "sra e","sra h", "sra l", "sra (hl)", "sra a", // 2f
        "sll b", "sll c", "sll d", "sll e","sll h", "sll l", "sll (hl)", "sll a",
        "srl b", "srl c", "srl d", "srl e","srl h", "srl l", "srl (hl)", "srl a", // 3f
        "bit 0,b", "bit 0,c", "bit 0,d", "bit 0,e","bit 0,h", "bit 0,l", "bin 0,(hl)", "bit 0,a",
        "bit 1,b", "bit 1,c", "bit 1,d", "bit 1,e","bit 1,h", "bit 1,l", "bit 1,(hl)", "bit 1,a", // 4f
        "bit 2,b","bit 2,c","bit 2,d","bit 2,e","bit 2,h","bit 2,l","bit 2,(hl)","bit 2,a",
        "bit 3,b","bit 3,c","bit 3,d","bit 3,e","bit 3,h","bit 3,l","bit 3,(hl)","bit 3,a", // 5f
        "bit 4,b","bit 4,c","bit 4,d","bit 4,e","bit 4,h","bit 4,l","bit 4,(hl)","bit 4,a",
        "bit 5,b","bit 5,c","bit 5,d","bit 5,e","bit 5,h","bit 5,l","bit 5,(hl)","bit 5,a", // 6f
        "bit 6,b","bit 6,c","bit 6,d","bit 6,e","bit 6,h","bit 6,l","bit 6,(hl)","bit 6,a",
        "bit 7,b","bit 7,c","bit 7,d","bit 7,e","bit 7,h","bit 7,l","bit 7,(hl)","bit 7,a", // 7f
        "res 0,b","res 0,c","res 0,d","res 0,e","res 0,h","res 0,l","res 0,(hl)","res 0,a",
        "res 1,b","res 1,c","res 1,d","res 1,e","res 1,h","res 1,l","res 1,(hl)","res 1,a", // 8f
        "res 2,b","res 2,c","res 2,d","res 2,e","res 2,h","res 2,l","res 2,(hl)","res 2,a",
        "res 3,b","res 3,c","res 3,d","res 3,e","res 3,h","res 3,l","res 3,(hl)","res 3,a", // 9f
        "res 4,b","res 4,c","res 4,d","res 4,e","res 4,h","res 4,l","res 4,(hl)","res 4,a",
        "res 5,b","res 5,c","res 5,d","res 5,e","res 5,h","res 5,l","res 5,(hl)","res 5,a", // af
        "res 6,b","res 6,c","res 6,d","res 6,e","res 6,h","res 6,l","res 6,(hl)","res 6,a",
        "res 7,b","res 7,c","res 7,d","res 7,e","res 7,h","res 7,l","res 7,(hl)","res 7,a", // bf
        "set 0,b","set 0,c","set 0,d","set 0,e","set 0,h","set 0,l","set 0,(hl)","set 0,a",
        "set 1,b","set 1,c","set 1,d","set 1,e","set 1,h","set 1,l","set 1,(hl)","set 1,a", // cf
        "set 2,b","set 2,c","set 2,d","set 2,e","set 2,h","set 2,l","set 2,(hl)","set 2,a",
        "set 3,b","set 3,c","set 3,d","set 3,e","set 3,h","set 3,l","set 3,(hl)","set 3,a", // df
        "set 4,b","set 4,c","set 4,d","set 4,e","set 4,h","set 4,l","set 4,(hl)","set 4,a",
        "set 5,b","set 5,c","set 5,d","set 5,e","set 5,h","set 5,l","set 5,(hl)","set 5,a", // ef
        "set 6,b","set 6,c","set 6,d","set 6,e","set 6,h","set 6,l","set 6,(hl)","set 6,a",
        "set 7,b","set 7,c","set 7,d","set 7,e","set 7,h","set 7,l","set 7,(hl)","set 7,a" // ff
    };
    
    private String mnemoTabDD[][] = {
        null,null,null,null,null,null,null,null,null, {"add ix,bc","0",""}, null,
        null,null,null,null,null, // 0f
        null,null,null,null,null,null,null,null,null, {"add ix,de","0",""}, null,
        null,null,null,null,null, // 1f
        null,{"ld ix,","2",""},{"ld (","2","),ix"},{"inc ix","0",""},null,null,
        null,null,null,{"add ix,ix","0",""},{"ld ix,(","2",")"},{"dec ix","0",""},
        null,null,null,null, // 2f
        null,null,null,null,{"inc (ix+","1",")"},{"dec (ix+","1",")"},null, //36
        null,null,{"add ix,sp","0",""},null,null,null,null,null,null, // 3f
        null,null,null,null,null,null,{"ld b,(ix+","1",")"},null,null,null,null,
        null,null,null,{"ld c,(ix+","1",")"},null, // 4f
        null,null,null,null,null,null,{"ld d,(ix+","1",")"},null,null,null,null,
        null,null,null,{"ld e,(ix+","1",")"},null, // 5f
        null,null,null,null,null,null,{"ld h,(ix+","1",")"},null,null,null,null,
        null,null,null,{"ld l,(ix+","1",")"},null, // 6f
        {"ld (ix+","1","),b"},{"ld (ix+","1","),c"},{"ld (ix+","1","),d"},{"ld (ix+","1","),e"},
        {"ld (ix+","1","),h"},{"ld (ix+","1","),l"},null,{"ld (ix+","1","),a"},
        null,null,null,null,null,null,{"ld a,(ix+","1",")"},null, // 7f
        null,null,null,null,null,null,{"add a,(ix+","1",")"},null,null,null,null,
        null,null,null,{"adc a,(ix+","1",")"},null, // 8f
        null,null,null,null,null,null,{"sub (ix+","1",")"},null,null,null,null,
        null,null,null,{"sbc a,(ix+","1",")"},null, // 9f
        null,null,null,null,null,null,{"and (ix+","1",")"},null,null,null,null,
        null,null,null,{"xor (ix+","1",")"},null, // af
        null,null,null,null,null,null,{"or (ix+","1",")"},null,null,null,null,
        null,null,null,{"cp (ix+","1",")"},null, // bf
        null,null,null,null,null,null,null,null,null,null,null,null, // cb
        null,null,null,null, // cf
        null,null,null,null,null,null,null,null,null,null,null,null,null,null,
        null,null, // df
        null,{"pop ix","0",""},null,{"ex (sp),ix","0",""},null,{"push ix","0",""},
        null,null,null,{"jp (ix)","0",""},null,null,null,null,null,null, // ef
        null,null,null,null,null,null,null,null,null,{"ld sp,ix","0",""},null,
        null,null,null,null,null // ff
    };

    private String mnemoTabFD[][] = {
        null,null,null,null,null,null,null,null,null, {"add iy,bc","0",""}, null,
        null,null,null,null,null, // 0f
        null,null,null,null,null,null,null,null,null, {"add iy,de","0",""}, null,
        null,null,null,null,null, // 1f
        null,{"ld iy,","2",""},{"ld (","2","),iy"},{"inc iy","0",""},null,null,
        null,null,null,
        {"add iy,iy","0",""},{"ld iy,(","2",")"},{"dec iy","0",""},null,null,null,
        null, // 2f
        null,null,null,null,{"inc (iy+","1",")"},{"dec (iy+","1",")"},null, //36
        null,null,{"add iy,sp","0",""},null,null,null,null,null,null, // 3f
        null,null,null,null,null,null,{"ld b,(iy+","1",")"},null,null,null,null,
        null,null,null,{"ld c,(iy+","1",")"},null, // 4f
        null,null,null,null,null,null,{"ld d,(iy+","1",")"},null,null,null,null,
        null,null,null,{"ld e,(iy+","1",")"},null, // 5f
        null,null,null,null,null,null,{"ld h,(iy+","1",")"},null,null,null,null,
        null,null,null,{"ld l,(iy+","1",")"},null, // 6f
        {"ld (iy+","1","),b"},{"ld (iy+","1","),c"},{"ld (iy+","1","),d"},{"ld (iy+","1","),e"},
        {"ld (iy+","1","),h"},{"ld (iy+","1","),l"},null,{"ld (iy+","1","),a"},
        null,null,null,null,null,null,{"ld a,(iy+","1",")"},null, // 7f
        null,null,null,null,null,null,{"add a,(iy+","1",")"},null,null,null,null,
        null,null,null,{"adc a,(iy+","1",")"},null, // 8f
        null,null,null,null,null,null,{"sub (iy+","1",")"},null,null,null,null,
        null,null,null,{"sbc a,(iy+","1",")"},null, // 9f
        null,null,null,null,null,null,{"and (iy+","1",")"},null,null,null,null,
        null,null,null,{"xor (iy+","1",")"},null, // af
        null,null,null,null,null,null,{"or (iy+","1",")"},null,null,null,null,
        null,null,null,{"cp (iy+","1",")"},null, // bf
        null,null,null,null,null,null,null,null,null,null,null,null, // cb
        null,null,null,null, // cf
        null,null,null,null,null,null,null,null,null,null,null,null,null,null,
        null,null, // df
        null,{"pop iy","0",""},null,{"ex (sp),iy","0",""},null,{"push iy","0",""},
        null,null,null,{"jp (iy)","0",""},null,null,null,null,null,null, // ef
        null,null,null,null,null,null,null,null,null,{"ld sp,iy","0",""},null,
        null,null,null,null,null // ff
    };
    
    private class FlagsModel extends AbstractTableModel {
        private String[] flags = {"S","Z","H","P/V","N","C"};
        private int[] flagsI = {0,0,0,0,0,0};
        private int set;
        
        public FlagsModel(int set) { this.set = set; }
        public int getRowCount() { return 2; }
        public int getColumnCount() { return 6; }
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
            short F = 0;
            if (set == 0) F = cpu.getF();
            else F = cpu.getF_S();
            flagsI[0] = ((F&CpuZ80.flagS)!=0) ? 1 : 0;
            flagsI[1] = ((F&CpuZ80.flagZ)!=0) ? 1 : 0;
            flagsI[2] = ((F&CpuZ80.flagH)!=0) ? 1 : 0;
            flagsI[3] = ((F&CpuZ80.flagPV)!=0) ? 1 : 0;
            flagsI[4] = ((F&CpuZ80.flagN)!=0) ? 1 : 0;
            flagsI[5] = ((F&CpuZ80.flagC)!=0) ? 1 : 0;
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
    
    private String f(int what) {
        return String.format("%04X", what);
    }
    
    private String f(short what) {
        return String.format("%02X", what);
    }
    
    public void updateGUI() {
        txtRegA.setText(f(cpu.getA())); txtRegF.setText(f(cpu.getF()));
        txtRegB.setText(f(cpu.getB())); txtRegC.setText(f(cpu.getC()));
        txtRegBC.setText(f(cpu.getBC())); txtRegD.setText(f(cpu.getD()));
        txtRegE.setText(f(cpu.getE())); txtRegDE.setText(f(cpu.getDE()));
        txtRegH.setText(f(cpu.getH())); txtRegL.setText(f(cpu.getL()));
        txtRegHL.setText(f(cpu.getHL()));
        flagModel1.fireTableDataChanged();
        txtRegA1.setText(f(cpu.getA_S())); txtRegF1.setText(f(cpu.getF_S()));
        txtRegB1.setText(f(cpu.getB_S())); txtRegC1.setText(f(cpu.getC_S()));
        txtRegBC1.setText(f(cpu.getBC_S())); txtRegD1.setText(f(cpu.getD_S()));
        txtRegE1.setText(f(cpu.getE_S())); txtRegDE1.setText(f(cpu.getDE_S()));
        txtRegH1.setText(f(cpu.getH_S())); txtRegL1.setText(f(cpu.getL_S()));
        txtRegHL1.setText(f(cpu.getHL_S()));
        flagModel2.fireTableDataChanged();
        
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

    public ICPUInstruction cpuDecode(int memPos) {
        short val;
        int tmp;
        int actPos = memPos;
        ICPUInstruction instr;
        String mnemo, oper;
        
        if (this.mem == null) return null;
        val = ((Short)mem.read(actPos++)).shortValue();
        oper = String.format("%02X",val);
        
        mnemo = null;
        switch (val) {
            case 0xCB:
                val = (Short)mem.read(actPos++);
                oper += String.format(" %02X", val);
                mnemo = mnemoTabCB[val]; break;
            case 0xDD:
                val = ((Short)mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val); tmp = 0;
                switch (val) {
                    case 0xCB:
                        tmp = (Integer)mem.readWord(actPos); actPos += 2;
                        val = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
                        oper += String.format(" %02X %02X", tmp, val);
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
                        } break;
                    case 0x36:
                        tmp = (Integer)mem.readWord(actPos); actPos += 2;
                        oper += String.format(" %02X %02X",(tmp>>>8)&0xff,tmp&0xff);
                        mnemo = "ld (ix+" + String.format("%02X", tmp&0xff)
                                +")," + String.format("%02X", (tmp>>>8)&0xff);break;
                    default:
                        if (mnemoTabDD[val] == null) break;
                        else if (mnemoTabDD[val][1].equals("0")) {
                            mnemo = mnemoTabDD[val][0]; break;
                        } else if (mnemoTabDD[val][1].equals("1")) {
                            tmp = (Short)mem.read(actPos++);
                            oper += String.format(" %02X",tmp);
                            mnemo = mnemoTabDD[val][0] + String.format("%02X", tmp)
                                    + mnemoTabDD[val][2];
                            break;
                        } else if (mnemoTabDD[val][1].equals("2")) {
                            tmp = (Integer)mem.readWord(actPos); actPos += 2;
                            oper += String.format(" %02X %02X",tmp&0xff,(tmp>>>8)&0xff);
                            mnemo = mnemoTabDD[val][0] + String.format("%04X", tmp)
                                    + mnemoTabDD[val][2];
                            break;
                        }
                } break;
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
                        case 0x63: mnemo = "ld (" + String.format("%04X", tmp)+"),hl"; break;
                        case 0x6B: mnemo = "ld hl,(" + String.format("%04X", tmp)+")"; break;
                        case 0x73: mnemo = "ld (" + String.format("%04X", tmp)+"),sp"; break;
                        case 0x7B: mnemo = "ld sp,(" + String.format("%04X", tmp)+")"; break;
                    }
                }break;
            case 0xFD:
                val = ((Short)mem.read(actPos++)).shortValue();
                oper += String.format(" %02X", val); tmp = 0;
                switch (val) {
                    case 0xCB:
                        tmp = (Integer)mem.readWord(actPos); actPos += 2;
                        val = (short)((tmp >>> 8)&0xff); tmp &= 0xff;
                        oper += String.format(" %02X %02X", tmp, val);
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
                        } break;
                    case 0x36:
                        tmp = (Integer)mem.readWord(actPos); actPos += 2;
                        oper += String.format(" %02X %02X",(tmp>>>8)&0xff,tmp&0xff);
                        mnemo = "ld (iy+" + String.format("%02X", tmp&0xff)
                                +")," + String.format("%02X", (tmp>>>8)&0xff);break;
                    default:
                        if (mnemoTabFD[val] == null) break;
                        else if (mnemoTabFD[val][1].equals("0")) {
                            mnemo = mnemoTabFD[val][0]; break;
                        } else if (mnemoTabFD[val][1].equals("1")) {
                            tmp = (Short)mem.read(actPos++);
                            oper += String.format(" %02X",tmp);
                            mnemo = mnemoTabFD[val][0] + String.format("%02X", tmp)
                                    + mnemoTabFD[val][2];
                            break;
                        } else if (mnemoTabFD[val][1].equals("2")) {
                            tmp = (Integer)mem.readWord(actPos); actPos += 2;
                            oper += String.format(" %02X %02X",tmp&0xff,(tmp>>>8)&0xff);
                            mnemo = mnemoTabFD[val][0] + String.format("%04X", tmp)
                                    + mnemoTabFD[val][2];
                            break;
                        }
                } break;
            default:
                if (mnemoTab[val][1].equals("0")) {
                    mnemo = mnemoTab[val][0]; break;
                } else if (mnemoTab[val][1].equals("1")) {
                    tmp = (Short)mem.read(actPos++);
                    oper += String.format(" %02X",tmp);
                    mnemo = mnemoTab[val][0] + String.format("%02X", tmp)
                            + mnemoTab[val][2];
                    break;
                } else if (mnemoTab[val][1].equals("2")) {
                    tmp = (Integer)mem.readWord(actPos++);
                    oper += String.format(" %02X %02X",tmp&0xff,(tmp>>>8)&0xff);
                    mnemo = mnemoTab[val][0] + String.format("%04X", tmp)
                            + mnemoTab[val][2];
                    break;
                }
        }
        tmp = 0;
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
        short val;
        if (mem == null) return 0;
        val = (Short)mem.read(memPos++);
        switch (val) {
            case 0xCB: return memPos+1;
            case 0xDD:
            case 0xFD:
                val = ((Short)mem.read(memPos++)).shortValue();
                switch (val) {
                    case 0xCB:
                        val = (Short)mem.read(memPos+1); memPos += 2;
                        switch (val) {
                            case 0x06: case 0x0E: case 0x16: case 0x1E: case 0x26:
                            case 0x2E: case 0x36: case 0x3E: case 0x46: case 0x4E:
                            case 0x56: case 0x5E: case 0x66: case 0x6E: case 0x76:
                            case 0x7E: case 0x86: case 0x8E: case 0x96: case 0x9E:
                            case 0xA6: case 0xAE: case 0xB6: case 0xBE: case 0xC6:
                            case 0xCE: case 0xD6: case 0xDE: case 0xE6: case 0xEE:
                            case 0xF6: case 0xFE: return memPos;
                            default: memPos -= 2;
                        } break;
                    case 0x36: return memPos + 2;
                    default:
                        if (mnemoTabDD[val] == null) return memPos;
                        else if (mnemoTabDD[val][1].equals("0")) return memPos;
                        else if (mnemoTabDD[val][1].equals("1")) return memPos+1;
                        else if (mnemoTabDD[val][1].equals("2")) return memPos+2;
                } memPos--; break;
            case 0xED:
                val = ((Short)mem.read(memPos++)).shortValue();
                switch(val) {
                    case 0x40: case 0x41: case 0x42: case 0x44: case 0x45: case 0x46:
                    case 0x47: case 0x48: case 0x49: case 0x4A: case 0x4D: case 0x4F:
                    case 0x50: case 0x51: case 0x52: case 0x56: case 0x57: case 0x58:
                    case 0x59: case 0x5A: case 0x5E: case 0x5F: case 0x60: case 0x61:
                    case 0x62: case 0x67: case 0x68: case 0x69: case 0x6A: case 0x6F:
                    case 0x70: case 0x71: case 0x72: case 0x78: case 0x79: case 0x7A:
                    case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA8: case 0xA9:
                    case 0xAA: case 0xAB: case 0xB0: case 0xB1: case 0xB2: case 0xB3:
                    case 0xB8: case 0xB9: case 0xBA: case 0xBB: return memPos;
                    case 0x43: case 0x4B: case 0x53: case 0x5B: case 0x63: case 0x6B:
                    case 0x73: case 0x7B: return memPos+2;
                    default: memPos--;
                }
            default:
                if (mnemoTab[val][1].equals("0")) return memPos;
                else if (mnemoTab[val][1].equals("1")) return memPos+1;
                else if (mnemoTab[val][1].equals("2")) return memPos+2;
        }
        return memPos;
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
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
