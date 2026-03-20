/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.CpuImpl;
import net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine;
import net.emustudio.plugins.cpu.zilogZ80.InstructionPrinter;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatByteHexString;
import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatWordHexString;
import static net.emustudio.emulib.runtime.ui.Constants.*;

public class StatusPanel extends JPanel {
    private final GUI gui;
    private final CpuImpl cpu;
    private final Context8080 context;
    private final FlagsModel flagModel1;
    private final FlagsModel flagModel2;

    private volatile CPU.RunState runState = CPU.RunState.STATE_STOPPED_NORMAL;

    // Set 1
    private final JTextField txtA1 = regField();
    private final JTextField txtF1 = regField();
    private final JTextField txtB1 = regField();
    private final JTextField txtC1 = regField();
    private final JTextField txtBC1 = regField();
    private final JTextField txtD1 = regField();
    private final JTextField txtE1 = regField();
    private final JTextField txtDE1 = regField();
    private final JTextField txtH1 = regField();
    private final JTextField txtL1 = regField();
    private final JTextField txtHL1 = regField();
    private final JTable tblFlags1 = new JTable();

    // Set 2
    private final JTextField txtA2 = regField();
    private final JTextField txtF2 = regField();
    private final JTextField txtB2 = regField();
    private final JTextField txtC2 = regField();
    private final JTextField txtBC2 = regField();
    private final JTextField txtD2 = regField();
    private final JTextField txtE2 = regField();
    private final JTextField txtDE2 = regField();
    private final JTextField txtH2 = regField();
    private final JTextField txtL2 = regField();
    private final JTextField txtHL2 = regField();
    private final JTable tblFlags2 = new JTable();

    // Extra registers
    private final JTextField txtPC = regField();
    private final JTextField txtSP = regField();
    private final JTextField txtIX = regField();
    private final JTextField txtIY = regField();
    private final JTextField txtI = regField();
    private final JTextField txtR = regField();

    // Run control
    private final JLabel lblRunState = new JLabel("BREAKPOINT");
    private final JLabel lblFrequency = new JLabel("0.00 kHz");
    private final JSpinner spnFrequency;
    private final JCheckBox chkPrintInstructions = new JCheckBox("Dump instructions history");

    public StatusPanel(CpuImpl cpu, Context8080 context, boolean dumpInstructions, GUI gui) {
        this.gui = gui;
        this.cpu = cpu;
        this.context = context;
        this.flagModel1 = new FlagsModel(0, cpu.getEngine());
        this.flagModel2 = new FlagsModel(1, cpu.getEngine());
        this.spnFrequency = new JSpinner(new SpinnerNumberModel(context.getCPUFrequency(), 1, null, 100));

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
        cpu.getFrequencyCalculator().addListener(f -> lblFrequency.setText(String.format("%.2f kHz", f)));
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
        spnFrequency.setEnabled(runState != CPU.RunState.STATE_RUNNING);
    }

    private void initComponents() {
        lblRunState.setFont(FONT_MONOSPACED_BIG_BOLD);
        lblRunState.setForeground(CPU_RUN_STATE_COLOR);
        lblFrequency.setFont(lblFrequency.getFont().deriveFont(java.awt.Font.BOLD));
        spnFrequency.setName("CPU frequency");

        // Tabbed register sets
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Set 1", createRegisterSetPanel(
                txtA1, txtF1, txtB1, txtC1, txtBC1, txtD1, txtE1, txtDE1, txtH1, txtL1, txtHL1, tblFlags1));
        tabbedPane.addTab("Set 2", createRegisterSetPanel(
                txtA2, txtF2, txtB2, txtC2, txtBC2, txtD2, txtE2, txtDE2, txtH2, txtL2, txtHL2, tblFlags2));

        // Extra registers: PC IX I / SP IY R
        JPanel panelExtra = gui.panel("insets dialog", "[]6[66!]10[]6[66!]10[]6[66!]", "[][]");
        panelExtra.add(gui.labelBold("PC"));
        panelExtra.add(txtPC, "growx");
        panelExtra.add(gui.labelBold("IX"));
        panelExtra.add(txtIX, "growx");
        panelExtra.add(gui.labelBold("I"));
        panelExtra.add(txtI, "growx, wrap");
        panelExtra.add(gui.labelBold("SP"));
        panelExtra.add(txtSP, "growx");
        panelExtra.add(gui.labelBold("IY"));
        panelExtra.add(txtIY, "growx");
        panelExtra.add(gui.labelBold("R"));
        panelExtra.add(txtR, "growx");

        // Run control
        JPanel panelRun = gui.section("Run control", "insets dialog", "[]6[]6[]push[]", "[]6[]6[]6[]12[]");
        panelRun.add(lblRunState, "span, wrap");
        panelRun.add(new JSeparator(), "span, growx, wrap");
        panelRun.add(gui.label("CPU Frequency:"));
        panelRun.add(spnFrequency, "w 91!");
        panelRun.add(gui.labelBold("kHz"), "wrap");
        panelRun.add(gui.label("Runtime frequency:"));
        panelRun.add(lblFrequency, "span, wrap");
        chkPrintInstructions.addActionListener(this::chkPrintInstructionsActionPerformed);
        panelRun.add(chkPrintInstructions, "span");

        setLayout(new BorderLayout());
        JPanel content = gui.panel("insets dialog", "[grow]", "[][][]");
        content.add(tabbedPane, "growx, wrap");
        content.add(panelExtra, "growx, wrap");
        content.add(panelRun, "growx");
        add(content, BorderLayout.CENTER);
    }

    private JPanel createRegisterSetPanel(
            JTextField txtA, JTextField txtF,
            JTextField txtB, JTextField txtC, JTextField txtBC,
            JTextField txtD, JTextField txtE, JTextField txtDE,
            JTextField txtH, JTextField txtL, JTextField txtHL,
            JTable tblFlags) {

        JPanel panel = gui.panel("insets dialog", "[]6[66!]6[]6[66!]6[]6[66!]", "[][][][]6[]6[54!]");

        panel.add(gui.labelBold("A"));
        panel.add(txtA, "growx");
        panel.add(gui.labelBold("F"));
        panel.add(txtF, "growx, wrap");

        panel.add(gui.labelBold("B"));
        panel.add(txtB, "growx");
        panel.add(gui.labelBold("C"));
        panel.add(txtC, "growx");
        panel.add(gui.labelBold("BC"));
        panel.add(txtBC, "growx, wrap");

        panel.add(gui.labelBold("D"));
        panel.add(txtD, "growx");
        panel.add(gui.labelBold("E"));
        panel.add(txtE, "growx");
        panel.add(gui.labelBold("DE"));
        panel.add(txtDE, "growx, wrap");

        panel.add(gui.labelBold("H"));
        panel.add(txtH, "growx");
        panel.add(gui.labelBold("L"));
        panel.add(txtL, "growx");
        panel.add(gui.labelBold("HL"));
        panel.add(txtHL, "growx, wrap");

        panel.add(gui.label("Flags:"), "span, wrap");

        tblFlags.setRowSelectionAllowed(false);
        JScrollPane scrollPane = new JScrollPane(tblFlags);
        scrollPane.setBorder(null);
        panel.add(scrollPane, "span, growx, h 54!");

        return panel;
    }

    private void chkPrintInstructionsActionPerformed(java.awt.event.ActionEvent evt) {
        if (chkPrintInstructions.isSelected()) {
            cpu.getEngine().setDispatchListener(new InstructionPrinter(cpu.getDisassembler(), cpu.getEngine(), true, System.err));
        } else {
            cpu.getEngine().setDispatchListener(null);
        }
    }

    private static JTextField regField() {
        JTextField field = new JTextField("00");
        field.setEditable(false);
        field.setFont(FONT_MONOSPACED);
        return field;
    }
}
