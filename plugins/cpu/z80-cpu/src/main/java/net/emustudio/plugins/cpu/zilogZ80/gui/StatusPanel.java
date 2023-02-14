/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.zilogZ80.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.CpuImpl;
import net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine;
import net.emustudio.plugins.cpu.zilogZ80.InstructionPrinter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatByteHexString;
import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatWordHexString;
import static net.emustudio.plugins.cpu.zilogZ80.gui.Constants.*;

public class StatusPanel extends JPanel {
    private final CpuImpl cpu;
    private final Context8080 context;
    private final FlagsModel flagModel1;
    private final FlagsModel flagModel2;

    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_NORMAL;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox chkPrintInstructions;
    private JLabel lblFrequency;
    private JLabel lblRunState;
    private JSpinner spnFrequency;
    private JTable tblFlags1;
    private JTable tblFlags2;
    private JTextField txtA1;
    private JTextField txtA2;
    private JTextField txtB1;
    private JTextField txtB2;
    private JTextField txtBC1;
    private JTextField txtBC2;
    private JTextField txtC1;
    private JTextField txtC2;
    private JTextField txtD1;
    private JTextField txtD2;
    private JTextField txtDE1;
    private JTextField txtDE2;
    private JTextField txtE1;
    private JTextField txtE2;
    private JTextField txtF1;
    private JTextField txtF2;
    private JTextField txtH1;
    private JTextField txtH2;
    private JTextField txtHL1;
    private JTextField txtHL2;
    private JTextField txtI;
    private JTextField txtIX;
    private JTextField txtIY;
    private JTextField txtL1;
    private JTextField txtL2;
    private JTextField txtPC;
    private JTextField txtR;
    private JTextField txtSP;
    public StatusPanel(CpuImpl cpu, Context8080 context, boolean dumpInstructions) {
        this.cpu = cpu;
        this.context = context;
        this.flagModel1 = new FlagsModel(0, cpu.getEngine());
        this.flagModel2 = new FlagsModel(1, cpu.getEngine());

        initComponents();
        chkPrintInstructions.setSelected(dumpInstructions);
        tblFlags1.setModel(flagModel1);
        tblFlags2.setModel(flagModel2);

        setupListeners();
    }

    private void setupListeners() {
        cpu.addCPUListener(new CPU.CPUListener() {

            @Override
            public void runStateChanged(CPU.RunState state) {
                runState = state;
            }

            @Override
            public void internalStateChanged() {
                updateGUI();
            }

        });
        cpu.getEngine().addFrequencyChangedListener(newFrequency -> lblFrequency.setText(String.format("%.2f kHz", newFrequency)));
        spnFrequency.addChangeListener(e -> {
            int i = (Integer) spnFrequency.getModel().getValue();
            try {
                context.setCPUFrequency(i);
            } catch (IndexOutOfBoundsException ex) {
                spnFrequency.getModel().setValue(context.getCPUFrequency());
            }
        });
    }

    private String wordHex(int upper, int lower) {
        return formatWordHexString(((upper << 8) | lower) & 0xFFFF);
    }

    private String byteHex(int what) {
        return formatByteHexString(what);
    }

    public void updateGUI() {
        EmulatorEngine engine = cpu.getEngine();
        txtA1.setText(byteHex(engine.regs[EmulatorEngine.REG_A]));
        txtF1.setText(byteHex(engine.flags));
        txtB1.setText(byteHex(engine.regs[EmulatorEngine.REG_B]));
        txtC1.setText(byteHex(engine.regs[EmulatorEngine.REG_C]));
        txtBC1.setText(wordHex(engine.regs[EmulatorEngine.REG_B], engine.regs[EmulatorEngine.REG_C]));
        txtD1.setText(byteHex(engine.regs[EmulatorEngine.REG_D]));
        txtE1.setText(byteHex(engine.regs[EmulatorEngine.REG_E]));
        txtDE1.setText(wordHex(engine.regs[EmulatorEngine.REG_D], engine.regs[EmulatorEngine.REG_E]));
        txtH1.setText(byteHex(engine.regs[EmulatorEngine.REG_H]));
        txtL1.setText(byteHex(engine.regs[EmulatorEngine.REG_L]));
        txtHL1.setText(wordHex(engine.regs[EmulatorEngine.REG_H], engine.regs[EmulatorEngine.REG_L]));
        flagModel1.fireTableDataChanged();
        txtA2.setText(byteHex(engine.regs2[EmulatorEngine.REG_A]));
        txtF2.setText(byteHex(engine.flags2));
        txtB2.setText(byteHex(engine.regs2[EmulatorEngine.REG_B]));
        txtC2.setText(byteHex(engine.regs2[EmulatorEngine.REG_C]));
        txtBC2.setText(wordHex(engine.regs2[EmulatorEngine.REG_B], engine.regs2[EmulatorEngine.REG_C]));
        txtD2.setText(byteHex(engine.regs2[EmulatorEngine.REG_D]));
        txtE2.setText(byteHex(engine.regs2[EmulatorEngine.REG_E]));
        txtDE2.setText(wordHex(engine.regs2[EmulatorEngine.REG_D], engine.regs2[EmulatorEngine.REG_E]));
        txtH2.setText(byteHex(engine.regs2[EmulatorEngine.REG_H]));
        txtL2.setText(byteHex(engine.regs2[EmulatorEngine.REG_L]));
        txtHL2.setText(wordHex(engine.regs2[EmulatorEngine.REG_H], engine.regs2[EmulatorEngine.REG_L]));
        flagModel2.fireTableDataChanged();

        txtSP.setText(formatWordHexString(engine.SP));
        txtPC.setText(formatWordHexString(engine.PC));
        txtIX.setText(formatWordHexString(engine.IX));
        txtIY.setText(formatWordHexString(engine.IY));
        txtI.setText(formatByteHexString(engine.I));
        txtR.setText(formatByteHexString(engine.R));

        lblRunState.setText(runState.toString());
        if (runState == CPU.RunState.STATE_RUNNING) {
            spnFrequency.setEnabled(false);
        } else {
            spnFrequency.setEnabled(true);
        }
    }

    private void initComponents() {
        JTabbedPane jTabbedPane1 = new JTabbedPane();
        JPanel panelSet1 = new JPanel();
        JLabel jLabel13 = new JLabel();
        txtA1 = new JTextField();
        JLabel jLabel14 = new JLabel();
        txtB1 = new JTextField();
        txtF1 = new JTextField();
        JLabel jLabel15 = new JLabel();
        JLabel jLabel16 = new JLabel();
        txtC1 = new JTextField();
        txtD1 = new JTextField();
        JLabel jLabel17 = new JLabel();
        JLabel jLabel18 = new JLabel();
        txtH1 = new JTextField();
        JLabel jLabel19 = new JLabel();
        JLabel jLabel20 = new JLabel();
        txtE1 = new JTextField();
        txtL1 = new JTextField();
        txtHL1 = new JTextField();
        txtDE1 = new JTextField();
        txtBC1 = new JTextField();
        JLabel jLabel21 = new JLabel();
        JLabel jLabel22 = new JLabel();
        JLabel jLabel23 = new JLabel();
        JScrollPane jScrollPane2 = new JScrollPane();
        tblFlags1 = new JTable();
        JLabel jLabel24 = new JLabel();
        JPanel panelSet2 = new JPanel();
        JLabel jLabel1 = new JLabel();
        txtB2 = new JTextField();
        JLabel jLabel2 = new JLabel();
        txtC2 = new JTextField();
        JLabel jLabel3 = new JLabel();
        txtBC2 = new JTextField();
        JLabel jLabel4 = new JLabel();
        txtD2 = new JTextField();
        JLabel jLabel5 = new JLabel();
        txtE2 = new JTextField();
        JLabel jLabel6 = new JLabel();
        txtDE2 = new JTextField();
        JLabel jLabel7 = new JLabel();
        txtH2 = new JTextField();
        JLabel jLabel8 = new JLabel();
        txtL2 = new JTextField();
        JLabel jLabel9 = new JLabel();
        txtHL2 = new JTextField();
        JLabel jLabel10 = new JLabel();
        txtA2 = new JTextField();
        JLabel jLabel11 = new JLabel();
        txtF2 = new JTextField();
        JScrollPane jScrollPane1 = new JScrollPane();
        tblFlags2 = new JTable();
        JLabel jLabel12 = new JLabel();
        JPanel jPanel1 = new JPanel();
        JLabel jLabel26 = new JLabel();
        txtPC = new JTextField();
        JLabel jLabel25 = new JLabel();
        txtSP = new JTextField();
        JLabel jLabel27 = new JLabel();
        txtIX = new JTextField();
        JLabel jLabel28 = new JLabel();
        txtIY = new JTextField();
        JLabel jLabel29 = new JLabel();
        txtI = new JTextField();
        JLabel jLabel30 = new JLabel();
        txtR = new JTextField();
        JPanel jPanel2 = new JPanel();
        lblRunState = new JLabel();
        JSeparator jSeparator1 = new JSeparator();
        JLabel jLabel31 = new JLabel();
        spnFrequency = new JSpinner();
        JLabel jLabel33 = new JLabel();
        JLabel jLabel35 = new JLabel();
        lblFrequency = new JLabel();
        chkPrintInstructions = new JCheckBox();

        jLabel13.setFont(MONOSPACED_PLAIN);
        jLabel13.setText("A");

        txtA1.setEditable(false);
        txtA1.setFont(MONOSPACED_PLAIN);
        txtA1.setText("00");

        jLabel14.setFont(MONOSPACED_BOLD);
        jLabel14.setText("B");

        txtB1.setEditable(false);
        txtB1.setFont(MONOSPACED_PLAIN);
        txtB1.setText("00");

        txtF1.setEditable(false);
        txtF1.setFont(MONOSPACED_PLAIN);
        txtF1.setText("00");

        jLabel15.setFont(MONOSPACED_BOLD);
        jLabel15.setText("F");

        jLabel16.setFont(MONOSPACED_BOLD);
        jLabel16.setText("C");

        txtC1.setEditable(false);
        txtC1.setFont(MONOSPACED_PLAIN);
        txtC1.setText("00");

        txtD1.setEditable(false);
        txtD1.setFont(MONOSPACED_PLAIN);
        txtD1.setText("00");

        jLabel17.setFont(MONOSPACED_BOLD);
        jLabel17.setText("D");

        jLabel18.setFont(MONOSPACED_BOLD);
        jLabel18.setText("H");

        txtH1.setEditable(false);
        txtH1.setFont(MONOSPACED_PLAIN);
        txtH1.setText("00");

        jLabel19.setFont(MONOSPACED_BOLD);
        jLabel19.setText("L");

        jLabel20.setFont(MONOSPACED_BOLD);
        jLabel20.setText("E");

        txtE1.setEditable(false);
        txtE1.setFont(MONOSPACED_PLAIN);
        txtE1.setText("00");

        txtL1.setEditable(false);
        txtL1.setFont(MONOSPACED_PLAIN);
        txtL1.setText("00");

        txtHL1.setEditable(false);
        txtHL1.setFont(MONOSPACED_PLAIN);
        txtHL1.setText("00");

        txtDE1.setEditable(false);
        txtDE1.setFont(MONOSPACED_PLAIN);
        txtDE1.setText("00");

        txtBC1.setEditable(false);
        txtBC1.setFont(MONOSPACED_PLAIN);
        txtBC1.setText("00");

        jLabel21.setFont(MONOSPACED_BOLD);
        jLabel21.setText("BC");

        jLabel22.setFont(MONOSPACED_BOLD);
        jLabel22.setText("DE");

        jLabel23.setFont(MONOSPACED_BOLD);
        jLabel23.setText("HL");

        jScrollPane2.setBorder(null);

        tblFlags1.setModel(new DefaultTableModel(
                new Object[][]{

                },
                new String[]{

                }
        ));
        tblFlags1.setRowSelectionAllowed(false);
        jScrollPane2.setViewportView(tblFlags1);

        jLabel24.setText("Flags:");

        GroupLayout panelSet1Layout = new GroupLayout(panelSet1);
        panelSet1.setLayout(panelSet1Layout);
        panelSet1Layout.setHorizontalGroup(
                panelSet1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelSet1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addGroup(panelSet1Layout.createSequentialGroup()
                                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel17)
                                                        .addComponent(jLabel14))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(panelSet1Layout.createSequentialGroup()
                                                                .addComponent(txtB1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel16))
                                                        .addGroup(panelSet1Layout.createSequentialGroup()
                                                                .addComponent(txtD1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel20)))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(txtC1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtE1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel21)
                                                        .addComponent(jLabel22))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtBC1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtDE1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelSet1Layout.createSequentialGroup()
                                                .addComponent(jLabel18)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtH1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel19)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtL1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel23)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtHL1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelSet1Layout.createSequentialGroup()
                                                .addComponent(jLabel13)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtA1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel15)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtF1, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel24))
                                .addContainerGap(24, Short.MAX_VALUE))
        );
        panelSet1Layout.setVerticalGroup(
                panelSet1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelSet1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel13)
                                        .addComponent(txtA1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel15)
                                        .addComponent(txtF1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel14)
                                        .addComponent(txtB1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel16)
                                        .addComponent(txtC1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel21)
                                        .addComponent(txtBC1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel17)
                                                .addComponent(txtD1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(txtE1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel20)
                                                .addComponent(txtDE1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel22)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSet1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel18)
                                        .addComponent(txtH1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel19)
                                        .addComponent(txtL1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtHL1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel23))
                                .addGap(9, 9, 9)
                                .addComponent(jLabel24)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(20, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Set 1", panelSet1);

        jLabel1.setFont(MONOSPACED_BOLD);
        jLabel1.setText("B");

        txtB2.setEditable(false);
        txtB2.setFont(MONOSPACED_PLAIN);
        txtB2.setText("00");

        jLabel2.setFont(MONOSPACED_BOLD);
        jLabel2.setText("C");

        txtC2.setEditable(false);
        txtC2.setFont(MONOSPACED_PLAIN);
        txtC2.setText("00");

        jLabel3.setFont(MONOSPACED_BOLD);
        jLabel3.setText("BC");

        txtBC2.setEditable(false);
        txtBC2.setFont(MONOSPACED_PLAIN);
        txtBC2.setText("00");

        jLabel4.setFont(MONOSPACED_BOLD);
        jLabel4.setText("D");

        txtD2.setEditable(false);
        txtD2.setFont(MONOSPACED_PLAIN);
        txtD2.setText("00");

        jLabel5.setFont(MONOSPACED_BOLD);
        jLabel5.setText("E");

        txtE2.setEditable(false);
        txtE2.setFont(MONOSPACED_PLAIN);
        txtE2.setText("00");

        jLabel6.setFont(MONOSPACED_BOLD);
        jLabel6.setText("DE");

        txtDE2.setEditable(false);
        txtDE2.setFont(MONOSPACED_PLAIN);
        txtDE2.setText("00");

        jLabel7.setFont(MONOSPACED_BOLD);
        jLabel7.setText("H");

        txtH2.setEditable(false);
        txtH2.setFont(MONOSPACED_PLAIN);
        txtH2.setText("00");

        jLabel8.setFont(MONOSPACED_BOLD);
        jLabel8.setText("L");

        txtL2.setEditable(false);
        txtL2.setFont(MONOSPACED_PLAIN);
        txtL2.setText("00");

        jLabel9.setFont(MONOSPACED_BOLD);
        jLabel9.setText("HL");

        txtHL2.setEditable(false);
        txtHL2.setFont(MONOSPACED_PLAIN);
        txtHL2.setText("00");

        jLabel10.setFont(MONOSPACED_BOLD);
        jLabel10.setText("A");

        txtA2.setEditable(false);
        txtA2.setFont(MONOSPACED_PLAIN);
        txtA2.setText("00");

        jLabel11.setFont(MONOSPACED_BOLD);
        jLabel11.setText("F");

        txtF2.setEditable(false);
        txtF2.setFont(MONOSPACED_PLAIN);
        txtF2.setText("00");

        jScrollPane1.setBorder(null);

        tblFlags2.setModel(new DefaultTableModel(
                new Object[][]{

                },
                new String[]{

                }
        ));
        tblFlags2.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(tblFlags2);

        jLabel12.setText("Flags:");

        GroupLayout panelSet2Layout = new GroupLayout(panelSet2);
        panelSet2.setLayout(panelSet2Layout);
        panelSet2Layout.setHorizontalGroup(
                panelSet2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelSet2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                        .addGroup(panelSet2Layout.createSequentialGroup()
                                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel4)
                                                        .addComponent(jLabel1))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addGroup(panelSet2Layout.createSequentialGroup()
                                                                .addComponent(txtB2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel2))
                                                        .addGroup(panelSet2Layout.createSequentialGroup()
                                                                .addComponent(txtD2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel5)))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(txtC2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtE2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel3)
                                                        .addComponent(jLabel6))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtBC2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtDE2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelSet2Layout.createSequentialGroup()
                                                .addComponent(jLabel7)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtH2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel8)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtL2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel9)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtHL2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelSet2Layout.createSequentialGroup()
                                                .addComponent(jLabel10)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtA2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel11)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtF2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel12))
                                .addContainerGap(24, Short.MAX_VALUE))
        );
        panelSet2Layout.setVerticalGroup(
                panelSet2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelSet2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel10)
                                        .addComponent(txtA2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel11)
                                        .addComponent(txtF2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(txtB2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2)
                                        .addComponent(txtC2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtBC2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel4)
                                                .addComponent(txtD2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(txtE2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel5)
                                                .addComponent(txtDE2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel6)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelSet2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(txtH2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel8)
                                        .addComponent(txtL2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtHL2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel9))
                                .addGap(9, 9, 9)
                                .addComponent(jLabel12)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(20, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Set 2", panelSet2);

        jLabel26.setFont(MONOSPACED_BOLD);
        jLabel26.setText("PC");

        txtPC.setEditable(false);
        txtPC.setFont(MONOSPACED_PLAIN);
        txtPC.setText("00");

        jLabel25.setFont(MONOSPACED_BOLD);
        jLabel25.setText("SP");

        txtSP.setEditable(false);
        txtSP.setFont(MONOSPACED_PLAIN);
        txtSP.setText("00");

        jLabel27.setFont(MONOSPACED_BOLD);
        jLabel27.setText("IX");

        txtIX.setEditable(false);
        txtIX.setFont(MONOSPACED_PLAIN);
        txtIX.setText("00");

        jLabel28.setFont(MONOSPACED_BOLD);
        jLabel28.setText("IY");

        txtIY.setEditable(false);
        txtIY.setFont(MONOSPACED_PLAIN);
        txtIY.setText("00");

        jLabel29.setFont(MONOSPACED_BOLD);
        jLabel29.setText("I");

        txtI.setEditable(false);
        txtI.setFont(MONOSPACED_PLAIN);
        txtI.setText("00");

        jLabel30.setFont(MONOSPACED_BOLD);
        jLabel30.setText("R");

        txtR.setEditable(false);
        txtR.setFont(MONOSPACED_PLAIN);
        txtR.setText("00");

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel25)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSP, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addGap(10, 10, 10)
                                                .addComponent(jLabel28)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtIY, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel26)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtPC, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                .addGap(10, 10, 10)
                                                .addComponent(jLabel27)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtIX, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel29)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtI, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel30)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtR, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel26)
                                        .addComponent(txtPC, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtIX, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel27)
                                        .addComponent(jLabel29)
                                        .addComponent(txtI, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel28)
                                        .addComponent(txtIY, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtSP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel25)
                                        .addComponent(jLabel30)
                                        .addComponent(txtR, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(BorderFactory.createTitledBorder("Run control"));

        lblRunState.setFont(MONOSPACED_BIG_BOLD);
        lblRunState.setForeground(new java.awt.Color(0, 153, 51));
        lblRunState.setText("BREAKPOINT");

        jLabel31.setText("CPU Frequency:");

        spnFrequency.setModel(new SpinnerNumberModel(context.getCPUFrequency(), 1, null, 100));
        spnFrequency.setName("CPU frequency");

        jLabel33.setFont(jLabel33.getFont().deriveFont(jLabel33.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel33.setText("kHz");

        jLabel35.setText("Runtime frequency:");

        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | java.awt.Font.BOLD));
        lblFrequency.setText("0.00 kHz");

        chkPrintInstructions.setText("Dump instructions history");
        chkPrintInstructions.addActionListener(this::chkPrintInstructionsActionPerformed);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jSeparator1)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblRunState)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(jLabel31)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(spnFrequency, GroupLayout.PREFERRED_SIZE, 91, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel33))
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(jLabel35)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblFrequency))
                                                        .addComponent(chkPrintInstructions))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblRunState)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel31)
                                        .addComponent(spnFrequency, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel33))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel35)
                                        .addComponent(lblFrequency))
                                .addGap(18, 18, 18)
                                .addComponent(chkPrintInstructions)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jTabbedPane1, GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jTabbedPane1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void chkPrintInstructionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkPrintInstructionsActionPerformed
        if (chkPrintInstructions.isSelected()) {
            cpu.getEngine().setDispatchListener(new InstructionPrinter(cpu.getDisassembler(), cpu.getEngine(), true, System.err));
        } else {
            cpu.getEngine().setDispatchListener(null);
        }
    }//GEN-LAST:event_chkPrintInstructionsActionPerformed
    // End of variables declaration//GEN-END:variables
}
