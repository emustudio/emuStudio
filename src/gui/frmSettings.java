/*
 * frmSettings.java
 *
 * Created on Štvrtok, 2008, september 25, 9:21
 */

package gui;

import gui.utils.emuFileFilter;
import gui.utils.tableMemory;
import java.io.File;
import java.util.Collections;
import java.util.Vector;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import memImpl.Memory;
import memImpl.MemoryContext;
import plugins.ISettingsHandler;
import plugins.ISettingsHandler.pluginType;
import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
public class frmSettings extends javax.swing.JDialog {
    private MemoryContext memContext;
    private Memory mem;
    private ROMmodel rom_model;
    private ImagesModel images_model;
    private tableMemory tblMem;
    private ISettingsHandler settings;
    private Vector<String> imageNames = new Vector<String>();
    private Vector<String> imageFullNames = new Vector<String>();
    private Vector<String> imageAddresses = new Vector<String>();

    private class ImagesModel extends AbstractTableModel {
        public int getRowCount() { 
            return imageNames.size();
        }
        public int getColumnCount() { return 2; }
        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "File name";
            else return "Load address";
        }
        @Override
        public Class<?> getColumnClass(int col) { return String.class; }
        @Override
        public boolean isCellEditable(int r, int c) { return false; }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) return imageNames.get(rowIndex);
            else return imageAddresses.get(rowIndex);
        }
        public void setValueAt(int r, int c) {
            fireTableCellUpdated(r,c);
        }
    }

    private class ROMmodel extends AbstractTableModel {
        public int getRowCount() { 
            return memContext.getROMRanges().size();
        }
        public int getColumnCount() { return 2; }
        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "From";
            else return "To";
        }
        @Override
        public Class<?> getColumnClass(int col) { return Integer.class; }
        @Override
        public boolean isCellEditable(int r, int c) { return false; }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Vector keys = new Vector(memContext.getROMRanges().keySet());
            Collections.sort(keys);
            Object[] ar = keys.toArray();
            if (columnIndex == 0) {
                return ar[rowIndex];
            } else
                return memContext.getROMRanges().get(ar[rowIndex]);
        }
        public void setValueAt(int r, int c) {
            fireTableCellUpdated(r,c);
        }
    }
    
    /** Creates new form frmSettings */
    public frmSettings(java.awt.Frame parent, boolean modal, Memory mem,
            tableMemory tblMem, ISettingsHandler settings) {
        super(parent, modal);
        this.mem = mem;
        this.memContext = (MemoryContext)mem.getContext();
        this.tblMem = tblMem;
        this.settings = settings;
        initComponents();
        
        // first tab (after start)
        String s = settings.readSetting(pluginType.memory, null, "banksCount");
        if (s != null) txtBanksCount.setText(s);
        else txtBanksCount.setText("0");

        s = settings.readSetting(pluginType.memory, null, "commonBoundary");
        if (s != null) txtCommonBoundary.setText(s);
        else txtCommonBoundary.setText("0");

        int i = 0;
        String r = null;
        while (true) {
            s = settings.readSetting(pluginType.memory, null, "imageName" + i);
            r = settings.readSetting(pluginType.memory, null, "imageAddress" + i);
            if (s == null) break;
            imageFullNames.add(s);
            imageNames.add(new File(s).getName());
            if (r != null) imageAddresses.add(r);
            else imageAddresses.add("0");
            i++;
        }
        images_model = new ImagesModel();
        tblImages.setModel(images_model);
        
        // second tab (ROM ranges)
        rom_model = new ROMmodel();
        tblROM.setModel(rom_model);
        this.setLocationRelativeTo(null);
        InputVerifier vf = new InputVerifier() {
            public boolean verify(JComponent input) {
                JTextField tf = (JTextField) input;
                try { Integer.decode(tf.getText()); }
                catch (NumberFormatException e) { return false; }
                return true;
            }
        };
        txtFrom.setInputVerifier(vf);
        txtTo.setInputVerifier(vf);
        tblROM.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblROM.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int i = tblROM.getSelectedRow();
                txtFrom.setText(tblROM.getValueAt(i,0).toString());
                txtTo.setText(tblROM.getValueAt(i,1).toString());
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        paneTabs = new javax.swing.JTabbedPane();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        txtCommonBoundary = new javax.swing.JTextField();
        txtBanksCount = new javax.swing.JTextField();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        tblImages = new javax.swing.JTable();
        javax.swing.JButton btnAddImage = new javax.swing.JButton();
        javax.swing.JButton btnRemoveImage = new javax.swing.JButton();
        javax.swing.JPanel jPanel4 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel6 = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane2 = new javax.swing.JScrollPane();
        tblROM = new javax.swing.JTable();
        javax.swing.JPanel jPanel5 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        txtFrom = new javax.swing.JTextField();
        javax.swing.JLabel jLabel8 = new javax.swing.JLabel();
        txtTo = new javax.swing.JTextField();
        javax.swing.JButton btnAddRange = new javax.swing.JButton();
        javax.swing.JButton btnRemoveRange = new javax.swing.JButton();
        chkSave = new javax.swing.JCheckBox();
        javax.swing.JButton btnOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");

        jLabel1.setText("These settings will be loaded after a start of the emulator");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Memory banking"));

        jLabel2.setText("Bank count:");

        jLabel3.setText("Common boundary:");

        txtCommonBoundary.setText("0");

        txtBanksCount.setText("0");

        jLabel4.setText("<html>Banks are at ranges <strong>0..Common</strong> and a common area from <strong>Common..0FFFFh</strong> which is common to all banks");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtBanksCount)
                            .addComponent(txtCommonBoundary, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtBanksCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtCommonBoundary, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("File images to load"));

        tblImages.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(tblImages);

        btnAddImage.setText("Add image");
        btnAddImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddImageActionPerformed(evt);
            }
        });

        btnRemoveImage.setText("Remove image");
        btnRemoveImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveImageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 431, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(btnRemoveImage)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAddImage)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAddImage)
                    .addComponent(btnRemoveImage))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        paneTabs.addTab("After start", jPanel1);

        jLabel5.setText("<html>You can set RO (Read Only) parts in operating memory by defining their ranges.");

        jLabel6.setFont(jLabel6.getFont().deriveFont(jLabel6.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel6.setText("Current ROM:");

        jScrollPane2.setViewportView(tblROM);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Edit ROM ranges"));

        jLabel7.setText("From:");

        txtFrom.setText("0");

        jLabel8.setText("To:");

        txtTo.setText("0");

        btnAddRange.setText("Add range");
        btnAddRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddRangeActionPerformed(evt);
            }
        });

        btnRemoveRange.setText("Remove range");
        btnRemoveRange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveRangeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtTo, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtFrom, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 115, Short.MAX_VALUE)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(btnAddRange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnRemoveRange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddRange))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(btnRemoveRange)
                    .addComponent(txtTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        chkSave.setText("Save this ROM into the configuration");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
                    .addComponent(jLabel6)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(chkSave))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkSave)
                .addContainerGap(64, Short.MAX_VALUE))
        );

        paneTabs.addTab("ROM ranges", jPanel4);

        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(paneTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 503, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(424, Short.MAX_VALUE)
                .addComponent(btnOK, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(paneTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 472, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnOK)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void btnAddRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddRangeActionPerformed
        try {
            memContext.setROM(Integer.decode(txtFrom.getText()),
                    Integer.decode(txtTo.getText()));
        } catch(Exception e) {
            StaticDialogs.showErrorMessage("Range (from,to) has to be positive integer vector!");
            return;
        }
        tblROM.revalidate();
        tblROM.repaint();
        tblMem.revalidate();
        tblMem.repaint();
}//GEN-LAST:event_btnAddRangeActionPerformed

private void btnRemoveRangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveRangeActionPerformed
        try {
            memContext.setRAM(Integer.decode(txtFrom.getText()),
                    Integer.decode(txtTo.getText()));
        } catch(Exception e) {
            StaticDialogs.showErrorMessage("Range (from,to) has to be positive integer vector!");
            return;
        }
        tblROM.revalidate();
        tblROM.repaint();
        tblMem.revalidate();
        tblMem.repaint();
}//GEN-LAST:event_btnRemoveRangeActionPerformed

private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOKActionPerformed
    if (paneTabs.getSelectedIndex() == 0) { // tab0
        int bCount = 0, bCommon = 0;
        try { bCount = Integer.parseInt(txtBanksCount.getText()); }
        catch(Exception e) {
            StaticDialogs.showErrorMessage("Banks count has to be positive integer !");
            return;
        }
        if (bCount < 0) {
            StaticDialogs.showErrorMessage("Banks count has to be positive integer !");
            return;
        }
        try { bCommon = Integer.parseInt(txtCommonBoundary.getText()); }
        catch(Exception e) {
            StaticDialogs.showErrorMessage("Common boundary has to be positive integer !");
            return;
        }
        if (bCommon < 0) {
            StaticDialogs.showErrorMessage("Common boundary has to be positive integer !");
            return;
        }
        mem.saveSettings0(bCount, bCommon, imageFullNames, imageAddresses);
    } else { // tab1
        if (chkSave.isSelected())
            mem.saveSettings1();
    }
    dispose();
}//GEN-LAST:event_btnOKActionPerformed

private void btnAddImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddImageActionPerformed
        JFileChooser f = new JFileChooser();
        emuFileFilter f1 = new emuFileFilter();
        emuFileFilter f2 = new emuFileFilter();

        f1.addExtension("hex");
        f1.addExtension("bin");
        f1.setDescription("Image file (*.hex, *.bin)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");
        
        f.setDialogTitle("Add an image");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Add");
        f.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File fileSource = f.getSelectedFile();
            if (fileSource.canRead() == true) {
                imageNames.add(fileSource.getName());
                imageFullNames.add(fileSource.getAbsolutePath());
                int adr = 0;
                if (!fileSource.getName().toLowerCase().endsWith(".hex")) {
                    // ask for address where to load image
                    String sadr = JOptionPane.showInputDialog("Enter starting address:", 0);
                    try { adr = Integer.decode(sadr); }
                    catch(NumberFormatException e) {}
                }
                imageAddresses.add(String.valueOf(adr));
                tblImages.revalidate();
                tblImages.repaint();
            } else {
                StaticDialogs.showErrorMessage("File " + fileSource.getPath()
                    + " can't be read.");
            }
        }
}//GEN-LAST:event_btnAddImageActionPerformed

private void btnRemoveImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveImageActionPerformed
        int i = tblImages.getSelectedRow();
        if (i == -1) {
            StaticDialogs.showErrorMessage("Image has to be selected!");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, 
                "Are you sure to remove image from the list?") == JOptionPane.YES_OPTION) {
            imageNames.remove(i); imageFullNames.remove(i); imageAddresses.remove(i);
            tblImages.revalidate();
            tblImages.repaint();
        }
}//GEN-LAST:event_btnRemoveImageActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JCheckBox chkSave;
    javax.swing.JTabbedPane paneTabs;
    javax.swing.JTable tblImages;
    javax.swing.JTable tblROM;
    javax.swing.JTextField txtBanksCount;
    javax.swing.JTextField txtCommonBoundary;
    javax.swing.JTextField txtFrom;
    javax.swing.JTextField txtTo;
    // End of variables declaration//GEN-END:variables

}
