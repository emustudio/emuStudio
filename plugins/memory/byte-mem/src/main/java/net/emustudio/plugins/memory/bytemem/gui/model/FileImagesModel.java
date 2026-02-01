/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.model;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FileImagesModel extends AbstractTableModel {
    private final static Logger LOGGER = LoggerFactory.getLogger(FileImagesModel.class);

    private final List<String> imageFullFileNames = new ArrayList<>();
    private final List<String> imageShortFileNames = new ArrayList<>();
    private final List<Integer> imageAddresses = new ArrayList<>();
    private final List<Integer> imageBanks = new ArrayList<>();

    public FileImagesModel(PluginSettings settings, Dialogs dialogs) {
        for (int i = 0; ; i++) {
            try {
                Optional<String> imageName = settings.getString("imageName" + i);
                Optional<Integer> imageAddress = settings.getInt("imageAddress" + i);
                Optional<Integer> imageBank = settings.getInt("imageBank" + i);

                if (imageName.isPresent() && imageAddress.isPresent()) {
                    String fileName = imageName.get();
                    int address = imageAddress.get();

                    imageFullFileNames.add(fileName);
                    imageShortFileNames.add(new File(fileName).getName());
                    imageAddresses.add(address);
                    imageBanks.add(imageBank.orElse(0));
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid number format of setting 'imageAddress" + i + "'", e);
                dialogs.showError(
                        "Invalid number format of setting 'imageAddress" + i + "'. Please see log file for more details"
                );
            }
        }
    }

    @Override
    public int getRowCount() {
        return imageShortFileNames.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "File name";
            case 1:
                return "Address";
            case 2:
                return "Bank";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (col == 2) {
            return Integer.class;
        } else {
            return String.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return imageShortFileNames.get(rowIndex);
            case 1:
                return String.format("0x%04X", imageAddresses.get(rowIndex));
            case 2:
                return imageBanks.get(rowIndex);
        }
        return null;
    }

    public List<String> getImageFullNames() {
        return Collections.unmodifiableList(imageFullFileNames);
    }

    public List<Integer> getImageAddresses() {
        return Collections.unmodifiableList(imageAddresses);
    }

    public List<Integer> getImageBanks() {
        return Collections.unmodifiableList(imageBanks);
    }

    public void addImage(Path fileSource, int address, int bank) {
        imageShortFileNames.add(fileSource.getFileName().toString());
        imageFullFileNames.add(fileSource.toString());
        imageAddresses.add(address);
        imageBanks.add(bank);

        fireTableDataChanged();
    }

    public void removeImageAt(int index) {
        imageShortFileNames.remove(index);
        imageFullFileNames.remove(index);
        imageAddresses.remove(index);
        imageBanks.remove(index);

        fireTableDataChanged();
    }

    public String getFileNameAtRow(int index) {
        return imageFullFileNames.get(index);
    }

    public int getImageAddressAtRow(int index) {
        return imageAddresses.get(index);
    }

    public int getImageBankAtRow(int index) {
        return imageBanks.get(index);
    }
}
