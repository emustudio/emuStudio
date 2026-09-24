/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class HardDisk implements AutoCloseable {
    static final int DEFAULT_SECTOR_SIZE = 128;
    static final int DEFAULT_SECTORS_PER_TRACK = 32;
    static final int DEFAULT_TRACKS = 2048;

    private int sectorSize = DEFAULT_SECTOR_SIZE;
    private int sectorsPerTrack = DEFAULT_SECTORS_PER_TRACK;
    private SeekableByteChannel image;
    private Path imagePath;
    private String activity = "Detached";

    void configure(int sectorSize, int sectorsPerTrack) {
        if (sectorSize < 128 || sectorSize > 1024 || Integer.bitCount(sectorSize) != 1) {
            throw new IllegalArgumentException("Sector size must be a power of two between 128 and 1024");
        }
        if (sectorsPerTrack < 1 || sectorsPerTrack > 255) {
            throw new IllegalArgumentException("Sectors per track must be between 1 and 255");
        }
        this.sectorSize = sectorSize;
        this.sectorsPerTrack = sectorsPerTrack;
    }

    void attach(Path path) throws IOException {
        close();
        image = java.nio.file.Files.newByteChannel(path,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        imagePath = path.toAbsolutePath();
        activity = "Idle";
    }

    boolean isAttached() {
        return image != null;
    }

    Path getImagePath() {
        return imagePath;
    }

    int getSectorSize() {
        return sectorSize;
    }

    int getSectorsPerTrack() {
        return sectorsPerTrack;
    }

    int getTracks() throws IOException {
        if (image == null || image.size() == 0) {
            return DEFAULT_TRACKS;
        }
        long trackSize = (long) sectorsPerTrack * sectorSize;
        return (int) Math.min(0x10000L, (image.size() + trackSize - 1) / trackSize);
    }

    long getCapacity() throws IOException {
        return image == null || image.size() == 0
                ? (long) DEFAULT_TRACKS * sectorsPerTrack * sectorSize
                : image.size();
    }

    String getActivity() {
        return activity;
    }

    byte[] read(int track, int sector) throws IOException {
        byte[] result = new byte[sectorSize];
        java.util.Arrays.fill(result, (byte) 0xE5);
        long offset = offset(track, sector);
        if (offset < image.size()) {
            image.position(offset);
            ByteBuffer target = ByteBuffer.wrap(result);
            while (target.hasRemaining() && image.read(target) >= 0) {
                // Short raw images represent unwritten sectors; remaining bytes stay E5h.
            }
        }
        activity = "Read T" + track + " S" + sector;
        return result;
    }

    void write(int track, int sector, byte[] data) throws IOException {
        image.position(offset(track, sector));
        ByteBuffer source = ByteBuffer.wrap(data, 0, sectorSize);
        while (source.hasRemaining()) {
            image.write(source);
        }
        activity = "Write T" + track + " S" + sector;
    }

    private long offset(int track, int sector) {
        return ((long) track * sectorsPerTrack + sector) * sectorSize;
    }

    @Override
    public void close() throws IOException {
        if (image != null) {
            image.close();
            image = null;
        }
        imagePath = null;
        activity = "Detached";
    }
}
