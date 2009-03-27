/**
 * RAMStatusPanel.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package ramcpu.gui;

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

import ramcpu.impl.RAM;

@SuppressWarnings("serial")
public class RAMStatusPanel extends JPanel {
	public RAMStatusPanel(final RAM cpu, final IMemoryContext mem) {
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
				String s = cpu.getR0();
				if (s == null || s.equals("")) s = "<empty>";
				txtR0.setText(s);
				txtIP.setText(String.format("%04d", cpu.getInstrPosition()));
			}
        	
        });
	}
	
	private void initComponents() {
		JLabel lblR0 = new JLabel("R0");
		JLabel lblIP = new JLabel("IP");
		txtR0 = new JTextField("<empty>");
		txtIP = new JTextField("0000");
		lblStatus = new JLabel("breakpoint");
		
		lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
		txtR0.setEditable(false);
        txtR0.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));
        txtIP.setEditable(false);
        txtIP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
        		layout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addGroup(layout.createSequentialGroup()
        		        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		        				.addComponent(lblR0)
        		        				.addComponent(lblIP))
		                	    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
		                	    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		                	    		.addComponent(txtR0,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
		                	    		.addComponent(txtIP,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)))
		                .addComponent(lblStatus,GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
		                .addContainerGap(20,Short.MAX_VALUE));
        layout.setVerticalGroup(
        		layout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblR0)
        				.addComponent(txtR0))
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblIP)
        				.addComponent(txtIP))
        	    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        	    .addComponent(lblStatus)
        	    .addContainerGap());
	}
	private JTextField txtR0;
	private JTextField txtIP;
	private JLabel lblStatus;
}
