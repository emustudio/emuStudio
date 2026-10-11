/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.emustudio.application.gui.actions.opencomputer.*;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.application.gui.schema.SchemaPreviewPanel;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
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
import static net.emustudio.application.settings.ConfigFiles.loadConfigurations;

public class OpenComputerDialog extends DialogBase {
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenComputerDialog.class);

    private final ConfigurationsListModel configurationsModel;
    private final SchemaPreviewPanel preview;
    private final AppSettings appSettings;
    private final Dialogs dialogs;
    private final EmuStudioGui gui;

    private final AddNewComputerAction addNewComputerAction;
    private final DeleteComputerAction deleteComputerAction;
    private final EditComputerAction editComputerAction;
    private final OpenComputerAction openComputerAction;
    private final RenameComputerAction renameComputerAction;
    private final SaveSchemaAction saveSchemaAction;

    private final JList<ComputerConfig> lstConfig = new JList<>();
    private JPanel configurationPanel;

    public OpenComputerDialog(AppSettings appSettings, Dialogs dialogs, Consumer<ComputerConfig> selectComputer, EmuStudioGui gui) {
        super((java.awt.Frame) null, "emuStudio - Open virtual computer", true);
        this.configurationsModel = new ConfigurationsListModel();
        this.appSettings = Objects.requireNonNull(appSettings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = Objects.requireNonNull(gui);
        this.preview = new SchemaPreviewPanel(null, dialogs);

        addNewComputerAction = new AddNewComputerAction(dialogs, appSettings, this::update, this, gui);
        deleteComputerAction = new DeleteComputerAction(dialogs, this::update, lstConfig);
        editComputerAction = new EditComputerAction(dialogs, appSettings, this::update, this, lstConfig, gui);
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
        JSplitPane splitConfig = gui.splitPane();
        JToolBar toolConfig = gui.toolBarVertical();
        JButton btnAdd = gui.toolbarButton(addNewComputerAction);
        JButton btnDelete = gui.toolbarButton(deleteComputerAction);
        JButton btnEdit = gui.toolbarButton(editComputerAction);
        JButton btnRename = gui.toolbarButton(renameComputerAction);
        JButton btnSaveSchemaImage = gui.toolbarButton(saveSchemaAction);
        JScrollPane scrollPreview = gui.scrollPane(preview);
        configurationPanel = gui.panel("insets 10, fillx, wrap 1", "[grow]", "");
        JScrollPane scrollConfiguration = gui.scrollPane(configurationPanel);
        scrollPreview.setMinimumSize(new Dimension(0, 100));
        scrollConfiguration.setMinimumSize(new Dimension(0, 100));
        JSplitPane splitPreview = gui.splitPaneTopToBottom(scrollPreview, scrollConfiguration, 0.5);
        JButton btnClose = new JButton();
        JLabel lblLogo = gui.createLogoJLabel();

        lstConfig.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                lstConfigMouseClicked(evt);
            }
        });
        lstConfig.addListSelectionListener(this::lstConfigValueChanged);
        lstConfig.registerKeyboardAction(openComputerAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JScrollPane configScrollPane = gui.scrollPane(lstConfig);
        configScrollPane.setBorder(BorderFactory.createTitledBorder("Virtual computers"));
        gui.styleList(lstConfig);

        toolConfig.add(btnAdd);
        toolConfig.add(btnDelete);
        toolConfig.add(btnEdit);
        toolConfig.add(btnRename);
        toolConfig.add(btnSaveSchemaImage);

        JPanel panelConfig = gui.panel("insets 0, fill", "[][grow]", "[grow]");
        panelConfig.add(toolConfig, "growy");
        panelConfig.add(configScrollPane, "grow");

        splitConfig.setResizeWeight(0.3);
        splitConfig.setLeftComponent(panelConfig);
        splitConfig.setRightComponent(splitPreview);

        JLabel lblIntroduction = gui.labelPadded("<html><h1>Welcome to emuStudio!</h1><i>Version:" + getVersion() + "</i>", 5, 10, 5, 10);

        JLabel lblPlease = gui.labelPadded("Please select computer you wish to emulate:", 5, 10, 5, 10);

        // Create a vertical panel for introduction and selection prompt
        JPanel textPanel = gui.panel("insets 0, flowy, fill", "[grow]", "[]0[grow]");
        textPanel.add(lblIntroduction);
        textPanel.add(lblPlease);

        // Create header panel with logo and introduction
        JPanel headerPanel = gui.panel("insets 15", "[]10[grow]", "[grow]");
        headerPanel.add(lblLogo);
        headerPanel.add(textPanel, "grow");

        JButton btnOpen = gui.button("Open computer", openComputerAction);
        gui.buttonMakePrimary(btnOpen);

        btnClose.setText("Exit");
        btnClose.addActionListener(this::btnCloseActionPerformed);

        SwingUtilities.invokeLater(() -> {
            splitConfig.setDividerLocation(270);
            splitPreview.setDividerLocation(0.5);
        });

        // Center Panel: Header + SplitPane
        JPanel centerPanel = gui.panel("insets 0, fill", "[grow]", "[]0[grow]");
        centerPanel.add(headerPanel, "growx, wrap");
        centerPanel.add(splitConfig, "grow");

        // Buttons Panel: Bottom Right
        JPanel buttonsPanel = gui.panel("insets 5 10 10 10, fillx", "[grow][][]", "[]");
        buttonsPanel.add(btnOpen, "align right, skip 1, split 2, tag ok, wmin 100");
        buttonsPanel.add(btnClose, "tag cancel, wmin 80");

        // Root Container to hold both centers
        JPanel root = gui.panel("fill, insets 0", "[grow]", "[grow]0[]");

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
        configurationPanel.removeAll();
        Optional.ofNullable(lstConfig.getSelectedValue()).ifPresentOrElse(computer -> {
            Schema schema = new Schema(computer, appSettings);
            preview.setSchema(schema);
            computer.getCPU().ifPresent(plugin -> addPluginConfiguration("CPU", plugin));
            computer.getMemory().ifPresent(plugin -> addPluginConfiguration("Memory", plugin));
            computer.getCompiler().ifPresent(plugin -> addPluginConfiguration("Compiler", plugin));
            computer.getDevices().forEach(plugin ->
                    addPluginConfiguration("Device (" + plugin.getPluginName() + ")", plugin));
        }, () -> preview.setSchema(null));
        preview.repaint();
        configurationPanel.revalidate();
        configurationPanel.repaint();
    }

    private void addPluginConfiguration(String title, PluginConfig plugin) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Key", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.addRow(new Object[]{"name", plugin.getPluginName()});
        model.addRow(new Object[]{"path", plugin.getPluginFile()});
        plugin.getConfig().<UnmodifiableConfig>getOptional("settings")
                .ifPresent(settings -> addSettingsRows(model, "", settings));

        JTable table = new JTable(model);
        table.setName(title);
        gui.styleTable(table);
        table.setTableHeader(null);

        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        configurationPanel.add(header, "gaptop 5");
        configurationPanel.add(table, "growx");
    }

    private void addSettingsRows(DefaultTableModel model, String prefix, UnmodifiableConfig settings) {
        settings.entrySet().stream().sorted(java.util.Comparator.comparing(UnmodifiableConfig.Entry::getKey))
                .forEach(entry -> {
                    String key = prefix + entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof UnmodifiableConfig) {
                        addSettingsRows(model, key + ".", (UnmodifiableConfig) value);
                    } else {
                        model.addRow(new Object[]{key, String.valueOf(value)});
                    }
                });
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
