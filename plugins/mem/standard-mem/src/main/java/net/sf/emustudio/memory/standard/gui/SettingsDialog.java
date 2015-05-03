/*
 * SettingsDialog.java
 *
 * Created on Štvrtok, 2008, september 25, 9:21
 *
 * Copyright (C) 2008-2013 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.memory.standard.gui;

import emulib.emustudio.SettingsManager;
import emulib.runtime.StaticDialogs;
import emulib.runtime.UniversalFileFilter;
import net.sf.emustudio.memory.standard.StandardMemoryContext.AddressRange;
import net.sf.emustudio.memory.standard.gui.utils.NiceButton;
import net.sf.emustudio.memory.standard.gui.utils.TableMemory;
import net.sf.emustudio.memory.standard.impl.AddressRangeImpl;
import net.sf.emustudio.memory.standard.impl.MemoryContextImpl;
import net.sf.emustudio.memory.standard.impl.MemoryImpl;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.InputVerifier;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsDialog extends JDialog {

    private MemoryContextImpl memContext;
    private MemoryImpl memory;
    private ROMmodel romModel;
    private ImagesModel imagesModel;
    private TableMemory tblMem;
    private List<String> imageNames = new ArrayList<String>();
    private List<String> imageFullNames = new ArrayList<String>();
    private List<Integer> imageAddresses = new ArrayList<Integer>();

    public SettingsDialog(JDialog parent, long pluginID,
            MemoryImpl mem, MemoryContextImpl memContext, TableMemory tblMem,
            SettingsManager settings) {
        super(parent, true);
        this.memory = mem;
        this.memContext = memContext;
        this.tblMem = tblMem;
        initComponents();

        // first tab (after start)
        String s = settings.readSetting(pluginID, "banksCount");
        if (s != null) {
            txtBanksCount.setText(s);
        } else {
            txtBanksCount.setText("0");
        }

        s = settings.readSetting(pluginID, "commonBoundary");
        if (s != null) {
            txtCommonBoundary.setText(String.format("0x%04X", Integer.decode(s)));
        } else {
            txtCommonBoundary.setText("0");
        }

        int i = 0;
        String r;
        while (true) {
            s = settings.readSetting(pluginID, "imageName" + i);
            r = settings.readSetting(pluginID, "imageAddress" + i);
            if (s == null) {
                break;
            }
            imageFullNames.add(s);
            imageNames.add(new File(s).getName());
            if (r != null) {
                try {
                    imageAddresses.add(Integer.decode(r));
                } catch (Exception e) {
                    imageAddresses.add(0);
                }
            } else {
                imageAddresses.add(0);
            }
            i++;
        }
        imagesModel = new ImagesModel();
        tblImages.setModel(imagesModel);

        // second tab (ROM ranges)
        romModel = new ROMmodel();
        tblROM.setModel(romModel);
        this.setLocationRelativeTo(null);
        InputVerifier vf = new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                JTextField tf = (JTextField) input;
                try {
                    Integer.decode(tf.getText());
                } catch (NumberFormatException e) {
                    return false;
                }
                return true;
            }
        };
        txtFrom.setInputVerifier(vf);
        txtTo.setInputVerifier(vf);
        tblROM.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblROM.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int i = tblROM.getSelectedRow();
                txtFrom.setText("0x" + tblROM.getValueAt(i, 0).toString());
                txtTo.setText("0x" + tblROM.getValueAt(i, 1).toString());
            }
        });
    }

    private class ImagesModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return imageNames.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "File name";
            } else {
                return "Load address (hex)";
            }
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return imageNames.get(rowIndex);
            } else {
                return String.format("%04X", imageAddresses.get(rowIndex));
            }
        }

        public void setValueAt(int r, int c) {
            fireTableCellUpdated(r, c);
        }
    }

    private class ROMmodel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return memContext.getROMRanges().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "From (hex)";
            } else {
                return "To (hex)";
            }
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            for (AddressRange range : memContext.getROMRanges()) {
                if (columnIndex == 0) {
                    return String.format("%04X", range.getStartAddress());
                } else {
                    return String.format("%04X", range.getStopAddress());
                }
            }
            return "";
        }

        public void setValueAt(int r, int c) {
            fireTableCellUpdated(r, c);
        }
    }

    private void initComponents() {
        paneTabs = new JTabbedPane();
        JPanel panelAfterStart = new JPanel();
        JLabel lblDesc1 = new JLabel("These settings will be loaded after a start of the emulator");
        JPanel panelBanking = new JPanel();
        JLabel lblBanksCount = new JLabel("Bank count:");
        JLabel lblCommonBoundary = new JLabel("Common boundary:");
        txtCommonBoundary = new JTextField("0");
        txtBanksCount = new JTextField("0");
        JLabel lblDesc2 = new JLabel("<html>Banks are at ranges <strong>0..Common</strong>,<br/>common area is from <strong>Common..0FFFFh</strong> which is common to all banks");
        JPanel panelImages = new JPanel();
        JScrollPane scrollImages = new JScrollPane();
        tblImages = new JTable();
        NiceButton btnAddImage = new NiceButton("Add image");
        NiceButton btnRemoveImage = new NiceButton("Remove image");
        JPanel panelROMRanges = new JPanel();
        JLabel lblDesc3 = new JLabel("<html>You can set RO (Read Only) parts in operating memory by defining their ranges.");
        JLabel lblCurrentROM = new JLabel("Current ROM:");
        JScrollPane scrollROM = new JScrollPane();
        tblROM = new JTable();
        JPanel panelROM = new JPanel();
        JLabel lblFrom = new JLabel("From:");
        txtFrom = new JTextField("0");
        JLabel lblTo = new JLabel("To:");
        txtTo = new JTextField("0");
        NiceButton btnAddRange = new NiceButton("Add range");
        NiceButton btnRemoveRange = new NiceButton("Remove range");
        chkSave = new JCheckBox("Save this ROM into the configuration");
        NiceButton btnOK = new NiceButton("OK");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Settings");

        panelBanking.setBorder(BorderFactory.createTitledBorder("Memory banking"));

        GroupLayout panelBankingLayout = new GroupLayout(panelBanking);
        panelBanking.setLayout(panelBankingLayout);

        panelBankingLayout.setHorizontalGroup(
                panelBankingLayout.createSequentialGroup().addContainerGap().addGroup(panelBankingLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(panelBankingLayout.createSequentialGroup().addGroup(panelBankingLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblBanksCount).addComponent(lblCommonBoundary)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelBankingLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtBanksCount).addComponent(txtCommonBoundary)).addContainerGap(70, Short.MAX_VALUE)).addComponent(lblDesc2, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)).addContainerGap());
        panelBankingLayout.setVerticalGroup(
                panelBankingLayout.createSequentialGroup().addContainerGap().addGroup(panelBankingLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblBanksCount).addComponent(txtBanksCount)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelBankingLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblCommonBoundary).addComponent(txtCommonBoundary)).addComponent(lblDesc2).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        panelImages.setBorder(BorderFactory.createTitledBorder("File images to load"));
        scrollImages.setViewportView(tblImages);

        btnAddImage.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddImageActionPerformed(evt);
            }
        });

        btnRemoveImage.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveImageActionPerformed(evt);
            }
        });

        GroupLayout panelImagesLayout = new GroupLayout(panelImages);
        panelImages.setLayout(panelImagesLayout);

        panelImagesLayout.setHorizontalGroup(
                panelImagesLayout.createSequentialGroup().addContainerGap().addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scrollImages, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE).addGroup(GroupLayout.Alignment.TRAILING, panelImagesLayout.createSequentialGroup().addComponent(btnRemoveImage).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnAddImage))).addContainerGap());
        panelImagesLayout.setVerticalGroup(
                panelImagesLayout.createSequentialGroup().addContainerGap().addComponent(scrollImages, GroupLayout.PREFERRED_SIZE, 131, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelImagesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(btnRemoveImage).addComponent(btnAddImage)).addContainerGap());

        GroupLayout panelAfterLayout = new GroupLayout(panelAfterStart);
        panelAfterStart.setLayout(panelAfterLayout);

        panelAfterLayout.setHorizontalGroup(
                panelAfterLayout.createSequentialGroup().addContainerGap().addGroup(panelAfterLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblDesc1).addComponent(panelBanking).addComponent(panelImages)).addContainerGap());
        panelAfterLayout.setVerticalGroup(
                panelAfterLayout.createSequentialGroup().addContainerGap().addComponent(lblDesc1).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelBanking).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelImages).addContainerGap());

        paneTabs.addTab("After start", panelAfterStart);

        lblCurrentROM.setFont(lblCurrentROM.getFont().deriveFont(lblCurrentROM.getFont().getStyle() | java.awt.Font.BOLD));
        scrollROM.setViewportView(tblROM);

        panelROM.setBorder(BorderFactory.createTitledBorder("Edit ROM ranges"));

        btnAddRange.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddRangeActionPerformed(evt);
            }
        });

        btnRemoveRange.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveRangeActionPerformed(evt);
            }
        });

        GroupLayout panelROMLayout = new GroupLayout(panelROM);
        panelROM.setLayout(panelROMLayout);

        panelROMLayout.setHorizontalGroup(
                panelROMLayout.createSequentialGroup().addContainerGap().addGroup(panelROMLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblFrom).addComponent(lblTo)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelROMLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(txtFrom).addComponent(txtTo)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(panelROMLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false).addComponent(btnAddRange, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(btnRemoveRange, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap());
        panelROMLayout.setVerticalGroup(
                panelROMLayout.createSequentialGroup().addContainerGap().addGroup(panelROMLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblFrom).addComponent(txtFrom).addComponent(btnAddRange)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(panelROMLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblTo).addComponent(txtTo).addComponent(btnRemoveRange)).addContainerGap());

        GroupLayout panelROMRangesLayout = new GroupLayout(panelROMRanges);
        panelROMRanges.setLayout(panelROMRangesLayout);

        panelROMRangesLayout.setHorizontalGroup(
                panelROMRangesLayout.createSequentialGroup().addContainerGap().addGroup(panelROMRangesLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblDesc3, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE).addComponent(lblCurrentROM).addComponent(scrollROM, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE).addComponent(panelROM).addComponent(chkSave)).addContainerGap());
        panelROMRangesLayout.setVerticalGroup(
                panelROMRangesLayout.createSequentialGroup().addContainerGap().addComponent(lblDesc3).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(lblCurrentROM).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(scrollROM, GroupLayout.PREFERRED_SIZE, 143, GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(panelROM).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(chkSave).addContainerGap());

        paneTabs.addTab("ROM ranges", panelROMRanges);

        btnOK.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(paneTabs)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(btnOK)
                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createSequentialGroup().addComponent(paneTabs)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(btnOK).addContainerGap());
        pack();
    }

    private void btnAddRangeActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            memContext.setROM(new AddressRangeImpl(Integer.decode(txtFrom.getText()),
                    Integer.decode(txtTo.getText())));
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Range (from,to) has to be positive integer ArrayList!");
            return;
        }
        tblROM.revalidate();
        tblROM.repaint();
        tblMem.revalidate();
        tblMem.repaint();
    }

    private void btnRemoveRangeActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            memContext.setRAM(new AddressRangeImpl(Integer.decode(txtFrom.getText()),
                    Integer.decode(txtTo.getText())));
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Range (from,to) has to be positive integer ArrayList!");
            return;
        }
        tblROM.revalidate();
        tblROM.repaint();
        tblMem.revalidate();
        tblMem.repaint();
    }

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {
        if (paneTabs.getSelectedIndex() == 0) { // tab0
            int bCount, bCommon;
            try {
                bCount = Integer.parseInt(txtBanksCount.getText());
            } catch (Exception e) {
                StaticDialogs.showErrorMessage("Banks count has to be positive integer !");
                return;
            }
            if (bCount < 0) {
                StaticDialogs.showErrorMessage("Banks count has to be positive integer !");
                return;
            }
            try {
                bCommon = Integer.decode(txtCommonBoundary.getText());
            } catch (Exception e) {
                StaticDialogs.showErrorMessage("Common boundary has to be positive integer !");
                return;
            }
            if (bCommon < 0) {
                StaticDialogs.showErrorMessage("Common boundary has to be positive integer !");
                return;
            }
            memory.saveCoreSettings(bCount, bCommon, imageFullNames, imageAddresses);
        } else { // tab1
            if (chkSave.isSelected()) {
                memory.saveROMRanges();
            }
        }
        dispose();
    }

    private void btnAddImageActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser f = new JFileChooser();
        UniversalFileFilter f1 = new UniversalFileFilter();
        UniversalFileFilter f2 = new UniversalFileFilter();

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
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fileSource = f.getSelectedFile();
            if (fileSource.canRead() == true) {
                imageNames.add(fileSource.getName());
                imageFullNames.add(fileSource.getAbsolutePath());
                int adr = 0;
                if (!fileSource.getName().toLowerCase().endsWith(".hex")) {
                    // ask for address where to load image
                    String sadr = JOptionPane.showInputDialog("Enter starting address:", 0);
                    try {
                        adr = Integer.decode(sadr);
                    } catch (NumberFormatException e) {
                    }
                }
                imageAddresses.add(adr);
                tblImages.revalidate();
                tblImages.repaint();
            } else {
                StaticDialogs.showErrorMessage("File " + fileSource.getPath()
                        + " can't be read.");
            }
        }
    }

    private void btnRemoveImageActionPerformed(java.awt.event.ActionEvent evt) {
        int i = tblImages.getSelectedRow();
        if (i == -1) {
            StaticDialogs.showErrorMessage("Image has to be selected!");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Are you sure to remove image from the list?", "Remove image", i) == JOptionPane.YES_OPTION) {
            imageNames.remove(i);
            imageFullNames.remove(i);
            imageAddresses.remove(i);
            tblImages.revalidate();
            tblImages.repaint();
        }
    }
    JCheckBox chkSave;
    JTabbedPane paneTabs;
    JTable tblImages;
    JTable tblROM;
    JTextField txtBanksCount;
    JTextField txtCommonBoundary;
    JTextField txtFrom;
    JTextField txtTo;
}
