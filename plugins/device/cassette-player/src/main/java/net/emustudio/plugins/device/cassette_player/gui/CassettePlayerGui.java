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
package net.emustudio.plugins.device.cassette_player.gui;

import net.emustudio.emulib.runtime.interaction.BrowseButton;
import net.emustudio.emulib.runtime.interaction.CachedComboBoxModel;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.cassette_player.CassetteController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Objects;

public class CassettePlayerGui extends JDialog {
    private final JButton btnBrowse;
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnLoad = new JButton("Load");
    private final JButton btnPlay = new JButton("Play");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnUnload = new JButton("Unload");
    private final JLabel lblStatus = new JLabel("STOPPED");
    private final JLabel lblPulseInfo = new JLabel();
    private final JComboBox<Path> cmbDirs = new JComboBox<>();
    private final JList<String> lstTapes = new JList<>();
    private final JLabel lblDetail = new JLabel();
    private final JLabel lblProgram = new JLabel();

    private final CachedComboBoxModel<Path> cmbDirsModel = new CachedComboBoxModel<>();
    private final TapesListModel lstTapesModel = new TapesListModel();
    private final CassetteController controller;

    public CassettePlayerGui(JFrame parent, Dialogs dialogs, CassetteController controller) {
        super(parent);
        this.controller = Objects.requireNonNull(controller);
        this.btnBrowse = new BrowseButton(dialogs, "Select Directory", "Select", p -> {
            cmbDirsModel.add(p);
            cmbDirs.setSelectedIndex(0);
        });

        initComponents();
        setLocationRelativeTo(parent);
        setCassetteState(controller.getState());
    }

    public void setProgramDetail(String program, String detail) {
        lblProgram.setText(program);
        lblDetail.setText(detail);
    }

    public void setPulseInfo(String pulseInfo) {
        lblPulseInfo.setText(pulseInfo);
    }

    public void setCassetteState(CassetteController.CassetteState state) {
        this.lblStatus.setText(state.name());
        switch (state) {
            case CLOSED:
                btnLoad.setEnabled(false);
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnUnload.setEnabled(false);
                break;

            case PLAYING:
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
                btnStop.setEnabled(false);
                btnPlay.setEnabled(false);
                btnLoad.setEnabled(true);
                btnUnload.setEnabled(false);
                break;
        }
    }

    private void initComponents() {
        JPanel panelTapeSelection = new JPanel();
        JLabel lblTapes = new JLabel("Available tapes:");
        JScrollPane scrollTapes = new JScrollPane();
        JPanel panelTapeControl = new JPanel();
        JPanel panelMetadata = new JPanel();
        JTabbedPane tabPane = new JTabbedPane();
        JPanel panelStatus = new JPanel();
        JLabel lblProgramLabel = new JLabel("Program:");
        JLabel lblDetailLabel = new JLabel("Detail:");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setTitle("Cassette Player");

        lstTapes.setModel(lstTapesModel);

        cmbDirs.setModel(cmbDirsModel);
        cmbDirs.addActionListener(e -> {
            Path path = (Path) cmbDirs.getSelectedItem();
            if (path != null) {
                lstTapesModel.reset(path);
            }
        });

        btnRefresh.addActionListener(e -> lstTapesModel.refresh());
        btnLoad.addActionListener(e -> {
            int index = lstTapes.getSelectedIndex();
            if (index != -1) {
                controller.load(lstTapesModel.getFilePath(index));
            }
        });
        btnPlay.addActionListener(e -> controller.play());
        btnStop.addActionListener(e -> controller.stop(false));
        btnUnload.addActionListener(e -> controller.stop(true));

        scrollTapes.setViewportView(lstTapes);

        GroupLayout panelTapeSelectionLayout = new GroupLayout(panelTapeSelection);
        panelTapeSelection.setLayout(panelTapeSelectionLayout);
        panelTapeSelectionLayout.setHorizontalGroup(
                panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeSelectionLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(scrollTapes, GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE)
                                        .addGroup(panelTapeSelectionLayout.createSequentialGroup()
                                                .addComponent(btnRefresh)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnBrowse)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnLoad))
                                        .addGroup(panelTapeSelectionLayout.createSequentialGroup()
                                                .addComponent(lblTapes)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(cmbDirs, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        panelTapeSelectionLayout.setVerticalGroup(
                panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeSelectionLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblTapes)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbDirs, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollTapes, GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnBrowse)
                                        .addComponent(btnLoad)
                                        .addComponent(btnRefresh))
                                .addContainerGap())
        );

        tabPane.addTab("Tape selection", panelTapeSelection);
        tabPane.addTab("Tape control", panelTapeControl);

        lblStatus.setFont(new java.awt.Font("Monospaced", Font.BOLD, 18));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        panelMetadata.setBorder(BorderFactory.createTitledBorder("Block Metadata"));

        lblPulseInfo.setVerticalAlignment(SwingConstants.TOP);
        lblPulseInfo.setVerticalTextPosition(SwingConstants.TOP);

        GroupLayout panelMetadataLayout = new GroupLayout(panelMetadata);
        panelMetadata.setLayout(panelMetadataLayout);
        panelMetadataLayout.setHorizontalGroup(
                panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelMetadataLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblPulseInfo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelMetadataLayout.createSequentialGroup()
                                                .addGroup(panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblProgramLabel)
                                                        .addComponent(lblDetailLabel))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblDetail, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblProgram, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
        );
        panelMetadataLayout.setVerticalGroup(
                panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelMetadataLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblProgramLabel)
                                        .addComponent(lblProgram))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblDetailLabel)
                                        .addComponent(lblDetail))
                                .addGap(18, 18, 18)
                                .addComponent(lblPulseInfo, GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                                .addContainerGap())
        );

        panelStatus.setBorder(BorderFactory.createTitledBorder("Tape Status"));

        GroupLayout panelStatusLayout = new GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelStatusLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()));
        panelStatusLayout.setVerticalGroup(
                panelStatusLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblStatus, GroupLayout.PREFERRED_SIZE, 43, GroupLayout.PREFERRED_SIZE));

        GroupLayout panelTapeControlLayout = new GroupLayout(panelTapeControl);
        panelTapeControl.setLayout(panelTapeControlLayout);
        panelTapeControlLayout.setHorizontalGroup(
                panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeControlLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(panelMetadata, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelTapeControlLayout.createSequentialGroup()
                                                .addComponent(btnPlay)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnStop)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 395, Short.MAX_VALUE)
                                                .addComponent(btnUnload))
                                        .addComponent(panelStatus, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap()));
        panelTapeControlLayout.setVerticalGroup(
                panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeControlLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelStatus, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelMetadata, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnPlay)
                                        .addComponent(btnStop)
                                        .addComponent(btnUnload))
                                .addContainerGap()));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(tabPane)
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(tabPane)));

        pack();
    }
}
