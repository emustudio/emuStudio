/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88disk.cpmfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RawDisc implements AutoCloseable {
    public final static int SECTOR_SIZE = 137;
    public final static int SECTORS_COUNT = 32; // 26, 32
    public final static int SECTOR_SKEW = 17;
    public final static int BLOCK_LENGTH = 1024; // 2048, 4096, 8192 and 16384

    private final TrackAndSector position = new TrackAndSector(0, 0);
    private final FileChannel channel;

    public RawDisc(Path imageFile, OpenOption... openOptions) throws IOException {
        this.channel = FileChannel.open(Objects.requireNonNull(imageFile), openOptions);
    }

    public void reset() {
        reset(0, 0);
    }

    public void reset(int track) {
        reset(track, 0);
    }

    public void reset(int track, int sector) {
        position.track = track;
        position.sector = sector;
    }

    public ByteBuffer readSector() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(SECTOR_SIZE);

        channel.position(SECTORS_COUNT * SECTOR_SIZE * position.track + SECTOR_SIZE * position.sector);
        if (channel.read(buffer) != SECTOR_SIZE) {
            throw new IOException("Could not read whole sector! (" + position + ")");
        }

        buffer.flip();
        return buffer.asReadOnlyBuffer();
    }

    public void writeSector(ByteBuffer buffer) throws IOException {
        channel.position(SECTORS_COUNT * SECTOR_SIZE * position.track + SECTOR_SIZE * position.sector);

        int expected = buffer.remaining();
        if (channel.write(buffer) != expected) {
            throw new IOException("Could not write whole sector! (" + position + ")");
        }
    }

    public List<ByteBuffer> readBlock() throws IOException {
        int numberOfSectors = BLOCK_LENGTH / SECTOR_SIZE;

        List<ByteBuffer> block = new ArrayList<>();
        for (int i = 0; i < numberOfSectors; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(SECTOR_SIZE);

            channel.position(SECTORS_COUNT * SECTOR_SIZE * position.track + SECTOR_SIZE * position.sector);
            channel.read(buffer);
            buffer.flip();
            block.add(buffer.asReadOnlyBuffer());

            position.sector = (position.sector + SECTOR_SKEW) % SECTORS_COUNT;
        }
        return block;
    }

    @Override
    public void close() throws Exception {
        channel.close();
    }
}
