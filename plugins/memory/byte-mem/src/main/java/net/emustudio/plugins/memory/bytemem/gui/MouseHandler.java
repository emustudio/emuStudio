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
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.plugins.memory.bytemem.gui.model.MemoryTableModel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Objects;

public class MouseHandler extends MouseAdapter {
    private final MemoryTableModel tableModel;
    private final Runnable updateMemoryValue;

    public MouseHandler(MemoryTableModel tableModel, Runnable updateMemoryValue) {
        this.tableModel = Objects.requireNonNull(tableModel);
        this.updateMemoryValue = Objects.requireNonNull(updateMemoryValue);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int page = tableModel.getPage();
        int rotation = e.getWheelRotation();
        try {
            tableModel.setPage(page + rotation);
        } catch (IndexOutOfBoundsException ignored) {
            if (rotation < 0) {
                tableModel.setPage(tableModel.getPageCount() - 1);
            } else {
                tableModel.setPage(0);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mousePressed(e);
        updateMemoryValue.run();
    }
}
