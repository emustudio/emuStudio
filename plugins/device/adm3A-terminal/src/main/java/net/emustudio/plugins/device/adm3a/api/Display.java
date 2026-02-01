/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
