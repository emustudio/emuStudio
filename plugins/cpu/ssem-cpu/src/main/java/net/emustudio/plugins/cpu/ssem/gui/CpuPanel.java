/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.ssem.EmulatorEngine;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static net.emustudio.emulib.runtime.helpers.RadixUtils.formatBinaryString;
import static net.emustudio.emulib.runtime.ui.Constants.*;

public class CpuPanel extends JPanel {
    private final GUI gui;
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

    public CpuPanel(CPU cpu, EmulatorEngine engine, MemoryContext<Byte> memory, GUI gui) {
        this.gui = gui;
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
        JPanel panelRun = gui.section("Run control", "insets dialog", "[grow]push[]6[]", "[]");
        panelRun.add(lblRunState);
        panelRun.add(lblSpeed);
        panelRun.add(gui.labelBold("ins/s"));

        // Columns: label, hex, dec, binary
        // Registers
        JPanel panelRegs = gui.section("Registers", "insets dialog", "[]6[80!]6[80!]6[grow]", "[][]");
        panelRegs.add(gui.label("A"));
        panelRegs.add(txtA, "growx");
        panelRegs.add(txtDecA, "growx");
        panelRegs.add(txtBinA, "growx, wrap");
        panelRegs.add(gui.label("CI"));
        panelRegs.add(txtCI, "growx");
        panelRegs.add(txtDecCI, "growx");
        panelRegs.add(txtBinCI, "growx");

        // Memory snippet
        JPanel panelMem = gui.section("Memory snippet", "insets dialog", "[]6[80!]6[80!]6[grow]", "[][][]");
        panelMem.add(gui.label("M[CI]"));
        panelMem.add(txtMCI, "growx");
        panelMem.add(txtDecMCI, "growx");
        panelMem.add(txtBinMCI, "growx, wrap");
        panelMem.add(gui.label("line"));
        panelMem.add(txtLine, "growx");
        panelMem.add(txtDecLine, "growx");
        panelMem.add(txtBinLine, "growx, wrap");
        panelMem.add(gui.label("M[line]"));
        panelMem.add(txtMLine, "growx");
        panelMem.add(txtDecMLine, "growx");
        panelMem.add(txtBinMLine, "growx");

        setLayout(new BorderLayout());
        JPanel content = gui.panel("insets dialog", "[grow]", "[][][]");
        content.add(panelRegs, "growx, wrap");
        content.add(panelMem, "growx, wrap");
        content.add(panelRun, "growx");
        add(content, BorderLayout.CENTER);
    }

    private final class Updater implements CPU.CPUListener {

        @Override
        public void runStateChanged(CPU.RunState rs) {
            SwingUtilities.invokeLater(() -> lblRunState.setText(rs.toString().toUpperCase()));
        }

        @Override
        public void internalStateChanged() {
            final int acc = engine.Acc.get();
            final int ci = engine.CI.get();
            final String accHex = String.format("%08x", acc);
            final String accDec = String.format("%d", acc);
            final String ciHex = String.format("%08x", ci / 4);
            final String ciDec = String.format("%d", ci / 4);
            final String accBinary = formatBinary(acc);
            final String ciBinary = formatBinary(ci);
            final String[] memoryValues = new String[9];

            try {
                Byte[] mCI = memory.read(ci, 4);
                byte line = (byte) NumberUtils.reverseBits(mCI[0] & 0b11111000, 8);
                Byte[] mLine = memory.read(line * 4, 4);

                memoryValues[0] = String.format("%08x", NumberUtils.readInt(mCI, NumberUtils.Strategy.REVERSE_BITS));
                memoryValues[1] = String.format("%02x", line);
                memoryValues[2] = String.format("%08x", NumberUtils.readInt(mLine, NumberUtils.Strategy.REVERSE_BITS));
                memoryValues[3] = String.format("%d", NumberUtils.readInt(mCI, NumberUtils.Strategy.REVERSE_BITS));
                memoryValues[4] = String.format("%d", line);
                memoryValues[5] = String.format("%d", NumberUtils.readInt(mLine, NumberUtils.Strategy.REVERSE_BITS));
                memoryValues[6] = formatBinary(NumberUtils.readInt(mCI, NumberUtils.Strategy.BIG_ENDIAN));
                memoryValues[7] = formatBinary(line, 8);
                memoryValues[8] = formatBinary(NumberUtils.readInt(mLine, NumberUtils.Strategy.BIG_ENDIAN));
            } catch (IndexOutOfBoundsException e) {
                for (int i = 0; i < memoryValues.length; i++) {
                    memoryValues[i] = "?";
                }
            }

            SwingUtilities.invokeLater(() -> {
                txtA.setText(accHex);
                txtDecA.setText(accDec);
                txtCI.setText(ciHex);
                txtDecCI.setText(ciDec);
                txtBinA.setText(accBinary);
                txtBinCI.setText(ciBinary);
                txtMCI.setText(memoryValues[0]);
                txtLine.setText(memoryValues[1]);
                txtMLine.setText(memoryValues[2]);
                txtDecMCI.setText(memoryValues[3]);
                txtDecLine.setText(memoryValues[4]);
                txtDecMLine.setText(memoryValues[5]);
                txtBinMCI.setText(memoryValues[6]);
                txtBinLine.setText(memoryValues[7]);
                txtBinMLine.setText(memoryValues[8]);
            });
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
