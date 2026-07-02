/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.CPU.RunState;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.intel8080.CpuImpl;
import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;
import net.emustudio.plugins.cpu.intel8080.InstructionPrinter;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatByteHexString;
import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatWordHexString;

public class StatusPanel extends JPanel {
    private final GUI gui;
    private final CpuImpl cpu;
    private final EmulatorEngine engine;
    private final Context8080 context;
    private final AbstractTableModel flagModel;

    private final JLabel lblFrequency = new JLabel("0,0 kHz");
    private final JLabel lblRun = new JLabel("Stopped");
    private final JSpinner spnFrequency = new JSpinner();
    private final JTable tblFlags = new JTable();
    private final JTextField txtFlags = regField("00");
    private final JTextField txtRegA = regField("00");
    private final JTextField txtRegB = regField("00");
    private final JTextField txtRegBC = regField("0000");
    private final JTextField txtRegC = regField("00");
    private final JTextField txtRegD = regField("00");
    private final JTextField txtRegDE = regField("0000");
    private final JTextField txtRegE = regField("00");
    private final JTextField txtRegH = regField("00");
    private final JTextField txtRegHL = regField("0000");
    private final JTextField txtRegL = regField("00");
    private final JTextField txtRegPC = regField("0000");
    private final JTextField txtRegSP = regField("0000");
    private final JCheckBox chkPrintInstructions = new JCheckBox("Dump instructions history");

    private volatile RunState runState = RunState.STATE_STOPPED_NORMAL;

    public StatusPanel(CpuImpl cpu, Context8080 context, boolean dumpInstructions, GUI gui) {
        this.gui = gui;
        this.cpu = cpu;
        this.context = context;
        this.engine = cpu.getEngine();
        this.flagModel = new FlagsModel(engine);

        initComponents();
        chkPrintInstructions.setSelected(dumpInstructions);
        tblFlags.setModel(flagModel);

        setupListeners();
    }

    private void setupListeners() {
        cpu.addCPUListener(new CPU.CPUListener() {
            @Override
            public void runStateChanged(RunState state) {
                runState = state;
            }

            @Override
            public void internalStateChanged() {
                updateGUI();
            }
        });
        cpu.getFrequencyCalculator().addListener(
                f -> SwingUtilities.invokeLater(() -> lblFrequency.setText(String.format("%.2f kHz", f)))
        );
        spnFrequency.addChangeListener(e -> {
            int i = (Integer) spnFrequency.getModel().getValue();
            try {
                StatusPanel.this.context.setCPUFrequency(i);
            } catch (IndexOutOfBoundsException ex) {
                spnFrequency.getModel().setValue(context.getCPUFrequency());
            }
        });
    }

    public void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            txtRegA.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_A]));
            txtRegB.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_B]));
            txtRegC.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_C]));
            txtRegBC.setText(formatWordHexString((short) engine.regs[EmulatorEngine.REG_B], (short) engine.regs[EmulatorEngine.REG_C]));
            txtRegD.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_D]));
            txtRegE.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_E]));
            txtRegDE.setText(formatWordHexString((short) engine.regs[EmulatorEngine.REG_D], (short) engine.regs[EmulatorEngine.REG_E]));
            txtRegH.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_H]));
            txtRegL.setText(formatByteHexString(engine.regs[EmulatorEngine.REG_L]));
            txtRegHL.setText(formatWordHexString((short) engine.regs[EmulatorEngine.REG_H], (short) engine.regs[EmulatorEngine.REG_L]));
            txtRegSP.setText(formatWordHexString(engine.SP));
            txtRegPC.setText(formatWordHexString(engine.PC));
            txtFlags.setText(formatByteHexString(engine.flags));
            flagModel.fireTableDataChanged();
            lblRun.setText(runState.toString());
            spnFrequency.setEnabled(runState != RunState.STATE_RUNNING);
        });
    }

    private void initComponents() {
        tblFlags.setAutoCreateRowSorter(true);
        tblFlags.setBackground(Color.white);
        tblFlags.setBorder(null);
        tblFlags.setRowSelectionAllowed(false);

        chkPrintInstructions.addActionListener(e -> {
            if (chkPrintInstructions.isSelected()) {
                engine.setDispatchListener(new InstructionPrinter(cpu.getDisassembler(), engine, true, System.err));
            } else {
                engine.setDispatchListener(null);
            }
        });

        // Registers panel
        JPanel paneRegisters = gui.panel("insets dialog", "[][grow]6[][grow]6[][grow]", "[][][][][][]6[][grow]");
        paneRegisters.add(gui.labelBold("B"));
        paneRegisters.add(txtRegB, "growx");
        paneRegisters.add(gui.labelBold("C"));
        paneRegisters.add(txtRegC, "growx");
        paneRegisters.add(gui.labelBold("BC"));
        paneRegisters.add(txtRegBC, "growx, wrap");
        paneRegisters.add(gui.labelBold("D"));
        paneRegisters.add(txtRegD, "growx");
        paneRegisters.add(gui.labelBold("E"));
        paneRegisters.add(txtRegE, "growx");
        paneRegisters.add(gui.labelBold("DE"));
        paneRegisters.add(txtRegDE, "growx, wrap");
        paneRegisters.add(gui.labelBold("H"));
        paneRegisters.add(txtRegH, "growx");
        paneRegisters.add(gui.labelBold("L"));
        paneRegisters.add(txtRegL, "growx");
        paneRegisters.add(gui.labelBold("HL"));
        paneRegisters.add(txtRegHL, "growx, wrap");
        paneRegisters.add(gui.labelBold("A"));
        paneRegisters.add(txtRegA, "growx");
        paneRegisters.add(gui.labelBold("F"));
        paneRegisters.add(txtFlags, "growx");
        paneRegisters.add(gui.labelBold("PC"));
        paneRegisters.add(txtRegPC, "growx, wrap");
        paneRegisters.add(new JLabel());
        paneRegisters.add(new JLabel());
        paneRegisters.add(new JLabel());
        paneRegisters.add(new JLabel());
        paneRegisters.add(gui.labelBold("SP"));
        paneRegisters.add(txtRegSP, "growx, wrap");
        paneRegisters.add(gui.label("Flags (F):"), "span, wrap");
        paneRegisters.add(tblFlags, "span, growx");

        // Run control panel
        SpinnerNumberModel spFrequencyModel = new SpinnerNumberModel();
        spFrequencyModel.setValue(context.getCPUFrequency());
        spFrequencyModel.setStepSize(100);
        spnFrequency.setModel(spFrequencyModel);

        lblRun.setFont(lblRun.getFont().deriveFont(lblRun.getFont().getStyle() | Font.BOLD));
        lblRun.setForeground(new Color(0, 102, 0));
        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | Font.BOLD));

        JPanel panelRun = gui.section("Run control", "insets dialog", "[]6[grow]6[]", "[][][][]");
        panelRun.add(lblRun, "span, wrap");
        panelRun.add(gui.label("CPU frequency:"));
        panelRun.add(spnFrequency, "growx");
        panelRun.add(gui.labelBold("kHz"), "wrap");
        panelRun.add(gui.label("Runtime frequency:"));
        panelRun.add(lblFrequency, "span, wrap");
        panelRun.add(chkPrintInstructions, "span");

        // Main layout
        setBorder(null);
        setLayout(new BorderLayout());
        JPanel content = gui.panel("insets 0", "[grow]", "[]6[]");
        content.add(paneRegisters, "growx, wrap");
        content.add(panelRun, "growx");
        add(content, BorderLayout.CENTER);
    }

    private static JTextField regField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1, Color.lightGray));
        return field;
    }
}
