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
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.brainduck.CpuImpl;
import net.emustudio.plugins.cpu.brainduck.EmulatorEngine;
import net.emustudio.plugins.memory.brainduck.api.RawMemoryContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import static net.emustudio.plugins.cpu.brainduck.gui.Constants.MONOSPACED_PLAIN;

public class StatusPanel extends JPanel {
    private final ColumnsRepainter columnsRepainter = new ColumnsRepainter();
    private final MemoryTableModel tableModel;
    private final Byte[] memory;
    private final EmulatorEngine cpu;

    private class CPUStatusListener implements CPU.CPUListener {
        private volatile long nanoStartTime;

        @Override
        public void runStateChanged(CPU.RunState state) {
            switch (state) {
                case STATE_RUNNING:
                    lblRunState.setText("running");
                    lblTime.setText("N/A");
                    nanoStartTime = System.nanoTime();
                    break;
                case STATE_STOPPED_NORMAL:
                    lblRunState.setText("stopped (normal)");
                    break;
                case STATE_STOPPED_BREAK:
                    lblRunState.setText("breakpoint");
                    break;
                case STATE_STOPPED_ADDR_FALLOUT:
                    lblRunState.setText("stopped (address fallout)");
                    break;
                case STATE_STOPPED_BAD_INSTR:
                    lblRunState.setText("stopped (instruction fallout)");
                    break;
            }
            long tmpNanoTime = 0;
            if (state != CPU.RunState.STATE_RUNNING && nanoStartTime != 0) {
                tmpNanoTime = System.nanoTime() - nanoStartTime;
                nanoStartTime = 0;
            }
            lblTime.setText(String.format("%.2f ms", (double) tmpNanoTime / 1000000.0));
        }

        @Override
        public void internalStateChanged() {
            int P = cpu.getP();

            txtP.setText(String.format("%04X", P));
            txtIP.setText(String.format("%04X", cpu.IP));
            lblLoopLevel.setText(String.valueOf(cpu.getLoopLevel()));
            try {
                txtMemP.setText(String.format("%02X", memory[P] & 0xFF));
            } catch (ArrayIndexOutOfBoundsException e) {
                txtMemP.setText("[unreachable]");
            } finally {
                tableModel.setP(P);
                columnsRepainter.repaint(tblMemory);
                tblMemory.revalidate();
                tblMemory.repaint();
            }
        }

    }

    public StatusPanel(RawMemoryContext memory, CpuImpl cpu) {
        this.memory = memory.getRawMemory();
        this.cpu = cpu.getEngine();
        this.tableModel = new MemoryTableModel(memory);

        initComponents();
        tblMemory.setModel(tableModel);
        tblMemory.getTableHeader().setReorderingAllowed(false);
        columnsRepainter.setMainColumn(2, tblMemory);

        cpu.addCPUListener(new CPUStatusListener());
    }

    private void initComponents() {
        JPanel jPanel1 = new JPanel();
        JLabel jLabel1 = new JLabel();
        JLabel jLabel2 = new JLabel();
        JLabel jLabel3 = new JLabel();
        txtMemP = new JTextField();
        txtP = new JTextField();
        txtIP = new JTextField();
        JLabel jLabel4 = new JLabel();
        JLabel jLabel5 = new JLabel();
        JLabel jLabel6 = new JLabel();
        JSeparator jSeparator1 = new JSeparator();
        JLabel jLabel7 = new JLabel();
        lblTime = new JLabel();
        JLabel jLabel8 = new JLabel();
        lblLoopLevel = new JLabel();
        JPanel jPanel2 = new JPanel();
        lblRunState = new JLabel();
        JPanel jPanel3 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        tblMemory = new JTable();

        jPanel1.setBorder(BorderFactory.createTitledBorder("Internal state"));

        jLabel1.setText("IP:");
        jLabel2.setText("P:");
        jLabel3.setText("*P:");

        txtMemP.setEditable(false);
        txtMemP.setFont(MONOSPACED_PLAIN);
        txtMemP.setText("0");

        txtP.setEditable(false);
        txtP.setFont(MONOSPACED_PLAIN);
        txtP.setText("0");

        txtIP.setEditable(false);
        txtIP.setFont(MONOSPACED_PLAIN);
        txtIP.setText("0");

        jLabel4.setText("h");
        jLabel5.setText("h");
        jLabel6.setText("h");

        jLabel7.setText("Execution time:");

        lblTime.setFont(lblTime.getFont().deriveFont(lblTime.getFont().getStyle() | java.awt.Font.BOLD));
        lblTime.setText("0 ms");

        jLabel8.setText("Loop level:");

        lblLoopLevel.setFont(lblLoopLevel.getFont().deriveFont(lblLoopLevel.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoopLevel.setText("0");

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel3)
                                .addComponent(jLabel2)
                                .addComponent(jLabel1))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(txtIP)
                                .addComponent(txtP)
                                .addComponent(txtMemP))
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel5)
                                        .addContainerGap()))
                                .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel6)
                                    .addContainerGap())))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(jSeparator1)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel7)
                                        .addComponent(jLabel8))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblLoopLevel)
                                        .addComponent(lblTime))
                                    .addGap(0, 0, Short.MAX_VALUE)))
                            .addContainerGap())))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(txtIP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(txtP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(txtMemP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 7, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7)
                        .addComponent(lblTime))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(lblLoopLevel)))
        );

        jPanel2.setBorder(BorderFactory.createTitledBorder("Run state"));

        lblRunState.setFont(lblRunState.getFont().deriveFont(lblRunState.getFont().getStyle() | java.awt.Font.BOLD));
        lblRunState.setHorizontalAlignment(SwingConstants.CENTER);
        lblRunState.setText("stopped (breakpoint)");

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblRunState, GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                    .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblRunState)
                    .addContainerGap(21, Short.MAX_VALUE))
        );

        jPanel3.setBorder(BorderFactory.createTitledBorder("Memory view"));

        jScrollPane1.setBorder(null);

        tblMemory.setFont(MONOSPACED_PLAIN);
        tblMemory.setModel(new DefaultTableModel(
            new Object[][]{
                {null, null, null, null, null}
            },
            new String[]{
                "00", "01", "02", "03", "04"
            }
        ) {
            Class[] types = new Class[]{
                java.lang.Byte.class, java.lang.Byte.class, java.lang.Byte.class, java.lang.Byte.class, java.lang.Byte.class
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        tblMemory.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(tblMemory);

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE)
                    .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    private JLabel lblLoopLevel;
    private JLabel lblRunState;
    private JLabel lblTime;
    private JTable tblMemory;
    private JTextField txtIP;
    private JTextField txtMemP;
    private JTextField txtP;
}
