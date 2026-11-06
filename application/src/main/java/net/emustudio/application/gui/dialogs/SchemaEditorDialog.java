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
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.emulib.runtime.ui.StandardIcons.*;
import static net.emustudio.application.settings.ConfigFiles.listPluginFiles;

public class SchemaEditorDialog extends DialogBase implements KeyListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(SchemaEditorDialog.class);
    private final static PluginComboModel EMPTY_MODEL = new PluginComboModel(Collections.emptyList());

    private final Schema schema;
    private final Dialogs dialogs;
    private final GUI gui;

    private final DrawingPanel panel;
    private final AtomicInteger pluginLoadId = new AtomicInteger();
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
    private final JLabel toolStatus = new JLabel("Select and move components. Choose a toolbar tool to edit the schema.");
    private String pluginHint = "Choose a component tool first";

    public SchemaEditorDialog(JDialog parent, Schema schema, Dialogs dialogs, GUI gui) {
        super(parent, "Computer editor [" + schema.getComputerConfig().getName() + "]", true);

        this.schema = Objects.requireNonNull(schema);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);

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

        panel.addToolListener(this::resetTool);
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
            resetTool();
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
        JToolBar toolDraw = gui.toolBar();
        JButton btnSave = gui.toolbarButton(
                this::btnSaveActionPerformed,
                ICON_SAVE,
                "Save & Close"
        );
        JToolBar.Separator separator1 = new JToolBar.Separator();
        btnCompiler = gui.toolbarToggleButton(
                this::btnCompilerActionPerformed,
                this::btnCompilerItemStateChanged,
                ICON_COMPILER,
                "Set compiler"
        );
        btnCPU = gui.toolbarToggleButton(
                this::btnCPUActionPerformed,
                this::btnCPUItemStateChanged,
                ICON_CPU,
                "Set CPU"
        );
        btnRAM = gui.toolbarToggleButton(
                this::btnRAMActionPerformed,
                this::btnRAMItemStateChanged,
                ICON_MEMORY,
                "Set operating memory"
        );
        btnDevice = gui.toolbarToggleButton(
                this::btnDeviceActionPerformed,
                this::btnDeviceItemStateChanged,
                ICON_DEVICE,
                "Add device"
        );
        JToolBar.Separator separator2 = new JToolBar.Separator();
        btnLine = gui.toolbarToggleButton(
                this::btnLineActionPerformed,
                this::btnLineItemStateChanged,
                ICON_CONNECTION,
                "Add connection"
        );
        btnBidirection = gui.toolbarToggleButton(
                this::btnBidirectionActionPerformed,
                ICON_BIDIRECTION,
                "Bidirectional connection"
        );
        JToolBar.Separator separator3 = new JToolBar.Separator();
        btnDelete = gui.toolbarToggleButton(
                this::btnDeleteActionPerformed,
                this::btnDeleteItemStateChanged,
                ICON_DELETE,
                "Delete component or connection"
        );
        JToolBar.Separator separator4 = new JToolBar.Separator();
        cmbPlugin = new JComboBox<>();
        cmbPlugin.setEnabled(false);
        cmbPlugin.getAccessibleContext().setAccessibleName("Plugin to place");
        DefaultListCellRenderer pluginRenderer = new DefaultListCellRenderer();
        cmbPlugin.setRenderer((list, value, index, selected, focus) ->
                pluginRenderer.getListCellRendererComponent(list, value == null ? pluginHint : value,
                        index, selected, focus));
        JToolBar.Separator separator5 = new JToolBar.Separator();
        btnUseGrid = gui.toolbarToggleButton(
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

        cmbPlugin.setToolTipText(pluginHint);
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

        for (Component component : toolDraw.getComponents())
            if (component instanceof AbstractButton) component.setFocusable(true);
        toolStatus.setName("schemaToolStatus");
        toolStatus.getAccessibleContext().setAccessibleDescription("Current schema tool and next action");
        JPanel mainPanel = gui.panel("insets dialog", "[grow][]", "[][][grow]");
        mainPanel.add(toolDraw, "growx, span, wrap");
        mainPanel.add(toolStatus, "growx, span, wrap");
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
        pluginLoadId.incrementAndGet();
        panel.setTool(Tool.TOOL_NOTHING, null);
        cmbPlugin.setModel(EMPTY_MODEL);
        cmbPlugin.setEnabled(false);
        if (buttonSelected) {
            resetTool();
            return;
        }
        panel.setTool(Tool.TOOL_CONNECTION, null);
        buttonSelected = true;
        setToolStatus("Connection tool — choose components to connect; Esc cancels.", "Connection tool does not need a plugin");
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
            String type = btnCompiler.isSelected() ? "compiler" : btnCPU.isSelected() ? "CPU"
                    : btnRAM.isSelected() ? "memory" : "device";
            setToolStatus("Place " + type + " — click the canvas; Esc cancels.", "Choose a " + type + " plugin");
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
        pluginLoadId.incrementAndGet();
        panel.setTool(Tool.TOOL_NOTHING, null);
        cmbPlugin.setModel(EMPTY_MODEL);
        cmbPlugin.setEnabled(false);
        if (buttonSelected) {
            resetTool();
        } else {
            panel.setTool(Tool.TOOL_DELETE, null);
            buttonSelected = true;
            setToolStatus("Delete tool — choose a component or connection; Esc cancels.", "Delete tool does not need a plugin");
        }
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        try {
            schema.save();
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not save computer schema", e);
            dialogs.showError("Could not save computer schema. Please consult log file for details.", "Save schema");
            return;
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
        int loadId = pluginLoadId.incrementAndGet();
        String type = pluginType == PLUGIN_TYPE.CPU ? "CPU" : pluginType.name().toLowerCase(java.util.Locale.ROOT);
        cmbPlugin.setModel(EMPTY_MODEL);
        cmbPlugin.setEnabled(false);
        setToolStatus("Loading " + type + " plugins…", "Loading plugins…");

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws IOException {
                return listPluginFiles(pluginType);
            }

            @Override
            protected void done() {
                if (loadId != pluginLoadId.get() || !isDisplayable()) {
                    return;
                }
                try {
                    cmbPlugin.setModel(new PluginComboModel(get()));
                    cmbPlugin.setEnabled(cmbPlugin.getItemCount() > 0);
                    if (cmbPlugin.getItemCount() == 0)
                        setToolStatus("No " + type + " plugins installed. Choose another tool.", "No plugins installed");
                    selectFirstPlugin();
                } catch (Exception e) {
                    LOGGER.error("Could not load plugin files", e);
                    cmbPlugin.setModel(EMPTY_MODEL);
                    cmbPlugin.setEnabled(false);
                    setToolStatus("Could not load " + type + " plugins. Choose the tool again to retry.", "Plugin loading failed");
                }
            }
        }.execute();
    }

    private boolean checkUnsetDrawingTool() {
        if (buttonSelected) {
            resetTool();
            return false;
        }
        return true;
    }

    private void selectFirstPlugin() {
        if (cmbPlugin.getItemCount() > 0) {
            cmbPlugin.setSelectedIndex(0);
        }
    }

    private void setToolStatus(String status, String hint) {
        toolStatus.setText(status);
        pluginHint = hint;
        cmbPlugin.setToolTipText(hint);
        cmbPlugin.repaint();
    }

    private void resetTool() {
        pluginLoadId.incrementAndGet();
        groupDraw.clearSelection();
        buttonSelected = false;
        panel.setTool(Tool.TOOL_NOTHING, null);
        cmbPlugin.setModel(EMPTY_MODEL);
        cmbPlugin.setEnabled(false);
        setToolStatus("Select and move components. Choose a toolbar tool to edit the schema.", "Choose a component tool first");
    }
}
