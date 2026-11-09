/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.gui;

import net.emustudio.emulib.plugins.cpu.CPUContext;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.CPU.RunState;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.intel8080.CpuImpl;
import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatHex;

public class StatusPanel extends JPanel {
    private final GUI gui;
    private final CpuImpl cpu;
    private final EmulatorEngine engine;
    private final Context8080 context;
    private final FlagsModel flagModel;

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

    private volatile RunState runState = RunState.STATE_STOPPED_NORMAL;

    public StatusPanel(CpuImpl cpu, Context8080 context, GUI gui, CPUContext execution) {
        this.gui = gui;
        this.cpuContext = execution;
        this.cpu = cpu;
        this.context = context;
        this.engine = cpu.getEngine();
        this.flagModel = new FlagsModel(engine);

        initComponents();
        add(cpu.debuggingEditor(), BorderLayout.SOUTH);
        tblFlags.setModel(flagModel);

        setupListeners();
    }

    private void setupListeners() {
        observeCPU(new PanelUpdater());
        spnFrequency.addChangeListener(e -> {
            int i = (Integer) spnFrequency.getModel().getValue();
            try {
                context.setCPUFrequency(i);
            } catch (IndexOutOfBoundsException ex) {
                spnFrequency.getModel().setValue(context.getCPUFrequency());
            }
        });
    }

    public void updateGUI() {
        final int regA = engine.regs[EmulatorEngine.REG_A];
        final int regB = engine.regs[EmulatorEngine.REG_B];
        final int regC = engine.regs[EmulatorEngine.REG_C];
        final int regD = engine.regs[EmulatorEngine.REG_D];
        final int regE = engine.regs[EmulatorEngine.REG_E];
        final int regH = engine.regs[EmulatorEngine.REG_H];
        final int regL = engine.regs[EmulatorEngine.REG_L];
        final int sp = engine.SP;
        final int pc = engine.PC;
        final int flags = engine.flags;
        final RunState state = runState;
        SwingUtilities.invokeLater(() -> {
            txtRegA.setText(formatHex(regA, 2));
            txtRegB.setText(formatHex(regB, 2));
            txtRegC.setText(formatHex(regC, 2));
            txtRegBC.setText(formatHex(((regB & 0xFF) << 8) | (regC & 0xFF), 4));
            txtRegD.setText(formatHex(regD, 2));
            txtRegE.setText(formatHex(regE, 2));
            txtRegDE.setText(formatHex(((regD & 0xFF) << 8) | (regE & 0xFF), 4));
            txtRegH.setText(formatHex(regH, 2));
            txtRegL.setText(formatHex(regL, 2));
            txtRegHL.setText(formatHex(((regH & 0xFF) << 8) | (regL & 0xFF), 4));
            txtRegSP.setText(formatHex(sp, 4));
            txtRegPC.setText(formatHex(pc, 4));
            txtFlags.setText(formatHex(flags, 2));
            flagModel.update(flags);
            lblRun.setText(state == RunState.STATE_STOPPED_BREAK ? "Paused" : state.toString());
            spnFrequency.setEnabled(state != RunState.STATE_RUNNING);
        });
    }

    private void initComponents() {
        tblFlags.setAutoCreateRowSorter(true);
        tblFlags.setBackground(UIManager.getColor("Table.background"));
        tblFlags.setForeground(UIManager.getColor("Table.foreground"));
        tblFlags.setBorder(null);
        tblFlags.setRowSelectionAllowed(false);

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
        lblRun.setForeground(UIManager.getColor("Label.foreground"));
        lblFrequency.setFont(lblFrequency.getFont().deriveFont(lblFrequency.getFont().getStyle() | Font.BOLD));

        JPanel panelRun = gui.section("Run control", "insets dialog", "[]6[grow]6[]", "[][][][]");
        panelRun.add(lblRun, "span, wrap");
        panelRun.add(gui.label("CPU frequency:"));
        panelRun.add(spnFrequency, "growx");
        panelRun.add(gui.labelBold("kHz"), "wrap");
        panelRun.add(gui.label("Runtime frequency:"));
        panelRun.add(lblFrequency, "span, wrap");

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

    private final class PanelUpdater implements CPUContext.CPUListener {
        public void runStateChanged(RunState state) {
            runState = state;
            SwingUtilities.invokeLater(() -> lblRun.setText(state == RunState.STATE_STOPPED_BREAK ? "Paused" : state.toString()));
        }

        public void internalStateChanged() {
            SwingUtilities.invokeLater(StatusPanel.this::updateFrequency);
            updateGUI();
        }

    }

    private final Timer frequencyRefresh = new Timer(1000, event -> updateFrequency());

    private void updateFrequency() {
        float frequencyKhz = cpuContext.getCpuFrequencyKhz();
        lblFrequency.setText(Float.isNaN(frequencyKhz) ? "N/A" : String.format("%.2f kHz", frequencyKhz));
    }

    private CPUContext.CPUListener cpuListener;
    private boolean listening;
    private final CPUContext cpuContext;

    private void observeCPU(CPUContext.CPUListener listener) {
        cpuListener = listener;
        cpuContext.addCPUListener(listener);
        listening = true;
    }

    @Override public void addNotify() {
        super.addNotify();
        frequencyRefresh.start();
        if (!listening) {
            cpuContext.addCPUListener(cpuListener);
            listening = true;
        }
    }

    @Override public void removeNotify() {
        if (listening) cpuContext.removeCPUListener(cpuListener);
        listening = false;
        frequencyRefresh.stop();
        super.removeNotify();
    }
}
