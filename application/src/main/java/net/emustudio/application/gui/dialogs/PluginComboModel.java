/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import net.emustudio.application.gui.NamePath;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PluginComboModel implements ComboBoxModel<String> {

    private final Map<Integer, NamePath> namePathsByIndex = new HashMap<>();
    private final Map<String, Integer> nameIndexes = new HashMap<>();
    private NamePath selected = null;

    PluginComboModel(List<Path> pluginPaths) {
        final AtomicInteger i = new AtomicInteger();
        Objects.requireNonNull(pluginPaths).forEach(path -> {
            String fileName = path.getFileName().toString();
            int suffixIndex = fileName.lastIndexOf(".jar");
            if (suffixIndex != -1) {
                fileName = fileName.substring(0, fileName.length() - suffixIndex - 1);
            }
            this.namePathsByIndex.put(i.get(), new NamePath(fileName, path));
            this.nameIndexes.put(fileName, i.getAndIncrement());
        });
    }

    @Override
    public void setSelectedItem(Object item) {
        if (item == null) {
            selected = null;
        } else {
            selected = namePathsByIndex.get(nameIndexes.get(String.valueOf(item)));
        }
    }

    @Override
    public String getSelectedItem() {
        return selected.name;
    }

    @Override
    public int getSize() {
        return nameIndexes.size();
    }

    @Override
    public String getElementAt(int index) {
        return namePathsByIndex.get(index).name;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
    }

    public Optional<NamePath> getSelectedNamePath() {
        return Optional.ofNullable(selected);
    }
}
