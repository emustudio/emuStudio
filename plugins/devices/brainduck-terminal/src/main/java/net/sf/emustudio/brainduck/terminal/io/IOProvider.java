/*
 * KISS, DRY, YAGNI
 *
 * Copyright (C) 2014 Peter Jakubčo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.emustudio.brainduck.terminal.io;

import java.io.Closeable;

public interface IOProvider extends Closeable {
    public static IOProvider DUMMY = new IOProvider() {

        @Override
        public void write(int c) {
        }

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() {
        }

        @Override
        public void reset() {
        }

        @Override
        public void showGUI() {
        }
    };
    
    /**
     * Clears the screen.
     */
    public void reset();

    /**
     * Writes a char to the screen.
     *
     * @param c the character to be written
     */
    public void write(int c);

    /**
     * Get a char from the input buffer.
     * 
     * If it is empty, waits for user input. If the input cannot be read,
     * returns -1.
     *
     * @return char from the input buffer
     */
    public int read();
    
    /**
     * Show GUI if supported.
     */
    public void showGUI();
    
}
