/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.brainduck.terminal.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public class FileIOProvider implements InputProvider, OutputProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileIOProvider.class);
    private static final File OUTPUT_FILE_NAME = new File("BrainTerminal.out");
    private static final File INPUT_FILE_NAME = new File("BrainTerminal.in");

    private final Reader reader;
    private final FileWriter writer;

    public FileIOProvider() throws IOException {
        if (INPUT_FILE_NAME.exists()) {
            this.reader = new FileReader(INPUT_FILE_NAME);
        } else {
            this.reader = new Reader() {
                @Override
                public int read(char[] cbuf, int off, int len) throws IOException {
                    return -1;
                }

                @Override
                public void close() throws IOException {

                }
            };
        }
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
