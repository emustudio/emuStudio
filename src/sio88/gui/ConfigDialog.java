/**
 * ConfigDialog.java
 * 
 * (c) Copyright 2009, P. Jakubco
 * 
 * KISS, YAGNI
 */
package sio88.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import plugins.ISettingsHandler;
import runtime.StaticDialogs;
import sio88.Mits88SIO;

@SuppressWarnings("serial")
public class ConfigDialog extends JDialog {
	private ISettingsHandler settings;
	private long hash;
	
	public ConfigDialog(long hash, ISettingsHandler settings) {
		super((Frame)null,true);
		this.settings = settings;
		this.hash = hash;

		initComponents();
		readSettings();
		this.setLocationRelativeTo(null);
	}

	private void readSettings() {
		String s;
		s = settings.readSetting(hash, "port1");
		if (s != null) txtPort1.setText(s);
		else txtPort1.setText(String.valueOf(Mits88SIO.CPU_PORT1));
		s = settings.readSetting(hash, "port2");
		if (s != null) txtPort2.setText(s);
		else txtPort2.setText(String.valueOf(Mits88SIO.CPU_PORT2));
	}
	
	private void writeSettings() {
		settings.writeSetting(hash, "port1", txtPort1.getText());
		settings.writeSetting(hash, "port2", txtPort2.getText());
	}
	
	private void initComponents() {
		JLabel lblDesc = new JLabel("The device has 2 I/O ports (status,data)\n that must be connected to CPU.");
		JLabel lblDesc2 = new JLabel("Settings will appear after emulator restart.");
		JLabel lblPort1LBL = new JLabel("CPU port1:");
		JLabel lblStatus = new JLabel("(status)");
		JLabel lblPort2LBL = new JLabel("CPU port2:");
		JLabel lblData = new JLabel("(data)");
		txtPort1 = new JTextField();
		txtPort2 = new JTextField();
		btnOK = new JButton("OK");
		btnDefault = new JButton("Default");
		
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("MITS 88-SIO Configuration");
        setResizable(false);
		
		btnOK.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnOKActionPerformed(e);
			}
		});

		btnDefault.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnDefaultActionPerformed(e);
			}
		});
		
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(lblDesc)
						.addComponent(lblDesc2)
						.addGroup(layout.createSequentialGroup()
								.addComponent(lblPort1LBL)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(txtPort1))
						.addComponent(lblStatus)
						.addGroup(layout.createSequentialGroup()
								.addComponent(lblPort2LBL)
								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(txtPort2))
						.addComponent(lblData)
						.addGroup(GroupLayout.Alignment.TRAILING,layout.createSequentialGroup()
								.addComponent(btnDefault)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(btnOK)))
				.addContainerGap());
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(lblDesc)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(lblDesc2)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblPort1LBL)
						.addComponent(txtPort1))
				.addComponent(lblStatus)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblPort2LBL)
						.addComponent(txtPort2))
				.addComponent(lblData)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(btnDefault)
						.addComponent(btnOK))
				.addContainerGap());
		pack();
	}

	private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
		try { Integer.decode(txtPort1.getText()); }
		catch (NumberFormatException e) {
			StaticDialogs.showErrorMessage("Wrong port1 number format");
			txtPort1.grabFocus();
			return;
		}
		try { Integer.decode(txtPort2.getText()); }
		catch (NumberFormatException e) {
			StaticDialogs.showErrorMessage("Wrong port2 number format");
			txtPort2.grabFocus();
			return;
		}
		writeSettings();
		dispose();
	}
	
	private void btnDefaultActionPerformed(java.awt.event.ActionEvent evt) {
		txtPort1.setText(String.valueOf(Mits88SIO.CPU_PORT1));
		txtPort2.setText(String.valueOf(Mits88SIO.CPU_PORT2));
	}

	private JTextField txtPort1;
	private JTextField txtPort2;
	private JButton btnOK;
	private JButton btnDefault;
}

