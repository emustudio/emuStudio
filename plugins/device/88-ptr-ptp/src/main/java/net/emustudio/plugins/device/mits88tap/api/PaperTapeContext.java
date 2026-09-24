/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.Context;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** File-control port published by the Altair paper tape device. */
@PluginContext
public interface PaperTapeContext extends Context {
    void attachReader(Path path) throws IOException;

    void detachReader();

    void rewindReader();

    void attachPunch(Path path) throws IOException;

    void detachPunch();

    Optional<Path> getReaderPath();

    Optional<Path> getPunchPath();

    int getReaderPosition();

    int getReaderLength();
}
