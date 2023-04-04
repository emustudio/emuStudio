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
    private final JLabel lblHeader = new JLabel();
    private final JLabel lblStatus = new JLabel("STOPPED");
    private final JComboBox<Path> cmbDirs = new JComboBox<>();
    private final JList<String> lstTapes = new JList<>();

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
    }

    public void setMetadata(String metadata) {
        try {
            SwingUtilities.invokeAndWait(() -> lblHeader.setText(metadata));
        } catch (Exception ignored) {

        }
    }

    private void initComponents() {
        JPanel panelTapeSelection = new JPanel();
        JLabel lblTapes = new JLabel("Available tapes:");
        JScrollPane scrollTapes = new JScrollPane();
        JPanel panelTapeControl = new JPanel();
        JPanel panelMetadata = new JPanel();

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
        btnPlay.addActionListener(e -> {
            CassetteController.CassetteState state = controller.play();
            lblStatus.setText(state.name());
        });
        btnStop.addActionListener(e -> {
            CassetteController.CassetteState state = controller.stop(false);
            lblStatus.setText(state.name());
        });
        btnUnload.addActionListener(e -> {
            CassetteController.CassetteState state = controller.stop(true);
            lblStatus.setText(state.name());
        });

        panelTapeSelection.setBorder(BorderFactory.createTitledBorder("Tape selection"));

        scrollTapes.setViewportView(lstTapes);

        GroupLayout panelTapeSelectionLayout = new GroupLayout(panelTapeSelection);
        panelTapeSelection.setLayout(panelTapeSelectionLayout);
        panelTapeSelectionLayout.setHorizontalGroup(
                panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeSelectionLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(scrollTapes, GroupLayout.DEFAULT_SIZE, 349, Short.MAX_VALUE)
                                        .addGroup(panelTapeSelectionLayout.createSequentialGroup()
                                                .addComponent(btnRefresh)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnBrowse)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnLoad)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnUnload))
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
                                .addComponent(scrollTapes, GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelTapeSelectionLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnBrowse)
                                        .addComponent(btnUnload)
                                        .addComponent(btnLoad)
                                        .addComponent(btnRefresh))
                                .addContainerGap())
        );

        panelTapeControl.setBorder(BorderFactory.createTitledBorder("Tape control"));

        lblStatus.setFont(new java.awt.Font("Monospaced", Font.BOLD, 18));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        panelMetadata.setBorder(BorderFactory.createTitledBorder("Metadata"));

        lblHeader.setFont(new java.awt.Font("Monospaced", Font.PLAIN, 15));

        GroupLayout panelMetadataLayout = new GroupLayout(panelMetadata);
        panelMetadata.setLayout(panelMetadataLayout);
        panelMetadataLayout.setHorizontalGroup(
                panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelMetadataLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblHeader, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        panelMetadataLayout.setVerticalGroup(
                panelMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelMetadataLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblHeader)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout panelTapeControlLayout = new GroupLayout(panelTapeControl);
        panelTapeControl.setLayout(panelTapeControlLayout);
        panelTapeControlLayout.setHorizontalGroup(
                panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeControlLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(lblStatus, GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                                        .addComponent(panelMetadata, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(panelTapeControlLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(btnPlay)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnStop)))
                                .addContainerGap())
        );
        panelTapeControlLayout.setVerticalGroup(
                panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(panelTapeControlLayout.createSequentialGroup()
                                .addComponent(lblStatus)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelMetadata, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelTapeControlLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnPlay)
                                        .addComponent(btnStop))
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelTapeSelection, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelTapeControl, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(panelTapeControl, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelTapeSelection, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        pack();
    }
}
