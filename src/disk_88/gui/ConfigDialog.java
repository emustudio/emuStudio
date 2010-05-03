/**
 * ConfigDialog.java
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
package disk_88.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import disk_88.Drive;
import disk_88.DiskImpl;
import disk_88.gui.utils.NiceButton;

import plugins.ISettingsHandler;
import runtime.StaticDialogs;

@SuppressWarnings("serial")
public class ConfigDialog extends JDialog {
    private ISettingsHandler settings;
    private ArrayList<Drive> drives;
    private long hash;
    private DiskFrame gui;
    
    public ConfigDialog(long hash, ISettingsHandler settings, 
    		ArrayList<Drive> drives, DiskFrame gui) {
    	initComponents();
    	this.hash = hash;
    	this.settings = settings;
    	this.drives = drives;
    	this.gui = gui;
    	readSettings();
        driveCombo.setSelectedIndex(0);
        updateGUI(0);
    	setLocationRelativeTo(null);
    }
    
    private void readSettings() {
    	String s;
    	s = settings.readSetting(hash, "always_on_top");
    	if (s != null && s.toUpperCase().equals("TRUE"))
    		chkAlwaysOnTop.setSelected(true);
    	else chkAlwaysOnTop.setSelected(false);
    	s = settings.readSetting(hash, "port1CPU");
    	if (s != null) txtPort1.setText(s);
    	s = settings.readSetting(hash, "port2CPU");
    	if (s != null) txtPort2.setText(s);
    	s = settings.readSetting(hash, "port3CPU");
    	if (s != null) txtPort3.setText(s);
    }

    private void writeSettings() {
    	if (chkAlwaysOnTop.isSelected())
    		settings.writeSetting(hash, "always_on_top", "true");
    	else settings.writeSetting(hash, "always_on_top", "false");
    	settings.writeSetting(hash, "port1CPU", txtPort1.getText());
    	settings.writeSetting(hash, "port2CPU", txtPort2.getText());
    	settings.writeSetting(hash, "port3CPU", txtPort3.getText());
    		
    	for (int i = 0; i < 16; i++) {
            File f = ((Drive)drives.get(i)).getImageFile();
            if (f != null)
            	settings.writeSetting(hash, "image"+i, f.getAbsolutePath());
            else settings.removeSetting(hash, "image"+i);    		
    	}
    }
    
    private void updateGUI(int drive) {
        File f = ((Drive)drives.get(drive)).getImageFile();
        if (f != null) {
            txtImage.setText(f.getAbsolutePath());
            umountButton.setEnabled(true);
        } else {
            txtImage.setText("");
            umountButton.setEnabled(false);
        }
    }
    
    private void initComponents() {
        driveCombo = new JComboBox();
        JLabel lblImage = new JLabel("Image:");
        txtImage = new JTextField();
        browseButton = new NiceButton("Browse...");
        mountButton = new NiceButton("Mount");
        umountButton = new NiceButton("Un-mount");
        createButton = new NiceButton("Create...");
        JPanel panelImages = new JPanel();
        JPanel panelOther = new JPanel();
        chkAlwaysOnTop = new JCheckBox("GUI always on top");
        btnOK = new NiceButton("OK");
        chkSaveSettings = new JCheckBox("Save settings");
        JPanel panelPorts = new JPanel();
        JLabel lblWarning = new JLabel("These settings will be affected after restart");
        JLabel lblPort1 = new JLabel("Port1:");
        JLabel lblPort1D = new JLabel("(IN: flags, OUT: select/unselect drive)");
        JLabel lblPort2 = new JLabel("Port2:");
        JLabel lblPort2D = new JLabel("(IN: current sector, OUT: set flags)");
        JLabel lblPort3 = new JLabel("Port3:");
        JLabel lblPort3D = new JLabel("(IN: read data, OUT: write data)");
        btnDefault = new NiceButton("Default");
        txtPort1 = new JTextField("0x8");
        txtPort2 = new JTextField("0x9");
        txtPort3 = new JTextField("0xA");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MITS 88-DISK Configuration");
        setResizable(false);

        driveCombo.setModel(new DefaultComboBoxModel(
        		new String[] { "Drive 0 (A)", "Drive 1 (B)", "Drive 2 (C)",
        				"Drive 3 (D)", "Drive 4 (E)", "Drive 5 (F)", "Drive 6 (G)",
        				"Drive 7 (H)", "Drive 8 (I)", "Drive 9 (J)", "Drive 10 (K)",
        				"Drive 11 (L)", "Drive 12 (M)", "Drive 13 (N)", "Drive 14 (O)",
        				"Drive 15 (P)" }));
        driveCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                driveComboItemStateChanged(evt);
            }
        });
        txtImage.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {}
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                txtImageInputMethodTextChanged(evt);
            }
        });
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });
        mountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mountButtonActionPerformed(evt);
            }
        });
        umountButton.setEnabled(false);
        umountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                umountButtonActionPerformed(evt);
            }
        });
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createButtonActionPerformed(evt);
            }
        });
        
        GroupLayout panelImagesLayout = new GroupLayout(panelImages);
        panelImages.setLayout(panelImagesLayout);
        panelImagesLayout.setHorizontalGroup(
            panelImagesLayout.createSequentialGroup()
	        .addContainerGap()
	        .addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
	            .addComponent(driveCombo) //, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
	            .addComponent(lblImage)
	            .addGroup(GroupLayout.Alignment.TRAILING, panelImagesLayout.createSequentialGroup()
	                .addComponent(txtImage, GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
	                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
	                .addComponent(browseButton)
	                .addContainerGap())
	            .addGroup(panelImagesLayout.createSequentialGroup()
	                .addComponent(mountButton)
	                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
	                .addComponent(umountButton)
	                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
	                .addComponent(createButton)
	                .addContainerGap()))
	        .addContainerGap());
        panelImagesLayout.setVerticalGroup(
            panelImagesLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(driveCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lblImage)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(txtImage) //, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(browseButton))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(mountButton)
                .addComponent(umountButton)
                .addComponent(createButton))            
            .addContainerGap());

        btnDefault.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnDefaultActionPerformed(e);
			}        	
        });
        
        GroupLayout panelPortsLayout = new GroupLayout(panelPorts);
        panelPorts.setLayout(panelPortsLayout);
        panelPortsLayout.setHorizontalGroup(
        		panelPortsLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblWarning)
        				.addGroup(panelPortsLayout.createSequentialGroup()
        						.addComponent(lblPort1)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(txtPort1))
        				.addComponent(lblPort1D)
        				.addGroup(panelPortsLayout.createSequentialGroup()
        						.addComponent(lblPort2)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(txtPort2))
        				.addComponent(lblPort2D)
        				.addGroup(panelPortsLayout.createSequentialGroup()
        						.addComponent(lblPort3)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(txtPort3))
        				.addComponent(lblPort3D)
        				.addGroup(GroupLayout.Alignment.TRAILING, panelPortsLayout.createSequentialGroup()
        						.addComponent(btnDefault)))
        		.addContainerGap());
        panelPortsLayout.setVerticalGroup(
        		panelPortsLayout.createSequentialGroup()
        		.addContainerGap()
        		.addComponent(lblWarning)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPort1)
        				.addComponent(txtPort1))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(lblPort1D)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPort2)
        				.addComponent(txtPort2))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(lblPort2D)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(panelPortsLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPort3)
        				.addComponent(txtPort3))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(lblPort3D)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addComponent(btnDefault)
        		.addContainerGap());
        
        GroupLayout panelOtherLayout = new GroupLayout(panelOther);
        panelOther.setLayout(panelOtherLayout);
        panelOtherLayout.setHorizontalGroup(
            panelOtherLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelOtherLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkAlwaysOnTop)
                        .addComponent(chkSaveSettings))
                .addContainerGap());
        panelOtherLayout.setVerticalGroup(
            panelOtherLayout.createSequentialGroup()
            	.addContainerGap()
                .addComponent(chkAlwaysOnTop)
                .addComponent(chkSaveSettings)
                .addContainerGap());

        btnOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnOKActionPerformed(e);
			}        	
        });
        
        JTabbedPane tab = new JTabbedPane();
        
        tab.addTab("Mounted images", panelImages);
        tab.addTab("CPU ports connection", panelPorts);
        tab.addTab("Other", panelOther);
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tab)
                .addContainerGap())
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            		.addContainerGap()
            		.addComponent(btnOK)
            		.addContainerGap()));
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tab)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnOK)
                .addContainerGap());
        pack();
    }

    private void driveComboItemStateChanged(java.awt.event.ItemEvent evt) {
        int i = driveCombo.getSelectedIndex();
        updateGUI(i);
    }
    
    private void txtImageInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
        if (txtImage.getText().equals("")) mountButton.setEnabled(false);
        else mountButton.setEnabled(true);
    }
    private void umountButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int i = driveCombo.getSelectedIndex();
        ((Drive)drives.get(i)).umount();
        updateGUI(i);
    }

    private void mountButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int i = driveCombo.getSelectedIndex();
        try { ((Drive) drives.get(i)).mount(txtImage.getText()); }
        catch (IOException ex) {
            StaticDialogs.showErrorMessage(ex.getMessage());
            txtImage.grabFocus();
        }
        updateGUI(i);
    }
    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser f = new JFileChooser();
        ImageFilter f1 = new ImageFilter();
        ImageFilter f2 = new ImageFilter();
        
        f1.addExtension("dsk");
        f1.addExtension("bin");
        f1.setDescription("Image files (*.dsk, *.bin)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");
        
        f.setDialogTitle("Open an image");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Open");
        int i = driveCombo.getSelectedIndex();
        File ff = ((Drive)drives.get(i)).getImageFile();
        f.setSelectedFile(ff);
        if (ff == null)
            f.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if(returnVal == JFileChooser.APPROVE_OPTION)
            txtImage.setText(f.getSelectedFile().getAbsolutePath());
    }

    private void createButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser f = new JFileChooser();
        ImageFilter f1 = new ImageFilter();
        ImageFilter f2 = new ImageFilter();
        
        f1.addExtension("dsk");
        f1.addExtension("bin");
        f1.setDescription("Image files (*.dsk, *.bin)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");
        
        f.setDialogTitle("Create new image");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Save");
        f.setSelectedFile(null);
        int returnVal = f.showSaveDialog(this);
        f.setVisible(true);
        if(returnVal == JFileChooser.APPROVE_OPTION)
            try {
                Drive.createNewImage(f.getSelectedFile().getAbsolutePath());
            } catch(IOException e) {
                StaticDialogs.showErrorMessage("Couldn't create an image file");
            }
    }
    
    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
    	
    	try { Integer.decode(txtPort1.getText()); }
    	catch(NumberFormatException e) {
    		StaticDialogs.showErrorMessage("Port1: Bad number");
    		txtPort1.grabFocus();
    		return;
    	}
    	try { Integer.decode(txtPort2.getText()); }
    	catch(NumberFormatException e) {
    		StaticDialogs.showErrorMessage("Port2: Bad number");
    		txtPort2.grabFocus();
    		return;
    	}
    	try { Integer.decode(txtPort3.getText()); }
    	catch(NumberFormatException e) {
    		StaticDialogs.showErrorMessage("Port3: Bad number");
    		txtPort3.grabFocus();
    		return;
    	}
    	
    	gui.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
    	if (chkSaveSettings.isSelected()) writeSettings();
    	dispose();
    }
    
    private void btnDefaultActionPerformed(java.awt.event.ActionEvent evt) {
    	txtPort1.setText(String.valueOf(DiskImpl.CPU_PORT1));
    	txtPort2.setText(String.valueOf(DiskImpl.CPU_PORT2));
    	txtPort3.setText(String.valueOf(DiskImpl.CPU_PORT3));
    }

    private JCheckBox chkAlwaysOnTop;
    private NiceButton browseButton;
    private NiceButton createButton;
    private JComboBox driveCombo;
    private NiceButton mountButton;
    private JTextField txtImage;
    private NiceButton umountButton;
    private NiceButton btnOK;
    private JCheckBox chkSaveSettings;
    private NiceButton btnDefault;
    private JTextField txtPort1;
    private JTextField txtPort2;
    private JTextField txtPort3;

    
}
