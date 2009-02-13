/**
 * BrainStatusPanel.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
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

import braincpu.impl.BrainCPU;

@SuppressWarnings("serial")
public class BrainStatusPanel extends JPanel {
	public BrainStatusPanel(final BrainCPU cpu) {
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
			}
        	
        });
	}
	
	private void initComponents() {
		JLabel lblP = new JLabel("P");
		JLabel lblIP = new JLabel("IP");
		txtP = new JTextField("0000");
		txtIP = new JTextField("0000");
		lblStatus = new JLabel("breakpoint");
		
		lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD));
		txtP.setEditable(false);
        txtP.setBorder(BorderFactory.createMatteBorder(1, 5, 1, 1,Color.lightGray));
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
        		        				.addComponent(lblP)
        		        				.addComponent(lblIP))
		                	    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
		                	    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
		                	    		.addComponent(txtP,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)
		                	    		.addComponent(txtIP,GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE)))
		                .addComponent(lblStatus))
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
        	    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        	    .addComponent(lblStatus)
        	    .addContainerGap());
	}
	private JTextField txtP;
	private JTextField txtIP;
	private JLabel lblStatus;
}
