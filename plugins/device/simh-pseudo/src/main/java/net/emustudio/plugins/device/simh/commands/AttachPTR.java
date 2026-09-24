/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;

import java.io.IOException;
import java.nio.file.Path;

public class AttachPTR extends AttachTape {
    public final static AttachPTR INS = new AttachPTR();

    @Override
    protected void attach(PaperTapeContext tape, Path path) throws IOException {
        tape.attachReader(path);
    }
}
