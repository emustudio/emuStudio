/**
 * MemoryWindow.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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
package RAMmemory.gui;

import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

import emuLib8.plugins.memory.IMemory.IMemListener;

import RAMmemory.impl.RAMContext;
import java.util.Iterator;

@SuppressWarnings("serial")
public class MemoryWindow extends JFrame {

    private RAMContext mem;
    private RAMTableModel ram;

    private class RAMTableModel extends AbstractTableModel {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return mem.getSize();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                String label = mem.getLabel(rowIndex);
                if (label != null) {
                    return String.valueOf(rowIndex) + " (" + label + ")";
                } else {
                    return String.valueOf(rowIndex);
                }
            } else {
                C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E i =
                        (C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) mem.read(rowIndex);
                return i.getCodeStr() + " " + i.getOperandStr();
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int col) {
            return (col == 0) ? "Addr" : "Instruction";
        }
    }

    public MemoryWindow(RAMContext mem) {
        this.mem = mem;
        initComponents();
        setLocationRelativeTo(null);
        ram = new RAMTableModel();
        tableProgram.setModel(ram);
        refillTable();
        mem.addMemoryListener(new IMemListener() {

            @Override
            public void memChange(EventObject evt, int pos) {
                refillTable();
            }
        });
    }

    // TODO: bug: S(n) computation is not always correct
    private void setComplexity() {
        HashMap<String, Integer> labels;
        HashMap<Integer, Integer> levels = new HashMap<Integer, Integer>();
        ArrayList<Integer> registers = new ArrayList<Integer>();
        int memcompl = 0;

        labels = mem.getSwitchedLabels();
        int j = mem.getSize();
        int i;
        for (i = 0; i < j; i++) {
            levels.put(i, 0);
        }

        // bottom-up cycles search
        for (i = j - 1; i >= 0; i--) {
            C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E in =
                    (C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) mem.read(i);
            switch (in.getCode()) {
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JMP:
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JGTZ:
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JZ:
                    String lab = in.getOperandLabel();
                    Integer pos = labels.get(lab.toUpperCase() + ":");
                    if (pos != null && (pos <= i)) {
                        // ak je definicia labelu vyssie ako jump (cyklus)
                        // pripocitaj 1 ku levelu vsetkych instrukcii v
                        // rozpati <pos;i>
                        for (int n = pos; n <= i; n++) {
                            int old = levels.get(n);
                            levels.put(n, old + 1);
                        }
                    }
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.LOAD:
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.STORE:
                    if (!registers.contains(0)) {
                        memcompl++;
                        registers.add(0);
                    }
                default:
                    // other instructions has parameters - registers or
                    // direct values
                    if ((in.getCode() != C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.HALT)
                            && (in.getDirection() != '=')) {
                        int operand = (Integer) in.getOperand();
                        if (!registers.contains(operand)) {
                            memcompl++;
                            registers.add(operand);
                        }
                    }
            }
        }
        // count levels and make string
        i = 0;
        j = 0;
        String time = null;
        String n = "";
        boolean was = false;
        while (true) {
            Iterator<Integer> keys = levels.keySet().iterator();
            while (keys.hasNext()) {
                Integer pos = keys.next();
                if (levels.get(pos) == i) {
                    was = true;
                    j++;
                    levels.remove(pos);
                }
            }
            if (was) {
                if (time == null) {
                    time = String.valueOf(j) + n;
                } else {
                    time += "+" + String.valueOf(j) + n;
                }
                n += "*n";
                i++;
                j = 0;
                was = false;
            } else {
                break;
            }
        }
        if (time == null || time.equals("")) {
            lblTime.setText("0");
        } else {
            lblTime.setText(time);
        }
        lblMemory.setText(String.valueOf(memcompl));
    }

    public final void refillTable() {
        int add = 0, mul = 0, div = 0, sub = 0, load = 0, store = 0,
                jmp = 0, jz = 0, jgtz = 0, halt = 0, read = 0, write = 0;
        int count = 0, i, j;

        ram.fireTableDataChanged();

        j = mem.getSize();
        for (i = 0; i < j; i++) {
            count++;
            switch (((C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) mem.read(i)).getCode()) {
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.LOAD:
                    load++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.STORE:
                    store++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.READ:
                    read++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.WRITE:
                    write++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.ADD:
                    add++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.SUB:
                    sub++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.MUL:
                    mul++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.DIV:
                    div++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JMP:
                    jmp++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JGTZ:
                    jgtz++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JZ:
                    jz++;
                    break;
                case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.HALT:
                    halt++;
                    break;
            }
        }
        lblCount.setText(String.valueOf(count));
        lblLOAD.setText(String.valueOf(load));
        lblSTORE.setText(String.valueOf(store));
        lblREAD.setText(String.valueOf(read));
        lblWRITE.setText(String.valueOf(write));
        lblADD.setText(String.valueOf(add));
        lblSUB.setText(String.valueOf(sub));
        lblMUL.setText(String.valueOf(mul));
        lblDIV.setText(String.valueOf(div));
        lblJMP.setText(String.valueOf(jmp));
        lblJGTZ.setText(String.valueOf(jgtz));
        lblJZ.setText(String.valueOf(jz));
        lblHALT.setText(String.valueOf(halt));

        setComplexity();
    }

    public final void initComponents() {
        toolMemory = new JToolBar();
        btnClear = new JButton();
        scrollProgram = new JScrollPane();
        tableProgram = new JTable();
        lblTapeContent = new JLabel("Tape content:");
        panelInfo = new JPanel();
        panelComplexity = new JPanel();
        lblTimeLBL = new JLabel("Time T(n):");
        JLabel lblMemoryLBL = new JLabel("Memory S(n):");
        lblTime = new JLabel("0");
        lblMemory = new JLabel("0");
        panelInstr = new JPanel();
        JLabel lblCountLBL = new JLabel("Instructions count:");
        lblCount = new JLabel("0");
        JLabel lblAddLBL = new JLabel("ADD:");
        JLabel lblSubLBL = new JLabel("SUB:");
        JLabel lblMulLBL = new JLabel("MUL:");
        JLabel lblDivLBL = new JLabel("DIV:");
        lblMUL = new JLabel("0");
        lblSUB = new JLabel("0");
        lblADD = new JLabel("0");
        lblDIV = new JLabel("0");
        JLabel lblJmpLBL = new JLabel("JMP:");
        JLabel lblJzLBL = new JLabel("JZ:");
        JLabel lblJgtzLBL = new JLabel("JGTZ:");
        JLabel lblHaltLBL = new JLabel("HALT:");
        JLabel lblLoadLBL = new JLabel("LOAD:");
        JLabel lblStoreLBL = new JLabel("STORE:");
        JLabel lblReadLBL = new JLabel("READ:");
        JLabel lblWriteLBL = new JLabel("WRITE:");
        lblHALT = new JLabel("0");
        lblJZ = new JLabel("0");
        lblJGTZ = new JLabel("0");
        lblJMP = new JLabel("0");
        lblSTORE = new JLabel("0");
        lblLOAD = new JLabel("0");
        lblWRITE = new JLabel("0");
        lblREAD = new JLabel("0");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Program tape for RAM");

        toolMemory.setFloatable(false);
        toolMemory.setRollover(true);

        tableProgram.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnClear.setIcon(new ImageIcon(getClass().getResource("/RAMmemory/resources/edit-delete.png"))); // NOI18N
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnClear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                mem.clearMemory();
            }
        });
        toolMemory.add(btnClear);

        scrollProgram.setViewportView(tableProgram);

        panelInfo.setBorder(BorderFactory.createTitledBorder("Instructions Info"));
        panelComplexity.setBorder(BorderFactory.createTitledBorder("Uniform Complexity"));
        lblTime.setFont(lblTime.getFont().deriveFont(lblTime.getFont().getStyle() | java.awt.Font.BOLD));
        lblMemory.setFont(lblMemory.getFont().deriveFont(lblMemory.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout panelComplLayout = new GroupLayout(panelComplexity);
        panelComplexity.setLayout(panelComplLayout);
        panelComplLayout.setHorizontalGroup(
                panelComplLayout.createSequentialGroup().addContainerGap().addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblMemoryLBL).addComponent(lblTimeLBL)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblMemory).addComponent(lblTime)).addContainerGap(134, Short.MAX_VALUE));

        panelComplLayout.setVerticalGroup(
                panelComplLayout.createSequentialGroup().addContainerGap().addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblTimeLBL).addComponent(lblTime)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblMemoryLBL).addComponent(lblMemory)).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        panelInstr.setBorder(BorderFactory.createTitledBorder("Instructions usage"));

        lblCount.setFont(lblCount.getFont().deriveFont(lblCount.getFont().getStyle() | java.awt.Font.BOLD));
        lblMUL.setFont(lblMUL.getFont().deriveFont(lblMUL.getFont().getStyle() | java.awt.Font.BOLD));
        lblSUB.setFont(lblSUB.getFont().deriveFont(lblSUB.getFont().getStyle() | java.awt.Font.BOLD));
        lblADD.setFont(lblADD.getFont().deriveFont(lblADD.getFont().getStyle() | java.awt.Font.BOLD));
        lblDIV.setFont(lblDIV.getFont().deriveFont(lblDIV.getFont().getStyle() | java.awt.Font.BOLD));

        lblHALT.setFont(lblHALT.getFont().deriveFont(lblHALT.getFont().getStyle() | java.awt.Font.BOLD));
        lblJZ.setFont(lblJZ.getFont().deriveFont(lblJZ.getFont().getStyle() | java.awt.Font.BOLD));
        lblJGTZ.setFont(lblJGTZ.getFont().deriveFont(lblJGTZ.getFont().getStyle() | java.awt.Font.BOLD));
        lblJMP.setFont(lblJMP.getFont().deriveFont(lblJMP.getFont().getStyle() | java.awt.Font.BOLD));
        lblSTORE.setFont(lblSTORE.getFont().deriveFont(lblSTORE.getFont().getStyle() | java.awt.Font.BOLD));
        lblLOAD.setFont(lblLOAD.getFont().deriveFont(lblLOAD.getFont().getStyle() | java.awt.Font.BOLD));
        lblWRITE.setFont(lblWRITE.getFont().deriveFont(lblWRITE.getFont().getStyle() | java.awt.Font.BOLD));
        lblREAD.setFont(lblREAD.getFont().deriveFont(lblREAD.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout panelInstrLayout = new GroupLayout(panelInstr);
        panelInstr.setLayout(panelInstrLayout);
        panelInstrLayout.setHorizontalGroup(
                panelInstrLayout.createSequentialGroup().addContainerGap().addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(panelInstrLayout.createSequentialGroup().addComponent(lblCountLBL).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(lblCount)).addGroup(panelInstrLayout.createSequentialGroup().addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblAddLBL).addComponent(lblSubLBL).addComponent(lblMulLBL).addComponent(lblDivLBL)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblADD).addComponent(lblSUB).addComponent(lblMUL).addComponent(lblDIV)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblJmpLBL).addComponent(lblJzLBL).addComponent(lblJgtzLBL).addComponent(lblHaltLBL)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblJMP).addComponent(lblJZ).addComponent(lblJGTZ).addComponent(lblHALT)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblLoadLBL).addComponent(lblStoreLBL).addComponent(lblReadLBL).addComponent(lblWriteLBL)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblLOAD).addComponent(lblSTORE).addComponent(lblREAD).addComponent(lblWRITE)))).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        panelInstrLayout.setVerticalGroup(
                panelInstrLayout.createSequentialGroup().addContainerGap().addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblCountLBL).addComponent(lblCount)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblAddLBL).addComponent(lblADD).addComponent(lblJmpLBL).addComponent(lblJMP).addComponent(lblLoadLBL).addComponent(lblLOAD)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblSubLBL).addComponent(lblSUB).addComponent(lblJzLBL).addComponent(lblJZ).addComponent(lblStoreLBL).addComponent(lblSTORE)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblMulLBL).addComponent(lblMUL).addComponent(lblJgtzLBL).addComponent(lblJGTZ).addComponent(lblReadLBL).addComponent(lblREAD)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblDivLBL).addComponent(lblDIV).addComponent(lblHaltLBL).addComponent(lblHALT).addComponent(lblWriteLBL).addComponent(lblWRITE)).addContainerGap());

        GroupLayout panelInfoLayout = new GroupLayout(panelInfo);
        panelInfo.setLayout(panelInfoLayout);
        panelInfoLayout.setHorizontalGroup(
                panelInfoLayout.createSequentialGroup().addContainerGap().addGroup(panelInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(panelInstr).addComponent(panelComplexity)).addContainerGap());

        panelInfoLayout.setVerticalGroup(
                panelInfoLayout.createSequentialGroup().addContainerGap().addComponent(panelInstr).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelComplexity).addContainerGap());

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(toolMemory, GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(lblTapeContent).addContainerGap(444, Short.MAX_VALUE)).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(scrollProgram, GroupLayout.PREFERRED_SIZE, 233, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(panelInfo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(toolMemory, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(lblTapeContent).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(panelInfo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(scrollProgram, GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)).addContainerGap()));

        pack();
    }
    private JButton btnClear;
    private JLabel lblHALT;
    private JLabel lblJZ;
    private JLabel lblJGTZ;
    private JLabel lblJMP;
    private JLabel lblSTORE;
    private JLabel lblLOAD;
    private JLabel lblTimeLBL;
    private JLabel lblWRITE;
    private JLabel lblREAD;
    private JPanel panelComplexity;
    private JPanel panelInstr;
    private JScrollPane scrollProgram;
    private JLabel lblADD;
    private JLabel lblCount;
    private JLabel lblDIV;
    private JLabel lblTime;
    private JLabel lblMUL;
    private JLabel lblSUB;
    private JLabel lblMemory;
    private JLabel lblTapeContent;
    private JTable tableProgram;
    private JPanel panelInfo;
    private JToolBar toolMemory;
}
