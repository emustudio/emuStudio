/*
 * ConfigDialog.java
 *
 * Created on Streda, 2008, január 2, 13:32
 */

package terminal.gui;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import plugins.ISettingsHandler;

import terminal.TerminalDisplay;
import terminal.gui.TerminalWindow;


/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class ConfigDialog extends JDialog {
	private ISettingsHandler settings;
	private long hash;

	private TerminalDisplay terminal;
    private TerminalWindow window;
    
    /** Creates new form ConfigDialog */
    public ConfigDialog(ISettingsHandler settings, long hash, TerminalWindow window, 
    		TerminalDisplay lblTerminal) {
    	super((JFrame)null,true);
        initComponents();
        
        this.settings = settings;
        this.hash = hash;
        this.terminal = lblTerminal;
        this.window = window;

        readSettings();
        this.setLocationRelativeTo(null);
    }
    
    private void readSettings() {
    	String s;
    	s = settings.readSetting(hash, "duplex_mode");
        if (s != null && s.toUpperCase().equals("HALF"))
        	radioHalf.setSelected(true);
        else radioFull.setSelected(true);
        
        s = settings.readSetting(hash, "always_on_top");
        if (s != null && s.toUpperCase().equals("TRUE"))
        	chkAlwaysOnTop.setSelected(true);
        else chkAlwaysOnTop.setSelected(false);
        
        s = settings.readSetting(hash, "anti_aliasing");
        if (s != null && s.toUpperCase().equals("TRUE"))
        	chkAntiAliasing.setSelected(true);
        else chkAntiAliasing.setSelected(false);    	
    }
    
    private void saveSettings() {
    	if (radioHalf.isSelected())
    		settings.writeSetting(hash, "duplex_mode", "half");
    	else settings.writeSetting(hash, "duplex_mode", "full");
    	if (chkAlwaysOnTop.isSelected())
    		settings.writeSetting(hash, "always_on_top", "true");
    	else settings.writeSetting(hash, "always_on_top", "false");
    	if (chkAntiAliasing.isSelected())
    		settings.writeSetting(hash, "anti_aliasing", "true");
    	else settings.writeSetting(hash, "anti_aliasing", "false");
    }
    
    private void initComponents() {
        ButtonGroup btnGroup = new ButtonGroup();
        JPanel controlPanel = new JPanel();
        chkAlwaysOnTop = new JCheckBox();
        JButton btnClearScreen = new JButton();
        JButton btnRollLine = new JButton();
        chkAntiAliasing = new JCheckBox();
        radioFull = new JRadioButton();
        radioHalf = new JRadioButton();
        chkSaveSettings = new JCheckBox();
        JButton btnOK = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Terminal configuration");
        setResizable(false);

        controlPanel.setBorder(BorderFactory.createTitledBorder("Display"));

        chkAlwaysOnTop.setText("Always on top");

        btnClearScreen.setText("Clear screen");
        btnClearScreen.setFocusable(false);
        btnClearScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearScreenActionPerformed(evt);
            }
        });

        btnRollLine.setText("Roll line");
        btnRollLine.setFocusable(false);
        btnRollLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRollLineActionPerformed(evt);
            }
        });

        chkAntiAliasing.setText("Use anti-aliasing");

        btnGroup.add(radioFull);
        radioFull.setText("Full duplex mode");
        btnGroup.add(radioHalf);
        radioHalf.setText("Half duplex mode");
        
        chkSaveSettings.setText("Save settings");
        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });
        
        GroupLayout controlPanelLayout = new GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        
        controlPanelLayout.setHorizontalGroup(
        		controlPanelLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(controlPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(radioFull)
        				.addComponent(radioHalf)
        				.addComponent(chkAlwaysOnTop)
        				.addComponent(chkAntiAliasing)
        				.addGroup(controlPanelLayout.createSequentialGroup()
        						.addComponent(btnClearScreen)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(btnRollLine)))
        		.addContainerGap());
        controlPanelLayout.setVerticalGroup(
        		controlPanelLayout.createSequentialGroup()
        		.addContainerGap()
        		.addComponent(radioFull)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(radioHalf)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addComponent(chkAlwaysOnTop)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(chkAntiAliasing)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(controlPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(btnClearScreen)
        				.addComponent(btnRollLine))
        		.addContainerGap());
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(
        		layout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(controlPanel)
        				.addComponent(chkSaveSettings)
        				.addGroup(GroupLayout.Alignment.TRAILING,layout.createSequentialGroup()
        						.addComponent(btnOK)))
        		.addContainerGap());
        layout.setVerticalGroup(
        		layout.createSequentialGroup()
        		.addContainerGap()
        		.addComponent(controlPanel)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addComponent(chkSaveSettings)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(btnOK)
        		.addContainerGap());
        pack();
    }

    private void btnClearScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearScreenActionPerformed
        terminal.clear_screen();
    }//GEN-LAST:event_btnClearScreenActionPerformed

    private void btnRollLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRollLineActionPerformed
        terminal.roll_line();
    }//GEN-LAST:event_btnRollLineActionPerformed

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
    	terminal.setAntiAliasing(chkAntiAliasing.isSelected());
    	window.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
    	window.setHalfDuplex(radioHalf.isSelected());
    	if (chkSaveSettings.isSelected())
    		saveSettings();
    	dispose();
    }
    
    private JRadioButton radioFull;
    private JRadioButton radioHalf;
    private JCheckBox chkAlwaysOnTop;
    private JCheckBox chkAntiAliasing;
    private JCheckBox chkSaveSettings;
    
}
