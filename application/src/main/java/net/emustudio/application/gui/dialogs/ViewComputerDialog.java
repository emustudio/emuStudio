/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.application.gui.framework.EmuStudioUI.*;

public class ViewComputerDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewComputerDialog.class);

    private final VirtualComputer computer;
    private final List<Device> devices;
    private final SchemaPreviewPanel panelSchema;
    private final ButtonGroup pluginButtonGroup = new ButtonGroup();

    private JComboBox<String> cmbDevice;
    private JLabel lblComputerName;
    private JLabel lblCopyright;
    private JLabel lblFileName;
    private JLabel lblName;
    private JLabel lblSelectDevice;
    private JLabel lblVersion;
    private JTextArea txtDescription;

    public ViewComputerDialog(JFrame parent, VirtualComputer computer, AppSettings appSettings, Dialogs dialogs) {
        super(parent, "Computer information preview", true);
        this.computer = Objects.requireNonNull(computer);
        this.devices = computer.getDevices();
        this.panelSchema = new SchemaPreviewPanel(new Schema(computer.getComputerConfig(), appSettings), dialogs);

        buildContent();
        setMinimumSize(new Dimension(600, 450));
        setSize(new Dimension(800, 600));
        lblComputerName.setText(computer.getComputerConfig().getName());
        devices.forEach(device -> cmbDevice.addItem(device.getTitle()));

        // Select default info (CPU)
        lblSelectDevice.setVisible(false);
        cmbDevice.setVisible(false);
        showPluginInfo(computer.getCPU(), computer.getComputerConfig().getCPU());
    }

    private <T extends Plugin> void showPluginInfo(Optional<T> plugin, Optional<PluginConfig> config) {
        if (plugin.isPresent() && config.isPresent()) {
            Plugin p = plugin.get();
            PluginConfig c = config.get();
            lblName.setText(p.getTitle());
            lblVersion.setText(p.getVersion());
            lblFileName.setText(c.getPluginFile());
            lblCopyright.setText(p.getCopyright());
            txtDescription.setText(p.getDescription());
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
        lblComputerName = GUI.labelTitle("computer_name");
        lblComputerName.setHorizontalAlignment(SwingConstants.CENTER);

        lblSelectDevice = GUI.label("Select device:");
        cmbDevice = new JComboBox<>();
        lblName = GUI.labelBold("");
        lblFileName = GUI.label("");
        lblVersion = GUI.label("");
        lblCopyright = GUI.label("");

        txtDescription = GUI.textAreaReadOnly(5, 20);

        cmbDevice.addActionListener(e -> {
            int index = cmbDevice.getSelectedIndex();
            if (index >= 0 && index < devices.size()) {
                try {
                    showPluginInfo(Optional.of(devices.get(index)),
                            Optional.of(computer.getComputerConfig().getDevices().get(index)));
                } catch (Exception ex) {
                    showPluginInfo(Optional.empty(), Optional.empty());
                    LOGGER.error("Could not setup plugin information", ex);
                }
            } else {
                showPluginInfo(Optional.empty(), Optional.empty());
            }
        });

        // Info tab toolbar
        JToolBar infoToolbar = GUI.toolbarVertical();

        JToggleButton btnCompiler = GUI.toggle(ICON_COMPILER, "Compiler information", false,
                computer.getCompiler().isPresent(), () -> {
                    lblSelectDevice.setVisible(false);
                    cmbDevice.setVisible(false);
                    showPluginInfo(computer.getCompiler(), computer.getComputerConfig().getCompiler());
                });
        pluginButtonGroup.add(btnCompiler);
        infoToolbar.add(btnCompiler);

        JToggleButton btnCPU = GUI.toggle(ICON_CPU, "CPU information", true, true, () -> {
            lblSelectDevice.setVisible(false);
            cmbDevice.setVisible(false);
            showPluginInfo(computer.getCPU(), computer.getComputerConfig().getCPU());
        });
        pluginButtonGroup.add(btnCPU);
        infoToolbar.add(btnCPU);

        JToggleButton btnMemory = GUI.toggle(ICON_MEMORY, "Memory information", false,
                computer.getMemory().isPresent(), () -> {
                    lblSelectDevice.setVisible(false);
                    cmbDevice.setVisible(false);
                    showPluginInfo(computer.getMemory(), computer.getComputerConfig().getMemory());
                });
        pluginButtonGroup.add(btnMemory);
        infoToolbar.add(btnMemory);

        JToggleButton btnDevice = GUI.toggle(ICON_DEVICE, "Devices information", false,
                !devices.isEmpty(), () -> {
                    lblSelectDevice.setVisible(true);
                    cmbDevice.setVisible(true);
                    if (cmbDevice.getItemCount() > 0) {
                        cmbDevice.setSelectedIndex(0);
                    } else {
                        cmbDevice.setEnabled(false);
                        showPluginInfo(Optional.empty(), Optional.empty());
                    }
                });
        pluginButtonGroup.add(btnDevice);
        infoToolbar.add(btnDevice);

        JPanel descriptionPanel = GUI.section("Short description", "insets dialog", "[grow]", "[grow]");
        descriptionPanel.add(GUI.scrollable(txtDescription), "grow");

        JPanel infoPanel = GUI.panel("insets dialog", "[grow]", "[][][][][][grow]");
        infoPanel.add(lblSelectDevice, "split 2");
        infoPanel.add(cmbDevice, "grow, wrap");
        infoPanel.add(lblName, "wrap");
        infoPanel.add(lblFileName, "wrap");
        infoPanel.add(lblVersion, "wrap");
        infoPanel.add(lblCopyright, "wrap");
        infoPanel.add(descriptionPanel, "grow");

        JPanel infoTab = GUI.panel("insets dialog", "[][grow]", "[grow]");
        infoTab.add(infoToolbar, "grow");
        infoTab.add(infoPanel, "grow");

        // Schema tab
        JToolBar schemaToolbar = GUI.toolbarVertical();

        JButton btnSave = GUI.buttonIcon(ICON_SAVE, "Save schema image", e -> panelSchema.saveSchemaImage());
        schemaToolbar.add(btnSave);

        JScrollPane scrollPane = GUI.scrollable(panelSchema);

        JPanel schemaTab = GUI.panel("insets dialog", "[][grow]", "[grow]");
        schemaTab.add(schemaToolbar, "grow");
        schemaTab.add(scrollPane, "grow");

        // Main panel
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Computer info", infoTab);
        tabbedPane.addTab("Abstract schema", schemaTab);

        JPanel mainPanel = GUI.panel("insets dialog", "[grow]", "[][grow]");
        mainPanel.add(lblComputerName, "growx, wrap");
        mainPanel.add(tabbedPane, "grow");
        return mainPanel;
    }
}
