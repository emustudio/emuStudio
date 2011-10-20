/*
 * SchemaEditorFrame.java
 *
 * KISS, YAGNI
 *
 *  Copyright (C) 2010 vbmacher
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.gui;

import emustudio.architecture.ArchLoader;
import emustudio.architecture.drawing.DrawingPanel;
import emustudio.architecture.drawing.DrawingPanel.DrawEventListener;
import emustudio.architecture.drawing.DrawingPanel.PanelDrawTool;
import emustudio.architecture.drawing.Schema;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalExclusionType;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataListener;
import emuLib8.runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class SchemaEditorFrame extends javax.swing.JFrame implements KeyListener {

    /**
     * Schema of created computer.
     */
    private Schema schema;
    private boolean OOK = false;
    private DrawingPanel pan;
    private pluginModel empty_model = new pluginModel(null);
    private boolean buttonSelected = false;

    /**
     * This variable holds true if the window is for editing an existing
     * computer, false or for creating a new computer.
     */
    private boolean edit;

    private OpenComputerDialog odialog;


    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int kCode = e.getKeyCode();
        if (kCode == KeyEvent.VK_ESCAPE) {
            pan.cancelTasks();
            schema.selectElements(-1, -1, 0, 0);
        } else if (kCode == KeyEvent.VK_DELETE) {
            pan.cancelTasks();
            schema.deleteSelected();
            pan.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private class pluginModel implements ComboBoxModel {

        private String[] pluginNames;
        private Object selectedObject = null;

        public pluginModel(String[] wt) {
            this.pluginNames = wt;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            selectedObject = anItem;
        }

        @Override
        public Object getSelectedItem() {
            return selectedObject;
        }

        @Override
        public int getSize() {
            return (pluginNames == null) ? 0 : pluginNames.length;
        }

        @Override
        public Object getElementAt(int index) {
            return pluginNames[index];
        }

        @Override
        public void addListDataListener(ListDataListener l) {
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
        }
    }

    /**
     * Perform common initialization used in both constructors.
     */
    private void constructor(OpenComputerDialog odialog) {
        initComponents();
        setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        this.setLocationRelativeTo(null);
        this.odialog = odialog;
        btnUseGrid.setSelected(schema.getUseGrid());
        pan = new DrawingPanel(this.schema);
        scrollScheme.setViewportView(pan);
        scrollScheme.getHorizontalScrollBar().setUnitIncrement(10);
        scrollScheme.getVerticalScrollBar().setUnitIncrement(10);
        pan.addMouseListener(pan);
        pan.addMouseMotionListener(pan);
        addKeyListenerRecursively(this);

        pan.addEventListener(new DrawEventListener() {

            @Override
            public void toolUsed() {
                pan.cancelTasks();
                pan.setTool(PanelDrawTool.nothing, null);
                cmbPlugin.setModel(empty_model);
                groupDraw.clearSelection();
                buttonSelected = false;
            }

        });
    }

    /**
     * This method adds this key listener to all sub-components of given
     * component.
     *
     * @param c Component to add this key listener recursively
     */
    private void addKeyListenerRecursively(Component c) {
        c.addKeyListener((KeyListener) this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                addKeyListenerRecursively(children[i]);
            }
        }
    }

    /**
     * Determine whether user has pressed the OK button.
     *
     * @return true if the user pressed OK, false otherwise
     */
    public boolean getOK() {
        return OOK;
    }

    /**
     * Get the schema of edited virtual computer.
     *
     * @return the schema that was created or modified by this editor
     */
    public Schema getSchema() {
        return schema;
    }


    /**
     * This constructor is used for creating new virtual configurations.
     *
     * @param odialog parent GUI - the OpenComputerDialog instance
     */
    public SchemaEditorFrame(OpenComputerDialog odialog) {
        this.schema = new Schema();
        this.edit = false;
        constructor(odialog);
        this.setTitle("Computer editor: new computer");
    }

    /**
     * This constructor is used for editing the existing computer. If user
     * changes the configuration name, the origin will be renamed.
     *
     * @param odialog should be the OpenComputerFrame instance
     * @param schema Abstract schema of origin computer
     */
    public SchemaEditorFrame(OpenComputerDialog odialog, Schema schema) {
        this.schema = schema;
        this.edit = true;
        constructor(odialog);
        this.setTitle("Computer editor: " + schema.getConfigName());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        groupDraw = new javax.swing.ButtonGroup();
        javax.swing.JToolBar toolDraw = new javax.swing.JToolBar();
        javax.swing.JButton btnSave = new javax.swing.JButton();
        javax.swing.JToolBar.Separator jSeparator4 = new javax.swing.JToolBar.Separator();
        btnCompiler = new javax.swing.JToggleButton();
        btnCPU = new javax.swing.JToggleButton();
        btnRAM = new javax.swing.JToggleButton();
        btnDevice = new javax.swing.JToggleButton();
        javax.swing.JToolBar.Separator jSeparator5 = new javax.swing.JToolBar.Separator();
        btnLine = new javax.swing.JToggleButton();
        btnBidirection = new javax.swing.JToggleButton();
        javax.swing.JToolBar.Separator jSeparator2 = new javax.swing.JToolBar.Separator();
        btnDelete = new javax.swing.JToggleButton();
        javax.swing.JToolBar.Separator jSeparator1 = new javax.swing.JToolBar.Separator();
        cmbPlugin = new javax.swing.JComboBox();
        javax.swing.JToolBar.Separator jSeparator3 = new javax.swing.JToolBar.Separator();
        btnUseGrid = new javax.swing.JToggleButton();
        scrollScheme = new javax.swing.JScrollPane();
        sliderGridGap = new javax.swing.JSlider();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Computer editor");
        setIconImages(null);

        toolDraw.setFloatable(false);
        toolDraw.setRollover(true);

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/document-save.png"))); // NOI18N
        btnSave.setToolTipText("Save & Close");
        btnSave.setFocusable(false);
        btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        toolDraw.add(btnSave);
        toolDraw.add(jSeparator4);

        groupDraw.add(btnCompiler);
        btnCompiler.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/compile.png"))); // NOI18N
        btnCompiler.setToolTipText("Set compiler");
        btnCompiler.setFocusable(false);
        btnCompiler.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCompiler.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCompiler.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnCompilerItemStateChanged(evt);
            }
        });
        btnCompiler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompilerActionPerformed(evt);
            }
        });
        toolDraw.add(btnCompiler);

        groupDraw.add(btnCPU);
        btnCPU.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/cpu.gif"))); // NOI18N
        btnCPU.setToolTipText("Set CPU");
        btnCPU.setFocusable(false);
        btnCPU.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCPU.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
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
        toolDraw.add(btnCPU);

        groupDraw.add(btnRAM);
        btnRAM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/ram.gif"))); // NOI18N
        btnRAM.setToolTipText("Set main store");
        btnRAM.setFocusable(false);
        btnRAM.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnRAM.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnRAM.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnRAMItemStateChanged(evt);
            }
        });
        btnRAM.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRAMActionPerformed(evt);
            }
        });
        toolDraw.add(btnRAM);

        groupDraw.add(btnDevice);
        btnDevice.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/device.png"))); // NOI18N
        btnDevice.setToolTipText("Add device");
        btnDevice.setFocusable(false);
        btnDevice.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDevice.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
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
        toolDraw.add(btnDevice);
        toolDraw.add(jSeparator5);

        groupDraw.add(btnLine);
        btnLine.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/connection.png"))); // NOI18N
        btnLine.setToolTipText("Add connection");
        btnLine.setFocusable(false);
        btnLine.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnLine.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnLine.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                btnLineItemStateChanged(evt);
            }
        });
        btnLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLineActionPerformed(evt);
            }
        });
        toolDraw.add(btnLine);

        btnBidirection.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/bidirection.png"))); // NOI18N
        btnBidirection.setSelected(true);
        btnBidirection.setToolTipText("Bidirectional connection");
        btnBidirection.setFocusable(false);
        btnBidirection.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBidirection.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBidirection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBidirectionActionPerformed(evt);
            }
        });
        toolDraw.add(btnBidirection);
        toolDraw.add(jSeparator2);

        groupDraw.add(btnDelete);
        btnDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/edit-delete.png"))); // NOI18N
        btnDelete.setToolTipText("Delete component or connection");
        btnDelete.setFocusable(false);
        btnDelete.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnDelete.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
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
        toolDraw.add(btnDelete);
        toolDraw.add(jSeparator1);

        cmbPlugin.setToolTipText("Select plug-in");
        cmbPlugin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbPluginActionPerformed(evt);
            }
        });
        toolDraw.add(cmbPlugin);
        toolDraw.add(jSeparator3);

        btnUseGrid.setIcon(new javax.swing.ImageIcon(getClass().getResource("/emustudio/resources/grid_memory.gif"))); // NOI18N
        btnUseGrid.setSelected(true);
        btnUseGrid.setToolTipText("Use grid?");
        btnUseGrid.setFocusable(false);
        btnUseGrid.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnUseGrid.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnUseGrid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUseGridActionPerformed(evt);
            }
        });
        toolDraw.add(btnUseGrid);

        sliderGridGap.setMinimum(5);
        sliderGridGap.setOrientation(javax.swing.JSlider.VERTICAL);
        sliderGridGap.setPaintTicks(true);
        sliderGridGap.setSnapToTicks(true);
        sliderGridGap.setToolTipText("Set grid size");
        sliderGridGap.setValue(30);
        sliderGridGap.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sliderGridGapStateChanged(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(toolDraw, javax.swing.GroupLayout.DEFAULT_SIZE, 641, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(scrollScheme, javax.swing.GroupLayout.DEFAULT_SIZE, 614, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sliderGridGap, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(toolDraw, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sliderGridGap, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                    .addComponent(scrollScheme, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sliderGridGapStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderGridGapStateChanged
        pan.setGridGap(sliderGridGap.getValue());
        schema.setGridGap(sliderGridGap.getValue());
    }//GEN-LAST:event_sliderGridGapStateChanged

    private void btnCPUActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCPUActionPerformed
        if (buttonSelected) {
            groupDraw.clearSelection();
            cmbPlugin.setModel(empty_model);
            pan.cancelTasks();
            pan.setTool(PanelDrawTool.nothing, "");
            buttonSelected = false;
            return;
        }
        buttonSelected = true;
        String[] cpus = ArchLoader.getAllNames(ArchLoader.CPUS_DIR, ".jar");
        cmbPlugin.setModel(new pluginModel(cpus));
        try {
            cmbPlugin.setSelectedIndex(0);
        } catch (IllegalArgumentException e) {
        }
    }//GEN-LAST:event_btnCPUActionPerformed

    private void btnRAMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRAMActionPerformed
        if (buttonSelected) {
            cmbPlugin.setModel(empty_model);
            groupDraw.clearSelection();
            pan.cancelTasks();
            pan.setTool(PanelDrawTool.nothing, "");
            buttonSelected = false;
            return;
        }
        buttonSelected = true;
        String[] mems = ArchLoader.getAllNames(ArchLoader.MEMORIES_DIR, ".jar");
        cmbPlugin.setModel(new pluginModel(mems));
        try {
            cmbPlugin.setSelectedIndex(0);
        } catch (IllegalArgumentException e) {
        }
    }//GEN-LAST:event_btnRAMActionPerformed

    private void btnDeviceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeviceActionPerformed
        if (buttonSelected) {
            cmbPlugin.setModel(empty_model);
            groupDraw.clearSelection();
            pan.cancelTasks();
            pan.setTool(PanelDrawTool.nothing, "");
            buttonSelected = false;
            return;
        }
        buttonSelected = true;
        String[] devs = ArchLoader.getAllNames(ArchLoader.DEVICES_DIR, ".jar");
        cmbPlugin.setModel(new pluginModel(devs));
        try {
            cmbPlugin.setSelectedIndex(0);
        } catch (IllegalArgumentException e) {
        }
    }//GEN-LAST:event_btnDeviceActionPerformed

    private void btnLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLineActionPerformed
        pan.cancelTasks();
        pan.setTool(PanelDrawTool.nothing, "");
        cmbPlugin.setModel(empty_model);
        if (buttonSelected) {
            groupDraw.clearSelection();
            return;
        }
        pan.setTool(PanelDrawTool.connectLine, "");
        buttonSelected = true;
    }//GEN-LAST:event_btnLineActionPerformed

    private void cmbPluginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbPluginActionPerformed
        if (cmbPlugin.getSelectedIndex() == -1) {
            pan.cancelTasks();
            return;
        }
        String t = (String) cmbPlugin.getSelectedItem();
        if (btnCompiler.isSelected())
            pan.setTool(PanelDrawTool.shapeCompiler, t);
        if (btnCPU.isSelected())
            pan.setTool(PanelDrawTool.shapeCPU, t);
        else if (btnRAM.isSelected())
            pan.setTool(PanelDrawTool.shapeMemory, t);
        else if (btnDevice.isSelected())
            pan.setTool(PanelDrawTool.shapeDevice, t);
    }//GEN-LAST:event_cmbPluginActionPerformed

    private void btnCompilerItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnCompilerItemStateChanged
        if (!btnCompiler.isSelected()) {
            buttonSelected = false;
        }
    }//GEN-LAST:event_btnCompilerItemStateChanged

    private void btnCPUItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnCPUItemStateChanged
        if (!btnCPU.isSelected()) {
            buttonSelected = false;
        }
    }//GEN-LAST:event_btnCPUItemStateChanged

    private void btnRAMItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnRAMItemStateChanged
        if (!btnRAM.isSelected()) {
            buttonSelected = false;
        }
    }//GEN-LAST:event_btnRAMItemStateChanged

    private void btnDeviceItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnDeviceItemStateChanged
        if (!btnDevice.isSelected()) {
            buttonSelected = false;
        }
    }//GEN-LAST:event_btnDeviceItemStateChanged

    private void btnLineItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnLineItemStateChanged
        if (!btnLine.isSelected()) {
            buttonSelected = false;
        }
    }//GEN-LAST:event_btnLineItemStateChanged

    private void btnDeleteItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_btnDeleteItemStateChanged
        if (!btnDelete.isSelected()) {
            buttonSelected = false;
        }
    }//GEN-LAST:event_btnDeleteItemStateChanged

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        pan.cancelTasks();
        pan.setTool(PanelDrawTool.nothing, "");
        cmbPlugin.setModel(empty_model);
        if (buttonSelected) {
            groupDraw.clearSelection();
            return;
        }
        pan.setTool(PanelDrawTool.delete, "");
        buttonSelected = true;
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // check for correctness of the schema
        if (schema.getCpuElement() == null) {
            StaticDialogs.showErrorMessage("The computer must contain a CPU!");
            return;
        }

        String name = StaticDialogs.inputStringValue(
                edit ? "Enter computer name (leave for the origin name):"
                     : "Enter new computer name:", "Save & Close",
                edit ? schema.getConfigName()
                     : "");

        try {
            File f = new File(name + ".conf");
            f = null;
        } catch (NullPointerException np) {
            StaticDialogs.showErrorMessage("Computer name can not be empty!");
            return;
        } 
        catch(Exception e) {
            StaticDialogs.showErrorMessage("Computer name is wrong!");
            return;
        }

        if ((name == null) || (name.equals(""))) {
            OOK = false;
        } else {
            OOK = true;

            if (edit) {
                String old = schema.getConfigName();
                schema.setConfigName(name);

                if (!old.equals(name)) {
                    ArchLoader.renameConfig(name, old);
                }
                ArchLoader.saveSchema(schema);
                odialog.setArchName(name);
                odialog.update();
            } else {
                schema.setConfigName(name);
                ArchLoader.saveSchema(schema);
                odialog.update();
            }
        }
        dispose();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnUseGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUseGridActionPerformed
        pan.setUseGrid(btnUseGrid.isSelected());
        sliderGridGap.setEnabled(btnUseGrid.isSelected());
        schema.setUseGrid(btnUseGrid.isSelected());
        schema.setGridGap(sliderGridGap.getValue());
    }//GEN-LAST:event_btnUseGridActionPerformed

    private void btnCompilerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompilerActionPerformed
        if (buttonSelected) {
            groupDraw.clearSelection();
            cmbPlugin.setModel(empty_model);
            buttonSelected = false;
            pan.cancelTasks();
            pan.setTool(PanelDrawTool.nothing, "");
            return;
        }
        buttonSelected = true;
        String[] compilers = ArchLoader.getAllNames(ArchLoader.COMPILERS_DIR,
                    ".jar");
        cmbPlugin.setModel(new pluginModel(compilers));
        try {
            cmbPlugin.setSelectedIndex(0);
        } catch (IllegalArgumentException e) {
        }
    }//GEN-LAST:event_btnCompilerActionPerformed

    private void btnBidirectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBidirectionActionPerformed
        pan.setFutureLineDirection(btnBidirection.isSelected());
    }//GEN-LAST:event_btnBidirectionActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnBidirection;
    private javax.swing.JToggleButton btnCPU;
    private javax.swing.JToggleButton btnCompiler;
    private javax.swing.JToggleButton btnDelete;
    private javax.swing.JToggleButton btnDevice;
    private javax.swing.JToggleButton btnLine;
    private javax.swing.JToggleButton btnRAM;
    private javax.swing.JToggleButton btnUseGrid;
    private javax.swing.JComboBox cmbPlugin;
    private javax.swing.ButtonGroup groupDraw;
    private javax.swing.JScrollPane scrollScheme;
    private javax.swing.JSlider sliderGridGap;
    // End of variables declaration//GEN-END:variables

}
