/*
 * OpenArchDialog.java
 *
 * Created on Streda, 2007, august 8, 8:45
 */

package gui;
import architecture.*;
import architecture.drawing.Schema;
import javax.swing.*;
import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
public class OpenArchDialog extends javax.swing.JDialog {
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
    
    public OpenArchDialog(javax.swing.JDialog parent, boolean modal) {
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
                    .getAllNames(ArchLoader.configsDir,
                    ".conf");
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
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JButton btnOpen = new javax.swing.JButton();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        lstArchNames = new javax.swing.JList();
        btnEdit = new javax.swing.JButton();
        javax.swing.JButton btnPreview = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnNew = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Microcomputer configuration control");
        setResizable(false);

        btnOpen.setText("Open");
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });

        jLabel1.setText("Choose virtual architecture to open:");

        jScrollPane1.setViewportView(lstArchNames);

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btnNew)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDelete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                        .addComponent(btnPreview)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnOpen, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnOpen)
                    .addComponent(btnPreview)
                    .addComponent(btnNew)
                    .addComponent(btnEdit)
                    .addComponent(btnDelete))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
    javax.swing.JButton btnDelete;
    javax.swing.JButton btnEdit;
    javax.swing.JButton btnNew;
    javax.swing.JList lstArchNames;
    // End of variables declaration//GEN-END:variables
    
}
