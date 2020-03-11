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
package net.emustudio.plugins.devices.brainduck.terminal.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class FileIOProvider implements InputProvider, OutputProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileIOProvider.class);
    private static final File OUTPUT_FILE_NAME = new File("brainduck-terminal.out");
    private static final File INPUT_FILE_NAME = new File("brainduck-terminal.in");

    private final Reader reader;
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
                return EOF;
            }
            return character;
        } catch (IOException e) {
            LOGGER.error("Could not read from input file: " + INPUT_FILE_NAME, e);
            return EOF;
        }
    }

    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            LOGGER.warn("Could not close writer", e);
        }
        try {
            reader.close();
        } catch (IOException e) {
            LOGGER.warn("Could not close reader", e);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void showGUI() {
    }

}
