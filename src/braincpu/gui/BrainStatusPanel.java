/**
 * BrainStatusPanel.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package braincpu.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;

import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext.ICPUListener;
import plugins.memory.IMemoryContext;

import braincpu.impl.BrainCPU;

@SuppressWarnings("serial")
public class BrainStatusPanel extends JPanel {
	public BrainStatusPanel(final BrainCPU cpu, final IMemoryContext mem) {
        initComponents();

        cpu.getContext().addCPUListener(new ICPUListener() {
			@Override
			public void runChanged(EventObject evt, int state) {
	            switch (state) {
	                case ICPU.STATE_STOPPED_NORMAL:
	                	lblStatus.setText("stopped (normal)"); 
	                	break;
	                case ICPU.STATE_STOPPED_BREAK:
	                	lblStatus.setText("breakpoint"); 
	                	break;
	                case ICPU.STATE_STOPPED_ADDR_FALLOUT:
	                	lblStatus.setText("stopped (address fallout)"); 
	                	break;
	                case ICPU.STATE_STOPPED_BAD_INSTR:
	                	lblStatus.setText("stopped (instruction fallout)");
	                	break;
	            }

			}
			@Override
			public void stateUpdated(EventObject evt) {
				txtP.setText(String.format("%04X", cpu.getP()));
				txtIP.setText(String.format("%04X", cpu.getIP()));
				txtMemP.setText(String.format("%02X", (Short)mem.read(cpu.getP())));
			}
        	
        });
	}
	
	private void initComponents() {
		JLabel lblP = new JLabel("P");
		JLabel lblIP = new JLabel("IP");
		txtP = new JTextField("0000");
		txtIP = new JTextField("0000");
		lblStatus = new JLabel("breakpoint");
		JLabel lblMemP = new JLabel("mem(P):");
		txtMemP = new JTextField("00");
		
		lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
		txtP.setEditable(false);
        txtP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));
        txtIP.setEditable(false);
        txtIP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));
		txtMemP.setEditable(false);
        txtMemP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
        		layout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addGroup(layout.createSequentialGroup()
        		        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		        				.addComponent(lblP)
        		        				.addComponent(lblIP)
        		        				.addComponent(lblMemP))
		                	    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
		                	    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		                	    		.addComponent(txtP,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
		                	    		.addComponent(txtIP,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
		                	    		.addComponent(txtMemP,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)))
		                .addComponent(lblStatus,GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
		                .addContainerGap(20,Short.MAX_VALUE));
        layout.setVerticalGroup(
        		layout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblP)
        				.addComponent(txtP))
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblIP)
        				.addComponent(txtIP))
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblMemP)
        				.addComponent(txtMemP))
        	    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        	    .addComponent(lblStatus)
        	    .addContainerGap());
	}
	private JTextField txtP;
	private JTextField txtIP;
	private JLabel lblStatus;
	private JTextField txtMemP;
}
