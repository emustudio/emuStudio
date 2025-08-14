/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.drive;

public interface DriveListener {

    void driveSelect(boolean sel);

    void driveParamsChanged(DriveParameters parameters);
}
