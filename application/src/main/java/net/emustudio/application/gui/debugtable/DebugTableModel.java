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

package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public abstract class DebugTableModel extends AbstractTableModel {

    public abstract DebuggerColumn<?> getColumnAt(int index);

    public abstract void previousPage();

    public abstract void seekBackwardPage(int value);

    public abstract void firstPage();

    public abstract void nextPage();

    public abstract void seekForwardPage(int value);

    public abstract void lastPage();

    public abstract void currentPage();

    public abstract boolean isRowAtCurrentInstruction(int rowIndex);

    public abstract void memoryChanged(int from, int to);

    public abstract void setMemorySize(int memorySize);

    public abstract void setColumns(List<DebuggerColumn<?>> columns);

    public abstract void setDefaultColumns();

    public abstract int guessPreviousInstructionLocation();




    public static final DebugTableModel EMPTY = new DebugTableModel() {
        @Override
        public DebuggerColumn<?> getColumnAt(int index) {
            return null;
        }

        @Override
        public void previousPage() {

        }

        @Override
        public void seekBackwardPage(int value) {

        }

        @Override
        public void firstPage() {

        }

        @Override
        public void nextPage() {

        }

        @Override
        public void seekForwardPage(int value) {

        }

        @Override
        public void lastPage() {

        }

        @Override
        public void currentPage() {

        }

        @Override
        public boolean isRowAtCurrentInstruction(int rowIndex) {
            return false;
        }

        @Override
        public void memoryChanged(int from, int to) {

        }

        @Override
        public void setMemorySize(int memorySize) {

        }

        @Override
        public void setColumns(List<DebuggerColumn<?>> columns) {

        }

        @Override
        public void setDefaultColumns() {

        }

        @Override
        public int guessPreviousInstructionLocation() {
            return 0;
        }

        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public int getColumnCount() {
            return 0;
        }

        @Override
        public Object getValueAt(int i, int i1) {
            return null;
        }
    };
}
