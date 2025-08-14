/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.ports;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;

import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

/**
 * Port 1, Status port.
 * <p>
 * IN: disk flags
 * OUT: select/unselect drive
 */
public class StatusPort implements Context8080.CpuPortDevice {
    private final DriveCollection disk;

    public StatusPort(DriveCollection disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public byte read(int portAddress) {
        return disk.getCurrentDrive().map(Drive::getPort1status).orElse(Drive.DEAD_DRIVE);
    }

    @Override
    public void write(int portAddress, byte value) {
        if ((value & 0x80) != 0) {
            disk.getCurrentDrive().ifPresent(Drive::deselect);
            disk.unsetCurrentDrive();
        } else {
            disk.setCurrentDrive(value & 0x0F);
            disk.getCurrentDrive().ifPresent(Drive::select);
        }
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return DIALOG_TITLE + " Status Port";
    }
}
