/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ram.gui;

import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class MemoryDialog extends javax.swing.JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryDialog.class);

    private final Dialogs dialogs;

    private final MemoryContextImpl memory;
    private final RAMTableModel tableModel;
    private File lastOpenedFile;

    public MemoryDialog(MemoryContextImpl memory, Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.memory = Objects.requireNonNull(memory);

        initComponents();
        super.setLocationRelativeTo(null);

        tableModel = new RAMTableModel(memory);
        tableProgram.setModel(tableModel);

        refillTable();
        memory.addMemoryListener(new Memory.MemoryListener() {
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

    private void openRAM() {
        File currentDirectory = Objects.requireNonNullElse(lastOpenedFile, new File(System.getProperty("user.dir")));
        dialogs.chooseFile(
            "Load compiled RAM program", "Load", currentDirectory.toPath(), false,
            new FileExtensionsFilter("RAM compiler file", "ro")
        ).ifPresent(path -> {
            lastOpenedFile = path.toFile();
            try {
                memory.deserialize(lastOpenedFile.getAbsolutePath());
                tableModel.fireTableDataChanged();
                refillTable();
            } catch (IOException | ClassNotFoundException e) {
                dialogs.showError("Cannot open file " + lastOpenedFile.getPath() + ". Please see log file for details.");
                LOGGER.error("Could not open file {}", lastOpenedFile, e);
            }
        });
    }

    public final void refillTable() {
        int add = 0, mul = 0, div = 0, sub = 0, load = 0, store = 0,
            jmp = 0, jz = 0, jgtz = 0, halt = 0, read = 0, write = 0;
        int count = 0, i, j;

        j = memory.getSize();
        for (i = 0; i < j; i++) {
            count++;
            switch ((memory.read(i)).getCode()) {
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
        txtInstructionsCount.setText(String.valueOf(count));
        txtLOAD.setText(String.valueOf(load));
        txtSTORE.setText(String.valueOf(store));
        txtREAD.setText(String.valueOf(read));
        txtWRITE.setText(String.valueOf(write));
        txtADD.setText(String.valueOf(add));
        txtSUB.setText(String.valueOf(sub));
        txtMUL.setText(String.valueOf(mul));
        txtDIV.setText(String.valueOf(div));
        txtJMP.setText(String.valueOf(jmp));
        txtJGTZ.setText(String.valueOf(jgtz));
        txtJZ.setText(String.valueOf(jz));
        txtHALT.setText(String.valueOf(halt));

        computeComplexity();
    }

    // TODO: bug: S(n) computation is not always correct
    private void computeComplexity() {
        Map<String, Integer> labels;
        Map<Integer, Integer> levels = new HashMap<>();
        List<Integer> registers = new ArrayList<>();
        int memcompl = 0;

        labels = memory.getSwitchedLabels();
        int j = memory.getSize();
        int i;
        for (i = 0; i < j; i++) {
            levels.put(i, 0);
        }

        // bottom-up cycles search
        for (i = j - 1; i >= 0; i--) {
            RAMInstruction in = memory.read(i);
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
                        && (in.getDirection() != RAMInstruction.Direction.DIRECT)) {
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
                    time = j + n;
                } else {
                    time += "+" + j + n;
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
            txtTimeComplexity.setText("0");
        } else {
            txtTimeComplexity.setText(time);
        }
        txtMemoryComplexity.setText(String.valueOf(memcompl));
    }


    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JToolBar jToolBar1 = new javax.swing.JToolBar();
        JButton btnOpen = new JButton();
        // Variables declaration - do not modify//GEN-BEGIN:variables
        JButton btnClear = new JButton();
        javax.swing.JSplitPane jSplitPane1 = new javax.swing.JSplitPane();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        tableProgram = new javax.swing.JTable();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        txtInstructionsCount = new javax.swing.JTextField();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        txtMUL = new javax.swing.JTextField();
        txtADD = new javax.swing.JTextField();
        txtSUB = new javax.swing.JTextField();
        txtDIV = new javax.swing.JTextField();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel9 = new javax.swing.JLabel();
        txtJMP = new javax.swing.JTextField();
        txtJZ = new javax.swing.JTextField();
        txtJGTZ = new javax.swing.JTextField();
        txtHALT = new javax.swing.JTextField();
        javax.swing.JLabel jLabel10 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel11 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel12 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel13 = new javax.swing.JLabel();
        txtLOAD = new javax.swing.JTextField();
        txtSTORE = new javax.swing.JTextField();
        txtREAD = new javax.swing.JTextField();
        txtWRITE = new javax.swing.JTextField();
        javax.swing.JPanel jPanel4 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel14 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel15 = new javax.swing.JLabel();
        txtTimeComplexity = new javax.swing.JTextField();
        txtMemoryComplexity = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Program memory");

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/ram/gui/document-open.png"))); // NOI18N
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(this::btnOpenActionPerformed);
        jToolBar1.add(btnOpen);

        btnClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/memory/ram/gui/edit-delete.png"))); // NOI18N
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnClear.addActionListener(this::btnClearActionPerformed);
        jToolBar1.add(btnClear);

        jSplitPane1.setDividerLocation(450);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Tape content", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12))); // NOI18N

        tableProgram.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String[]{
                "Address", "Label", "Instruction"
            }
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tableProgram.setGridColor(java.awt.SystemColor.control);
        jScrollPane1.setViewportView(tableProgram);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 439, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Details", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12))); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Statistics", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12))); // NOI18N

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel1.setText("Instructions count:");

        txtInstructionsCount.setEditable(false);
        txtInstructionsCount.setText("0");

        jLabel2.setText("ADD:");

        jLabel3.setText("SUB:");

        jLabel4.setText("MUL:");

        jLabel5.setText("DIV:");

        txtMUL.setEditable(false);
        txtMUL.setText("0");

        txtADD.setEditable(false);
        txtADD.setText("0");

        txtSUB.setEditable(false);
        txtSUB.setText("0");

        txtDIV.setEditable(false);
        txtDIV.setText("0");

        jLabel6.setText("JMP:");

        jLabel7.setText("JZ:");

        jLabel8.setText("JGTZ:");

        jLabel9.setText("HALT:");

        txtJMP.setEditable(false);
        txtJMP.setText("0");

        txtJZ.setEditable(false);
        txtJZ.setText("0");

        txtJGTZ.setEditable(false);
        txtJGTZ.setText("0");

        txtHALT.setEditable(false);
        txtHALT.setText("0");

        jLabel10.setText("LOAD:");

        jLabel11.setText("STORE:");

        jLabel12.setText("READ:");

        jLabel13.setText("WRITE:");

        txtLOAD.setEditable(false);
        txtLOAD.setText("0");

        txtSTORE.setEditable(false);
        txtSTORE.setText("0");

        txtREAD.setEditable(false);
        txtREAD.setText("0");

        txtWRITE.setEditable(false);
        txtWRITE.setText("0");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(txtInstructionsCount, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(jLabel2)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(txtADD, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel6))
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel3)
                                        .addComponent(jLabel4)
                                        .addComponent(jLabel5))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                            .addComponent(txtMUL, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel8))
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                            .addComponent(txtSUB, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel7))
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                            .addComponent(txtDIV, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jLabel9)))))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(txtJMP, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel10))
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(txtJZ, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel11))
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(txtJGTZ, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel12))
                                .addGroup(jPanel3Layout.createSequentialGroup()
                                    .addComponent(txtHALT, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jLabel13)))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtWRITE, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtREAD, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtSTORE, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(txtLOAD, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGap(30, 30, 30))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(txtInstructionsCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(txtADD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6)
                        .addComponent(txtJMP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10)
                        .addComponent(txtLOAD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(txtSUB, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7)
                        .addComponent(txtJZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11)
                        .addComponent(txtSTORE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(txtMUL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8)
                        .addComponent(txtJGTZ, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel12)
                        .addComponent(txtREAD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel5)
                        .addComponent(txtDIV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9)
                        .addComponent(txtHALT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel13)
                        .addComponent(txtWRITE, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(61, 61, 61))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Uniform Complexity", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", Font.PLAIN, 12))); // NOI18N

        jLabel14.setFont(jLabel14.getFont().deriveFont(jLabel14.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel14.setText("Time T(n):");

        jLabel15.setFont(jLabel15.getFont().deriveFont(jLabel15.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel15.setText("Memory S(n):");

        txtTimeComplexity.setEditable(false);
        txtTimeComplexity.setText("N/A");

        txtMemoryComplexity.setEditable(false);
        txtMemoryComplexity.setText("N/A");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel14)
                        .addComponent(jLabel15))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(txtTimeComplexity)
                        .addComponent(txtMemoryComplexity))
                    .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel14)
                        .addComponent(txtTimeComplexity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel15)
                        .addComponent(txtMemoryComplexity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(134, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jSplitPane1))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        openRAM();
    }//GEN-LAST:event_btnOpenActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        memory.clear();
    }//GEN-LAST:event_btnClearActionPerformed

    private javax.swing.JTable tableProgram;
    private javax.swing.JTextField txtADD;
    private javax.swing.JTextField txtDIV;
    private javax.swing.JTextField txtHALT;
    private javax.swing.JTextField txtInstructionsCount;
    private javax.swing.JTextField txtJGTZ;
    private javax.swing.JTextField txtJMP;
    private javax.swing.JTextField txtJZ;
    private javax.swing.JTextField txtLOAD;
    private javax.swing.JTextField txtMUL;
    private javax.swing.JTextField txtMemoryComplexity;
    private javax.swing.JTextField txtREAD;
    private javax.swing.JTextField txtSTORE;
    private javax.swing.JTextField txtSUB;
    private javax.swing.JTextField txtTimeComplexity;
    private javax.swing.JTextField txtWRITE;
    // End of variables declaration//GEN-END:variables
}
