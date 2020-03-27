/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.application.internal.Unchecked;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.Dialogs.DialogAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This dialog manages the virtual computers. It offers a list of all
 * available virtual computers and allows to the user to select one for
 * emulation.
 * <p>
 * It is also available to create a new computer, delete or edit one.
 */
public class OpenComputerDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenComputerDialog.class);

    private ComputerConfig selectedComputerConfig;
    private boolean userPressedOK = false;
    private final ConfigurationsListModel configurationsModel;
    private final SchemaPreviewPanel preview;
    private final ConfigFiles configFiles;
    private final ApplicationConfig applicationConfig;
    private final Dialogs dialogs;

    private JLabel lblPreview;
    private JList<ComputerConfig> lstConfig;
    private JScrollPane scrollPreview;

    public OpenComputerDialog(ConfigFiles configFiles, ApplicationConfig applicationConfig, Dialogs dialogs) {
        this.configFiles = Objects.requireNonNull(configFiles);
        this.configurationsModel = new ConfigurationsListModel(configFiles);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.preview = new SchemaPreviewPanel(null, dialogs);

        initComponents();
        lstConfig.setModel(configurationsModel);
        super.setModal(true);
        super.setLocationRelativeTo(null);

        scrollPreview.setViewportView(preview);
    }

    public boolean userPressedOK() {
        return userPressedOK;
    }

    public ComputerConfig getSelectedComputerConfig() {
        return selectedComputerConfig;
    }


    void update() {
        configurationsModel.update();
        Optional.ofNullable(selectedComputerConfig).ifPresentOrElse(
            c -> lblPreview.setText(c.getName()),
            () -> lblPreview.setText("")
        );
        lstConfigValueChanged(null);
    }

    /**
     * Set the name of selected virtual computer.
     *
     * @param selectedComputerConfig new name of the virtual computer
     */
    void setSelectedComputerConfig(ComputerConfig selectedComputerConfig) {
        this.selectedComputerConfig = selectedComputerConfig;
    }
    
    private void initComponents() {
        JSplitPane splitConfig = new JSplitPane();
        JPanel panelConfig = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        lstConfig = new JList<>();
        JToolBar toolConfig = new JToolBar();
        JButton btnAdd = new JButton();
        JButton btnDelete = new JButton();
        JButton btnEdit = new JButton();
        JButton btnSaveSchemaImage = new JButton();
        JPanel panelPreview = new JPanel();
        scrollPreview = new JScrollPane();
        JToolBar toolPreview = new JToolBar();
        JLabel jLabel2 = new JLabel();
        JToolBar.Separator jSeparator1 = new JToolBar.Separator();
        lblPreview = new JLabel();
        JLabel jLabel1 = new JLabel();
        JButton btnOpen = new JButton();
        JButton btnClose = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("emuStudio - Open virtual computer");

        splitConfig.setDividerLocation(200);
        splitConfig.setMinimumSize(new java.awt.Dimension(50, 102));
        splitConfig.setPreferredSize(new java.awt.Dimension(300, 299));

        panelConfig.setPreferredSize(new java.awt.Dimension(200, 297));

        lstConfig.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstConfigMouseClicked(evt);
            }
        });
        lstConfig.addListSelectionListener(this::lstConfigValueChanged);
        jScrollPane1.setViewportView(lstConfig);

        toolConfig.setFloatable(false);
        toolConfig.setRollover(true);

        btnAdd.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/application/gui/dialogs/list-add.png")));
        btnAdd.setToolTipText("Create new computer...");
        btnAdd.setFocusable(false);
        btnAdd.setHorizontalTextPosition(SwingConstants.CENTER);
        btnAdd.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnAdd.addActionListener(this::btnAddActionPerformed);
        btnAdd.setBorderPainted(false);
        toolConfig.add(btnAdd);

        btnDelete.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/application/gui/dialogs/list-remove.png")));
        btnDelete.setToolTipText("Remove computer");
        btnDelete.setFocusable(false);
        btnDelete.setHorizontalTextPosition(SwingConstants.CENTER);
        btnDelete.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnDelete.addActionListener(this::btnDeleteActionPerformed);
        btnDelete.setBorderPainted(false);
        toolConfig.add(btnDelete);

        btnEdit.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/application/gui/dialogs/computer.png")));
        btnEdit.setToolTipText("Edit existing computer...");
        btnEdit.setFocusable(false);
        btnEdit.setHorizontalTextPosition(SwingConstants.CENTER);
        btnEdit.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnEdit.addActionListener(this::btnEditActionPerformed);
        btnEdit.setBorderPainted(false);
        toolConfig.add(btnEdit);

        btnSaveSchemaImage.setIcon(new ImageIcon(getClass().getResource("/net/emustudio/application/gui/dialogs/document-save.png")));
        btnSaveSchemaImage.setToolTipText("Save schema image");
        btnSaveSchemaImage.setFocusable(false);
        btnSaveSchemaImage.setHorizontalTextPosition(SwingConstants.CENTER);
        btnSaveSchemaImage.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnSaveSchemaImage.addActionListener(this::btnSaveSchemaImageActionPerformed);
        btnSaveSchemaImage.setBorderPainted(false);
        toolConfig.add(btnSaveSchemaImage);

        GroupLayout panelConfigLayout = new GroupLayout(panelConfig);
        panelConfig.setLayout(panelConfigLayout);
        panelConfigLayout.setHorizontalGroup(
            panelConfigLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(toolConfig, GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        panelConfigLayout.setVerticalGroup(
            panelConfigLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelConfigLayout.createSequentialGroup()
                    .addComponent(toolConfig, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE))
        );

        splitConfig.setLeftComponent(panelConfig);

        toolPreview.setFloatable(false);
        toolPreview.setRollover(true);

        jLabel2.setText("Computer preview:");
        toolPreview.add(jLabel2);

        jSeparator1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        toolPreview.add(jSeparator1);

        lblPreview.setFont(lblPreview.getFont().deriveFont(lblPreview.getFont().getStyle() | java.awt.Font.BOLD));
        toolPreview.add(lblPreview);

        GroupLayout panelPreviewLayout = new GroupLayout(panelPreview);
        panelPreview.setLayout(panelPreviewLayout);
        panelPreviewLayout.setHorizontalGroup(
            panelPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPreview, GroupLayout.DEFAULT_SIZE, 586, Short.MAX_VALUE)
                .addGroup(panelPreviewLayout.createSequentialGroup()
                    .addGap(10, 10, 10)
                    .addComponent(toolPreview, GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE))
        );
        panelPreviewLayout.setVerticalGroup(
            panelPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelPreviewLayout.createSequentialGroup()
                    .addComponent(toolPreview, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(scrollPreview, GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE))
        );

        splitConfig.setRightComponent(panelPreview);

        jLabel1.setText("Please select a virtual configuration that will be emulated:");

        btnOpen.setFont(btnOpen.getFont().deriveFont(btnOpen.getFont().getStyle() | java.awt.Font.BOLD));
        btnOpen.setText("Open");
        btnOpen.addActionListener(this::btnOpenActionPerformed);

        btnClose.setText("Close");
        btnClose.addActionListener(this::btnCloseActionPerformed);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(splitConfig, GroupLayout.DEFAULT_SIZE, 797, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(btnClose)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnOpen, GroupLayout.PREFERRED_SIZE, 81, GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLabel1)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(splitConfig, GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(btnOpen)
                        .addComponent(btnClose))
                    .addContainerGap())
        );

        pack();
    }

    private void lstConfigMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            btnOpenActionPerformed(null);
        }
    }

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {
        selectedComputerConfig = lstConfig.getSelectedValue();
        if (selectedComputerConfig == null) {
            dialogs.showError("A computer has to be selected!", "Open computer");
        } else {
            userPressedOK = true;
            dispose();
        }
    }

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {
        selectedComputerConfig = lstConfig.getSelectedValue();
        if (selectedComputerConfig == null) {
            dialogs.showError("A computer has to be selected!", "Edit computer");
        } else {
            Schema schema = new Schema(selectedComputerConfig, applicationConfig);
            SchemaEditorDialog d = new SchemaEditorDialog(this, schema, configFiles, dialogs);
            d.setVisible(true);
        }
    }

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {
        selectedComputerConfig = lstConfig.getSelectedValue();
        if (selectedComputerConfig == null) {
            dialogs.showError("A computer has to be selected!", "Delete computer");
        } else {
            DialogAnswer answer = dialogs.ask("Do you really want to delete selected computer?", "Delete computer");
            if (answer == DialogAnswer.ANSWER_YES) {
                try {
                    configFiles.removeConfiguration(selectedComputerConfig.getName());
                    lstConfig.clearSelection();
                    selectedComputerConfig = null;
                    update();
                } catch (IOException e) {
                    LOGGER.error("Could not remove computer configuration", e);
                    dialogs.showError("Computer could not be deleted. Please consult log for details.");
                }
            }
        }
    }

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {
        Optional<String> computerName = dialogs.readString("Enter computer name:", "Create new computer");
        computerName.ifPresentOrElse(name -> {
            try {
                configFiles
                    .loadConfiguration(name)
                    .ifPresentOrElse(
                        c -> dialogs.showError("Computer '" + name + "' already exists, choose another name."),
                        () -> {
                            ComputerConfig newComputer = Unchecked.call(() -> configFiles.createConfiguration(name));
                            Schema schema = new Schema(newComputer, applicationConfig);
                            SchemaEditorDialog di = new SchemaEditorDialog(this, schema, configFiles, dialogs);
                            di.setVisible(true);
                        }
                    );
            } catch (IOException e) {
                LOGGER.error("Could not load computer with name '" + name + "'", e);
                dialogs.showError("Could not load computer with name '" + name + "'. Please see log file for details.");
            }
        }, () -> dialogs.showError("Computer name must be non-empty"));
    }

    private void lstConfigValueChanged(ListSelectionEvent evt) {
        selectedComputerConfig = lstConfig.getSelectedValue();
        if (selectedComputerConfig == null) {
            preview.setSchema(null);
        } else {
            Schema schema = new Schema(selectedComputerConfig, applicationConfig);
            preview.setSchema(schema);
            lblPreview.setText(selectedComputerConfig.getName());
        }
        preview.repaint();
    }

    private void btnSaveSchemaImageActionPerformed(java.awt.event.ActionEvent evt) {
        preview.saveSchemaImage();
    }

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }


    private class ConfigurationsListModel extends AbstractListModel<ComputerConfig> {
        private List<ComputerConfig> computerConfigs = Collections.emptyList();

        ConfigurationsListModel(ConfigFiles configFiles) {
            try {
                computerConfigs = configFiles.loadConfigurations();
            } catch (IOException e) {
                LOGGER.error("Could not load computer configurations", e);
                dialogs.showError("Could not load computer configurations. Please consult log file for details.");
            }
        }

        @Override
        public ComputerConfig getElementAt(int index) {
            return computerConfigs.get(index);
        }

        @Override
        public int getSize() {
            return computerConfigs.size();
        }

        void update() {
            try {
                computerConfigs = configFiles.loadConfigurations();
                this.fireContentsChanged(this, -1, -1);
            } catch (IOException e) {
                LOGGER.error("Could not load computer configurations", e);
            }
        }
    }
}
