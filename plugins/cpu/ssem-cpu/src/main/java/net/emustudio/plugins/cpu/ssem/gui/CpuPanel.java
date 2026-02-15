/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.ssem.EmulatorEngine;

import javax.swing.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinaryString;
import static net.emustudio.emulib.runtime.ui.Constants.*;

public class CpuPanel extends JPanel {
    private final EmulatorEngine engine;
    private final MemoryContext<Byte> memory;

    private final JLabel lblRunState = new JLabel("BREAKPOINT");
    private final JLabel lblSpeed = new JLabel("0");
    private final JTextField txtA = readOnlyField("0");
    private final JTextField txtDecA = readOnlyField("0");
    private final JTextField txtBinA = readOnlyField("0000 0000  0000 0000  0000 0000  0000 0000");
    private final JTextField txtBinLine = readOnlyField("0000 0000");
    private final JTextField txtBinMCI = readOnlyField("0000 0000  0000 0000  0000 0000  0000 0000");
    private final JTextField txtBinMLine = readOnlyField("0000 0000  0000 0000  0000 0000  0000 0000");
    private final JTextField txtCI = readOnlyField("0");
    private final JTextField txtDecCI = readOnlyField("0");
    private final JTextField txtBinCI = readOnlyField("0000 0000  0000 0000  0000 0000  0000 0000");
    private final JTextField txtLine = readOnlyField("0");
    private final JTextField txtDecLine = readOnlyField("0");
    private final JTextField txtMCI = readOnlyField("0");
    private final JTextField txtDecMCI = readOnlyField("0");
    private final JTextField txtMLine = readOnlyField("0");
    private final JTextField txtDecMLine = readOnlyField("0");

    public CpuPanel(CPU cpu, EmulatorEngine engine, MemoryContext<Byte> memory) {
        this.engine = Objects.requireNonNull(engine);
        this.memory = Objects.requireNonNull(memory);

        initComponents();
        cpu.addCPUListener(new Updater());
        lblSpeed.setText(String.format("%.2f", EmulatorEngine.INSTRUCTIONS_PER_SECOND));
    }

    private void initComponents() {
        lblRunState.setFont(FONT_MONOSPACED_BIG_BOLD);
        lblRunState.setForeground(CPU_RUN_STATE_COLOR);
        lblSpeed.setFont(FONT_MONOSPACED);

        // Run control
        JPanel panelRun = GUI.section("Run control", "insets dialog", "[grow]push[]6[]", "[]");
        panelRun.add(lblRunState);
        panelRun.add(lblSpeed);
        panelRun.add(GUI.labelBold("ins/s"));

        // Columns: label, hex, dec, binary
        // Registers
        JPanel panelRegs = GUI.section("Registers", "insets dialog", "[]6[80!]6[80!]6[grow]", "[][]");
        panelRegs.add(GUI.label("A"));
        panelRegs.add(txtA, "growx");
        panelRegs.add(txtDecA, "growx");
        panelRegs.add(txtBinA, "growx, wrap");
        panelRegs.add(GUI.label("CI"));
        panelRegs.add(txtCI, "growx");
        panelRegs.add(txtDecCI, "growx");
        panelRegs.add(txtBinCI, "growx");

        // Memory snippet
        JPanel panelMem = GUI.section("Memory snippet", "insets dialog", "[]6[80!]6[80!]6[grow]", "[][][]");
        panelMem.add(GUI.label("M[CI]"));
        panelMem.add(txtMCI, "growx");
        panelMem.add(txtDecMCI, "growx");
        panelMem.add(txtBinMCI, "growx, wrap");
        panelMem.add(GUI.label("line"));
        panelMem.add(txtLine, "growx");
        panelMem.add(txtDecLine, "growx");
        panelMem.add(txtBinLine, "growx, wrap");
        panelMem.add(GUI.label("M[line]"));
        panelMem.add(txtMLine, "growx");
        panelMem.add(txtDecMLine, "growx");
        panelMem.add(txtBinMLine, "growx");

        setLayout(new net.miginfocom.swing.MigLayout("insets dialog", "[grow]", "[][][]"));
        add(panelRegs, "growx, wrap");
        add(panelMem, "growx, wrap");
        add(panelRun, "growx");
    }

    private final class Updater implements CPU.CPUListener {

        @Override
        public void runStateChanged(CPU.RunState rs) {
            lblRunState.setText(rs.toString().toUpperCase());
        }

        @Override
        public void internalStateChanged() {
            int acc = engine.Acc.get();
            int ci = engine.CI.get();

            txtA.setText(String.format("%08x", acc));
            txtDecA.setText(String.format("%d", acc));
            txtCI.setText(String.format("%08x", ci / 4));
            txtDecCI.setText(String.format("%d", ci / 4));
            txtBinA.setText(formatBinary(acc));
            txtBinCI.setText(formatBinary(ci));

            try {
                Byte[] mCI = memory.read(ci, 4);
                byte line = (byte) NumberUtils.reverseBits(mCI[0] & 0b11111000, 8);
                Byte[] mLine = memory.read(line * 4, 4);

                txtMCI.setText(String.format("%08x", NumberUtils.readInt(mCI, NumberUtils.Strategy.REVERSE_BITS)));
                txtLine.setText(String.format("%02x", line));
                txtMLine.setText(String.format("%08x", NumberUtils.readInt(mLine, NumberUtils.Strategy.REVERSE_BITS)));

                txtDecMCI.setText(String.format("%d", NumberUtils.readInt(mCI, NumberUtils.Strategy.REVERSE_BITS)));
                txtDecLine.setText(String.format("%d", line));
                txtDecMLine.setText(String.format("%d", NumberUtils.readInt(mLine, NumberUtils.Strategy.REVERSE_BITS)));

                txtBinMCI.setText(formatBinary(NumberUtils.readInt(mCI, NumberUtils.Strategy.BIG_ENDIAN)));
                txtBinLine.setText(formatBinary(line, 8));
                txtBinMLine.setText(formatBinary(NumberUtils.readInt(mLine, NumberUtils.Strategy.BIG_ENDIAN)));
            } catch (IndexOutOfBoundsException e) {
                txtLine.setText("?");
                txtDecLine.setText("?");
                txtMCI.setText("?");
                txtDecMCI.setText("?");
                txtMLine.setText("?");
                txtDecMLine.setText("?");
                txtBinMCI.setText("?");
                txtBinMLine.setText("?");
            }
        }

        private String formatBinary(int number) {
            return formatBinary(number, 32);
        }

        private String formatBinary(int number, int length) {
            return formatBinaryString(number, length, 4, true);
        }
    }

    private static JTextField readOnlyField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setFont(FONT_MONOSPACED);
        field.setHorizontalAlignment(JTextField.RIGHT);
        return field;
    }
}
