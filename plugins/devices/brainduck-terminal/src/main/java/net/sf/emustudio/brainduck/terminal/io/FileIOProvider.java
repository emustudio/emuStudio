/*
 * Copyright (C) 2014 Peter Jakubčo
 *
 * KISS, DRY, YAGNI
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

import emulib.runtime.LoggerFactory;
import emulib.runtime.interfaces.Logger;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileIOProvider implements IOProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileIOProvider.class);
    private static final String OUTPUT_FILE_NAME = "BrainTerminal.out";
    private static final String INPUT_FILE_NAME = "BrainTerminal.in";

    private final FileReader reader;
    private final FileWriter writer;

    public FileIOProvider() throws IOException {
        this.reader = new FileReader(INPUT_FILE_NAME);
        try {
            this.writer = new FileWriter(OUTPUT_FILE_NAME);
        } catch (IOException e) {
            reader.close();
            throw e;
        }
    }

    @Override
    public void write(int c) {
        try {
            writer.write(c);
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Could not write to output file: " + OUTPUT_FILE_NAME, e);
        }
    }

    @Override
    public int read() {
        try {
            int character = reader.read();
            if (character == -1) {
                return IOProvider.EOF;
            }
            return character;
        } catch (IOException e) {
            LOGGER.error("Could not read from input file: " + INPUT_FILE_NAME, e);
            return IOProvider.EOF;
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            LOGGER.warning("Could not close writer", e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            LOGGER.warning("Could not close reader", e);
        }
    }

    @Override
    public void reset() {
        try {
            reader.reset();
        } catch (IOException e) {
            LOGGER.error("Could not reset reader", e);
        }
    }

    @Override
    public void showGUI() {
    }

}
