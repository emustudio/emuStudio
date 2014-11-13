/*
 * StatusPanel.java
 *
 * Created on Nedeľa, 2008, august 24, 10:22
 *
 * Copyright (C) 2008-2013 Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.gui;

import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.RunState;
import net.sf.emustudio.intel8080.ExtendedContext;
import net.sf.emustudio.zilogZ80.FrequencyChangedListener;
import net.sf.emustudio.zilogZ80.impl.EmulatorImpl;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

import static emulib.runtime.RadixUtils.getByteHexString;
import static emulib.runtime.RadixUtils.getWordHexString;

/**
 * Status panel for the CPU.
 *
 * @author Peter Jakubčo
 */
public class StatusPanel extends javax.swing.JPanel {

    private RunState run_state;
    private EmulatorImpl cpu;
    private ExtendedContext context;
    private AbstractTableModel flagModel1;
    private AbstractTableModel flagModel2;

    /** Creates new form statusGUI */
    public StatusPanel(EmulatorImpl cpu, ExtendedContext context) {
        this.cpu = cpu;
        this.context = context;

        run_state = RunState.STATE_STOPPED_NORMAL;

        initComponents();
        cpu.addCPUListener(new CPU.CPUListener() {

            @Override
            public void runStateChanged(RunState state) {
                run_state = state;
            }

            @Override
            public void internalStateChanged() {
                updateGUI();
            }

        });
        cpu.addFrequencyChangedListener(new FrequencyChangedListener() {
            @Override
            public void frequencyChanged(float newFrequency) {
                lblFrequency.setText(String.format("%.2f kHz", newFrequency));
            }
        });
        spnFrequency.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int i = (Integer) ((SpinnerNumberModel) spnFrequency.getModel()).getValue();
                try {
                    StatusPanel.this.context.setCPUFrequency(i);
                } catch (IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel) spnFrequency.getModel()).setValue(StatusPanel.this.context.getCPUFrequency());
                }
            }
        });
        spnTestPeriode.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                int i = (Integer) ((SpinnerNumberModel) spnTestPeriode.getModel()).getValue();
                try {
                    StatusPanel.this.cpu.setSliceTime(i);
                } catch (IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel) spnTestPeriode.getModel()).setValue(StatusPanel.this.cpu.getSliceTime());
                }
            }
        });
        flagModel1 = new FlagsModel(0, cpu);
        flagModel2 = new FlagsModel(1, cpu);
        tblFlags.setModel(flagModel1);
        tblFlags2.setModel(flagModel2);
    }

    // user set frequency
    private void setCPUFreq(int f) {
        context.setCPUFrequency(f);
    }


    private String f(int what) {
        return getWordHexString(what);
    }

    private String f(short what) {
        return getByteHexString(what);

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

        txtRegSP.setText(getWordHexString(cpu.SP));
        txtRegPC.setText(getWordHexString(cpu.PC));
        txtRegIX.setText(getWordHexString(cpu.IX));
        txtRegIY.setText(getWordHexString(cpu.IY));
        txtRegI.setText(getByteHexString(cpu.I));
        txtRegR.setText(getByteHexString(cpu.R));

        if (run_state == RunState.STATE_RUNNING) {
            lblRun.setText("running");
            spnFrequency.setEnabled(false);
            spnTestPeriode.setEnabled(false);
        } else {
            spnFrequency.setEnabled(true);
            spnTestPeriode.setEnabled(true);
            switch (run_state) {
                case STATE_STOPPED_NORMAL:
                    lblRun.setText("stopped (normal)");
                    break;
                case STATE_STOPPED_BREAK:
                    lblRun.setText("breakpoint");
                    break;
                case STATE_STOPPED_ADDR_FALLOUT:
                    lblRun.setText("stopped (address fallout)");
                    break;
                case STATE_STOPPED_BAD_INSTR:
                    lblRun.setText("stopped (instruction fallout)");
                    break;
            }
        }
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
