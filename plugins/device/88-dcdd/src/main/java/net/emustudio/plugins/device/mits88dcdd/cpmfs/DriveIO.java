/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;

public class DriveIO implements AutoCloseable {
    public final static int SECTOR_SIZE = 128; //137;
    public final static int SECTORS_PER_TRACK = 32; // 26, 32
    public final static int SECTOR_SKEW = 17;
    //public final static int TRACKS_COUNT = 77;
    private final static int RAW_CHECKSUM_LENGTH = 9;

    private final FileChannel channel;

    private final int[] skewTab;
    private final int rawSectorSize;
    final int sectorSize;
    final int sectorsPerTrack;


    public DriveIO(Path imageFile, int sectorSize, int sectorsPerTrack, int sectorSkew, OpenOption... openOptions) throws IOException {
        this.rawSectorSize = sectorSize + RAW_CHECKSUM_LENGTH;
        this.sectorSize = sectorSize;
        this.sectorsPerTrack = sectorsPerTrack;
        this.skewTab = new int[sectorsPerTrack];

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

    public ByteBuffer readSector(Position position) throws IOException {
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

    @Override
    public void close() throws Exception {
        channel.close();
    }
}
