/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.Plugin;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.emulib.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.ui.StandardIcons.*;

public class ViewComputerDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewComputerDialog.class);

    private final VirtualComputer computer;
    private final List<Device> devices;
    private final SchemaPreviewPanel panelSchema;
    private final ButtonGroup pluginButtonGroup = new ButtonGroup();
    private final EmuStudioGui gui;

    private JComboBox<String> cmbDevice;
    private JLabel lblComputerName;
    private JLabel lblCategory;
    private JLabel lblCopyright;
    private JLabel lblFileName;
    private JLabel lblName;
    private JLabel lblSelectDevice;
    private JLabel lblVersion;
    private JTextArea txtDescription;
    private JPanel descriptionPanel, metadataPanel;

    public ViewComputerDialog(JFrame parent, VirtualComputer computer, AppSettings appSettings, Dialogs dialogs, EmuStudioGui gui) {
        super(parent, "Computer information preview", true);
        this.computer = Objects.requireNonNull(computer);
        this.devices = computer.getDevices();
        this.gui = Objects.requireNonNull(gui);
        this.panelSchema = new SchemaPreviewPanel(new Schema(computer.getComputerConfig(), appSettings), dialogs);

        buildContent();
        setMinimumSize(new Dimension(600, 450));
        setSize(new Dimension(800, 600));
        lblComputerName.setText(computer.getComputerConfig().getName());
        devices.forEach(device -> cmbDevice.addItem(device.getTitle()));

        // Select default info (CPU)
        lblSelectDevice.setVisible(false);
        cmbDevice.setVisible(false);
        showOptionalPluginInfo(computer.getCPU(), computer.getComputerConfig().getCPU());
    }

    private void showOptionalPluginInfo(Optional<? extends Plugin> plugin, Optional<PluginConfig> config) {
        showPluginInfo(plugin.orElse(null), config.orElse(null));
    }

    private void showPluginInfo(Plugin plugin, PluginConfig config) {
        boolean available = plugin != null && config != null;
        descriptionPanel.setVisible(available);
        metadataPanel.setVisible(available);
        if (plugin != null && config != null) {
            lblName.setText(plugin.getTitle());
            lblVersion.setText(plugin.getVersion());
            lblFileName.setText(config.getPluginFile());
            lblCopyright.setText(plugin.getCopyright());
            txtDescription.setText(plugin.getDescription());
            txtDescription.setCaretPosition(0);
            lblCopyright.setVisible(true);
            lblVersion.setVisible(true);
            lblFileName.setVisible(true);
            txtDescription.setVisible(true);
        } else {
            lblName.setText("Plug-in is not available. Please select another one.");
            lblCopyright.setVisible(false);
            lblVersion.setVisible(false);
            lblFileName.setVisible(false);
            txtDescription.setVisible(false);
        }
    }

    @Override
    protected JComponent initializeComponents() {
        lblComputerName = gui.labelTitle("computer_name");
        lblComputerName.setHorizontalAlignment(SwingConstants.CENTER);

        lblSelectDevice = gui.label("Select device:");
        cmbDevice = new JComboBox<>();
        lblSelectDevice.setLabelFor(cmbDevice);
        lblName = gui.labelBold("");
        lblFileName = gui.label("");
        lblVersion = gui.label("");
        lblCopyright = gui.label("");

        txtDescription = gui.textAreaReadOnly(0, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setFont(UIManager.getFont("Label.font"));
        txtDescription.getAccessibleContext().setAccessibleName("Plugin description");
        lblCategory = gui.labelBold("CPU information");

        cmbDevice.addActionListener(e -> {
            int index = cmbDevice.getSelectedIndex();
            if (index >= 0 && index < devices.size()) {
                try {
                    showPluginInfo(devices.get(index),
                            computer.getComputerConfig().getDevices().get(index));
                } catch (Exception ex) {
                    showPluginInfo(null, null);
                    LOGGER.error("Could not setup plugin information", ex);
                }
            } else {
                showPluginInfo(null, null);
            }
        });

        // Info tab toolbar
        JToolBar infoToolbar = gui.toolBar();
        infoToolbar.setOrientation(SwingConstants.VERTICAL);

        JToggleButton btnCompiler = gui.toolbarToggleButton(e -> {
            lblCategory.setText("Compiler information");
            lblSelectDevice.setVisible(false);
            cmbDevice.setVisible(false);
            showOptionalPluginInfo(computer.getCompiler(), computer.getComputerConfig().getCompiler());
        }, ICON_COMPILER, "Compiler information");
        btnCompiler.setEnabled(computer.getCompiler().isPresent());
        btnCompiler.setSelected(false);

        pluginButtonGroup.add(btnCompiler);
        infoToolbar.add(btnCompiler);

        JToggleButton btnCPU = gui.toolbarToggleButton(e -> {
            lblCategory.setText("CPU information");
            lblSelectDevice.setVisible(false);
            cmbDevice.setVisible(false);
            showOptionalPluginInfo(computer.getCPU(), computer.getComputerConfig().getCPU());
        }, ICON_CPU, "CPU information");
        btnCPU.setEnabled(computer.getCPU().isPresent());
        btnCPU.setSelected(true);

        pluginButtonGroup.add(btnCPU);
        infoToolbar.add(btnCPU);

        JToggleButton btnMemory = gui.toolbarToggleButton(e -> {
                    lblCategory.setText("Memory information");
                    lblSelectDevice.setVisible(false);
                    cmbDevice.setVisible(false);
                    showOptionalPluginInfo(computer.getMemory(), computer.getComputerConfig().getMemory());
                }, ICON_MEMORY, "Memory information");
        btnMemory.setEnabled(computer.getMemory().isPresent());
        btnMemory.setSelected(false);

        pluginButtonGroup.add(btnMemory);
        infoToolbar.add(btnMemory);

        JToggleButton btnDevice = gui.toolbarToggleButton(e -> {
                    lblCategory.setText("Device information");
                    lblSelectDevice.setVisible(true);
                    cmbDevice.setVisible(true);
                    if (cmbDevice.getItemCount() > 0) {
                        cmbDevice.setSelectedIndex(0);
                    } else {
                        cmbDevice.setEnabled(false);
                        showPluginInfo(null, null);
                    }
                }, ICON_DEVICE, "Devices information");
        btnDevice.setEnabled(!devices.isEmpty());
        btnDevice.setSelected(false);

        pluginButtonGroup.add(btnDevice);
        infoToolbar.add(btnDevice);

        for (java.awt.Component component : infoToolbar.getComponents()) component.setFocusable(true);
        descriptionPanel = gui.section("Description", "insets dialog", "[grow]", "[grow]");
        descriptionPanel.add(gui.scrollPane(txtDescription), "growx, h 20:pref:240");

        JPanel infoPanel = gui.panel("insets dialog, hidemode 3", "[grow]", "[][][][][][grow]");
        infoPanel.add(lblCategory, "wrap, gapbottom 6");
        infoPanel.add(lblSelectDevice, "split 2");
        infoPanel.add(cmbDevice, "grow, wrap");
        infoPanel.add(lblName, "wrap");
        infoPanel.add(descriptionPanel, "growx, wrap");
        metadataPanel = gui.panel("insets 0, hidemode 3", "[][grow]", "[][][]");
        metadataPanel.add(gui.label("Version"));
        metadataPanel.add(lblVersion, "growx, wrap");
        metadataPanel.add(gui.label("Plugin file"));
        metadataPanel.add(lblFileName, "growx, wrap");
        metadataPanel.add(lblCopyright, "span, growx");
        infoPanel.add(metadataPanel, "growx, wrap");

        JPanel infoTab = gui.panel("insets dialog", "[][grow]", "[grow]");
        infoTab.add(infoToolbar, "grow");
        infoTab.add(infoPanel, "grow");

        // Schema tab
        JToolBar schemaToolbar = gui.toolBar();
        schemaToolbar.setOrientation(SwingConstants.VERTICAL);

        JButton btnSave = gui.toolbarButton(event -> panelSchema.saveSchemaImage(), ICON_SAVE, "Save schema image");
        btnSave.setFocusable(true);
        schemaToolbar.add(btnSave);

        JScrollPane scrollPane = gui.scrollPane(panelSchema);

        JPanel schemaTab = gui.panel("insets dialog", "[][grow]", "[grow]");
        schemaTab.add(schemaToolbar, "grow");
        schemaTab.add(scrollPane, "grow");

        // Main panel
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Computer info", infoTab);
        tabbedPane.addTab("Abstract schema", schemaTab);

        JPanel mainPanel = gui.panel("insets dialog", "[grow]", "[][grow]");
        mainPanel.add(lblComputerName, "growx, wrap");
        mainPanel.add(tabbedPane, "grow");
        return mainPanel;
    }
}
