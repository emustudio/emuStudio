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

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88dcdd.DiskSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

public class DriveCollection implements Iterable<Drive> {
    private final static Logger LOGGER = LoggerFactory.getLogger(DriveCollection.class);

    public final static int DRIVES_COUNT = 16;
    private final List<Drive> drives = new ArrayList<>();

    private final Context8080 cpu;
    private final DiskSettings settings;
    private final Dialogs dialogs;

    private Optional<Integer> attachedCpuPort1 = Optional.empty();
    private Optional<Integer> attachedCpuPort2 = Optional.empty();
    private Optional<Integer> attachedCpuPort3 = Optional.empty();

    private volatile int currentDrive;
    private final AtomicReference<DeviceContext<Byte>> lastAttachedPort1 = new AtomicReference<>();
    private final AtomicReference<DeviceContext<Byte>> lastAttachedPort2 = new AtomicReference<>();
    private final AtomicReference<DeviceContext<Byte>> lastAttachedPort3 = new AtomicReference<>();

    public DriveCollection(Context8080 cpu, DiskSettings settings, Dialogs dialogs) {
        this.cpu = Objects.requireNonNull(cpu);
        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);

        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive(i, cpu, settings::getInterruptVector));
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

    public void reattach() throws PluginInitializationException {
        DeviceContext<Byte> port1 = lastAttachedPort1.get();
        DeviceContext<Byte> port2 = lastAttachedPort2.get();
        DeviceContext<Byte> port3 = lastAttachedPort3.get();
        if (port1 != null && port2 != null && port3 != null) {
            attach(port1, port2, port3);
        }
    }

    public void attach(DeviceContext<Byte> port1, DeviceContext<Byte> port2, DeviceContext<Byte> port3) throws PluginInitializationException {
        detach();
        int port1cpu = settings.getPort1CPU();
        int port2cpu = settings.getPort2CPU();
        int port3cpu = settings.getPort3CPU();

        if (!cpu.attachDevice(port1, port1cpu)) {
            throw new PluginInitializationException(
                ": " + DIALOG_TITLE + " (port 1) can not be attached to default CPU port " + port1cpu
            );
        }
        attachedCpuPort1 = Optional.of(port1cpu);
        lastAttachedPort1.set(port1);

        if (!cpu.attachDevice(port2, port2cpu)) {
            throw new PluginInitializationException(
                ": " + DIALOG_TITLE + " (port 2) can not be attached to default CPU port " + port2cpu
            );
        }
        attachedCpuPort2 = Optional.of(port2cpu);
        lastAttachedPort2.set(port2);

        if (!cpu.attachDevice(port3, port3cpu)) {
            throw new PluginInitializationException(
                ": " + DIALOG_TITLE + " (port 3) can not be attached to default CPU port " + port3cpu
            );
        }
        attachedCpuPort3 = Optional.of(port3cpu);
        lastAttachedPort3.set(port3);
    }

    public void detach() {
        attachedCpuPort1.ifPresent(cpu::detachDevice);
        attachedCpuPort2.ifPresent(cpu::detachDevice);
        attachedCpuPort3.ifPresent(cpu::detachDevice);
    }

    public void reset() {
        foreach((i, drive) -> {
            DiskSettings.DriveSettings driveSettings = settings.getDriveSettings(i);
            drive.setDriveSettings(driveSettings);

            Optional
                .ofNullable(driveSettings.imagePath)
                .map(Path::of)
                .ifPresent(path -> {
                    try {
                        drive.mount(path);
                    } catch (IOException ex) {
                        LOGGER.error("Could not mount image file {}", path, ex);
                        dialogs.showError(
                            "Could not mount image file: " + path + ". Please see log file for more details.",
                            "MITS 88-DCDD"
                        );
                    }
                });
            return null;
        });
    }
}
