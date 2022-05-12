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

    public Collection<Integer> getAll() {
        return Collections.unmodifiableCollection(ports);
    }

    public void clear() {
        ports.clear();
        fireContentsChanged(this, 0, ports.size() - 1);
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
