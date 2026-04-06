/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.ShortenedString;
import net.emustudio.emulib.runtime.ui.components.CachedComboBoxModel;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

// https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
public class TapePlayerGui extends DialogBase {
    private final GUI gui;
    private final static String FOLDER_OPEN_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/folder-open.png";
    private final static String PLAY_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-start.png";
    private final static String STOP_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-stop.png";
    private final static String EJECT_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-eject.png";
    private final static String REFRESH_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/view-refresh.png";
    private final static String LOAD_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/applications-multimedia.png";
    private final static String SAVE_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/document-save.png";
    private final static String COPY_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/edit-copy.png";

    private final JPanel panelTapeInfo;
    private final JButton btnBrowse;
    private final JButton btnRefresh = new JButton("Refresh", GUI.loadIcon(REFRESH_ICON));
    private final JButton btnLoad = new JButton("Load", GUI.loadIcon(LOAD_ICON));
    private final CachedComboBoxModel<ShortenedString<Path>> cmbDirsModel = new CachedComboBoxModel<>();
    private final JComboBox<ShortenedString<Path>> cmbDirs = new JComboBox<>(cmbDirsModel);
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final JList<String> lstTapes = new JList<>(lstTapesModel);
    private final JScrollPane scrollTapes;

    private final AtomicReference<ShortenedString<Path>> loadedFileName = new AtomicReference<>();

    private final JButton btnPlay = new JButton("Play", GUI.loadIcon(PLAY_ICON));
    private final JButton btnStop = new JButton("Stop", GUI.loadIcon(STOP_ICON));
    private final JButton btnEject = new JButton("Eject", GUI.loadIcon(EJECT_ICON));

    private final JTextArea txtFileName = new JTextArea("N/A");
    private final JLabel lblStatus;

    private final TapeEventsTableModel eventsModel = new TapeEventsTableModel();
    private final JTable tblEvents = new JTable(eventsModel);

    private final Dialogs dialogs;
    private final TapePlaybackController controller;

    public TapePlayerGui(JFrame parent, Dialogs dialogs, TapePlaybackController controller, GUI gui) {
        super(parent, "Audio Tape Player", false);
        this.gui = gui;
        this.panelTapeInfo = gui.panel("", "[][grow]", "[][]");
        this.scrollTapes = gui.scrollPane(lstTapes);
        this.lblStatus = gui.labelBold("Stopped");
        this.dialogs = Objects.requireNonNull(dialogs);
        this.controller = Objects.requireNonNull(controller);

        btnBrowse = gui.buttonBrowseDirectories(dialogs, "Select Directory", "Select", p -> {
            ShortenedString<Path> ps = new ShortenedString<>(p, Path::toString);
            ps.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth() - 36);
            cmbDirsModel.add(ps);
            cmbDirs.setSelectedIndex(0);
            cmbDirs.setMinimumSize(new Dimension(0, 0));
        });
        btnBrowse.setIcon(GUI.loadIcon(FOLDER_OPEN_ICON));
        btnBrowse.setText("");
        btnBrowse.setToolTipText("Select directory");
        btnBrowse.setFocusPainted(false);

        setupListeners();
        setCassetteState(controller.getState());
        buildContent();
    }

    public void addProgramDetail(long tstate, String eventType, String details) {
        SwingUtilities.invokeLater(() -> {
            eventsModel.addRow(tstate, 0, eventType, details);
            scrollToLastRow();
        });
    }

    public void addPulseRow(long tstate, int length, String eventType, String details) {
        SwingUtilities.invokeLater(() -> {
            eventsModel.addRow(tstate, length, eventType, details);
            scrollToLastRow();
        });
    }

    private void scrollToLastRow() {
        int lastRow = tblEvents.getRowCount() - 1;
        if (lastRow >= 0) {
            tblEvents.scrollRectToVisible(tblEvents.getCellRect(lastRow, 0, true));
        }
    }

    private void copySelectedRows() {
        int[] selectedRows = tblEvents.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int colCount = tblEvents.getColumnCount();
        for (int row : selectedRows) {
            for (int col = 0; col < colCount; col++) {
                if (col > 0) {
                    sb.append('\t');
                }
                Object value = tblEvents.getValueAt(row, col);
                sb.append(value != null ? value.toString() : "");
            }
            sb.append('\n');
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void saveLogs(Path path) {
        if (eventsModel.getRowCount() == 0) {
            dialogs.showInfo("No log events to save.", "Save Logs");
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            int colCount = eventsModel.getColumnCount();
            for (int col = 0; col < colCount; col++) {
                if (col > 0) {
                    writer.write('\t');
                }
                writer.write(eventsModel.getColumnName(col));
            }
            writer.newLine();
            for (int row = 0; row < eventsModel.getRowCount(); row++) {
                for (int col = 0; col < colCount; col++) {
                    if (col > 0) {
                        writer.write('\t');
                    }
                    Object value = eventsModel.getValueAt(row, col);
                    writer.write(value != null ? value.toString() : "");
                }
                writer.newLine();
            }
        } catch (IOException ex) {
            dialogs.showError("Could not save logs: " + ex.getMessage(), "Save Logs");
        }
    }

    public void setCassetteState(TapePlaybackController.CassetteState state) {
        SwingUtilities.invokeLater(() -> setCassetteStateImpl(state));
    }

    private void setCassetteStateImpl(TapePlaybackController.CassetteState state) {
        this.lblStatus.setText(state.name());
        switch (state) {
            case CLOSED:
                eventsModel.clear();
                btnLoad.setEnabled(false);
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnEject.setEnabled(false);
                break;

            case PLAYING:
                eventsModel.clear();
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(false);
                btnEject.setEnabled(true);
                btnStop.setEnabled(true);
                break;

            case STOPPED:
                btnStop.setEnabled(false);
                btnLoad.setEnabled(true);
                btnEject.setEnabled(true);
                btnPlay.setEnabled(true);
                break;

            case UNLOADED:
                eventsModel.clear();
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(true);
                btnEject.setEnabled(false);
                loadedFileName.set(null);
                txtFileName.setToolTipText("");
                txtFileName.setText("N/A");
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void setupListeners() {
        cmbDirs.addActionListener(e -> {
            ShortenedString<Path> path = (ShortenedString<Path>) cmbDirs.getSelectedItem();
            if (path != null) {
                lstTapesModel.reset(path.getValue());
            }
        });
        cmbDirs.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ShortenedString<Path> path = (ShortenedString<Path>) cmbDirs.getSelectedItem();

                if (path != null) {
                    // ComboBox has 2 components: text area and drop-down button. We need to eliminate button width.
                    // From observation, the drop-down button width is 36 pixels.
                    path.deriveMaxStringLength(cmbDirs, cmbDirs.getWidth() - 36);
                    String dirName = path.getValue().toString();
                    if (dirName.length() > path.getMaxStringLength()) {
                        cmbDirs.setToolTipText(dirName);
                    } else {
                        cmbDirs.setToolTipText(null);
                    }
                    revalidate();
                }
            }
        });

        panelTapeInfo.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ShortenedString<Path> ps = loadedFileName.get();
                if (ps != null) {
                    ps.deriveMaxStringLength(panelTapeInfo);
                    String shortened = ps.getShortenedString();
                    if (shortened.length() < ps.getValue().toString().length()) {
                        txtFileName.setToolTipText(ps.getValue().toString());
                    }
                    txtFileName.setText(shortened);
                }
            }
        });

        lstTapes.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = lstTapes.locationToIndex(e.getPoint());
                if (index > -1) {
                    String fileName = lstTapesModel.getFilePath(index).getFileName().toString();
                    if (!fileName.equals(lstTapesModel.elementAt(index))) {
                        lstTapes.setToolTipText(fileName);
                    } else {
                        lstTapes.setToolTipText(null);
                    }
                }
            }
        });
        lstTapes.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                lstTapesModel.resize(e.getComponent(), scrollTapes.getViewportBorderBounds().width);
            }
        });

        btnRefresh.addActionListener(e -> lstTapesModel.refresh());
        btnLoad.addActionListener(e -> {
            int index = lstTapes.getSelectedIndex();
            if (index != -1) {
                Path path = lstTapesModel.getFilePath(index);
                controller.load(path);

                ShortenedString<Path> ps = new ShortenedString<>(path, p -> p.getFileName().toString());
                loadedFileName.set(ps);
                ps.deriveMaxStringLength(panelTapeInfo);
                txtFileName.setText(ps.getShortenedString());
            }
        });
        btnPlay.addActionListener(e -> controller.play());
        btnStop.addActionListener(e -> controller.stop(false));
        btnEject.addActionListener(e -> controller.stop(true));
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panelAvailableTapes = gui.section("Available tapes", "insets 2", "[grow]", "[][grow][]");
        JPanel panelDirs = gui.panel("fillx", "[fill, grow][]", "[]");
        JToolBar toolbarAvailableTapes = gui.toolBar();
        JPanel panelTape = gui.section("Audio Tape", "insets 2", "[grow]", "[][grow][]");
        JSplitPane splitPane = gui.splitPaneLeftToRight(panelAvailableTapes, panelTape, 0.3);

        JLabel lblFileNameLabel = gui.label("File name:");
        JLabel lblStatusLabel = gui.label("Status:");

        gui.styleTable(tblEvents);
        tblEvents.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        tblEvents.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tblEvents.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblEvents.getColumnModel().getColumn(1).setPreferredWidth(70);
        tblEvents.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblEvents.getColumnModel().getColumn(3).setPreferredWidth(200);
        tblEvents.getColumnModel().getColumn(3).setCellRenderer(new WordWrapCellRenderer());
        tblEvents.setFillsViewportHeight(true);
        tblEvents.setPreferredScrollableViewportSize(new Dimension(470, 200));

        // Remove JTable's default ESC binding ("cancel") so it doesn't consume the key event
        // before DialogBase's ESC-to-close listener can handle it
        tblEvents.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "none");

        KeyStroke copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        tblEvents.getInputMap(JComponent.WHEN_FOCUSED).put(copyKeyStroke, "copy");
        tblEvents.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });

        JScrollPane scrollEvents = gui.scrollPane(tblEvents);
        scrollEvents.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        JToolBar toolbarTape = gui.toolBar();
        JPanel hSpacer1 = new JPanel(null);
        JPanel hSpacer2 = new JPanel(null);

        splitPane.setDividerLocation(250);

        cmbDirs.setMinimumSize(new Dimension(0, 0));
        panelDirs.add(cmbDirs, "cell 0 0");
        JToolBar btnBrowseToolBar = gui.toolBar();
        btnBrowseToolBar.add(btnBrowse);
        panelDirs.add(btnBrowseToolBar, "cell 1 0");

        toolbarAvailableTapes.add(btnRefresh);
        toolbarAvailableTapes.add(hSpacer2);
        toolbarAvailableTapes.add(btnLoad);

        lstTapes.setCellRenderer(new TapesListRenderer());
        lstTapes.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "none");

        panelAvailableTapes.add(panelDirs, "cell 0 0, growx");
        panelAvailableTapes.add(scrollTapes, "cell 0 1, grow");
        panelAvailableTapes.add(toolbarAvailableTapes, "cell 0 2, growx");

        toolbarTape.add(btnPlay);
        toolbarTape.add(btnStop);
        toolbarTape.addSeparator();
        toolbarTape.add(hSpacer1);
        toolbarTape.add(btnEject);
        toolbarTape.addSeparator();

        JButton btnCopyLogs = new JButton(GUI.loadIcon(COPY_ICON));
        btnCopyLogs.setToolTipText("Copy selected rows");
        btnCopyLogs.setFocusPainted(false);
        btnCopyLogs.addActionListener(e -> copySelectedRows());
        toolbarTape.add(btnCopyLogs);

        JButton btnSaveLogs = gui.buttonBrowseFiles(
                dialogs, "Save event logs", "Save", true,
                this::saveLogs,
                new FileExtensionsFilter("Tab-separated values", "tsv"),
                new FileExtensionsFilter("Text file", "txt"));
        btnSaveLogs.setIcon(GUI.loadIcon(SAVE_ICON));
        btnSaveLogs.setText("");
        btnSaveLogs.setToolTipText("Save logs");
        btnSaveLogs.setFocusPainted(false);
        toolbarTape.add(btnSaveLogs);

        txtFileName.setEditable(false);
        txtFileName.setLineWrap(true);
        txtFileName.setMinimumSize(new Dimension(0, 0));
        txtFileName.setBackground(UIManager.getColor("Panel.background"));
        lblStatus.setMinimumSize(new Dimension(0, 0));

        panelTapeInfo.add(lblFileNameLabel, "cell 0 0, alignx right");
        panelTapeInfo.add(txtFileName, "cell 1 0, growx");
        panelTapeInfo.add(lblStatusLabel, "cell 0 1, alignx right");
        panelTapeInfo.add(lblStatus, "cell 1 1, growx");

        panelTape.add(panelTapeInfo, "cell 0 0, growx");
        panelTape.add(scrollEvents, "cell 0 1, grow");
        panelTape.add(toolbarTape, "cell 0 2, growx");

        JPanel contentPanel = gui.panel("insets dialog, fill", "[fill]", "[fill]");
        contentPanel.add(splitPane, "push, grow");

        setPreferredSize(new Dimension(800, 450));
        return contentPanel;
    }
}
