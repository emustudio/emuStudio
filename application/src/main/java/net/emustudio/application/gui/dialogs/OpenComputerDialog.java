/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.gui.actions.opencomputer.*;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FadingBorder;
import net.emustudio.emulib.runtime.ui.components.ToolbarButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
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

import static net.emustudio.application.Resources.getVersion;
import static net.emustudio.application.gui.dialogs.AboutDialog.LOGO_FILE;
import static net.emustudio.application.settings.ConfigFiles.loadConfigurations;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

public class OpenComputerDialog extends DialogBase {
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenComputerDialog.class);

    private final ConfigurationsListModel configurationsModel;
    private final SchemaPreviewPanel preview;
    private final AppSettings appSettings;
    private final Dialogs dialogs;

    private final AddNewComputerAction addNewComputerAction;
    private final DeleteComputerAction deleteComputerAction;
    private final EditComputerAction editComputerAction;
    private final OpenComputerAction openComputerAction;
    private final RenameComputerAction renameComputerAction;
    private final SaveSchemaAction saveSchemaAction;

    private final JList<ComputerConfig> lstConfig = new JList<>();

    public OpenComputerDialog(AppSettings appSettings, Dialogs dialogs, Consumer<ComputerConfig> selectComputer) {
        super((java.awt.Frame) null, "emuStudio - Open virtual computer", true);
        this.configurationsModel = new ConfigurationsListModel();
        this.appSettings = Objects.requireNonNull(appSettings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.preview = new SchemaPreviewPanel(null, dialogs);

        addNewComputerAction = new AddNewComputerAction(dialogs, appSettings, this::update, this);
        deleteComputerAction = new DeleteComputerAction(dialogs, this::update, lstConfig);
        editComputerAction = new EditComputerAction(dialogs, appSettings, this::update, this, lstConfig);
        openComputerAction = new OpenComputerAction(dialogs, this, lstConfig, selectComputer);
        renameComputerAction = new RenameComputerAction(dialogs, this::update, lstConfig);
        saveSchemaAction = new SaveSchemaAction(preview);

        lstConfig.setModel(configurationsModel);

        buildContent();
        setMinimumSize(new Dimension(700, 500));
        setSize(900, 650);
    }

    void update() {
        configurationsModel.update();
        lstConfig.clearSelection();
        lstConfigValueChanged(null);
    }

    @Override
    protected JComponent initializeComponents() {
        JSplitPane splitConfig = GUI.splitPane();
        JPanel panelConfig = new JPanel();
        JScrollPane configScrollPane = new JScrollPane();
        JToolBar toolConfig = GUI.toolBar();
        ToolbarButton btnAdd = GUI.toolbarButton(addNewComputerAction);
        ToolbarButton btnDelete = new ToolbarButton(deleteComputerAction);
        ToolbarButton btnEdit = new ToolbarButton(editComputerAction);
        ToolbarButton btnRename = new ToolbarButton(renameComputerAction);
        ToolbarButton btnSaveSchemaImage = new ToolbarButton(saveSchemaAction);
        JScrollPane scrollPreview = GUI.scrollPane(preview);
        JButton btnOpen = new JButton();
        JButton btnClose = new JButton();


        lstConfig.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                lstConfigMouseClicked(evt);
            }
        });
        lstConfig.addListSelectionListener(this::lstConfigValueChanged);
        lstConfig.registerKeyboardAction(openComputerAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        configScrollPane.setViewportView(lstConfig);
        configScrollPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        GUI.styleList(lstConfig);

        toolConfig.setFloatable(false);
        toolConfig.setRollover(true);
        toolConfig.setOrientation(JToolBar.VERTICAL);
        toolConfig.add(btnAdd);
        toolConfig.add(btnDelete);
        toolConfig.add(btnEdit);
        toolConfig.add(btnRename);
        toolConfig.add(btnSaveSchemaImage);

        panelConfig.setLayout(new net.miginfocom.swing.MigLayout("insets 0, fill", "[][grow]", "[grow]"));
        panelConfig.add(toolConfig, "growy");
        panelConfig.add(configScrollPane, "grow");

        splitConfig.setResizeWeight(0.3);
        splitConfig.setLeftComponent(panelConfig);
        splitConfig.setRightComponent(scrollPreview);

        // Create logo panel
        JLabel lblLogo = new JLabel(loadIcon(LOGO_FILE));
        lblLogo.setBackground(Color.WHITE);
        lblLogo.setBorder(new FadingBorder(10, Color.WHITE));
        lblLogo.setOpaque(false);

        JLabel lblIntroduction = new JLabel("<html><h1>Welcome to emuStudio!</h1><i>Version:" + getVersion() + "</i>");
        lblIntroduction.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel lblPlease = new JLabel("Please select computer you wish to emulate:");
        lblPlease.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Create a vertical panel for introduction and selection prompt
        JPanel textPanel = new JPanel(new java.awt.BorderLayout());
        textPanel.add(lblIntroduction, java.awt.BorderLayout.NORTH);
        textPanel.add(lblPlease, java.awt.BorderLayout.CENTER);

        // Create header panel with logo and introduction
        JPanel headerPanel = new JPanel(new java.awt.BorderLayout(10, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Top, Left, Bottom, Right
        headerPanel.add(lblLogo, java.awt.BorderLayout.WEST);
        headerPanel.add(textPanel, java.awt.BorderLayout.CENTER);

        btnOpen.setText("Open computer");
        btnOpen.addActionListener(openComputerAction);
        GUI.buttonMakePrimary(btnOpen);

        btnClose.setText("Exit");
        btnClose.addActionListener(this::btnCloseActionPerformed);

        SwingUtilities.invokeLater(() -> splitConfig.setDividerLocation(270));

        // --- Layout Construction ---
        // We configure the clean structure directly on the content pane in the constructor,
        // so here we return null or a dummy.
        // But to respect the pattern, let's allow buildContent to assemble the pieces.

        // Center Panel: Header + SplitPane
        JPanel centerPanel = new JPanel(new java.awt.BorderLayout());
        centerPanel.add(headerPanel, java.awt.BorderLayout.NORTH);
        centerPanel.add(splitConfig, java.awt.BorderLayout.CENTER);

        // Buttons Panel: Bottom Right
        JPanel buttonsPanel = new JPanel(new net.miginfocom.swing.MigLayout("insets 5 10 10 10, fillx", "[grow][][]", "[]"));
        buttonsPanel.add(btnOpen, "align right, skip 1, split 2, tag ok, wmin 100");
        buttonsPanel.add(btnClose, "tag cancel, wmin 80");

        // Root Container to hold both centers
        JPanel root = new JPanel(new net.miginfocom.swing.MigLayout("fill, insets 0", "[grow]", "[grow]0[]"));

        root.add(centerPanel, "grow, push, wrap");
        root.add(buttonsPanel, "growx");
        return root;
    }

    private void lstConfigMouseClicked(MouseEvent evt) {
        if (evt.getClickCount() == 2) {
            openComputerAction.actionPerformed(new ActionEvent(evt.getSource(), 0, ""));
        }
    }

    private void lstConfigValueChanged(ListSelectionEvent evt) {
        Optional.ofNullable(lstConfig.getSelectedValue()).ifPresentOrElse(computer -> {
            Schema schema = new Schema(computer, appSettings);
            preview.setSchema(schema);
        }, () -> preview.setSchema(null));
        preview.repaint();
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        dispose();
    }


    private class ConfigurationsListModel extends AbstractListModel<ComputerConfig> {
        private List<ComputerConfig> computerConfigs = Collections.emptyList();

        ConfigurationsListModel() {
            try {
                computerConfigs = loadConfigurations();
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
                computerConfigs = loadConfigurations();
                this.fireContentsChanged(this, -1, -1);
            } catch (IOException e) {
                LOGGER.error("Could not load computer configurations", e);
            }
        }
    }
}
