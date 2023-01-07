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
package net.emustudio.plugins.device.brainduck.terminal.interaction;

import net.emustudio.plugins.device.brainduck.terminal.api.Keyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

public class KeyboardFromFile extends Keyboard {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeyboardFromFile.class);

    private final long delayNanos;
    private final Path inputFile;

    public KeyboardFromFile(Path inputFile, int delayInMilliseconds) {
        this.delayNanos = delayInMilliseconds * 1000000L;
        this.inputFile = Objects.requireNonNull(inputFile);
    }

    @Override
    public void process() {
        try (FileInputStream in = new FileInputStream(inputFile.toFile())) {
            int key;
            while ((key = in.read()) != -1) {
                notifyOnKey((byte) key);
                if (delayNanos > 0) {
                    LockSupport.parkNanos(delayNanos);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not process input file", e);
        }
    }
}
