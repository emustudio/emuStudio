/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio.gui;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PortListModel extends AbstractListModel<String> {
    private final List<Integer> ports = new ArrayList<>();

    public boolean add(int port) {
        if (ports.contains(port)) {
            return false;
        }
        ports.add(port);
        fireContentsChanged(this, 0, ports.size() - 1);

        return true;
    }

    public void addAll(Collection<Integer> ports) {
        this.ports.addAll(ports);
        fireContentsChanged(this, 0, this.ports.size() - 1);
    }

    public List<Integer> getAll() {
        return Collections.unmodifiableList(ports);
    }

    public void clear() {
        ports.clear();
        fireContentsChanged(this, 0, -1);
    }

    public void removeAt(int index) {
        ports.remove(index);
        fireContentsChanged(this, 0, ports.size() - 1);
    }

    public boolean contains(int port) {
        return ports.contains(port);
    }

    @Override
    public int getSize() {
        return ports.size();
    }

    @Override
    public String getElementAt(int index) {
        return String.format("0x%x", ports.get(index));
    }

}
