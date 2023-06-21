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
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.emulib.runtime.interaction.BrowseButton;
import net.emustudio.emulib.runtime.interaction.CachedComboBoxModel;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


public class TapePlayerGui extends JDialog {
    private final static String FOLDER_OPEN_ICON = "/net/emustudio/plugins/device/audiotape_player/gui/folder-open.png";

    private final JButton btnBrowse;
    private final JButton btnLoad = new JButton("Load");
    private final CachedComboBoxModel<PathString> cmbDirsModel = new CachedComboBoxModel<>();
    private final JComboBox<PathString> cmbDirs = new JComboBox<>(cmbDirsModel);
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final JList<String> lstTapes = new JList<>(lstTapesModel);
    private final JScrollPane scrollTapes = new JScrollPane(lstTapes);

    private final AtomicReference<PathString> loadedFileName = new AtomicReference<>();

    private final JButton btnPlay = new JButton("Play");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnUnload = new JButton("Unload");

    private final JPanel panelTapeInformation = new JPanel();
    private final JLabel lblFileName = new JLabel("N/A");
    private final JLabel lblStatus = new JLabel("Stopped");

    private final JLabel lblEvents = new JLabel("Tape events:");
    private final DefaultListModel<String> lstEventsModel = new DefaultListModel<>();
    private final JList<String> lstEvents = new JList<>(lstEventsModel);

    private final TapePlaybackController controller;

    public TapePlayerGui(JFrame parent, Dialogs dialogs, TapePlaybackController controller) {
        super(parent);
        Objects.requireNonNull(dialogs);
        this.controller = Objects.requireNonNull(controller);

        btnBrowse = new BrowseButton(dialogs, "Select Directory", "Select", p -> {
            cmbDirsModel.add(new PathString(p, true));
            cmbDirs.setSelectedIndex(0);
        });
        buildLayout(parent);
        setupListeners();
        setCassetteState(controller.getState());
    }

    public void addProgramDetail(String program, String detail) {
        lstEventsModel.add(0, program + ": " + detail);
    }

    public void addPulseInfo(String pulse) {
        JLabel pulseLabel = new JLabel(pulse);
        pulseLabel.setFont(pulseLabel.getFont().deriveFont(Font.ITALIC));
        lstEvents.add(pulseLabel);
    }

    public void setCassetteState(TapePlaybackController.CassetteState state) {
        this.lblStatus.setText(state.name());
        switch (state) {
            case CLOSED:
                lstEventsModel.removeAllElements();
                btnLoad.setEnabled(false);
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnUnload.setEnabled(false);
                break;

            case PLAYING:
                lstEventsModel.removeAllElements();
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(false);
                btnUnload.setEnabled(true);
                btnStop.setEnabled(true);
                break;

            case STOPPED:
                btnStop.setEnabled(false);
                btnLoad.setEnabled(true);
                btnUnload.setEnabled(true);
                btnPlay.setEnabled(true);
                break;

            case UNLOADED:
                lstEventsModel.removeAllElements();
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(true);
                btnUnload.setEnabled(false);
                loadedFileName.set(null);
                lblFileName.setToolTipText("");
                lblFileName.setText("N/A");
                break;
        }
    }

    private void buildLayout(JFrame parent) {
        JPanel panelTapeButtons = new JPanel();
        JPanel panelAvailableTapesButtons = new JPanel();
        JPanel panelAvailableTapes = new JPanel();
        JPanel panelTape = new JPanel();
        JScrollPane scrollEvents = new JScrollPane(lstEvents);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, true, panelAvailableTapes, panelTape
        );
        JLabel lblFileNameLabel = new JLabel("File name:");
        JLabel lblStatusLabel = new JLabel("Status:");

        // btnRefresh.addActionListener(e -> lstTapesModel.refresh());
        btnBrowse.setIcon(new ImageIcon(getClass().getResource(FOLDER_OPEN_ICON)));
        btnBrowse.setText("");
        btnBrowse.setToolTipText("Select directory");
        btnBrowse.setBorder(null);

        lstTapes.setCellRenderer(new TapesListRenderer());

        setTitle("Tape Player");
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));

        JPanel panelDirs = new JPanel();
        panelDirs.setLayout(new MigLayout("insets 2 2 2 2, fillx", "3[]3[]3", "5[]5"));
        panelDirs.add(cmbDirs, "pushx, growx");
        panelDirs.add(btnBrowse, "align right");

        panelAvailableTapes.setBorder(new TitledBorder("Available tapes"));
        panelTape.setBorder(new TitledBorder("Tape"));

        JPanel content = new JPanel();
        content.setLayout(new MigLayout("insets dialog, debug", "[]", "[]"));
        content.setBorder(null);

        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(0.3);
        content.add(splitPane, "grow, push");

        panelAvailableTapes.setLayout(new MigLayout());
        panelAvailableTapes.add(panelDirs, "growx, pushx, wrap");
        panelAvailableTapes.add(scrollTapes, "grow, push, wrap");
        panelAvailableTapes.add(panelAvailableTapesButtons, "pushx, dock south");

        panelAvailableTapesButtons.setLayout(new MigLayout("ins 2 2 2 2", "3[]3", "5[]5"));
        panelAvailableTapesButtons.add(btnLoad, "align left");

        panelTape.setLayout(new MigLayout());
        panelTape.add(panelTapeInformation, "growx, pushx, wrap");
        panelTape.add(lblEvents, "align left, wrap");
        panelTape.add(scrollEvents, "push, grow, wrap");
        panelTape.add(panelTapeButtons, "pushx, dock south");

        panelTapeInformation.setLayout(new MigLayout("debug, fillx"));
        panelTapeInformation.add(lblFileNameLabel, "align label");
        panelTapeInformation.add(lblFileName, "growx, width 0:0:100, wrap");
        panelTapeInformation.add(lblStatusLabel, "align label");
        panelTapeInformation.add(lblStatus, "pushx, wrap");

        panelTapeInformation.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                PathString ps = loadedFileName.get();
                if (ps != null) {
                    ps.deriveMaxStringLength(panelTapeInformation, panelTapeInformation.getWidth());
                    String shortened = ps.getPathShortened();
                    if (shortened.length() < ps.getPath().toString().length()) {
                        lblFileName.setToolTipText(ps.getPath().toString());
                    }
                    lblFileName.setText(shortened);
                }
            }
        });


        panelTapeButtons.setLayout(new MigLayout("ins 2 2 2 2", "3[]3", "5[]5"));
        panelTapeButtons.add(btnPlay, "align left");
        panelTapeButtons.add(btnStop, "pushx"); // gap
        panelTapeButtons.add(btnUnload, "align right");

        setContentPane(content);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
    }

    private void setupListeners() {
        cmbDirs.addActionListener(e -> {
            PathString path = (PathString) cmbDirs.getSelectedItem();
            if (path != null) {
                lstTapesModel.reset(path.getPath());
            }
        });

        cmbDirs.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                PathString path = (PathString) cmbDirs.getSelectedItem();
                if (path != null) {
                    String dirName = path.getPath().toString();
                    if (dirName.length() > path.getMaxStringLength()) {
                        cmbDirs.setToolTipText(dirName);
                    } else {
                        cmbDirs.setToolTipText(null);
                    }
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

        //  btnRefresh.addActionListener(e -> lstTapesModel.refresh());
        btnLoad.addActionListener(e -> {
            int index = lstTapes.getSelectedIndex();
            if (index != -1) {
                Path path = lstTapesModel.getFilePath(index);
                controller.load(path);

                PathString ps = new PathString(path);
                loadedFileName.set(ps);
                ps.deriveMaxStringLength(panelTapeInformation, panelTapeInformation.getWidth());
                lblFileName.setText(ps.getPathShortened());
            }
        });
        btnPlay.addActionListener(e -> controller.play());
        btnStop.addActionListener(e -> controller.stop(false));
        btnUnload.addActionListener(e -> controller.stop(true));
    }
}
