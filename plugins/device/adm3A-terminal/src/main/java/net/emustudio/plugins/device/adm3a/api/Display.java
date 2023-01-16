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
package net.emustudio.plugins.device.adm3a.api;

import net.emustudio.plugins.device.adm3a.interaction.Cursor;

import java.awt.*;

/**
 * Display interface.
 */
public interface Display extends AutoCloseable, Cursor.LineRoller {

    /**
     * Dummy display (does nothing useful).
     */
    Display DUMMY = new Display() {
        @Override
        public void rollLine() {

        }

        private final Point point = new Point(0, 0);

        @Override
        public void write(byte data) {
        }

        @Override
        public Point getCursorPoint() {
            return point;
        }

        @Override
        public int getRows() {
            return 0;
        }

        @Override
        public int getColumns() {
            return 0;
        }

        @Override
        public char[] getVideoMemory() {
            return new char[0];
        }

        @Override
        public void clearScreen() {

        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
        }
    };

    void reset();

    void write(byte data);

    Point getCursorPoint();

    int getRows();

    int getColumns();

    char[] getVideoMemory();

    void clearScreen();
}
