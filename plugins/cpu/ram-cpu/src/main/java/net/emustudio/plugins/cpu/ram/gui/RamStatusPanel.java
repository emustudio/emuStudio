/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.ram.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.ram.CpuImpl;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;

import javax.swing.*;

import static net.emustudio.plugins.cpu.ram.gui.Constants.MONOSPACED_BIG_BOLD;
import static net.emustudio.plugins.cpu.ram.gui.Constants.MONOSPACED_PLAIN;

public class RamStatusPanel extends JPanel {
    private final JLabel lblStatus = new JLabel("breakpoint");
    private final JTextField txtIP = new JTextField("0");
    private final JTextField txtInput = new JTextField("N/A");
    private final JTextField txtOutput = new JTextField("N/A");
    private final JTextField txtR0 = new JTextField("0");

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
                if (r0.equals("")) {
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
        JPanel panelInternalState = new JPanel();
        JLabel lblR0 = new JLabel("R0");
        JLabel lblIP = new JLabel("IP");
        JPanel jPanel2 = new JPanel();
        JPanel jPanel3 = new JPanel();
        JLabel lblNextInput = new JLabel("Next Input:");
        JLabel lblLastOutput = new JLabel("Last Output:");

        panelInternalState.setBorder(BorderFactory.createTitledBorder("Internal state"));

        lblR0.setFont(MONOSPACED_PLAIN);
        lblIP.setFont(MONOSPACED_PLAIN);

        txtR0.setEditable(false);
        txtR0.setFont(MONOSPACED_PLAIN);

        txtIP.setEditable(false);
        txtIP.setFont(MONOSPACED_PLAIN);

        GroupLayout panelInternalStateLayout = new GroupLayout(panelInternalState);
        panelInternalState.setLayout(panelInternalStateLayout);
        panelInternalStateLayout.setHorizontalGroup(
                panelInternalStateLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelInternalStateLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelInternalStateLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panelInternalStateLayout.createSequentialGroup()
                                                .addComponent(lblR0)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtR0, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelInternalStateLayout.createSequentialGroup()
                                                .addComponent(lblIP)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtIP)))
                                .addContainerGap(64, Short.MAX_VALUE))
        );
        panelInternalStateLayout.setVerticalGroup(
                panelInternalStateLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelInternalStateLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelInternalStateLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblR0)
                                        .addComponent(txtR0, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelInternalStateLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblIP)
                                        .addComponent(txtIP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(BorderFactory.createTitledBorder("Run state"));

        lblStatus.setFont(MONOSPACED_BIG_BOLD);
        lblStatus.setForeground(new java.awt.Color(0, 153, 51));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblStatus)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(BorderFactory.createTitledBorder("Input / output"));

        txtOutput.setEditable(false);
        txtOutput.setFont(MONOSPACED_PLAIN);

        txtInput.setEditable(false);
        txtInput.setFont(MONOSPACED_PLAIN);

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblLastOutput)
                                        .addComponent(lblNextInput))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtOutput, GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                                        .addComponent(txtInput))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblNextInput)
                                        .addComponent(txtInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblLastOutput)
                                        .addComponent(txtOutput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(panelInternalState, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelInternalState, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }
}
