/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem.STATUS_UNUSED;

/**
 * Drive raw I/O
 * <p>
 * Performs raw disk operations
 */
public class DriveIO implements AutoCloseable {
    public final CpmFormat cpmFormat;
    public final SectorOps sectorOps;

    private final FileChannel channel;

    public DriveIO(Path imageFile, CpmFormat cpmFormat, OpenOption... openOptions) throws IOException {
        this.cpmFormat = Objects.requireNonNull(cpmFormat);
        this.sectorOps = cpmFormat.sectorOps;
        this.channel = FileChannel.open(Objects.requireNonNull(imageFile), openOptions);
    }

    /**
     * Reads a CP/M "record".
     * <p>
     * It is a raw sector stripped from prefix & suffix.
     * It does not check validity.
     *
     * @param position logical position
     * @return record data
     * @throws IOException on reading error
     */
    public ByteBuffer readRecord(Position position) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(cpmFormat.sectorSize);
        channel.position(cpmFormat.positionToOffset(position));
        if (channel.read(buffer) != cpmFormat.sectorSize) {
            throw new IOException("Could not read whole sector! (" + position + ")");
        }
        return sectorOps.toRecord(buffer.flip());
    }

    /**
     * Writes a CP/M "record"
     *
     * @param position logical position
     * @param data     record data
     * @throws IOException              on writing error
     * @throws IllegalArgumentException if sector data does not have expected size (sector size)
     */
    public void writeRecord(Position position, ByteBuffer data) throws IOException {
        ByteBuffer sector = sectorOps.toSector(data, position);
        channel.position(cpmFormat.positionToOffset(position));

        int expected = sector.remaining();
        if (channel.write(sector) != expected) {
            throw new IOException("Could not write whole sector! (" + position + ")");
        }
    }

    /**
     * Reads a block
     *
     * @param blockNumber block number
     * @return list of records in a block
     * @throws IOException on reading error
     */
    public List<ByteBuffer> readBlock(int blockNumber) throws IOException {
        Position position = cpmFormat.blockToPosition(blockNumber);
        List<ByteBuffer> block = new ArrayList<>();
        for (int counter = 0; counter < cpmFormat.recordsPerBlock; counter++) {
            block.add(readRecord(position));
            position.next(cpmFormat.dpb.spt);
        }
        return block;
    }

    /**
     * Writes a block
     *
     * @param blockNumber block number
     * @param records     list of records
     * @throws IOException on writing error
     */
    public void writeBlock(int blockNumber, List<ByteBuffer> records) throws IOException {
        if (records.size() > cpmFormat.recordsPerBlock) {
            throw new IllegalArgumentException("Too many sectors per block");
        }

        Position position = cpmFormat.blockToPosition(blockNumber);
        for (ByteBuffer record : records) {
            writeRecord(position, record);
            position.next(cpmFormat.dpb.spt);
        }
    }

    @Override
    public void close() throws Exception {
        channel.close();
    }

    /**
     * Formats disk (creates new disk image file).
     *
     * @param imageFile disk image file name
     * @param cpmFormat CP/M format
     */
    public static void format(Path imageFile, CpmFormat cpmFormat) throws IOException {
        if (Files.exists(imageFile)) {
            throw new IllegalArgumentException("File already exists");
        }
        int fileSize = cpmFormat.tracks * cpmFormat.sectorSize * cpmFormat.dpb.spt;
        System.out.println("File size: " + fileSize);
        System.out.println("Sector size: " + cpmFormat.sectorSize);
        System.out.println("Sectors per track: " + cpmFormat.dpb.spt);
        System.out.println("Tracks: " + cpmFormat.tracks);

        try (FileChannel channel = FileChannel.open(imageFile, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
            MappedByteBuffer out = channel.map(READ_WRITE, 0, fileSize);
            for (int i = 0; i < fileSize; i++) {
                out.put((byte) STATUS_UNUSED);
            }
        }
    }
}
