/**
 * ConfigDialog.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 * 
 */
package disk.gui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import disk.Drive;

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
    }

    private void writeSettings() {
    	if (chkAlwaysOnTop.isSelected())
    		settings.writeSetting(hash, "always_on_top", "true");
    	else settings.writeSetting(hash, "always_on_top", "false");
    		
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
        lblImage = new JLabel("Image:");
        txtImage = new JTextField();
        browseButton = new JButton("Browse...");
        mountButton = new JButton("Mount");
        umountButton = new JButton("Un-mount");
        createButton = new JButton("Create new image");
        panelImages = new JPanel();
        panelOther = new JPanel();
        chkAlwaysOnTop = new JCheckBox("GUI always on top");
        btnOK = new JButton("OK");
        chkSaveSettings = new JCheckBox("Save settings");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MITS 88-DISK Configuration");

        panelImages.setBorder(BorderFactory.createTitledBorder("Mounted images"));

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
	            .addComponent(driveCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
	                .addContainerGap())));
        panelImagesLayout.setVerticalGroup(
            panelImagesLayout.createSequentialGroup()
            .addComponent(driveCombo) //, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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

        panelOther.setBorder(BorderFactory.createTitledBorder("Other"));

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
                .addComponent(chkAlwaysOnTop)
                .addComponent(chkSaveSettings)
                .addContainerGap());

        btnOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnOKActionPerformed(e);
			}        	
        });
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(panelImages, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelOther, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            		.addContainerGap()
            		.addComponent(btnOK)
            		.addContainerGap()));
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelImages, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelOther, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
    	gui.setAlwaysOnTop(chkAlwaysOnTop.isSelected());
    	if (chkSaveSettings.isSelected()) writeSettings();
    	dispose();
    }
    
    private JCheckBox chkAlwaysOnTop;
    private JButton browseButton;
    private JButton createButton;
    private JComboBox driveCombo;
    private JLabel lblImage;
    private JPanel panelImages;
    private JPanel panelOther;
    private JButton mountButton;
    private JTextField txtImage;
    private JButton umountButton;
    private JButton btnOK;
    private JCheckBox chkSaveSettings;

    
}
