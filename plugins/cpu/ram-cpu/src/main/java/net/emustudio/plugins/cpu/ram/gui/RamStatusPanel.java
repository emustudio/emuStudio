/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.ram.CpuImpl;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import net.emustudio.emulib.runtime.ExecutionState;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

import static net.emustudio.emulib.runtime.ui.Constants.*;

public class RamStatusPanel extends JPanel {
    private final GUI gui;
    private final CpuImpl cpu;
    private final AbstractTapeContext input, output;
    private final ExecutionState execution;
    private final JLabel lblStatus = new JLabel("breakpoint");
    private final JTextField txtIP = readOnlyField("0");
    private final JTextField txtInput = readOnlyField("N/A");
    private final JTextField txtOutput = readOnlyField("N/A");
    private final JTextField txtR0 = readOnlyField("0");

    public RamStatusPanel(final CpuImpl cpu, AbstractTapeContext input, AbstractTapeContext output, GUI gui, ExecutionState execution) {
        this.gui = gui;
        this.cpu = cpu;
        this.input = input;
        this.output = output;
        this.execution = execution;
        initComponents();
        add(cpu.debuggingEditor(), BorderLayout.SOUTH);

        PanelUpdater updater = new PanelUpdater();
        observeSnapshots(snapshot -> {
            updater.runStateChanged(snapshot.runState());
            if (!snapshot.isBusy() && snapshot.runState() != CPU.RunState.STATE_RUNNING) updater.internalStateChanged();
        });
    }

    private void initComponents() {
        JPanel panelInternalState = gui.section("Internal state", "insets dialog", "[][grow]", "[][]");
        panelInternalState.add(gui.label("R0"));
        panelInternalState.add(txtR0, "growx, wrap");
        panelInternalState.add(gui.label("IP"));
        panelInternalState.add(txtIP, "growx");

        JPanel panelIO = gui.section("Input / output", "insets dialog", "[][grow]", "[][]");
        panelIO.add(gui.label("Next Input:"));
        panelIO.add(txtInput, "growx, wrap");
        panelIO.add(gui.label("Last Output:"));
        panelIO.add(txtOutput, "growx");

        lblStatus.setFont(FONT_MONOSPACED_BIG_BOLD);
        lblStatus.setForeground(CPU_RUN_STATE_COLOR);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panelRunState = gui.section("Run state", "insets dialog", "[grow]", "[]");
        panelRunState.add(lblStatus, "growx");

        setLayout(new BorderLayout());
        JPanel content = gui.panel("insets dialog", "[grow]", "[][][]");
        content.add(panelInternalState, "growx, wrap");
        content.add(panelIO, "growx, wrap");
        content.add(panelRunState, "growx");
        add(content, BorderLayout.CENTER);
    }

    private static JTextField readOnlyField(String text) {
        JTextField field = new JTextField(text);
        field.setEditable(false);
        field.setFont(FONT_MONOSPACED);
        return field;
    }

    private final class PanelUpdater {
        public void runStateChanged(CPU.RunState state) {
            SwingUtilities.invokeLater(() -> lblStatus.setText(state.toString()));
        }

        public void internalStateChanged() {
            String r0 = cpu.getR0().toString();
            if (r0.isEmpty()) {
                r0 = "<empty>";
            }
            int outputPos = Math.max(0, output.getHeadPosition() - 1);
            String finalR0 = r0;
            int instruction = cpu.getInstructionLocation();
            String nextInput = input.getSymbolAt(input.getHeadPosition()).map(TapeSymbol::toString).orElse("<empty>");
            String lastOutput = output.getSymbolAt(outputPos).map(TapeSymbol::toString).orElse("<empty>");
            SwingUtilities.invokeLater(() -> {
                txtR0.setText(finalR0);
                txtIP.setText(String.format("%04d", instruction));
                txtInput.setText(nextInput);
                txtOutput.setText(lastOutput);
            });
        }

    }

    private Consumer<ExecutionState.Snapshot> snapshotListener;
    private ExecutionState.Subscription subscription;

    private void observeSnapshots(Consumer<ExecutionState.Snapshot> listener) {
        snapshotListener = listener;
        subscription = execution.subscribe(listener);
    }

    @Override public void addNotify() {
        super.addNotify();
        if (subscription == null) subscription = execution.subscribe(snapshotListener);
    }

    @Override public void removeNotify() {
        if (subscription != null) subscription.close();
        subscription = null;
        super.removeNotify();
    }
}
