/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.emustudio.plugins.device.simh.CpmUtils.cpmCommandLine;
import static net.emustudio.plugins.device.simh.CpmUtils.readCPMCommandLine;

abstract class AttachTape implements Command {
    private int lastStatus;

    @Override
    public final void reset(Control control) {
        lastStatus = 0;
    }

    @Override
    public final byte read(Control control) {
        byte result = (byte) lastStatus;
        control.clearCommand();
        return result;
    }

    @Override
    public final void start(Control control) {
        try {
            attach(control.getPaperTape(), readPath(control));
            lastStatus = 0;
        } catch (IOException | InvalidPathException e) {
            lastStatus = 1;
        }
        control.clearWriteCommand();
    }

    protected abstract void attach(PaperTapeContext tape, Path path) throws IOException;

    private Path readPath(Control control) {
        readCPMCommandLine(control.getMemory());
        int length = 0;
        while (length < cpmCommandLine.length && cpmCommandLine[length] != 0) {
            length++;
        }
        return Paths.get(new String(cpmCommandLine, 0, length));
    }
}
