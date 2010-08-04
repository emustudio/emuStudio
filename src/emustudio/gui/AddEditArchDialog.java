/*
 * AddEditArchDialog.java
 *
 * Created on Streda, 2008, júl 9, 9:40
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.gui;

import emustudio.architecture.ArchLoader;
import emustudio.architecture.drawing.DrawingPanel;
import emustudio.architecture.drawing.DrawingPanel.drawTool;
import emustudio.architecture.drawing.Schema;
import emustudio.gui.utils.NiceButton;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListDataListener;
import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class AddEditArchDialog extends JDialog implements KeyListener {
    private Schema schema;
    private boolean OOK = false;
    private DrawingPanel pan;
    private pluginModel empty_model = new pluginModel(null);
    
    private boolean buttonSelected = false;
    
    private class pluginModel implements ComboBoxModel {
        private String[] pluginNames;
        private Object selectedObject = null;
        public pluginModel(String[] wt) { this.pluginNames = wt; }
        public void setSelectedItem(Object anItem) { selectedObject = anItem; }
        public Object getSelectedItem() { return selectedObject; }
        public int getSize() { return (pluginNames == null) ? 0 : pluginNames.length; }
        public Object getElementAt(int index) { return pluginNames[index]; }
        public void addListDataListener(ListDataListener l) {}
        public void removeListDataListener(ListDataListener l) {}
    }

    private void constructor() {
        initComponents();
        String[] compilers = ArchLoader.getAllNames(ArchLoader.compilersDir,
                ".jar");
        cmbCompiler.setModel(new pluginModel(compilers));
        this.setLocationRelativeTo(null);
        pan = new DrawingPanel(this.schema, true, 30);
        scrollScheme.setViewportView(pan);
        scrollScheme.getHorizontalScrollBar().setUnitIncrement(10);
        scrollScheme.getVerticalScrollBar().setUnitIncrement(10);
        pan.addMouseListener(pan);
        pan.addMouseMotionListener(pan);
        addKeyListenerRecursively(this);        
    }
    
    /** Creates new form AddEditArchDialog */
    public AddEditArchDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        this.schema = new Schema();
        constructor();
        cmbCompiler.setSelectedIndex(-1);
        this.setTitle("Add new architecture");
    }
    
    public AddEditArchDialog(JDialog parent, boolean modal,
            Schema schema) {
        super(parent, modal);
        this.schema = schema;
        constructor();
        cmbCompiler.setSelectedItem(schema.getCompilerName());
        txtMemorySize.setText(String.valueOf(schema.getMemorySize()));
        this.setTitle("Edit architecture");
        txtArchName.setText(schema.getConfigName());
        txtArchName.setEnabled(false);
        btnBrowse.setEnabled(false);
    }

    public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            pan.cancelTasks();
    }
    public void keyReleased(KeyEvent e) {}
    
    private void addKeyListenerRecursively(Component c) {
        c.addKeyListener((KeyListener) this);
        if (c instanceof Container) {
            Container cont = (Container)c;
            Component[] children = cont.getComponents();
            for(int i = 0; i < children.length; i++)
                addKeyListenerRecursively(children[i]);
        }
    }
    
    public boolean getOK() { return OOK; }
    
    public Schema getSchema() { return schema; }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

        grpElements = new ButtonGroup();
        JToolBar toolBar = new JToolBar();
        btnCPU = new JToggleButton();
        btnMemory = new JToggleButton();
        btnDevice = new JToggleButton();
        JToolBar.Separator jSeparator1 = new JToolBar.Separator();
        btnConnect = new JToggleButton();
        btnDelete = new JToggleButton();
        JToolBar.Separator jSeparator2 = new JToolBar.Separator();
        JPanel panelSelectElement = new JPanel();
        cmbElement = new JComboBox();
        scrollScheme = new JScrollPane();
        chkUseGrid = new JCheckBox();
        sliderGridGap = new JSlider();
        NiceButton btnOK = new NiceButton();
        JLabel lblArchName = new JLabel();
        txtArchName = new JTextField();
        btnBrowse = new NiceButton();
        JLabel lblCompiler = new JLabel();
        cmbCompiler = new JComboBox();
        JLabel lblMemorySize = new JLabel();
        txtMemorySize = new JTextField();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Add new architecture");

        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        grpElements.add(btnCPU);
        btnCPU.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/cpu.png"))); // NOI18N
        btnCPU.setFocusable(false);
        btnCPU.setToolTipText("CPU (processor)");
        btnCPU.setHorizontalTextPosition(SwingConstants.CENTER);
        btnCPU.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnCPU.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnCPUItemStateChanged(evt);
            }
        });
        btnCPU.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCPUActionPerformed(evt);
            }
        });
        toolBar.add(btnCPU);

        grpElements.add(btnMemory);
        btnMemory.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/ram.png"))); // NOI18N
        btnMemory.setFocusable(false);
        btnMemory.setToolTipText("Operating memory");
        btnMemory.setHorizontalTextPosition(SwingConstants.CENTER);
        btnMemory.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnMemory.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnMemoryItemStateChanged(evt);
            }
        });
        btnMemory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMemoryActionPerformed(evt);
            }
        });
        toolBar.add(btnMemory);

        grpElements.add(btnDevice);
        btnDevice.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/input-gaming.png"))); // NOI18N
        btnDevice.setFocusable(false);
        btnDevice.setToolTipText("Peripheral device");
        btnDevice.setHorizontalTextPosition(SwingConstants.CENTER);
        btnDevice.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnDevice.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnDeviceItemStateChanged(evt);
            }
        });
        btnDevice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeviceActionPerformed(evt);
            }
        });
        toolBar.add(btnDevice);
        toolBar.add(jSeparator1);

        grpElements.add(btnConnect);
        btnConnect.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/network-wired.png"))); // NOI18N
        btnConnect.setFocusable(false);
        btnConnect.setToolTipText("Line connector");
        btnConnect.setHorizontalTextPosition(SwingConstants.CENTER);
        btnConnect.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnConnect.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnConnectItemStateChanged(evt);
            }
        });
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });
        toolBar.add(btnConnect);

        grpElements.add(btnDelete);
        btnDelete.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/edit-delete.png"))); // NOI18N
        btnDelete.setToolTipText("Delete element/line");
        btnDelete.setFocusable(false);
        btnDelete.setHorizontalTextPosition(SwingConstants.CENTER);
        btnDelete.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnDelete.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnDeleteItemStateChanged(evt);
            }
        });
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        toolBar.add(btnDelete);
        toolBar.add(jSeparator2);

        panelSelectElement.setOpaque(false);
        panelSelectElement.setPreferredSize(new java.awt.Dimension(250, 44));

        cmbElement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbElementActionPerformed(evt);
            }
        });

        GroupLayout panelSelectElementLayout = new GroupLayout(panelSelectElement);
        panelSelectElement.setLayout(panelSelectElementLayout);
        panelSelectElementLayout.setHorizontalGroup(
            panelSelectElementLayout.createSequentialGroup()
                        .addContainerGap()
            			.addComponent(cmbElement, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            			.addContainerGap()
        );
        panelSelectElementLayout.setVerticalGroup(
            panelSelectElementLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(cmbElement)
                .addContainerGap()
        );

        toolBar.add(panelSelectElement);

        chkUseGrid.setSelected(true);
        chkUseGrid.setText("Use grid");
        chkUseGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkUseGridActionPerformed(evt);
            }
        });

        sliderGridGap.setMaximum(40);
        sliderGridGap.setMinimum(3);
        sliderGridGap.setMinorTickSpacing(1);
        sliderGridGap.setPaintTicks(true);
        sliderGridGap.setToolTipText("Grid gap");
        sliderGridGap.setValue(30);
        sliderGridGap.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderGridGapStateChanged(evt);
            }
        });

        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });

        lblArchName.setText("Config name:");

        btnBrowse.setText("Browse...");
        btnBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseActionPerformed(evt);
            }
        });

        lblCompiler.setText("Compiler:");

        lblMemorySize.setText("Memory size:");

        txtMemorySize.setText("65536");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(
        		layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		.addComponent(toolBar)
        		.addComponent(scrollScheme,GroupLayout.PREFERRED_SIZE, 700, Short.MAX_VALUE)
        		.addGroup(layout.createSequentialGroup()
        				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
        						.addComponent(chkUseGrid)
        						.addComponent(lblCompiler)
        						.addComponent(lblMemorySize)
        						.addComponent(lblArchName))
        				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING,true)
        						.addComponent(sliderGridGap,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        						.addComponent(cmbCompiler,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        						.addComponent(txtMemorySize,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        						.addGroup(layout.createSequentialGroup()
        								.addComponent(txtArchName)
        								.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        								.addComponent(btnBrowse))))
        		.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
   						.addComponent(btnOK,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
   					          GroupLayout.PREFERRED_SIZE)));        
        layout.setVerticalGroup(layout.createSequentialGroup()
        		.addComponent(toolBar,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
        		          GroupLayout.PREFERRED_SIZE)
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addComponent(scrollScheme,GroupLayout.PREFERRED_SIZE, 300, Short.MAX_VALUE)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(chkUseGrid)
        				.addComponent(sliderGridGap))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblCompiler)
        				.addComponent(cmbCompiler))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblMemorySize)
        				.addComponent(txtMemorySize))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblArchName)
        				.addComponent(txtArchName)
        				.addComponent(btnBrowse))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        		.addComponent(btnOK,GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
        		          GroupLayout.PREFERRED_SIZE)
        		.addContainerGap());
        pack();
    }

private void btnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOKActionPerformed
    String s = txtArchName.getText();
    
    // todo: check name for legal file name
    if (s.equals("")) {
        StaticDialogs.showErrorMessage("Architecture name can not be empty!");
        txtArchName.grabFocus();
        return;
    }
    schema.setConfigName(s);
    
    // check for correctness of the schema
    if (schema.getCpuElement() == null || schema.getMemoryElement() == null) {
        StaticDialogs.showErrorMessage("Abstract schema must contain CPU and"
                + " MEMORY elements!");
        return;
    }
    // really??
    if (cmbCompiler.getSelectedIndex() == -1) {
        StaticDialogs.showErrorMessage("Compiler has to be selected!");
        cmbCompiler.grabFocus();
        return;
    }
    schema.setCompilerName(String.valueOf(cmbCompiler.getSelectedItem()));
    try { schema.setMemorySize(Integer.parseInt(txtMemorySize.getText())); }
    catch(Exception e) {
        StaticDialogs.showErrorMessage("Memory size has to be integer number!");
        txtMemorySize.grabFocus();
        return;
    }
    OOK = true;
    dispose();
}//GEN-LAST:event_btnOKActionPerformed

private void chkUseGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkUseGridActionPerformed
    pan.setUseGrid(chkUseGrid.isSelected());
    sliderGridGap.setEnabled(chkUseGrid.isSelected());
}//GEN-LAST:event_chkUseGridActionPerformed

private void sliderGridGapStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderGridGapStateChanged
    pan.setGridGap(sliderGridGap.getValue());
}//GEN-LAST:event_sliderGridGapStateChanged

private void btnCPUActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCPUActionPerformed
    pan.cancelTasks();
    pan.setTool(drawTool.nothing, "");
    if (buttonSelected) {
        grpElements.clearSelection();
        cmbElement.setModel(empty_model);
        btnCPU.setSelected(false);
        return;
    }
    buttonSelected = true;
    String[] cpus = ArchLoader.getAllNames(ArchLoader.cpusDir, ".jar");
    cmbElement.setModel(new pluginModel(cpus));
    try {
        cmbElement.setSelectedIndex(0);
    } catch(IllegalArgumentException e) {}
}//GEN-LAST:event_btnCPUActionPerformed

private void btnMemoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMemoryActionPerformed
    pan.cancelTasks();
    pan.setTool(drawTool.nothing, "");
    if (buttonSelected) {
        cmbElement.setModel(empty_model);
        grpElements.clearSelection();
        btnMemory.setSelected(false);
        return;
    }
    buttonSelected = true;
    String[] mems = ArchLoader.getAllNames(ArchLoader.memoriesDir, ".jar");
    cmbElement.setModel(new pluginModel(mems));
    try {
        cmbElement.setSelectedIndex(0);
    } catch(IllegalArgumentException e) {}
}//GEN-LAST:event_btnMemoryActionPerformed

private void btnDeviceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeviceActionPerformed
    pan.cancelTasks();
    pan.setTool(drawTool.nothing, "");
    if (buttonSelected) {
        cmbElement.setModel(empty_model);
        grpElements.clearSelection();
        btnDevice.setSelected(false);
        return;
    }
    buttonSelected = true;
    String[] devs = ArchLoader.getAllNames(ArchLoader.devicesDir, ".jar");
    cmbElement.setModel(new pluginModel(devs));
    try {
        cmbElement.setSelectedIndex(0);
    } catch(IllegalArgumentException e) {}
}//GEN-LAST:event_btnDeviceActionPerformed

private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
    pan.cancelTasks();
    pan.setTool(drawTool.nothing, "");
    cmbElement.setModel(empty_model);
    if (buttonSelected) {
        grpElements.clearSelection();
        btnConnect.setSelected(false);
        return;
    }
    pan.setTool(drawTool.connectLine, "");
    buttonSelected = true;
}//GEN-LAST:event_btnConnectActionPerformed

private void cmbElementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbElementActionPerformed
    if (cmbElement.getSelectedIndex() == -1) {
        pan.cancelTasks();
        return;
    }
    String t = (String)cmbElement.getSelectedItem();
    if (btnCPU.isSelected()) pan.setTool(drawTool.shapeCPU, t);
    else if (btnMemory.isSelected()) pan.setTool(drawTool.shapeMemory, t);
    else if (btnDevice.isSelected()) pan.setTool(drawTool.shapeDevice, t);
}//GEN-LAST:event_cmbElementActionPerformed

private void btnCPUItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnCPUItemStateChanged
    if (!btnCPU.isSelected()) buttonSelected = false;
}//GEN-LAST:event_btnCPUItemStateChanged

private void btnMemoryItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnMemoryItemStateChanged
    if (!btnMemory.isSelected()) buttonSelected = false;
}//GEN-LAST:event_btnMemoryItemStateChanged

private void btnDeviceItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnDeviceItemStateChanged
    if (!btnDevice.isSelected()) buttonSelected = false;
}//GEN-LAST:event_btnDeviceItemStateChanged

private void btnConnectItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnConnectItemStateChanged
    if (!btnConnect.isSelected()) buttonSelected = false;
}//GEN-LAST:event_btnConnectItemStateChanged

private void btnDeleteItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnDeleteItemStateChanged
    if (!btnDelete.isSelected()) buttonSelected = false;
}//GEN-LAST:event_btnDeleteItemStateChanged

private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
    pan.cancelTasks();
    pan.setTool(drawTool.nothing, "");
    cmbElement.setModel(empty_model);
    if (buttonSelected) {
        grpElements.clearSelection();
        btnConnect.setSelected(false);
        return;
    }
    pan.setTool(drawTool.delete, "");
    buttonSelected = true;
}//GEN-LAST:event_btnDeleteActionPerformed

private void btnBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseActionPerformed
    OpenArchDialog d = new OpenArchDialog(this,true);
    d.setVisible(true);
    if (d.getOK())
        txtArchName.setText(d.getArchName());
}//GEN-LAST:event_btnBrowseActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    NiceButton btnBrowse;
    JToggleButton btnCPU;
    JToggleButton btnConnect;
    JToggleButton btnDelete;
    JToggleButton btnDevice;
    JToggleButton btnMemory;
    ButtonGroup grpElements;
    JCheckBox chkUseGrid;
    JComboBox cmbCompiler;
    JComboBox cmbElement;
    JScrollPane scrollScheme;
    JSlider sliderGridGap;
    JTextField txtArchName;
    JTextField txtMemorySize;
    // End of variables declaration//GEN-END:variables

}
