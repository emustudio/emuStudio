/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PluginComboModel implements ComboBoxModel<String> {

    private final Map<Integer, String> namesByIndex = new HashMap<>();
    private final Map<Integer, String> fileNamesByIndex = new HashMap<>();
    private final Map<String, Integer> indexesByName = new HashMap<>();
    private String selectedName = null;
    private String selectedFileName = null;

    PluginComboModel(List<String> pluginFiles) {
        final AtomicInteger i = new AtomicInteger();
        Objects.requireNonNull(pluginFiles).forEach(fileName -> {
            String name = fileName;
            int suffixIndex = name.lastIndexOf(".jar");
            if (suffixIndex != -1) {
                name = name.substring(0, suffixIndex);
            }
            this.namesByIndex.put(i.get(), name);
            this.fileNamesByIndex.put(i.get(), fileName);
            this.indexesByName.put(name, i.getAndIncrement());
        });
    }

    @Override
    public String getSelectedItem() {
        return selectedName;
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item == null) {
            selectedName = null;
            selectedFileName = null;
        } else {
            int index = indexesByName.get(String.valueOf(item));
            selectedName = namesByIndex.get(index);
            selectedFileName = fileNamesByIndex.get(index);
        }
    }

    @Override
    public int getSize() {
        return indexesByName.size();
    }

    @Override
    public String getElementAt(int index) {
        return namesByIndex.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
    }

    public Optional<String> getSelectedFileName() {
        return Optional.ofNullable(selectedFileName);
    }
}
