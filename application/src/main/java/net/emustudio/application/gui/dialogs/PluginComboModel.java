/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
