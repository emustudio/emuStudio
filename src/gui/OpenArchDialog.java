/*
 * OpenArchDialog.java
 *
 * Created on Streda, 2007, august 8, 8:45
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package gui;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractListModel;
import javax.swing.GroupLayout;
import gui.utils.NiceButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import architecture.ArchLoader;
import architecture.drawing.Schema;
import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class OpenArchDialog extends JDialog {
    private String archName;
    private boolean OOK = false;
    private ArchListModel amodel;
        
    /** Creates new form OpenArchDialog */
    public OpenArchDialog() {
        super();
        initComponents();
        amodel = new ArchListModel();
        lstArchNames.setModel(amodel);
        this.setModal(true);
        this.setLocationRelativeTo(null);
    }
    
    public OpenArchDialog(JDialog parent, boolean modal) {
        super(parent,modal);
        initComponents();
        amodel = new ArchListModel();
        lstArchNames.setModel(amodel);
        this.setTitle("Browse for arhitecture names");
        btnEdit.setEnabled(false);
        btnEdit.setVisible(false);
        btnDelete.setEnabled(false);
        btnDelete.setVisible(false);
        btnNew.setEnabled(false);
        btnNew.setVisible(false);
        this.setLocationRelativeTo(parent);
    }
    
    // existing configurations list model
    private class ArchListModel extends AbstractListModel {
        private String[] allModels;
        public ArchListModel() {
            allModels = ArchLoader
                    .getAllNames(ArchLoader.configsDir,".conf");
        }
        public Object getElementAt(int index) {
            return allModels[index];
        }
        public int getSize() {
            if (allModels != null) return allModels.length;
            else return 0;
        }
        public void update() {
            allModels = ArchLoader
                    .getAllNames(ArchLoader.configsDir,
                    ".conf");
            this.fireContentsChanged(this, -1, -1);
        }
    }
    
    public boolean getOK() { return OOK; }
    
    public String getArchName() { return archName; }
    
    private void initComponents() {
        NiceButton btnOpen = new NiceButton();
        JLabel lblChoose = new JLabel();
        JScrollPane scrollConfigs = new JScrollPane();
        lstArchNames = new JList();
        btnEdit = new NiceButton();
        NiceButton btnPreview = new NiceButton();
        btnDelete = new NiceButton();
        btnNew = new NiceButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Microcomputer configuration control");
        setResizable(false);

        btnOpen.setText("Open");
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        lblChoose.setText("Choose virtual architecture to open:");

        scrollConfigs.setViewportView(lstArchNames);
        
        lstArchNames.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					btnOpenActionPerformed(null);
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseReleased(MouseEvent e) {}
        });

        btnEdit.setText("Edit...");
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        btnPreview.setText("Preview...");
        btnPreview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPreviewActionPerformed(evt);
            }
        });

        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnNew.setText("New...");
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblChoose)
        				.addComponent(scrollConfigs)
        				.addGroup(GroupLayout.Alignment.TRAILING,layout.createSequentialGroup()
        						.addComponent(btnNew)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(btnEdit)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(btnDelete)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        						.addComponent(btnPreview)
        						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						.addComponent(btnOpen)))
        				.addContainerGap());
        layout.setVerticalGroup(layout.createSequentialGroup()
        		.addContainerGap()
        		.addComponent(lblChoose)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(scrollConfigs)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(btnNew)
        				.addComponent(btnEdit)
        				.addComponent(btnDelete)
        				.addComponent(btnPreview)
        				.addComponent(btnOpen))
        		.addContainerGap());
        pack();
    }

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        if (lstArchNames.getSelectedIndex() == -1) {
            StaticDialogs.showErrorMessage("Virtual architecture has to be selected!");
            return;
        }
        archName = (String)lstArchNames.getSelectedValue();
        OOK = true;
        dispose();
}//GEN-LAST:event_btnOpenActionPerformed

private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
    if (lstArchNames.getSelectedIndex() == -1) {
        StaticDialogs.showErrorMessage("Virtual architecture has to be selected!");
        return;
    }
    archName = (String)lstArchNames.getSelectedValue();
    Schema s = ArchLoader.loadSchema(archName);
    if (s == null) return;
    AddEditArchDialog d = new AddEditArchDialog(this,true,s);
    d.setVisible(true);
    if (d.getOK()) ArchLoader.saveSchema(d.getSchema());
}//GEN-LAST:event_btnEditActionPerformed

private void btnPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviewActionPerformed
    if (lstArchNames.getSelectedIndex() == -1) {
        StaticDialogs.showErrorMessage("Virtual architecture has to be selected!");
        return;
    }
    archName = (String)lstArchNames.getSelectedValue();
    Schema s = ArchLoader.loadSchema(archName);
    if (s == null) return;
    new ArchPreviewDialog(this,true,s).setVisible(true);
}//GEN-LAST:event_btnPreviewActionPerformed

private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
    if (lstArchNames.getSelectedIndex() == -1) {
        StaticDialogs.showErrorMessage("Virtual architecture has to be selected!");
        return;
    }
    int r = JOptionPane.showConfirmDialog(this, "Do you really want to delete"
            + " selected architecture?", "Delete architecture", 
            JOptionPane.YES_NO_OPTION);
    archName = (String)lstArchNames.getSelectedValue();
    if ((r == JOptionPane.YES_OPTION) && ArchLoader.deleteConfig(archName)) {
        amodel.update();
        archName = "";
        lstArchNames.setSelectedIndex(-1);
    }
}//GEN-LAST:event_btnDeleteActionPerformed

private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
    AddEditArchDialog di = new AddEditArchDialog(null, true);
    di.setVisible(true);
    if (di.getOK()) {
        ArchLoader.saveSchema(di.getSchema());
        amodel.update();
    }
}//GEN-LAST:event_btnNewActionPerformed
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private NiceButton btnDelete;
    private NiceButton btnEdit;
    private NiceButton btnNew;
    private JList lstArchNames;
    // End of variables declaration//GEN-END:variables
    
}
