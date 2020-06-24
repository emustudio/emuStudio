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
import net.emustudio.application.gui.ToolbarButton;
import net.emustudio.application.gui.actions.opencomputer.*;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class OpenComputerDialog extends JDialog {
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenComputerDialog.class);

    private final ConfigurationsListModel configurationsModel;
    private final SchemaPreviewPanel preview;
    private final ConfigFiles configFiles;
    private final ApplicationConfig applicationConfig;
    private final Dialogs dialogs;

    private final AddNewComputerAction addNewComputerAction;
    private final DeleteComputerAction deleteComputerAction;
    private final EditComputerAction editComputerAction;
    private final OpenComputerAction openComputerAction;
    private final RenameComputerAction renameComputerAction;
    private final SaveSchemaAction saveSchemaAction;

    private final JList<ComputerConfig> lstConfig = new JList<>();

    public OpenComputerDialog(ConfigFiles configFiles, ApplicationConfig applicationConfig, Dialogs dialogs,
                              Consumer<ComputerConfig> selectComputer) {
        this.configFiles = Objects.requireNonNull(configFiles);
        this.configurationsModel = new ConfigurationsListModel(configFiles);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.preview = new SchemaPreviewPanel(null, dialogs);

        addNewComputerAction = new AddNewComputerAction(dialogs, configFiles, applicationConfig, this::update, this);
        deleteComputerAction = new DeleteComputerAction(dialogs, configFiles, this::update, lstConfig);
        editComputerAction = new EditComputerAction(dialogs, configFiles, applicationConfig, this::update, this, lstConfig);
        openComputerAction = new OpenComputerAction(dialogs, this, lstConfig, selectComputer);
        renameComputerAction = new RenameComputerAction(dialogs, configFiles, this::update, lstConfig);
        saveSchemaAction = new SaveSchemaAction(preview);

        setModal(true);
        initComponents();
        setLocationRelativeTo(null);

        lstConfig.setModel(configurationsModel);
    }

    void update() {
        configurationsModel.update();
        lstConfig.clearSelection();
        lstConfigValueChanged(null);
    }

    private void initComponents() {
        JSplitPane splitConfig = new JSplitPane();
        JPanel panelConfig = new JPanel();
        JScrollPane configScrollPane = new JScrollPane();
        JToolBar toolConfig = new JToolBar();
        ToolbarButton btnAdd = new ToolbarButton(addNewComputerAction, "Create new computer...");
        ToolbarButton btnDelete = new ToolbarButton(deleteComputerAction, "Delete computer");
        ToolbarButton btnEdit = new ToolbarButton(editComputerAction, "Edit computer...");
        ToolbarButton btnRename = new ToolbarButton(renameComputerAction, "Rename computer...");
        ToolbarButton btnSaveSchemaImage = new ToolbarButton(saveSchemaAction, "Save schema image...");
        JPanel panelPreview = new JPanel();
        JScrollPane scrollPreview = new JScrollPane();
        JLabel jLabel1 = new JLabel();
        JButton btnOpen = new JButton();
        JButton btnClose = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setTitle("emuStudio - Open virtual computer");

        splitConfig.setDividerLocation(300);
        splitConfig.setMinimumSize(new java.awt.Dimension(50, 102));
        splitConfig.setPreferredSize(new java.awt.Dimension(400, 300));

        panelConfig.setPreferredSize(new java.awt.Dimension(200, 300));

        lstConfig.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                lstConfigMouseClicked(evt);
            }
        });
        lstConfig.addListSelectionListener(this::lstConfigValueChanged);
        lstConfig.registerKeyboardAction(openComputerAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        configScrollPane.setViewportView(lstConfig);

        toolConfig.setFloatable(false);
        toolConfig.setRollover(true);

        toolConfig.add(btnAdd);
        toolConfig.add(btnDelete);
        toolConfig.add(btnEdit);
        toolConfig.add(btnRename);
        toolConfig.add(btnSaveSchemaImage);

        GroupLayout panelConfigLayout = new GroupLayout(panelConfig);
        panelConfig.setLayout(panelConfigLayout);
        panelConfigLayout.setHorizontalGroup(
            panelConfigLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(toolConfig, GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
                .addComponent(configScrollPane, GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        panelConfigLayout.setVerticalGroup(
            panelConfigLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelConfigLayout.createSequentialGroup()
                    .addComponent(toolConfig, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(configScrollPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
        );

        splitConfig.setLeftComponent(panelConfig);

        scrollPreview.setViewportView(preview);

        GroupLayout panelPreviewLayout = new GroupLayout(panelPreview);
        panelPreview.setLayout(panelPreviewLayout);
        panelPreviewLayout.setHorizontalGroup(
            panelPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPreview, GroupLayout.DEFAULT_SIZE, 586, Short.MAX_VALUE)
        );
        panelPreviewLayout.setVerticalGroup(
            panelPreviewLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(panelPreviewLayout.createSequentialGroup()
                    .addComponent(scrollPreview, GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE))
        );

        splitConfig.setRightComponent(panelPreview);

        jLabel1.setText("Please select a virtual configuration that will be emulated:");

        btnOpen.setFont(btnOpen.getFont().deriveFont(btnOpen.getFont().getStyle() | java.awt.Font.BOLD));
        btnOpen.setText("Open computer");
        btnOpen.addActionListener(openComputerAction);

        btnClose.setText("Exit");
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
                            .addComponent(btnOpen)))
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

    private void lstConfigMouseClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            openComputerAction.actionPerformed(new ActionEvent(evt.getSource(), 0, ""));
        }
    }

    private void lstConfigValueChanged(ListSelectionEvent evt) {
        Optional
            .ofNullable(lstConfig.getSelectedValue())
            .ifPresentOrElse(computer -> {
                Schema schema = new Schema(computer, applicationConfig);
                preview.setSchema(schema);
            }, () -> preview.setSchema(null));
        preview.repaint();
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
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
