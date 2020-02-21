/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.memory.standard.gui.model;

import emulib.emustudio.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileImagesModel extends AbstractTableModel {
    private final static Logger LOGGER = LoggerFactory.getLogger(FileImagesModel.class);

    private final List<String> imageFullFileNames = new ArrayList<>();
    private final List<String> imageShortFileNames = new ArrayList<>();
    private final List<Integer> imageAddresses = new ArrayList<>();

    public FileImagesModel(SettingsManager settings, long pluginID) {
        for (int i = 0; ; i++) {
            String fileName = settings.readSetting(pluginID, "imageName" + i);
            String address = settings.readSetting(pluginID, "imageAddress" + i);
            if (fileName == null || address == null) {
                break;
            }
            try {
                int addressInt = Integer.decode(address);

                imageFullFileNames.add(fileName);
                imageShortFileNames.add(new File(fileName).getName());
                imageAddresses.add(addressInt);
            } catch (NumberFormatException e) {
                LOGGER.error("[address={}] Unparseable image address. Ignoring the file", address, e);
            }
        }
    }

    @Override
    public int getRowCount() {
        return imageShortFileNames.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "File name";
        } else {
            return "Load address (hex)";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int r, int c) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return imageShortFileNames.get(rowIndex);
        } else {
            return String.format("0x%04X", imageAddresses.get(rowIndex));
        }
    }

    public void setValueAt(int r, int c) {
        fireTableCellUpdated(r, c);
    }

    public List<String> getImageFullNames() {
        return Collections.unmodifiableList(imageFullFileNames);
    }

    public List<Integer> getImageAddresses() {
        return Collections.unmodifiableList(imageAddresses);
    }

    public void addImage(File fileSource, int address) {
        imageShortFileNames.add(fileSource.getName());
        imageFullFileNames.add(fileSource.getAbsolutePath());
        imageAddresses.add(address);

        fireTableDataChanged();
    }

    public void removeImageAt(int index) {
        imageShortFileNames.remove(index);
        imageFullFileNames.remove(index);
        imageAddresses.remove(index);

        fireTableDataChanged();
    }

    public String getFileNameAtRow(int index) {
        return imageFullFileNames.get(index);
    }

    public int getImageAddressAtRow(int index) {
        return imageAddresses.get(index);
    }
}
