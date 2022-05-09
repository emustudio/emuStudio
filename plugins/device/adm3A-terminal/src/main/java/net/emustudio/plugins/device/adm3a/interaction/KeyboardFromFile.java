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
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.emulib.plugins.device.DeviceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

public class KeyboardFromFile implements Keyboard {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeyboardFromFile.class);

    private final List<DeviceContext<Byte>> devices = new CopyOnWriteArrayList<>();
    private final Path inputFile;
    private final int delayInMilliseconds;

    public KeyboardFromFile(Path inputFile, int delayInMilliseconds) {
        this.inputFile = Objects.requireNonNull(inputFile);
        this.delayInMilliseconds = delayInMilliseconds;
    }

    @Override
    public void connect(DeviceContext<Byte> device) {
        Optional.ofNullable(device).ifPresent(devices::add);
    }

    @Override
    public void disconnect(DeviceContext<Byte> device) {
        Optional.ofNullable(device).ifPresent(devices::remove);
    }

    @Override
    public void process() {
        LOGGER.info("Processing input file: '" + inputFile + "'; delay of chars read (ms): " + delayInMilliseconds);
        try (InputStream input = new FileInputStream(inputFile.toFile())) {
            int key;
            while ((key = input.read()) != -1) {
                inputReceived(key);
                if (delayInMilliseconds > 0) {
                    LockSupport.parkNanos(delayInMilliseconds * 1000000L);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not process input file", e);
        }
    }

    private void inputReceived(int input) {
        for (DeviceContext<Byte> device : devices) {
            device.writeData((byte) input);
        }
    }

    @Override
    public void destroy() {
        devices.clear();
    }
}
