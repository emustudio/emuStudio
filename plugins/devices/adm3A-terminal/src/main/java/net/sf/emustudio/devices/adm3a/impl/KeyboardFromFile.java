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
package net.sf.emustudio.devices.adm3a.impl;

import emulib.plugins.device.DeviceContext;
import net.sf.emustudio.devices.adm3a.InputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

class KeyboardFromFile implements InputProvider {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeyboardFromFile.class);

    private List<DeviceContext<Short>> inputObservers = new CopyOnWriteArrayList<>();
    private final File inputFile;

    KeyboardFromFile(File inputFile) throws FileNotFoundException {
        this.inputFile = Objects.requireNonNull(inputFile);
        if (!inputFile.canRead()) {
            throw new FileNotFoundException("Input file: '" + inputFile + "' cannot be found or cannot be read");
        }
    }

    void processInputFile(int delayInMilliseconds) {
        try(BufferedInputStream input = new BufferedInputStream(new FileInputStream(inputFile))) {
            int key;
            while ((key = input.read()) != -1) {
                notifyObservers((short) key);
                if (delayInMilliseconds > 0) {
                    LockSupport.parkNanos(delayInMilliseconds * 1000000);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not process input file", e);
        }
    }

    @Override
    public void addDeviceObserver(DeviceContext<Short> listener) {
        inputObservers.add(listener);
    }

    @Override
    public void removeDeviceObserver(DeviceContext<Short> listener) {
        inputObservers.remove(listener);
    }

    private void notifyObservers(short input) {
        for (DeviceContext<Short> observer : inputObservers) {
            try {
                observer.write(input);
            } catch (IOException e) {
                LOGGER.error("[observer={}, input={}] Could not notify observer about key hit", observer, input, e);
            }
        }
    }

    @Override
    public void destroy() {
        inputObservers.clear();
    }

}
