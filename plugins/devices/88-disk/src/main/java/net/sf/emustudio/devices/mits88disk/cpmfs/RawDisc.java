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
    public final static int SECTOR_SIZE = 128; //137;
    public final static int SECTORS_PER_TRACK = 32; // 26, 32
    public final static int SECTOR_SKEW = 17;
    public final static int BLOCK_LENGTH = 1024; // 2048, 4096, 8192 and 16384
    //public final static int TRACKS_COUNT = 77;
    public final static int DIRECTORY_TRACK = 6;
    private final static int RAW_CHECKSUM_LENGTH = 9;

    private final Position position = new Position(0, 0);
    private final FileChannel channel;

    private final int[] skewTab;
    private final int rawSectorSize;
    private final int sectorSize;
    private final int sectorsPerTrack;
    private final int blockLength;
    private final int directoryTrack;

    public RawDisc(Path imageFile, int sectorSize, int sectorsPerTrack, int sectorSkew, int blockLength,
                   int directoryTrack, OpenOption... openOptions) throws IOException {
        this.rawSectorSize = sectorSize + RAW_CHECKSUM_LENGTH;
        this.sectorSize = sectorSize;
        this.blockLength = blockLength;
        this.sectorsPerTrack = sectorsPerTrack;
        this.skewTab = new int[sectorsPerTrack];
        this.directoryTrack = directoryTrack;

        int currentSkew = 0;
        for (int i = 0; i < sectorsPerTrack; i++) {
            while (true) {
                int k = 0;
                while (k < i && skewTab[k] != currentSkew) {
                    k++;
                }
                if (k < i) {
                    currentSkew = (currentSkew + 1) % sectorsPerTrack;
                } else {
                    break;
                }
            }
            skewTab[i] = currentSkew;
            currentSkew = (currentSkew + sectorSkew) % sectorsPerTrack;
        }

        this.channel = FileChannel.open(Objects.requireNonNull(imageFile), openOptions);
    }

    private void reset(int track, int sector) {
        position.reset(track, sector);
    }

    private ByteBuffer readSector() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(rawSectorSize);

        channel.position(sectorsPerTrack * rawSectorSize * position.track + rawSectorSize * skewTab[position.sector]);
        if (channel.read(buffer) != rawSectorSize) {
            throw new IOException("Could not read whole sector! (" + position + ")");
        }

        buffer.flip();
        return buffer.asReadOnlyBuffer();
    }

//    public void writeSector(ByteBuffer buffer) throws IOException {
//        channel.position(sectorsPerTrack * rawSectorSize * position.track + rawSectorSize * skewTab[position.sector]);
//
//        int expected = buffer.remaining();
//        if (channel.write(buffer) != expected) {
//            throw new IOException("Could not write whole sector! (" + position + ")");
//        }
//    }

    List<ByteBuffer> readBlock(int blockNumber) throws IOException {
        int sectorsPerBlock = blockLength / sectorSize;
        final int sector = (blockNumber * sectorsPerBlock + sectorsPerTrack * directoryTrack) % sectorsPerTrack;
        int track = (blockNumber * sectorsPerBlock + sectorsPerTrack * directoryTrack) / sectorsPerTrack;

        reset(track, sector);
        List<ByteBuffer> block = new ArrayList<>();
        for (int counter = 0; counter < sectorsPerBlock; counter++) {
            block.add(readSector());
            position.sector++;
            if (position.sector >= sectorsPerTrack) {
                reset(track + 1, 0);
                track++;
            }
        }
        return block;
    }

    @Override
    public void close() throws Exception {
        channel.close();
    }

}
