/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.ram.CpuImpl;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;

import static net.emustudio.emulib.runtime.ui.Constants.*;

public class RamStatusPanel extends JPanel {
    private final JLabel lblStatus = new JLabel("breakpoint");
    private final JTextField txtIP = readOnlyField("0");
    private final JTextField txtInput = readOnlyField("N/A");
    private final JTextField txtOutput = readOnlyField("N/A");
    private final JTextField txtR0 = readOnlyField("0");

    public RamStatusPanel(final CpuImpl cpu, AbstractTapeContext input, AbstractTapeContext output) {
        initComponents();

        cpu.addCPUListener(new CPU.CPUListener() {
            @Override
            public void runStateChanged(CPU.RunState state) {
                lblStatus.setText(state.toString());
            }

            @Override
            public void internalStateChanged() {
                String r0 = cpu.getR0().toString();
                if (r0.isEmpty()) {
                    r0 = "<empty>";
                }
                txtR0.setText(r0);
                txtIP.setText(String.format("%04d", cpu.getInstructionLocation()));
                txtInput.setText(input.getSymbolAt(input.getHeadPosition()).map(TapeSymbol::toString).orElse("<empty>"));
                int outputPos = Math.max(0, output.getHeadPosition() - 1);
                txtOutput.setText(output.getSymbolAt(outputPos).map(TapeSymbol::toString).orElse("<empty>"));
            }
        });
    }

    private void initComponents() {
        JPanel panelInternalState = GUI.section("Internal state", "insets dialog", "[][grow]", "[][]");
        panelInternalState.add(GUI.label("R0"));
        panelInternalState.add(txtR0, "growx, wrap");
        panelInternalState.add(GUI.label("IP"));
        panelInternalState.add(txtIP, "growx");

        JPanel panelIO = GUI.section("Input / output", "insets dialog", "[][grow]", "[][]");
        panelIO.add(GUI.label("Next Input:"));
        panelIO.add(txtInput, "growx, wrap");
        panelIO.add(GUI.label("Last Output:"));
        panelIO.add(txtOutput, "growx");

        lblStatus.setFont(FONT_MONOSPACED_BIG_BOLD);
        lblStatus.setForeground(CPU_RUN_STATE_COLOR);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panelRunState = GUI.section("Run state", "insets dialog", "[grow]", "[]");
        panelRunState.add(lblStatus, "growx");

        setLayout(new net.miginfocom.swing.MigLayout("insets dialog", "[grow]", "[][][]"));
        add(panelInternalState, "growx, wrap");
        add(panelIO, "growx, wrap");
        add(panelRunState, "growx");
    }

    private static JTextField readOnlyField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setFont(FONT_MONOSPACED);
        return field;
    }
}
