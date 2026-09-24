/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

final class MdsDrive implements AutoCloseable {
    static final int TRACKS = 35;
    static final int SECTORS_PER_TRACK = 16;
    static final int SECTOR_SIZE = 137;
    static final long IMAGE_SIZE = (long) TRACKS * SECTORS_PER_TRACK * SECTOR_SIZE;

    private final byte[] sectorBuffer = new byte[SECTOR_SIZE];
    private SeekableByteChannel image;
    private Path imagePath;
    private int track;
    private int sector = -1;
    private int byteOffset = SECTOR_SIZE;
    private int flags;
    private boolean sectorTrue;
    private boolean dirty;

    void attach(Path path) throws IOException {
        if (!Files.isRegularFile(path) || !Files.isReadable(path) || !Files.isWritable(path)) {
            throw new IOException("Minidisk image must be a readable, writable file");
        }
        if (Files.size(path) < IMAGE_SIZE) {
            throw new IOException("Minidisk image must contain at least " + IMAGE_SIZE + " bytes");
        }
        close();
        Set<OpenOption> options = Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE);
        image = Files.newByteChannel(path, options);
        imagePath = path.toAbsolutePath();
        reset();
    }

    boolean isAttached() {
        return image != null;
    }

    Path getImagePath() {
        return imagePath;
    }

    int getTrack() {
        return track;
    }

    int getSector() {
        return Math.max(0, sector);
    }

    int status() {
        return (~flags) & 0xFF;
    }

    void select() throws IOException {
        flush();
        sector = -1;
        byteOffset = SECTOR_SIZE;
        sectorTrue = false;
        flags = 0x9E; // movement allowed, head loaded, read available; minidisk motor starts on select
        if (track == 0) {
            flags |= 0x40;
        }
    }

    void deselect() throws IOException {
        flush();
        flags = 0;
    }

    int readSectorPosition() throws IOException {
        flush();
        sectorTrue = !sectorTrue;
        if (!sectorTrue) {
            sector = (sector + 1) % SECTORS_PER_TRACK;
            byteOffset = SECTOR_SIZE;
        }
        return ((sector << 1) & 0x1E) | 0xC0 | (sectorTrue ? 1 : 0);
    }

    void control(int value) throws IOException {
        if ((value & 0x01) != 0) {
            flush();
            track = Math.min(TRACKS - 1, track + 1);
            flags &= ~0x40;
            resetSectorPosition();
        }
        if ((value & 0x02) != 0) {
            flush();
            track = Math.max(0, track - 1);
            if (track == 0) {
                flags |= 0x40;
            }
            resetSectorPosition();
        }
        // 88-MDS loads its head when selected and ignores the DCDD head-unload command.
        if ((value & 0x80) != 0) {
            byteOffset = 0;
            dirty = false;
            flags |= 0x01;
        }
    }

    int readData() throws IOException {
        if (sector < 0) {
            return 0;
        }
        if (byteOffset >= SECTOR_SIZE) {
            readSector();
            byteOffset = 0;
        }
        return sectorBuffer[byteOffset++] & 0xFF;
    }

    void writeData(int value) throws IOException {
        if (sector < 0 || byteOffset >= SECTOR_SIZE) {
            return;
        }
        sectorBuffer[byteOffset++] = (byte) value;
        dirty = true;
        if (byteOffset == SECTOR_SIZE) {
            flush();
            flags &= ~0x01;
        }
    }

    void reset() {
        track = 0;
        sector = -1;
        byteOffset = SECTOR_SIZE;
        flags = 0;
        sectorTrue = false;
        dirty = false;
    }

    private void resetSectorPosition() {
        sector = -1;
        byteOffset = SECTOR_SIZE;
        sectorTrue = false;
    }

    private long imageOffset() {
        return ((long) track * SECTORS_PER_TRACK + sector) * SECTOR_SIZE;
    }

    private void readSector() throws IOException {
        ByteBuffer target = ByteBuffer.wrap(sectorBuffer);
        image.position(imageOffset());
        while (target.hasRemaining()) {
            if (image.read(target) < 0) {
                throw new IOException("Unexpected end of minidisk image");
            }
        }
    }

    private void flush() throws IOException {
        if (!dirty || sector < 0 || image == null) {
            return;
        }
        ByteBuffer source = ByteBuffer.wrap(sectorBuffer);
        image.position(imageOffset());
        while (source.hasRemaining()) {
            image.write(source);
        }
        dirty = false;
    }

    @Override
    public void close() throws IOException {
        flush();
        if (image != null) {
            image.close();
            image = null;
        }
        imagePath = null;
        reset();
    }
}
