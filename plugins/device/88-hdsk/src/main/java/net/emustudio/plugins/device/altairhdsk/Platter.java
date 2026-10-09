/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/** Raw sectors in cylinder, surface, sector order, compatible with SIMH MHDSK. */
final class Platter implements AutoCloseable {
    static final int CYLINDERS = 406;
    static final int SECTORS = 24;
    static final int SECTOR_SIZE = 256;
    static final long CAPACITY = (long) CYLINDERS * 2 * SECTORS * SECTOR_SIZE;
    final Path path;
    final boolean readOnly;
    private final RandomAccessFile file;

    Platter(Path path, boolean readOnly) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) != CAPACITY) {
            throw new IOException("MHDSK platter must contain exactly " + CAPACITY + " bytes");
        }
        this.path = path;
        this.readOnly = readOnly;
        file = new RandomAccessFile(path.toFile(), readOnly ? "r" : "rw");
    }

    static void create(Path path) throws IOException {
        Files.createFile(path); // Never replace an existing image.
        try (RandomAccessFile image = new RandomAccessFile(path.toFile(), "rw")) {
            image.setLength(CAPACITY);
        }
    }

    void read(int cylinder, int surface, int sector, byte[] buffer) throws IOException {
        file.seek(offset(cylinder, surface, sector));
        file.readFully(buffer); // Truncated media is an error, never fabricated sector data.
    }

    void write(int cylinder, int surface, int sector, byte[] buffer) throws IOException {
        if (readOnly) { throw new IOException("Platter is write protected"); }
        if (file.length() != CAPACITY) { throw new EOFException("Platter was truncated"); }
        file.seek(offset(cylinder, surface, sector));
        file.write(buffer);
    }

    private long offset(int cylinder, int surface, int sector) {
        return ((long) cylinder * 2 + surface) * SECTORS * SECTOR_SIZE + (long) sector * SECTOR_SIZE;
    }

    @Override public void close() throws IOException { file.close(); }
}
