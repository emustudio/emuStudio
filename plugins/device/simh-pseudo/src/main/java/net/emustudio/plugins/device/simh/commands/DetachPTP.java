/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

public class DetachPTP implements Command {
    public final static DetachPTP INS = new DetachPTP();

    @Override
    public void start(Control control) {
        //detach_unit( & ptp_unit);
        control.clearCommand();
    }
}
