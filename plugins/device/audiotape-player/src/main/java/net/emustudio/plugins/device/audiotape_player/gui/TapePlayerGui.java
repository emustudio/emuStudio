/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.ShortenedString;
import net.emustudio.emulib.runtime.ui.components.CachedComboBoxModel;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

// https://stackoverflow.com/questions/25010068/miglayout-push-vs-grow
public class TapePlayerGui extends DialogBase {
    private final static String FOLDER_OPEN_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/folder-open.png";
    private final static String PLAY_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-start.png";
    private final static String STOP_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-playback-stop.png";
    private final static String EJECT_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/media-eject.png";
    private final static String REFRESH_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/view-refresh.png";
    private final static String LOAD_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/applications-multimedia.png";

    private final JPanel panelTapeInfo = GUI.panel("", "[][grow]", "[][]");
    private final JButton btnBrowse;
    private final JButton btnRefresh = new JButton("Refresh", GUI.loadIcon(REFRESH_ICON));
    private final JButton btnLoad = new JButton("Load", GUI.loadIcon(LOAD_ICON));
    private final CachedComboBoxModel<ShortenedString<Path>> cmbDirsModel = new CachedComboBoxModel<>();
    private final JComboBox<ShortenedString<Path>> cmbDirs = new JComboBox<>(cmbDirsModel);
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final JList<String> lstTapes = new JList<>(lstTapesModel);
    private final JScrollPane scrollTapes = GUI.scrollable(lstTapes);

    private final AtomicReference<ShortenedString<Path>> loadedFileName = new AtomicReference<>();

    private final JButton btnPlay = new JButton("Play", GUI.loadIcon(PLAY_ICON));
    private final JButton btnStop = new JButton("Stop", GUI.loadIcon(STOP_ICON));
    private final JButton btnEject = new JButton("Eject", GUI.loadIcon(EJECT_ICON));

    private final JTextArea txtFileName = new JTextArea("N/A");
    private final JLabel lblStatus = GUI.labelBold("Stopped");

    private final JTextArea txtEvents = GUI.textAreaReadOnly(0, 0);

    private final TapePlaybackController controller;

    public TapePlayerGui(JFrame parent, Dialogs dialogs, TapePlaybackController controller) {
        super(parent, "Audio Tape Player", false);
        Objects.requireNonNull(dialogs);
        this.controller = Objects.requireNonNull(controller);

        btnBrowse = GUI.browseDirectories(dialogs, "Select Directory", "Select", p -> {
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

    public void addProgramDetail(String program, String detail) {
        txtEvents.append("\n" + program + ": " + detail);
    }

    public void addPulseInfo(String pulse) {
        txtEvents.append("\n" + pulse);
    }

    public void setCassetteState(TapePlaybackController.CassetteState state) {
        this.lblStatus.setText(state.name());
        switch (state) {
            case CLOSED:
                txtEvents.setText("");
                btnLoad.setEnabled(false);
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnEject.setEnabled(false);
                break;

            case PLAYING:
                txtEvents.setText("");
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
                txtEvents.setText("");
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
        JPanel panelAvailableTapes = GUI.section("Available tapes", "insets 2", "[grow]", "[][grow][]");
        JPanel panelDirs = GUI.panel("fillx", "[fill, grow][]", "[]");
        JToolBar toolbarAvailableTapes = GUI.toolbar();
        JPanel panelTape = GUI.section("Audio Tape", "insets 2", "[grow]", "[][grow][]");
        JSplitPane splitPane = GUI.splitLeftRight(panelAvailableTapes, panelTape, 0.3);

        JLabel lblFileNameLabel = GUI.label("File name:");
        JLabel lblStatusLabel = GUI.label("Status:");

        JScrollPane scrollEvents = GUI.scrollable(txtEvents);
        JToolBar toolbarTape = GUI.toolbar();
        JPanel hSpacer1 = new JPanel(null);
        JPanel hSpacer2 = new JPanel(null);

        splitPane.setDividerLocation(250);

        cmbDirs.setMinimumSize(new Dimension(0, 0));
        panelDirs.add(cmbDirs, "cell 0 0");
        JToolBar btnBrowseToolBar = GUI.toolbar();
        btnBrowseToolBar.add(btnBrowse);
        panelDirs.add(btnBrowseToolBar, "cell 1 0");

        toolbarAvailableTapes.add(btnRefresh);
        toolbarAvailableTapes.add(hSpacer2);
        toolbarAvailableTapes.add(btnLoad);

        lstTapes.setCellRenderer(new TapesListRenderer());

        panelAvailableTapes.add(panelDirs, "cell 0 0, growx");
        panelAvailableTapes.add(scrollTapes, "cell 0 1, grow");
        panelAvailableTapes.add(toolbarAvailableTapes, "cell 0 2, growx");

        toolbarTape.add(btnPlay);
        toolbarTape.add(btnStop);
        toolbarTape.addSeparator();
        toolbarTape.add(hSpacer1);
        toolbarTape.add(btnEject);

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

        JPanel contentPanel = GUI.panel("insets dialog, fill", "[fill]", "[fill]");
        contentPanel.add(splitPane, "push, grow");
        return contentPanel;
    }
}
