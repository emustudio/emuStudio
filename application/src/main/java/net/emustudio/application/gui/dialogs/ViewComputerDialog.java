/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.framework.EDialog;
import net.emustudio.application.gui.framework.EPanel;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class ViewComputerDialog extends EDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(ViewComputerDialog.class);
    private final static String ICON_COMPILER = "/net/emustudio/application/gui/dialogs/compile.png";
    private final static String ICON_CPU = "/net/emustudio/application/gui/dialogs/cpu.gif";
    private final static String ICON_MEMORY = "/net/emustudio/application/gui/dialogs/ram.gif";
    private final static String ICON_DEVICE = "/net/emustudio/application/gui/dialogs/device.png";
    private final static String ICON_SAVE = "/net/emustudio/application/gui/dialogs/document-save.png";

    private final VirtualComputer computer;
    private final SchemaPreviewPanel panelSchema;
    private JToggleButton btnCompiler;
    private JToggleButton btnDevice;
    private JToggleButton btnMemory;
    private JComboBox<String> cmbDevice;
    private JLabel lblComputerName;
    private JLabel lblCopyright;
    private JLabel lblFileName;
    private JLabel lblName;
    private JLabel lblSelectDevice;
    private JLabel lblVersion;
    private JScrollPane scrollPane;
    private JTextArea txtDescription;

    public ViewComputerDialog(JFrame parent, VirtualComputer computer, AppSettings appSettings, Dialogs dialogs) {
        super(parent, "Computer information preview", true);
        this.computer = Objects.requireNonNull(computer);

        buildContent();

        lblComputerName.setText(computer.getComputerConfig().getName());

        final List<Device> devices = computer.getDevices();
        for (Device device : devices) {
            cmbDevice.addItem(device.getTitle());
        }

        cmbDevice.addActionListener(e -> {
            int i = cmbDevice.getSelectedIndex();
            if (i < 0) {
                setVisibleInfo(false);
            } else {
                try {
                    setInfo(devices.get(i), computer.getComputerConfig().getDevices().get(i));
                    setVisibleInfo(true);
                } catch (Exception ex) {
                    setVisibleInfo(false);
                    LOGGER.error("Could not setup plugin information", ex);
                }
            }
        });

        panelSchema = new SchemaPreviewPanel(new Schema(computer.getComputerConfig(), appSettings), dialogs);
        scrollPane.setViewportView(panelSchema);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);

        if (computer.getCompiler().isEmpty()) {
            btnCompiler.setEnabled(false);
        }
        if (computer.getMemory().isEmpty()) {
            btnMemory.setEnabled(false);
        }
        if (computer.getDevices().isEmpty()) {
            btnDevice.setEnabled(false);
        }

        // Select default info
        lblSelectDevice.setVisible(false);
        cmbDevice.setVisible(false);
        computer.getComputerConfig().getCPU().ifPresent(
                conf -> computer.getCPU().ifPresent(cpu -> setInfo(cpu, conf))
        );
    }

    private void setInfo(Plugin plugin, PluginConfig config) {
        lblName.setText(plugin.getTitle());
        lblVersion.setText(plugin.getVersion());
        lblFileName.setText(config.getPluginFile());
        lblCopyright.setText(plugin.getCopyright());
        txtDescription.setText(plugin.getDescription());
    }

    private void setVisibleInfo(boolean visible) {
        if (!visible) {
            lblName.setText("Plug-in is not available. Please select another one.");
        }
        lblCopyright.setVisible(visible);
        lblVersion.setVisible(visible);
        lblFileName.setVisible(visible);
        txtDescription.setVisible(visible);
    }

    @Override
    protected JComponent initializeComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        lblComputerName = new JLabel();
        JTabbedPane jTabbedPane1 = new JTabbedPane();
        JToolBar jToolBar1 = new JToolBar();
        btnCompiler = new JToggleButton(loadIcon(ICON_COMPILER));
        JToggleButton btnCPU = new JToggleButton(loadIcon(ICON_CPU));
        btnMemory = new JToggleButton(loadIcon(ICON_MEMORY));
        btnDevice = new JToggleButton(loadIcon(ICON_DEVICE));
        lblSelectDevice = new JLabel();
        cmbDevice = new JComboBox<>();
        lblName = new JLabel();
        lblFileName = new JLabel();
        lblVersion = new JLabel();
        lblCopyright = new JLabel();
        JPanel panelDescription = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        txtDescription = new JTextArea();
        JToolBar jToolBar2 = new JToolBar();
        JButton btnSaveSchema = new JButton(loadIcon(ICON_SAVE));
        scrollPane = new JScrollPane();

        lblComputerName.setFont(lblComputerName.getFont().deriveFont(lblComputerName.getFont().getStyle() | java.awt.Font.BOLD, lblComputerName.getFont().getSize() + 3));
        lblComputerName.setHorizontalAlignment(SwingConstants.CENTER);
        lblComputerName.setText("computer_name");

        jToolBar1.setFloatable(false);
        jToolBar1.setOrientation(SwingConstants.VERTICAL);
        jToolBar1.setRollover(true);
        jToolBar1.setDoubleBuffered(true);

        buttonGroup1.add(btnCompiler);
        btnCompiler.setToolTipText("Compiler information");
        btnCompiler.setFocusable(false);
        btnCompiler.addActionListener(this::btnCompilerActionPerformed);
        jToolBar1.add(btnCompiler);

        buttonGroup1.add(btnCPU);
        btnCPU.setSelected(true);
        btnCPU.setToolTipText("CPU information");
        btnCPU.setFocusable(false);
        btnCPU.addActionListener(this::btnCPUActionPerformed);
        jToolBar1.add(btnCPU);

        buttonGroup1.add(btnMemory);
        btnMemory.setToolTipText("Memory information");
        btnMemory.setFocusable(false);
        btnMemory.addActionListener(this::btnMemoryActionPerformed);
        jToolBar1.add(btnMemory);

        buttonGroup1.add(btnDevice);
        btnDevice.setToolTipText("Devices information");
        btnDevice.setFocusable(false);
        btnDevice.addActionListener(this::btnDeviceActionPerformed);
        jToolBar1.add(btnDevice);

        lblSelectDevice.setText("Select device:");

        lblName.setFont(lblName.getFont().deriveFont(lblName.getFont().getStyle() | java.awt.Font.BOLD));
        lblName.setText("plugin_name");

        lblFileName.setText("plugin_file_name");
        lblVersion.setText("plugin_version");
        lblCopyright.setText("plugin_copyright");

        panelDescription.setBorder(BorderFactory.createTitledBorder("Short description"));

        txtDescription.setColumns(20);
        txtDescription.setEditable(false);
        txtDescription.setLineWrap(true);
        txtDescription.setRows(5);
        txtDescription.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtDescription);

        EPanel descriptionPanel = new EPanel("insets dialog", "[grow]", "[grow]");
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("Short description"));
        descriptionPanel.add(jScrollPane1, "grow");

        EPanel infoPanel = new EPanel("insets dialog", "[grow]", "[][][][][][][][grow]");
        infoPanel.add(lblSelectDevice, "split 2");
        infoPanel.add(cmbDevice, "grow, wrap");
        infoPanel.add(lblName, "wrap");
        infoPanel.add(lblFileName, "wrap");
        infoPanel.add(lblVersion, "wrap");
        infoPanel.add(lblCopyright, "wrap");
        infoPanel.add(descriptionPanel, "grow");

        EPanel tabInfoPanel = new EPanel("insets dialog", "[][grow]", "[grow]");
        tabInfoPanel.add(jToolBar1, "grow");
        tabInfoPanel.add(infoPanel, "grow");

        jTabbedPane1.addTab("Computer info", tabInfoPanel);

        jToolBar2.setFloatable(false);
        jToolBar2.setOrientation(SwingConstants.VERTICAL);
        jToolBar2.setRollover(true);

        btnSaveSchema.setToolTipText("Save schema image");
        btnSaveSchema.setFocusable(false);
        btnSaveSchema.setHorizontalTextPosition(SwingConstants.CENTER);
        btnSaveSchema.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnSaveSchema.addActionListener(this::btnSaveSchemaActionPerformed);
        jToolBar2.add(btnSaveSchema);

        EPanel schemaPanel = new EPanel("insets dialog", "[][grow]", "[grow]");
        schemaPanel.add(jToolBar2, "grow");
        schemaPanel.add(scrollPane, "grow");

        jTabbedPane1.addTab("Abstract schema", schemaPanel);

        EPanel mainPanel = new EPanel("insets dialog", "[grow]", "[][grow]");
        mainPanel.add(lblComputerName, "growx, wrap");
        mainPanel.add(jTabbedPane1, "grow");

        return mainPanel;
    }

    private void btnCompilerActionPerformed(java.awt.event.ActionEvent evt) {
        lblSelectDevice.setVisible(false);
        cmbDevice.setVisible(false);
        computer.getComputerConfig().getCompiler().ifPresent(
                conf -> computer.getCompiler().ifPresent(compiler -> setInfo(compiler, conf))
        );
    }

    private void btnCPUActionPerformed(java.awt.event.ActionEvent evt) {
        lblSelectDevice.setVisible(false);
        cmbDevice.setVisible(false);
        computer.getComputerConfig().getCPU().ifPresent(
                conf -> computer.getCPU().ifPresent(cpu -> setInfo(cpu, conf))
        );
    }

    private void btnMemoryActionPerformed(java.awt.event.ActionEvent evt) {
        lblSelectDevice.setVisible(false);
        cmbDevice.setVisible(false);
        computer.getComputerConfig().getMemory().ifPresent(
                conf -> computer.getMemory().ifPresent(memory -> setInfo(memory, conf))
        );
    }

    private void btnDeviceActionPerformed(java.awt.event.ActionEvent evt) {
        lblSelectDevice.setVisible(true);
        cmbDevice.setVisible(true);
        setVisibleInfo(false);
        if (cmbDevice.getItemCount() > 0) {
            cmbDevice.setSelectedIndex(0);
            PluginConfig conf = computer.getComputerConfig().getDevices().get(0);
            Device device = computer.getDevices().get(0);
            setInfo(device, conf);
            setVisibleInfo(true);
        } else {
            cmbDevice.setEnabled(false);
        }
    }

    private void btnSaveSchemaActionPerformed(java.awt.event.ActionEvent evt) {
        panelSchema.saveSchemaImage();
    }
}
