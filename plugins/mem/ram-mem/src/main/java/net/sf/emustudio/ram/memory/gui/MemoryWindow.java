/*
 * MemoryWindow.java
 *
 * Copyright (C) 2009-2013 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.ram.memory.gui;

import emulib.plugins.memory.Memory.MemoryListener;
import emulib.runtime.StaticDialogs;
import emulib.runtime.UniversalFileFilter;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.impl.RAMMemoryContextImpl;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MemoryWindow extends JFrame {
    private RAMMemoryContextImpl memory;
    private RAMTableModel ram;

    public MemoryWindow(RAMMemoryContextImpl mem) {
        this.memory = mem;
        initComponents();
        setLocationRelativeTo(null);
        ram = new RAMTableModel(mem);
        tableProgram.setModel(ram);
        refillTable();
        mem.addMemoryListener(new MemoryListener() {
            @Override
            public void memoryChanged(int memoryPosition) {
                refillTable();
            }

            @Override
            public void memorySizeChanged() {
                refillTable();
            }
        });
    }

    // TODO: bug: S(n) computation is not always correct
    private void computeComplexity() {
        Map<String, Integer> labels;
        Map<Integer, Integer> levels = new HashMap<Integer, Integer>();
        List<Integer> registers = new ArrayList<Integer>();
        int memcompl = 0;

        labels = memory.getSwitchedLabels();
        int j = memory.getSize();
        int i;
        for (i = 0; i < j; i++) {
            levels.put(i, 0);
        }

        // bottom-up cycles search
        for (i = j - 1; i >= 0; i--) {
            RAMInstruction in = (RAMInstruction) memory.read(i);
            switch (in.getCode()) {
                case RAMInstruction.JMP:
                case RAMInstruction.JGTZ:
                case RAMInstruction.JZ:
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
                case RAMInstruction.LOAD:
                case RAMInstruction.STORE:
                    if (!registers.contains(0)) {
                        memcompl++;
                        registers.add(0);
                    }
                default:
                    // other instructions has parameters - registers or
                    // direct values
                    if ((in.getCode() != RAMInstruction.HALT)
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
                    keys = levels.keySet().iterator();
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

        j = memory.getSize();
        for (i = 0; i < j; i++) {
            count++;
            switch (((RAMInstruction) memory.read(i)).getCode()) {
                case RAMInstruction.LOAD:
                    load++;
                    break;
                case RAMInstruction.STORE:
                    store++;
                    break;
                case RAMInstruction.READ:
                    read++;
                    break;
                case RAMInstruction.WRITE:
                    write++;
                    break;
                case RAMInstruction.ADD:
                    add++;
                    break;
                case RAMInstruction.SUB:
                    sub++;
                    break;
                case RAMInstruction.MUL:
                    mul++;
                    break;
                case RAMInstruction.DIV:
                    div++;
                    break;
                case RAMInstruction.JMP:
                    jmp++;
                    break;
                case RAMInstruction.JGTZ:
                    jgtz++;
                    break;
                case RAMInstruction.JZ:
                    jz++;
                    break;
                case RAMInstruction.HALT:
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

        computeComplexity();
    }

    private void openRAM() {
        JFileChooser f = new JFileChooser();
        UniversalFileFilter f1 = new UniversalFileFilter();
        UniversalFileFilter f2 = new UniversalFileFilter();

        f1.addExtension("ram");
        f1.setDescription("RAM program file (*.ram)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");

        f.setDialogTitle("Load compiled RAM program");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Load");
        f.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fileSource = f.getSelectedFile();
            if (fileSource.canRead() == true) {
                memory.deserialize(fileSource.getAbsolutePath());
                tableProgram.revalidate();
                tableProgram.repaint();
                refillTable();
            } else {
                StaticDialogs.showErrorMessage("File " + fileSource.getPath() + " can't be read.");
            }
        }
    }

    private void initComponents() {
        toolMemory = new JToolBar();
        btnOpen = new JButton();
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

        btnOpen.setIcon(new ImageIcon(getClass().getResource("/net/sf/emustudio/ram/memory/gui/document-open.png"))); // NOI18N
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnOpen.setToolTipText("Open compiled RAM program");
        btnOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openRAM();
            }
        });

        btnClear.setIcon(new ImageIcon(getClass().getResource("/net/sf/emustudio/ram/memory/gui/edit-delete.png"))); // NOI18N
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnClear.setToolTipText("Clear program tape");
        btnClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                memory.clear();
            }
        });
        toolMemory.add(btnOpen);
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

    private JButton btnOpen;
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
