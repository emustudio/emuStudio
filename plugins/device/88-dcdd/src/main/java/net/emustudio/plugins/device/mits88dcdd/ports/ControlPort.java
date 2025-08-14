/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.ports;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88dcdd.drive.Drive;
import net.emustudio.plugins.device.mits88dcdd.drive.DriveCollection;

import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

/**
 * Port 2, Control port.
 * <p>
 * IN: sector pos
 * OUT: set flags
 */
public class ControlPort implements Context8080.CpuPortDevice {
    private final DriveCollection disk;

    public ControlPort(DriveCollection disk) {
        this.disk = Objects.requireNonNull(disk);
    }

    @Override
    public byte read(int portAddress) {
        return disk.getCurrentDrive().map(drive -> {
            drive.nextSectorIfHeadIsLoaded();
            return drive.getPort2status();
        }).orElse(Drive.SECTOR0);
    }

    @Override
    public void write(int portAddress, byte val) {
        disk.getCurrentDrive().ifPresent(drive -> drive.writeToPort2(val));
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return DIALOG_TITLE + " Control Port";
    }
}
