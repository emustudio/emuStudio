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
package net.emustudio.plugins.device.vt100.api;

import java.awt.*;

/**
 * Display interface.
 */
public interface Display extends AutoCloseable {

    /**
     * Dummy display (does nothing useful).
     */
    Display DUMMY = new Display() {

        @Override
        public void write(byte data) {
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
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
        public Point getCursorPoint() {
            return null;
        }

        @Override
        public char[] getVideoMemory() {
            return new char[0];
        }

        @Override
        public void rollUp() {
        }

        @Override
        public void rollDown() {
        }
    };

    void reset();

    int getRows();

    int getColumns();

    Point getCursorPoint();

    char[] getVideoMemory();

    void rollUp();

    void rollDown();

    void write(byte data);
}
