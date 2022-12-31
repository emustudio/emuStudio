/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.ToolbarButton;
import net.emustudio.application.gui.ToolbarToggleButton;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.DrawingPanel.Tool;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.gui.GuiUtils.addKeyListenerRecursively;
import static net.emustudio.application.settings.ConfigFiles.listPluginFiles;

public class SchemaEditorDialog extends JDialog implements KeyListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(SchemaEditorDialog.class);
    private final static PluginComboModel EMPTY_MODEL = new PluginComboModel(Collections.emptyList());

    private final Schema schema;
    private final Dialogs dialogs;

    private DrawingPanel panel;
    private boolean buttonSelected = false;
    private ToolbarToggleButton btnBidirection;
    private ToolbarToggleButton btnCPU;
    private ToolbarToggleButton btnCompiler;
    private ToolbarToggleButton btnDelete;
    private ToolbarToggleButton btnDevice;
    private ToolbarToggleButton btnLine;
    private ToolbarToggleButton btnRAM;
    private ToolbarToggleButton btnUseGrid;
    private JComboBox<String> cmbPlugin;
    private ButtonGroup groupDraw;
    private JScrollPane scrollScheme;
    private JSlider sliderGridGap;

    public SchemaEditorDialog(JDialog parent, Schema schema, Dialogs dialogs) {
        super(parent, true);

        this.schema = Objects.requireNonNull(schema);
        this.dialogs = Objects.requireNonNull(dialogs);

        initialize();
        setTitle("Computer editor [" + schema.getComputerConfig().getName() + "]");
        setLocationRelativeTo(parent);
    }

    public Schema getSchema() {
        return schema;
    }

    private void initialize() {
        initComponents();
        btnUseGrid.setSelected(schema.useSchemaGrid());
        panel = new DrawingPanel(this.schema);
        scrollScheme.setViewportView(panel);
        scrollScheme.getHorizontalScrollBar().setUnitIncrement(10);
        scrollScheme.getVerticalScrollBar().setUnitIncrement(10);
        sliderGridGap.setValue(schema.getSchemaGridGap());
        panel.addMouseListener(panel);
        panel.addMouseMotionListener(panel);
        addKeyListenerRecursively(this, this);

        panel.addToolListener(() -> {
            panel.setTool(Tool.TOOL_NOTHING, null);
            cmbPlugin.setModel(EMPTY_MODEL);
            groupDraw.clearSelection();
            buttonSelected = false;
        });
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int kCode = e.getKeyCode();
        if (kCode == KeyEvent.VK_ESCAPE) {
            panel.cancelDrawing();
            schema.select(-1, -1, 0, 0);
        } else if (kCode == KeyEvent.VK_DELETE) {
            panel.cancelDrawing();
            schema.deleteSelected();
            panel.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void initComponents() {

        groupDraw = new ButtonGroup();
        JToolBar toolDraw = new JToolBar();
        ToolbarButton btnSave = new ToolbarButton(
                this::btnSaveActionPerformed,
                "/net/emustudio/application/gui/dialogs/document-save.png",
                "Save & Close"
        );
        JToolBar.Separator separator1 = new JToolBar.Separator();
        btnCompiler = new ToolbarToggleButton(
                this::btnCompilerActionPerformed,
                this::btnCompilerItemStateChanged,
                "/net/emustudio/application/gui/dialogs/compile.png",
                "Set compiler"
        );
        btnCPU = new ToolbarToggleButton(
                this::btnCPUActionPerformed,
                this::btnCPUItemStateChanged,
                "/net/emustudio/application/gui/dialogs/cpu.gif",
                "Set CPU"
        );
        btnRAM = new ToolbarToggleButton(
                this::btnRAMActionPerformed,
                this::btnRAMItemStateChanged,
                "/net/emustudio/application/gui/dialogs/ram.gif",
                "Set operating memory"
        );
        btnDevice = new ToolbarToggleButton(
                this::btnDeviceActionPerformed,
                this::btnDeviceItemStateChanged,
                "/net/emustudio/application/gui/dialogs/device.png",
                "Add device"
        );
        JToolBar.Separator separator2 = new JToolBar.Separator();
        btnLine = new ToolbarToggleButton(
                this::btnLineActionPerformed,
                this::btnLineItemStateChanged,
                "/net/emustudio/application/gui/dialogs/connection.png",
                "Add connection"
        );
        btnBidirection = new ToolbarToggleButton(
                this::btnBidirectionActionPerformed,
                "/net/emustudio/application/gui/dialogs/bidirection.gif",
                "Bidirectional connection"
        );
        JToolBar.Separator separator3 = new JToolBar.Separator();
        btnDelete = new ToolbarToggleButton(
                this::btnDeleteActionPerformed,
                this::btnDeleteItemStateChanged,
                "/net/emustudio/application/gui/dialogs/edit-delete.png",
                "Delete component or connection"
        );
        JToolBar.Separator separator4 = new JToolBar.Separator();
        cmbPlugin = new JComboBox<>();
        JToolBar.Separator separator5 = new JToolBar.Separator();
        btnUseGrid = new ToolbarToggleButton(
                this::btnUseGridActionPerformed,
                "/net/emustudio/application/gui/dialogs/grid_memory.gif",
                "Set/unset using grid"
        );
        scrollScheme = new JScrollPane();
        sliderGridGap = new JSlider();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Virtual computer editor");
        setIconImages(null);

        toolDraw.setFloatable(false);
        toolDraw.setRollover(true);

        toolDraw.add(btnSave);
        toolDraw.add(separator1);
        toolDraw.add(btnCompiler);
        toolDraw.add(btnCPU);
        toolDraw.add(btnRAM);
        toolDraw.add(btnDevice);
        toolDraw.add(separator2);
        toolDraw.add(btnLine);

        btnBidirection.setSelected(true);
        toolDraw.add(btnBidirection);
        toolDraw.add(separator3);
        toolDraw.add(btnDelete);
        toolDraw.add(separator4);

        groupDraw.add(btnCompiler);
        groupDraw.add(btnCPU);
        groupDraw.add(btnRAM);
        groupDraw.add(btnDevice);
        groupDraw.add(btnLine);
        groupDraw.add(btnDelete);

        cmbPlugin.setToolTipText("Select plug-in");
        cmbPlugin.addActionListener(this::cmbPluginActionPerformed);
        toolDraw.add(cmbPlugin);
        toolDraw.add(separator5);

        btnUseGrid.setSelected(true);
        toolDraw.add(btnUseGrid);

        sliderGridGap.setMinimum(5);
        sliderGridGap.setOrientation(JSlider.VERTICAL);
        sliderGridGap.setPaintTicks(true);
        sliderGridGap.setSnapToTicks(true);
        sliderGridGap.setToolTipText("Set grid size");
        sliderGridGap.setValue(30);
        sliderGridGap.addChangeListener(this::sliderGridGapStateChanged);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(toolDraw, GroupLayout.DEFAULT_SIZE, 641, Short.MAX_VALUE)
                                                .addContainerGap())
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(scrollScheme, GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(sliderGridGap, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE))))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(toolDraw, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(sliderGridGap, GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                                        .addComponent(scrollScheme, GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE))
                                .addContainerGap())
        );

        pack();
    }

    private void sliderGridGapStateChanged(ChangeEvent evt) {
        panel.setGridGap(sliderGridGap.getValue());
        schema.setSchemaGridGap(sliderGridGap.getValue());
    }

    private void btnCompilerActionPerformed(ActionEvent evt) {
        if (checkUnsetDrawingTool()) {
            buttonSelected = true;
            resetComboWithPluginFiles(PLUGIN_TYPE.COMPILER);
        }
    }

    private void btnCPUActionPerformed(ActionEvent evt) {
        if (checkUnsetDrawingTool()) {
            buttonSelected = true;
            resetComboWithPluginFiles(PLUGIN_TYPE.CPU);
        }
    }

    private void btnRAMActionPerformed(ActionEvent evt) {
        if (checkUnsetDrawingTool()) {
            buttonSelected = true;
            resetComboWithPluginFiles(PLUGIN_TYPE.MEMORY);
        }
    }

    private void btnDeviceActionPerformed(ActionEvent evt) {
        if (checkUnsetDrawingTool()) {
            buttonSelected = true;
            resetComboWithPluginFiles(PLUGIN_TYPE.DEVICE);
        }
    }

    private void btnLineActionPerformed(ActionEvent evt) {
        panel.setTool(Tool.TOOL_NOTHING, null);
        cmbPlugin.setModel(EMPTY_MODEL);
        if (buttonSelected) {
            groupDraw.clearSelection();
            return;
        }
        panel.setTool(Tool.TOOL_CONNECTION, null);
        buttonSelected = true;
    }

    private void cmbPluginActionPerformed(ActionEvent evt) {
        Optional<String> pluginFile = ((PluginComboModel) cmbPlugin.getModel()).getSelectedFileName();
        pluginFile.ifPresentOrElse(fileName -> {
            if (btnCompiler.isSelected()) {
                panel.setTool(Tool.TOOL_COMPILER, fileName);
            }
            if (btnCPU.isSelected()) {
                panel.setTool(Tool.TOOL_CPU, fileName);
            } else if (btnRAM.isSelected()) {
                panel.setTool(Tool.TOOL_MEMORY, fileName);
            } else if (btnDevice.isSelected()) {
                panel.setTool(Tool.TOOL_DEVICE, fileName);
            }
        }, () -> panel.cancelDrawing());
    }

    private void btnCompilerItemStateChanged(ItemEvent evt) {
        if (!btnCompiler.isSelected()) {
            buttonSelected = false;
        }
    }

    private void btnCPUItemStateChanged(ItemEvent evt) {
        if (!btnCPU.isSelected()) {
            buttonSelected = false;
        }
    }

    private void btnRAMItemStateChanged(ItemEvent evt) {
        if (!btnRAM.isSelected()) {
            buttonSelected = false;
        }
    }

    private void btnDeviceItemStateChanged(ItemEvent evt) {
        if (!btnDevice.isSelected()) {
            buttonSelected = false;
        }
    }

    private void btnLineItemStateChanged(ItemEvent evt) {
        if (!btnLine.isSelected()) {
            buttonSelected = false;
        }
    }

    private void btnDeleteItemStateChanged(ItemEvent evt) {
        if (!btnDelete.isSelected()) {
            buttonSelected = false;
        }
    }

    private void btnDeleteActionPerformed(ActionEvent evt) {
        panel.setTool(Tool.TOOL_NOTHING, null);
        cmbPlugin.setModel(EMPTY_MODEL);
        if (buttonSelected) {
            groupDraw.clearSelection();
        } else {
            panel.setTool(Tool.TOOL_DELETE, null);
            buttonSelected = true;
        }
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        try {
            schema.save();
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not save computer schema", e);
            dialogs.showError("Could not save computer schema. Please consult log file for details.", "Save schema");
        }
        dispose();
    }

    private void btnUseGridActionPerformed(ActionEvent evt) {
        panel.setUsingGrid(btnUseGrid.isSelected());
        sliderGridGap.setEnabled(btnUseGrid.isSelected());
        schema.setUseSchemaGrid(btnUseGrid.isSelected());
        schema.setSchemaGridGap(sliderGridGap.getValue());
    }

    private void btnBidirectionActionPerformed(ActionEvent evt) {
        panel.setFutureLineDirection(btnBidirection.isSelected());
    }

    private void resetComboWithPluginFiles(PLUGIN_TYPE pluginType) {
        try {
            List<String> pluginFiles = listPluginFiles(pluginType);
            cmbPlugin.setModel(new PluginComboModel(pluginFiles));
            selectFirstPlugin();
        } catch (IOException e) {
            LOGGER.error("Could not load CPU plugin files", e);
            cmbPlugin.setModel(EMPTY_MODEL);
        }
    }

    private boolean checkUnsetDrawingTool() {
        if (buttonSelected) {
            cmbPlugin.setModel(EMPTY_MODEL);
            groupDraw.clearSelection();
            panel.setTool(Tool.TOOL_NOTHING, null);
            buttonSelected = false;
            return false;
        }
        return true;
    }

    private void selectFirstPlugin() {
        if (cmbPlugin.getItemCount() > 0) {
            cmbPlugin.setSelectedIndex(0);
        }
    }
}
