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
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

public class ViewComputerDialog extends JDialog {
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
        super(parent, true);
        this.computer = Objects.requireNonNull(computer);

        initComponents();
        setLocationRelativeTo(parent);

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

    private void initComponents() {
        ButtonGroup buttonGroup1 = new ButtonGroup();
        lblComputerName = new JLabel();
        JTabbedPane jTabbedPane1 = new JTabbedPane();
        JPanel panelTabInfo = new JPanel();
        JToolBar jToolBar1 = new JToolBar();
        btnCompiler = new JToggleButton(loadIcon(ICON_COMPILER));
        JToggleButton btnCPU = new JToggleButton(loadIcon(ICON_CPU));
        btnMemory = new JToggleButton(loadIcon(ICON_MEMORY));
        btnDevice = new JToggleButton(loadIcon(ICON_DEVICE));
        JPanel jPanel2 = new JPanel();
        lblSelectDevice = new JLabel();
        cmbDevice = new JComboBox<>();
        lblName = new JLabel();
        lblFileName = new JLabel();
        lblVersion = new JLabel();
        lblCopyright = new JLabel();
        JPanel panelDescription = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        txtDescription = new JTextArea();
        JPanel jPanel1 = new JPanel();
        JToolBar jToolBar2 = new JToolBar();
        JButton btnSaveSchema = new JButton(loadIcon(ICON_SAVE));
        scrollPane = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("Computer information preview");

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

        GroupLayout panelDescriptionLayout = new GroupLayout(panelDescription);
        panelDescription.setLayout(panelDescriptionLayout);
        panelDescriptionLayout.setHorizontalGroup(
                panelDescriptionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelDescriptionLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1)
                                .addContainerGap())
        );
        panelDescriptionLayout.setVerticalGroup(
                panelDescriptionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelDescriptionLayout.createSequentialGroup()
                                .addComponent(jScrollPane1)
                                .addContainerGap())
        );

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(panelDescription, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(lblSelectDevice)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cmbDevice, 0, 377, Short.MAX_VALUE))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblName)
                                                        .addComponent(lblVersion)
                                                        .addComponent(lblCopyright)
                                                        .addComponent(lblFileName))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblSelectDevice)
                                        .addComponent(cmbDevice, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(lblName)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblFileName)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lblVersion)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblCopyright)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(panelDescription, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout panelTabInfoLayout = new GroupLayout(panelTabInfo);
        panelTabInfo.setLayout(panelTabInfoLayout);
        panelTabInfoLayout.setHorizontalGroup(
                panelTabInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTabInfoLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jToolBar1, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        panelTabInfoLayout.setVerticalGroup(
                panelTabInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTabInfoLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelTabInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jToolBar1, GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
                                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        jTabbedPane1.addTab("Computer info", panelTabInfo);

        jToolBar2.setFloatable(false);
        jToolBar2.setOrientation(SwingConstants.VERTICAL);
        jToolBar2.setRollover(true);

        btnSaveSchema.setToolTipText("Save schema image");
        btnSaveSchema.setFocusable(false);
        btnSaveSchema.setHorizontalTextPosition(SwingConstants.CENTER);
        btnSaveSchema.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnSaveSchema.addActionListener(this::btnSaveSchemaActionPerformed);
        jToolBar2.add(btnSaveSchema);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jToolBar2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 505, Short.MAX_VALUE)
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(scrollPane)
                                        .addComponent(jToolBar2, GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE))
                                .addContainerGap())
        );

        jTabbedPane1.addTab("Abstract schema", jPanel1);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jTabbedPane1)
                                        .addComponent(lblComputerName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(lblComputerName)
                                .addGap(18, 18, 18)
                                .addComponent(jTabbedPane1)
                                .addContainerGap())
        );

        pack();
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
