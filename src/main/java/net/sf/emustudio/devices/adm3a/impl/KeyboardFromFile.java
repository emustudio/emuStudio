/*
 * KeyboardFromFile.java
 *
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import net.sf.emustudio.devices.adm3a.InputProvider;

/**
 *
 * @author vbmacher
 */
public class KeyboardFromFile implements InputProvider {
    private List<DeviceContext<Short>> inputObservers;
    private File inputFile;

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public void processInputFile() throws FileNotFoundException, IOException {
        if (inputFile == null) {
            return;
        }
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(inputFile));
        try {
            int key;
            while ((key = input.read()) != -1) {
                notifyObservers((short)key);
            }
        } finally {
            input.close();
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
            observer.write(input);
        }
    }

    @Override
    public void destroy() {
        inputObservers.clear();
    }

}
