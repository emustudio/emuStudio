/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88dcdd.drive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class DriveCollection implements Iterable<Drive> {
    private final static int DRIVES_COUNT = 16;
    private final List<Drive> drives = new ArrayList<>();

    private volatile int currentDrive;

    public DriveCollection() {
        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive(i));
        }

        this.currentDrive = DRIVES_COUNT;
    }

    public void destroy() {
        drives.clear();
    }

    public Optional<Drive> getCurrentDrive() {
        return (currentDrive >= DRIVES_COUNT || currentDrive < 0) ?
            Optional.empty() : Optional.of(drives.get(currentDrive));
    }

    public void setCurrentDrive(int index) {
        if (index < 0 || index >= DRIVES_COUNT) {
            throw new IllegalArgumentException("Index of drive must be between 0 and " + DRIVES_COUNT);
        }
        currentDrive = index;
    }

    public void unsetCurrentDrive() {
        currentDrive = DRIVES_COUNT;
    }

    public Iterator<Drive> iterator() {
        return drives.iterator();
    }

    public Drive get(int index) {
        return drives.get(index);
    }

    public void foreach(BiFunction<Integer, Drive, Void> function) {
        int i = 0;
        for (Drive drive : drives) {
            function.apply(i, drive);
            i++;
        }
    }
}
