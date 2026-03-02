/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.util.Objects;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_MOTHERBOARD;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

/**
 * This is the dialog form that displays when the emuStudio automatization
 * is running.
 */
public class AutoDialog extends DialogBase {
    private final VirtualComputer computer;

    private final JLabel lblAction = GUI.label("Initializing...");
    private final JButton btnStop = new JButton("Stop");

    public AutoDialog(VirtualComputer computer) {
        super((JFrame) null, "Automatic Emulation", false);
        this.computer = Objects.requireNonNull(computer);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = GUI.panelHorizontal();

        JLabel lblPerforming = GUI.labelBold("Running automatic emulation, please wait...");
        lblPerforming.setIcon(loadIcon(ICON_MOTHERBOARD));

        btnStop.addActionListener(e -> computer.getCPU().ifPresent(CPU::stop));
        btnStop.setEnabled(false);

        panel.add(lblPerforming, "wrap, gapbottom 10");
        panel.add(lblAction, "growx, wrap, gapbottom 10");
        panel.add(btnStop, "align right");

        return panel;
    }

    /**
     * Sets abstract action. The abstract action is represented by a state
     * string and whether to enable the "Stop" button.
     *
     * @param action           action to show in the dialog
     * @param enableStopButton whether to enable the "Stop" button
     */
    public void setAction(String action, boolean enableStopButton) {
        lblAction.setText(action);
        lblAction.repaint();
        btnStop.setEnabled(enableStopButton);
    }
}
