/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.DrawingPanel.Tool;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.application.gui.GUIProvider;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioUI.*;
import static net.emustudio.application.settings.ConfigFiles.listPluginFiles;

public class SchemaEditorDialog extends DialogBase implements KeyListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(SchemaEditorDialog.class);
    private final static PluginComboModel EMPTY_MODEL = new PluginComboModel(Collections.emptyList());

    private final Schema schema;
    private final Dialogs dialogs;

    private final DrawingPanel panel;
    private boolean buttonSelected = false;
    private JToggleButton btnBidirection;
    private JToggleButton btnCPU;
    private JToggleButton btnCompiler;
    private JToggleButton btnDelete;
    private JToggleButton btnDevice;
    private JToggleButton btnLine;
    private JToggleButton btnRAM;
    private JToggleButton btnUseGrid;
    private JComboBox<String> cmbPlugin;
    private ButtonGroup groupDraw;
    private JScrollPane scrollScheme;
    private JSlider sliderGridGap;

    public SchemaEditorDialog(JDialog parent, Schema schema, Dialogs dialogs) {
        super(parent, "Computer editor [" + schema.getComputerConfig().getName() + "]", true);

        this.schema = Objects.requireNonNull(schema);
        this.dialogs = Objects.requireNonNull(dialogs);

        buildContent();

        // Additional initialization after buildContent is called
        btnUseGrid.setSelected(schema.useSchemaGrid());
        panel = new DrawingPanel(this.schema);
        scrollScheme.setViewportView(panel);
        scrollScheme.getHorizontalScrollBar().setUnitIncrement(10);
        scrollScheme.getVerticalScrollBar().setUnitIncrement(10);
        sliderGridGap.setValue(schema.getSchemaGridGap());
        panel.addMouseListener(panel);
        panel.addMouseMotionListener(panel);
        GUI.addKeyListenerRecursively(this, this);

        panel.addToolListener(() -> {
            panel.setTool(Tool.TOOL_NOTHING, null);
            cmbPlugin.setModel(EMPTY_MODEL);
            groupDraw.clearSelection();
            buttonSelected = false;
        });
    }

    @Override
    protected boolean shouldCloseOnEscape() {
        // ESC is used for canceling drawing operations in the schema editor
        return false;
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

    public Schema getSchema() {
        return schema;
    }

    @Override
    protected JComponent initializeComponents() {

        groupDraw = new ButtonGroup();
        JToolBar toolDraw = GUIProvider.getGUI().toolBar();
        JButton btnSave = GUIProvider.getGUI().toolbarButton(
                this::btnSaveActionPerformed,
                ICON_SAVE,
                "Save & Close"
        );
        JToolBar.Separator separator1 = new JToolBar.Separator();
        btnCompiler = GUIProvider.getGUI().toolbarToggleButton(
                this::btnCompilerActionPerformed,
                this::btnCompilerItemStateChanged,
                ICON_COMPILER,
                "Set compiler"
        );
        btnCPU = GUIProvider.getGUI().toolbarToggleButton(
                this::btnCPUActionPerformed,
                this::btnCPUItemStateChanged,
                ICON_CPU,
                "Set CPU"
        );
        btnRAM = GUIProvider.getGUI().toolbarToggleButton(
                this::btnRAMActionPerformed,
                this::btnRAMItemStateChanged,
                ICON_MEMORY,
                "Set operating memory"
        );
        btnDevice = GUIProvider.getGUI().toolbarToggleButton(
                this::btnDeviceActionPerformed,
                this::btnDeviceItemStateChanged,
                ICON_DEVICE,
                "Add device"
        );
        JToolBar.Separator separator2 = new JToolBar.Separator();
        btnLine = GUIProvider.getGUI().toolbarToggleButton(
                this::btnLineActionPerformed,
                this::btnLineItemStateChanged,
                ICON_CONNECTION,
                "Add connection"
        );
        btnBidirection = GUIProvider.getGUI().toolbarToggleButton(
                this::btnBidirectionActionPerformed,
                ICON_BIDIRECTION,
                "Bidirectional connection"
        );
        JToolBar.Separator separator3 = new JToolBar.Separator();
        btnDelete = GUIProvider.getGUI().toolbarToggleButton(
                this::btnDeleteActionPerformed,
                this::btnDeleteItemStateChanged,
                ICON_DELETE,
                "Delete component or connection"
        );
        JToolBar.Separator separator4 = new JToolBar.Separator();
        cmbPlugin = new JComboBox<>();
        JToolBar.Separator separator5 = new JToolBar.Separator();
        btnUseGrid = GUIProvider.getGUI().toolbarToggleButton(
                this::btnUseGridActionPerformed,
                ICON_GRID,
                "Set/unset using grid"
        );
        scrollScheme = new JScrollPane();
        scrollScheme.setPreferredSize(new Dimension(800, 600));
        sliderGridGap = new JSlider();


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

        JPanel mainPanel = GUIProvider.getGUI().panel("insets dialog", "[grow][]", "[][grow]");
        mainPanel.add(toolDraw, "growx, span, wrap");
        mainPanel.add(scrollScheme, "grow");
        mainPanel.add(sliderGridGap, "w 31!, growy");

        return mainPanel;
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
        }, panel::cancelDrawing);
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
