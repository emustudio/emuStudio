/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
        public int[] getAttributeMemory() {
            return new int[0];
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

    /**
     * Returns per-cell attribute memory. Each element is a packed attribute int
     * as defined by {@link net.emustudio.plugins.device.vt100.VideoAttribute}.
     * The array is parallel to {@link #getVideoMemory()}.
     *
     * @return packed attribute array
     */
    int[] getAttributeMemory();

    void rollUp();

    void rollDown();

    void write(byte data);
}
