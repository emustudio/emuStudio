/**
 * AutoDialog.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import architecture.Main;

@SuppressWarnings("serial")
public class AutoDialog extends JDialog {

	public AutoDialog() {
        super();
        initComponents();
        this.setLocationRelativeTo(null);
    }

    private void initComponents() {
        JLabel lblPerforming = new JLabel();
        lblAction = new JLabel();
        btnStop = new JButton("Stop");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        lblPerforming.setFont(lblPerforming.getFont().deriveFont(lblPerforming.getFont().getStyle() | java.awt.Font.BOLD));
        lblPerforming.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/motherboard-icon.gif"))); // NOI18N
        lblPerforming.setText("Performing emulation, please wait...");

        lblAction.setText("Initializing...");
        
        btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnStopActionPerformed(e);
			}        	
        });
        btnStop.setEnabled(false);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblAction, GroupLayout.PREFERRED_SIZE, 338, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblPerforming)
                .addGroup(GroupLayout.Alignment.TRAILING,layout.createSequentialGroup()
                	.addComponent(btnStop)))
            .addContainerGap());
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblPerforming)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblAction)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnStop)
                .addContainerGap());

        pack();
    }
    
    public void setAction(String action, boolean enableButton) {
    	lblAction.setText(action);
    	lblAction.repaint();
  		btnStop.setEnabled(enableButton);
    }

    private void btnStopActionPerformed(ActionEvent e) {
    	Main.currentArch.getCPU().stop();
    }
    
    private JLabel lblAction;
    private JButton btnStop;
}
