/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.brainduck.CpuImpl;
import net.emustudio.plugins.cpu.brainduck.EmulatorEngine;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.ui.Constants.*;

public class StatusPanel extends JPanel {
    private final GUI gui;
    private final ColumnsRepainter columnsRepainter = new ColumnsRepainter();
    private final MemoryTableModel tableModel;
    private final Byte[] memory;
    private final EmulatorEngine cpu;

    private final JLabel lblLoopLevel = new JLabel("0");
    private final JLabel lblRunState = new JLabel("stopped (breakpoint)");
    private final JLabel lblTime = new JLabel("0 ms");
    private final JTable tblMemory = new JTable();
    private final JTextField txtIP = readOnlyField("0");
    private final JTextField txtMemP = readOnlyField("0");
    private final JTextField txtP = readOnlyField("0");

    public StatusPanel(ByteMemoryContext memory, CpuImpl cpu, GUI gui) {
        this.gui = gui;
        this.memory = memory.getRawMemory()[0];
        this.cpu = cpu.getEngine();
        this.tableModel = new MemoryTableModel(this.memory);

        initComponents();
        tblMemory.setModel(tableModel);
        tblMemory.getTableHeader().setReorderingAllowed(false);
        columnsRepainter.setMainColumn(2, tblMemory);

        cpu.addCPUListener(new CPUStatusListener());
    }

    private void initComponents() {
        lblTime.setFont(lblTime.getFont().deriveFont(lblTime.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoopLevel.setFont(lblLoopLevel.getFont().deriveFont(lblLoopLevel.getFont().getStyle() | java.awt.Font.BOLD));

        JPanel panelInternal = gui.section("Internal state", "insets dialog", "[][grow]6[]", "[][][]6[][][]");
        panelInternal.add(gui.label("IP:"));
        panelInternal.add(txtIP, "growx");
        panelInternal.add(gui.label("h"), "wrap");
        panelInternal.add(gui.label("P:"));
        panelInternal.add(txtP, "growx");
        panelInternal.add(gui.label("h"), "wrap");
        panelInternal.add(gui.label("*P:"));
        panelInternal.add(txtMemP, "growx");
        panelInternal.add(gui.label("h"), "wrap");
        panelInternal.add(new JSeparator(), "span, growx, h 2!, wrap");
        panelInternal.add(gui.label("Execution time:"));
        panelInternal.add(lblTime, "span, wrap");
        panelInternal.add(gui.label("Loop level:"));
        panelInternal.add(lblLoopLevel, "span");

        lblRunState.setFont(FONT_MONOSPACED_BIG_BOLD);
        lblRunState.setForeground(CPU_RUN_STATE_COLOR);
        lblRunState.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panelRunState = gui.section("Run state", "insets dialog", "[grow]", "[]");
        panelRunState.add(lblRunState, "growx");

        tblMemory.setFont(FONT_MONOSPACED);
        tblMemory.setRowSelectionAllowed(false);

        JPanel panelMemory = gui.section("Memory view", "insets dialog", "[grow]", "[grow]");
        panelMemory.add(new JScrollPane(tblMemory), "grow");

        setLayout(new BorderLayout());
        JPanel content = gui.panel("insets dialog", "[grow]", "[][][]");
        content.add(panelInternal, "growx, wrap");
        content.add(panelRunState, "growx, wrap");
        content.add(panelMemory, "growx");
        add(content, BorderLayout.CENTER);
    }

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

    private static JTextField readOnlyField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setFont(FONT_MONOSPACED);
        return field;
    }
}
