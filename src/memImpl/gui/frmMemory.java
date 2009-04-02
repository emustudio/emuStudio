/*
 * frmMemory.java
 *
 * Created on Nede�a, 2007, okt�ber 28, 10:40
 */

package memImpl.gui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import memImpl.Memory;
import memImpl.MemoryContext;
import memImpl.gui.utils.EmuFileFilter;
import memImpl.gui.utils.memoryTableModel;
import memImpl.gui.utils.tableMemory;
import plugins.ISettingsHandler;
import runtime.StaticDialogs;


/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class frmMemory extends JFrame {
    private MemoryContext memContext;
    private Memory mem;
    private tableMemory tblMemory;
    private memoryTableModel memModel;
    private ISettingsHandler settings;
    
    /** Creates new form frmMemory */
    public frmMemory(Memory mem, ISettingsHandler settings) {
        this.mem = mem;
        this.settings = settings;
        this.memContext = (MemoryContext)mem.getContext();
        this.memModel = new memoryTableModel(memContext);
        
        initComponents();
        tblMemory = new tableMemory(memModel,paneMemory);
        paneMemory.setViewportView(tblMemory);
        tblMemory.setVisible(true);
        paneMemory.repaint();
        memModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                ((SpinnerNumberModel)spnPage.getModel()).setValue(memModel.getPage());
            }
        });
        lblPageCount.setText(String.valueOf(memModel.getPageCount()));
        lblBanksCount.setText(String.valueOf(memContext.getBanksCount()));
        spnPage.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int i = (Integer)((SpinnerNumberModel)spnPage.getModel()).getValue();
                try { memModel.setPage(i); } catch(IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel)spnPage.getModel())
                    .setValue(memModel.getPage());
                }
            }
        });
        spnBank.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int i = (Integer)((SpinnerNumberModel)spnBank.getModel()).getValue();
                try { memModel.setCurrentBank(i); } catch(IndexOutOfBoundsException ex) {
                    ((SpinnerNumberModel)spnBank.getModel())
                    .setValue(memModel.getCurrentBank());
                }
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                destroyME();
            }
        });

        memModel.addTableModelListener(new TableModelListener() {
             public void tableChanged(TableModelEvent e) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                updateMemVal(row, column);
            }
        });
        tblMemory.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mousePressed(e);
                int row = tblMemory.getSelectedRow();
                int col = tblMemory.getSelectedColumn();
                updateMemVal(row, col);
            }
        });
        tblMemory.addKeyListener(new KeyAdapter() {
            private boolean right_correct = false; // perform correction
            private boolean left_correct = false; // perform correction
            private boolean up_correct = false; // perform correction
            private boolean down_correct = false; // perform correction
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                int key = e.getKeyCode();
                if ((key == KeyEvent.VK_RIGHT) // ->
                        && (tblMemory.getSelectedColumn() == memModel.getColumnCount()-1)) {
                    if (tblMemory.getSelectedRow() == memModel.getRowCount()-1) {
                        int i = (Integer)((SpinnerNumberModel)spnPage.getModel()).getValue();
                        try { memModel.setPage(i+1); } catch(IndexOutOfBoundsException ex) {
                            ((SpinnerNumberModel)spnPage.getModel()).setValue(0);
                        }
                        tblMemory.setRowSelectionInterval(0, 0);
                        tblMemory.scrollRectToVisible(tblMemory.getCellRect(0, 0, true));
                    } else {
                        int row = tblMemory.getSelectedRow();
                        tblMemory.setRowSelectionInterval(row+1, row+1);
                        right_correct = true;
                    }
                } else if ((key == KeyEvent.VK_LEFT) // <-
                        && (tblMemory.getSelectedColumn() == 0)) {
                    if (tblMemory.getSelectedRow() == 0) {
                        int i = (Integer)((SpinnerNumberModel)spnPage.getModel()).getValue();
                        try { memModel.setPage(i-1); } catch(IndexOutOfBoundsException ex) {
                            ((SpinnerNumberModel)spnPage.getModel()).setValue(memModel.getPageCount()-1);
                        }
                        tblMemory.setRowSelectionInterval(memModel.getRowCount()-1,
                                memModel.getRowCount()-1);
                        left_correct = true;
                    } else {
                        int row = tblMemory.getSelectedRow();
                        tblMemory.setRowSelectionInterval(row-1, row-1);
                        left_correct = true;
                    }
                } else if ((key == KeyEvent.VK_UP) // ^
                        && (tblMemory.getSelectedRow() == 0)) {
                    int i = (Integer)((SpinnerNumberModel)spnPage.getModel()).getValue();
                    int col = tblMemory.getSelectedColumn();
                    try { memModel.setPage(i-1); } catch(IndexOutOfBoundsException ex) {
                        ((SpinnerNumberModel)spnPage.getModel()).setValue(memModel.getPageCount()-1);
                    }
                    tblMemory.setColumnSelectionInterval(col, col);
                    up_correct = true;
                } else if ((key == KeyEvent.VK_DOWN) // v
                        && (tblMemory.getSelectedRow() == memModel.getRowCount()-1)) {
                    int i = (Integer)((SpinnerNumberModel)spnPage.getModel()).getValue();
                    int col = tblMemory.getSelectedColumn();
                    try { memModel.setPage(i+1); } catch(IndexOutOfBoundsException ex) {
                        ((SpinnerNumberModel)spnPage.getModel()).setValue(0);
                    }
                    tblMemory.setColumnSelectionInterval(col, col);
                    down_correct = true;
                }

            }
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);

                if (right_correct) {
                    try { tblMemory.setColumnSelectionInterval(0, 0);}
                    catch(Exception ex) {}
                    right_correct = false;
                }
                if (left_correct) {
                    try { tblMemory.setColumnSelectionInterval(memModel.getColumnCount()-1,
                                memModel.getColumnCount()-1); }
                    catch(Exception ex) {}
                    left_correct = false;
                }
                if (up_correct) {
                    try { tblMemory.setRowSelectionInterval(memModel.getRowCount()-1,
                            memModel.getRowCount()-1); }
                    catch(Exception ex) {}
                    int row = tblMemory.getSelectedRow();
                    int col = tblMemory.getSelectedColumn();
                    tblMemory.scrollRectToVisible(tblMemory.getCellRect(row, col, true));
                    up_correct = false;
                }
                if (down_correct) {
                    try { tblMemory.setRowSelectionInterval(0, 0); }
                    catch(Exception ex) {}
                    int row = tblMemory.getSelectedRow();
                    int col = tblMemory.getSelectedColumn();
                    tblMemory.scrollRectToVisible(tblMemory.getCellRect(row, col, true));
                    down_correct = false;
                }
                int row = tblMemory.getSelectedRow();
                int col = tblMemory.getSelectedColumn();
                updateMemVal(row, col);
            }
            
        });
        this.setLocationRelativeTo(null);
    }
    
    private void updateMemVal(int row, int column) {
        if (tblMemory.isCellSelected(row, column) == false) return;
        int address = memModel.getRowCount() * memModel.getColumnCount()
            * memModel.getPage()+ row * memModel.getColumnCount() + column;

        int data = Integer.parseInt(memModel.getValueAt(row, column).toString(),16);
        txtAddress.setText(String.format("%04X", address));
        txtChar.setText(String.format("%c", data & 0xFF));
        txtValDec.setText(String.format("%02d", data));
        txtValHex.setText(String.format("%02X", data));
        txtValOct.setText(String.format("%02o", data));
        txtValBin.setText(Integer.toBinaryString(data));
    }
    
    public void updateBank(short bank) {
    	lblRuntimeBank.setText(String.valueOf(bank));
    }
    
    private void destroyME() { dispose(); }
    
    private void initComponents() {
        JToolBar toolBar = new JToolBar();
        JButton btnClearMemory = new JButton();
        JButton btnOpenImage = new JButton();
        JButton btnDump = new JButton();
        JButton btnSettings = new JButton();
        JToolBar.Separator jSeparator2 = new JToolBar.Separator();
        JLabel lblPageNumber = new JLabel("Page number:");
        spnPage = new JSpinner();
        JLabel lblPageCountLBL = new JLabel("Page count:");
        lblPageCount = new JLabel("0");
        JButton btnFindAddress = new JButton();
        paneMemory = new JScrollPane();
        JPanel panelValue = new JPanel();
        JLabel lblAddress = new JLabel("Address:");
        txtAddress = new JTextField("0000");
        JLabel lblValue = new JLabel("Value:");
        JLabel lblChar = new JLabel("Char:");
        txtChar = new JTextField();
        txtValDec = new JTextField("00");
        txtValHex = new JTextField("00");
        JLabel lblDEC = new JLabel("(dec)");
        JLabel lblHEX = new JLabel("(hex)");
        txtValOct = new JTextField("000");
        txtValBin = new JTextField("00000000");
        JLabel lblOCT = new JLabel("(oct)");
        JLabel lblBIN = new JLabel("(bin)");
        JLabel lblBank = new JLabel("Bank:");
        spnBank = new JSpinner();
        JLabel lblBanksCountLBL = new JLabel("Banks count:");
        lblBanksCount = new JLabel("0");
        JLabel lblRuntimeBankLBL = new JLabel("Run-time bank:");
        lblRuntimeBank = new JLabel("0");
        JToolBar.Separator jSeparator1 = new JToolBar.Separator();
        JPanel panelMemory = new JPanel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Operating memory plugin");

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        btnClearMemory.setIcon(new ImageIcon(getClass().getResource("/resources/edit-delete.png"))); // NOI18N
        btnClearMemory.setToolTipText("Clear memory");
        btnClearMemory.setFocusable(false);
        btnClearMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearMemoryActionPerformed(evt);
            }
        });
        toolBar.add(btnClearMemory);
        toolBar.add(jSeparator1);

        btnOpenImage.setIcon(new ImageIcon(getClass().getResource("/resources/document-open.png"))); // NOI18N
        btnOpenImage.setToolTipText("Load image");
        btnOpenImage.setFocusable(false);
        btnOpenImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenImageActionPerformed(evt);
            }
        });
        toolBar.add(btnOpenImage);

        btnDump.setIcon(new ImageIcon(getClass().getResource("/resources/document-save.png"))); // NOI18N
        btnDump.setToolTipText("Dump memory...");
        btnDump.setFocusable(false);
        btnDump.setHorizontalTextPosition(SwingConstants.CENTER);
        btnDump.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnDump.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDumpActionPerformed(evt);
            }
        });
        toolBar.add(btnDump);

        btnSettings.setIcon(new ImageIcon(getClass().getResource("/resources/preferences-system.png"))); // NOI18N
        btnSettings.setToolTipText("Settings...");
        btnSettings.setFocusable(false);
        btnSettings.setHorizontalTextPosition(SwingConstants.CENTER);
        btnSettings.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSettingsActionPerformed(evt);
            }
        });
        toolBar.add(btnSettings);
        toolBar.add(jSeparator2);

        btnFindAddress.setIcon(new ImageIcon(getClass().getResource("/resources/edit-find.png"))); // NOI18N
        btnFindAddress.setToolTipText("Find address");
        btnFindAddress.setFocusable(false);
        btnFindAddress.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFindAddressActionPerformed(evt);
            }
        });
        toolBar.add(btnFindAddress);

        panelMemory.setBorder(BorderFactory.createTitledBorder("Memory control"));

        spnBank.setMinimumSize(new java.awt.Dimension(70, 25));
        spnBank.setPreferredSize(new java.awt.Dimension(70, 25));
        spnPage.setMinimumSize(new java.awt.Dimension(70, 25));
        spnPage.setPreferredSize(new java.awt.Dimension(70, 25));
        lblPageCount.setFont(lblPageCount.getFont().deriveFont(lblPageCount.getFont().getStyle() | java.awt.Font.BOLD));        
        lblBanksCount.setFont(lblBanksCount.getFont().deriveFont(lblBanksCount.getFont().getStyle() | java.awt.Font.BOLD));        
        lblRuntimeBank.setFont(lblRuntimeBank.getFont().deriveFont(lblRuntimeBank.getFont().getStyle() | java.awt.Font.BOLD));
        
        GroupLayout panelMemoryLayout = new GroupLayout(panelMemory);
        panelMemory.setLayout(panelMemoryLayout);
        
        panelMemoryLayout.setHorizontalGroup(
        		panelMemoryLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblPageNumber)
        				.addComponent(lblPageCountLBL))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(spnPage)
        				.addComponent(lblPageCount))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblBank)
        				.addComponent(lblBanksCountLBL)
        				.addComponent(lblRuntimeBankLBL))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(spnBank)
        				.addComponent(lblBanksCount)
        				.addComponent(lblRuntimeBank))
        		.addContainerGap());
        panelMemoryLayout.setVerticalGroup(
        		panelMemoryLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPageNumber)
        				.addComponent(spnPage)
        				.addComponent(lblBank)
        				.addComponent(spnBank))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPageCountLBL)
        				.addComponent(lblPageCount)
        				.addComponent(lblBanksCountLBL)
        				.addComponent(lblBanksCount))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelMemoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblRuntimeBankLBL)
        				.addComponent(lblRuntimeBank)));
        
        panelValue.setBorder(BorderFactory.createTitledBorder("Selected value"));

        lblAddress.setFont(lblAddress.getFont().deriveFont(lblAddress.getFont().getStyle() | java.awt.Font.BOLD));
        txtAddress.setEditable(false);
        txtAddress.setHorizontalAlignment(JTextField.RIGHT);

        lblValue.setFont(lblValue.getFont().deriveFont(lblValue.getFont().getStyle() | java.awt.Font.BOLD));
        lblChar.setFont(lblChar.getFont().deriveFont(lblChar.getFont().getStyle() | java.awt.Font.BOLD));
        txtChar.setEditable(false);
        txtValDec.setEditable(false);
        txtValDec.setHorizontalAlignment(JTextField.RIGHT);
        txtValHex.setEditable(false);
        txtValHex.setHorizontalAlignment(JTextField.RIGHT);
        txtValOct.setEditable(false);
        txtValOct.setHorizontalAlignment(JTextField.RIGHT);
        txtValBin.setEditable(false);
        txtValBin.setHorizontalAlignment(JTextField.RIGHT);

        GroupLayout panelValueLayout = new GroupLayout(panelValue);
        panelValue.setLayout(panelValueLayout);
        
        panelValueLayout.setHorizontalGroup(
        		panelValueLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblAddress)
        				.addComponent(lblChar))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(txtAddress)
        				.addComponent(txtChar))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addComponent(lblValue)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(txtValDec)
        				.addComponent(txtValHex))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblDEC)
        				.addComponent(lblHEX))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(txtValOct)
        				.addComponent(txtValBin))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblOCT)
        				.addComponent(lblBIN))
        		.addContainerGap());
        panelValueLayout.setVerticalGroup(
        		panelValueLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblAddress)
        				.addComponent(txtAddress)
        				.addComponent(lblValue)
        				.addComponent(txtValDec)
        				.addComponent(lblDEC)
        				.addComponent(txtValOct)
        				.addComponent(lblOCT))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelValueLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblChar)
        				.addComponent(txtChar)
        				.addComponent(txtValHex)
        				.addComponent(lblHEX)
        				.addComponent(txtValBin)
        				.addComponent(lblBIN))
        		.addContainerGap());
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(
        		layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		.addComponent(toolBar)
        		.addComponent(paneMemory,GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE)
        		.addGroup(layout.createSequentialGroup()
        				.addContainerGap()
        				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                				.addComponent(panelMemory)
                				.addComponent(panelValue))
        				.addContainerGap()));
        layout.setVerticalGroup(
        		layout.createSequentialGroup()
        		.addComponent(toolBar)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(paneMemory,GroupLayout.PREFERRED_SIZE, 186, GroupLayout.PREFERRED_SIZE)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(panelMemory)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(panelValue)
        		.addContainerGap());
        
        pack();
    }

    private void btnOpenImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenImageActionPerformed
        JFileChooser f = new JFileChooser();
        EmuFileFilter f1 = new EmuFileFilter();
        EmuFileFilter f2 = new EmuFileFilter();

        f1.addExtension("hex");
        f1.addExtension("bin");
        f1.setDescription("Image file (*.hex, *.bin)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");
        
        f.setDialogTitle("Load an image");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Load");
        f.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File fileSource = f.getSelectedFile();
            if (fileSource.canRead() == true) {
                if (fileSource.getName().toLowerCase().endsWith(".hex"))
                    memContext.loadHex(fileSource.getAbsolutePath(),0);
                else {
                    // ask for address where to load image
                    int adr = 0;
                    String sadr = JOptionPane.showInputDialog("Enter starting address:", 0);
                    try { adr = Integer.decode(sadr); }
                    catch(NumberFormatException e) {}
                    memContext.loadBin(fileSource.getAbsolutePath(),adr,0);
                }
                this.tblMemory.revalidate();
                this.tblMemory.repaint();
            } else {
                StaticDialogs.showErrorMessage("File " + fileSource.getPath()
                    + " can't be read.");
            }
        }
}//GEN-LAST:event_btnOpenImageActionPerformed

    private void btnClearMemoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearMemoryActionPerformed
       memContext.clearMemory();
       tblMemory.revalidate();
       tblMemory.repaint();
}//GEN-LAST:event_btnClearMemoryActionPerformed
    
    private void btnFindAddressActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFindAddressActionPerformed
        int address = 0;
        try {
            address = Integer.decode(JOptionPane.showInputDialog(this,
                    "Find address:","Find Address",
                    JOptionPane.QUESTION_MESSAGE,null,null,0).toString()).intValue();
        } catch(NumberFormatException e) {return;} catch (NullPointerException f) {return;}
        if (address <0 || address >= memContext.getSize()) {
            JOptionPane.showMessageDialog(this,"Error: Address out of bounds",
                    "Find Address",JOptionPane.ERROR_MESSAGE);
            return;
        }
        memModel.setPage(address / (memModel.getRowCount() * memModel.getColumnCount()));
        int c = (address & 0xF);
        int r = (address & 0xF0)>>4;
        try {
        	tblMemory.setColumnSelectionInterval(c, c);
        	tblMemory.setRowSelectionInterval(r, r);
        	tblMemory.scrollRectToVisible(tblMemory.getCellRect(r, c, false));
        	updateMemVal(r,c);
        } catch(Exception e) {}
}//GEN-LAST:event_btnFindAddressActionPerformed

private void btnSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSettingsActionPerformed
    new frmSettings(this,true,mem,tblMemory,settings).setVisible(true);
}//GEN-LAST:event_btnSettingsActionPerformed

private void btnDumpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDumpActionPerformed
        JFileChooser f = new JFileChooser();
        EmuFileFilter f1 = new EmuFileFilter();
        EmuFileFilter f2 = new EmuFileFilter();

        f1.addExtension("txt");
        f1.setDescription("Human-readable dump (*.txt)");
        f2.addExtension("bin");
        f2.setDescription("Binary dump (*.bin)");
        
        f.setDialogTitle("Dump memory into a file");
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Dump");
        f.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = f.showOpenDialog(this);
        f.setVisible(true);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File fileSource = f.getSelectedFile();
            try {
                if (fileSource.exists()) fileSource.delete();
                fileSource.createNewFile();
                if (f.getFileFilter().equals(f1)) {
                    // human-readable format
                    BufferedWriter out = new BufferedWriter(new FileWriter(fileSource));
                    for (int i =0; i < memContext.getSize(); i++)
                        out.write(String.format("%X:\t%02X\n", i,(Short)memContext.read(i)));
                    out.close();
                } else {
                    // binary format
                    FileOutputStream fos = new FileOutputStream(fileSource);
                    DataOutputStream ds = new DataOutputStream( fos );
                    for (int i =0; i < memContext.getSize(); i++)
                        ds.writeByte((Short)memContext.read(i)&0xff);
                    ds.close();
                }
            } catch(IOException e) {
                StaticDialogs.showErrorMessage("Error: Dumpfile couldn't be created.");
            }
        }
}//GEN-LAST:event_btnDumpActionPerformed

    private JLabel lblBanksCount;
    private JLabel lblPageCount;
    private JLabel lblRuntimeBank;
    private JScrollPane paneMemory;
    private JSpinner spnBank;
    private JSpinner spnPage;
    private JTextField txtAddress;
    private JTextField txtChar;
    private JTextField txtValBin;
    private JTextField txtValDec;
    private JTextField txtValHex;
    private JTextField txtValOct;    
}
