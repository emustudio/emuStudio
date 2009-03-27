/**
 * SettingsDialog.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
 */
package abstracttape.gui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import plugins.ISettingsHandler;

@SuppressWarnings("serial")
public class SettingsDialog extends JDialog {
	private ISettingsHandler settings;
	private long hash;
	private TapeDialog gui;
	
	public SettingsDialog(ISettingsHandler settings, long hash, TapeDialog gui) {
		this.settings = settings;
		this.hash = hash;
		initComponents();
		String s = settings.readSetting(hash, "alwaysOnTop");
		boolean b;
		if (s == null || !s.equals("true"))
			b = false;
		else
			b = true;
		
		chkAlwaysOnTop.setSelected(b);
		this.gui = gui;
	}
	
	public void initComponents() {
		chkAlwaysOnTop = new JCheckBox("Always on top");
		JButton btnOK = new JButton("OK");
			
		setTitle("AbstractTape settings");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		chkAlwaysOnTop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (gui != null)
					gui.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
			}
		});
		btnOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (chkAlwaysOnTop.isSelected())
					settings.writeSetting(hash, "alwaysOnTop", "true");
				else
					settings.writeSetting(hash, "alwaysOnTop", "false");
			}
		});
		
		Container pane = this.getContentPane();
		GroupLayout layout = new GroupLayout(pane);
		pane.setLayout(layout);
		
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(chkAlwaysOnTop)
						.addComponent(btnOK))
				.addContainerGap());
		layout.setVerticalGroup(
				layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(chkAlwaysOnTop)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
				.addComponent(btnOK));
		pack();
	}
	private JCheckBox chkAlwaysOnTop;
}
