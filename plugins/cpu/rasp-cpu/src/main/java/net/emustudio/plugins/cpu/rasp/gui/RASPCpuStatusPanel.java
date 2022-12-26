/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2006-2022  Peter Jakubčo
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

package net.emustudio.plugins.cpu.rasp.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.rasp.CpuImpl;

import javax.swing.*;
import java.awt.*;

public class RASPCpuStatusPanel extends JPanel {

    private javax.swing.JTextPane accValue;
    private javax.swing.JTextPane instrPointerValue;
    private javax.swing.JLabel runState;
    public RASPCpuStatusPanel(CpuImpl cpu) {
        initComponents();
        cpu.addCPUListener(new CPU.CPUListener() {

            @Override
            public void runStateChanged(CPU.RunState state) {
                switch (state) {
                    case STATE_STOPPED_NORMAL:
                        runState.setText("NORMAL  - STOPPED");
                        break;
                    case STATE_STOPPED_BREAK:
                        runState.setText("BREAKPOINT");
                        break;
                    case STATE_STOPPED_ADDR_FALLOUT:
                        runState.setText("ADDRESS FALLOUT - STOPPED");
                        break;
                    case STATE_STOPPED_BAD_INSTR:
                        runState.setText("INSTRUCTION FALLOUT - STOPPED");
                        break;
                }
            }

            @Override
            public void internalStateChanged() {
                accValue.setText(String.valueOf(cpu.getACC()));
                instrPointerValue.setText(String.valueOf(cpu.getInstructionLocation()));
            }
        });
    }

    private void initComponents() {

        runState = new javax.swing.JLabel();
        JPanel jPanel1 = new JPanel();
        JLabel jLabel1 = new JLabel();
        JLabel jLabel2 = new JLabel();
        accValue = new javax.swing.JTextPane();
        instrPointerValue = new javax.swing.JTextPane();
        JLabel jLabel3 = new JLabel();

        runState.setFont(new java.awt.Font("DejaVu Sans", Font.BOLD, 13));
        runState.setText("BREAKPOINT");

        jLabel1.setText("ACCUMULATOR (R0):");

        jLabel2.setText("INSTRUCTION POINTER (IP):");

        accValue.setEditable(false);
        accValue.setText("0");

        instrPointerValue.setEditable(false);
        instrPointerValue.setText("0");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(accValue)
                                        .addComponent(instrPointerValue)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(accValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(instrPointerValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel3.setText("RUNNING STATUS:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(0, 60, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel3)
                                        .addComponent(runState))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(9, 9, 9)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(runState)
                                .addContainerGap(186, Short.MAX_VALUE))
        );
    }
}
