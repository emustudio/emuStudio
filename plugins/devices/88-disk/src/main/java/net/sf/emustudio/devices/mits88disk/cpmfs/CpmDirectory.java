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

import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NotThreadSafe
public class CpmDirectory {
    private final static int ENTRY_SIZE = 32;
//    public final static int DIRECTORY_ENTRIES = 256;
    private final static int RECORD_BYTES = 128;
    public final static boolean BLOCKS_ARE_TWO_BYTES = false;

    private final RawDisc disc;
    private final boolean blocksAreTwoBytes; // TODO: capacity blocks < 256 ? false : true;

    public CpmDirectory(RawDisc disc, boolean blocksAreTwoBytes) {
        this.disc = Objects.requireNonNull(disc);
        this.blocksAreTwoBytes = blocksAreTwoBytes;
    }

    private List<CpmFile> readAllFiles() throws IOException {
        List<ByteBuffer> directorySectors = disc.readBlock(0);
        List<ByteBuffer> entries = getEntries(directorySectors);
        return getFilesFromEntries(entries);
    }

    public List<CpmFile> filterValidFiles() throws IOException {
        return readAllFiles().stream()
            .filter(file -> file.status < 32)
            .filter(file -> file.extentNumber == 0)
            .collect(Collectors.toList());
    }

    public String findDiscLabel() throws IOException {
        for (CpmFile file : readAllFiles()) {
            if (file.status == 32) {
                return file.fileName + file.fileExt;
            }
        }
        return "";
    }

    private static List<ByteBuffer> getEntries(List<ByteBuffer> directorySectors) {
        List<ByteBuffer> entries = new ArrayList<>();

        for (ByteBuffer sector : directorySectors) {
            sector.position(3);
            int numberOfEntries = sector.remaining() / ENTRY_SIZE;

            for (int i = 0; i < numberOfEntries; i++) {
                byte[] entry = new byte[ENTRY_SIZE];
                sector.get(entry);

                entries.add(ByteBuffer.wrap(entry).asReadOnlyBuffer());
            }
        }
        return entries;
    }

    private static List<CpmFile> getFilesFromEntries(List<ByteBuffer> entries) {
        return entries.stream().map(CpmFile::fromEntry).collect(Collectors.toList());
    }

    public void catFile(String fileName) throws IOException {
        List<CpmFile> foundExtents = readAllFiles().stream()
            .filter(file -> file.status < 32)
            .filter(file -> file.toString().equals(fileName))
            .collect(Collectors.toList());

        if (foundExtents.isEmpty()) {
            System.err.println("File was not found");
            return;
        }

        int maxBlocksCount = 16;
        if (blocksAreTwoBytes) {
            maxBlocksCount /= 2;
        }

        foundExtents.sort(Comparator.comparingInt(o -> o.extentNumber));
        int expectingExtentNumber = 0;
        for (CpmFile extent : foundExtents) {
            if (extent.extentNumber != expectingExtentNumber) {
                System.err.println(
                    String.format("[file=%s, extent=%d, expectingExtent=%d] ERROR: Expecting different extent number!",
                        fileName, extent.extentNumber, expectingExtentNumber)
                );
                break;
            }

            int recordsLeft = extent.rc & 0xFF;
            for (int i = 0; i < maxBlocksCount; i++) {
                int nextBlock = extent.blockPointers.get(i) & 0xFF;
                if (blocksAreTwoBytes) {
                    i++;
                    nextBlock = (nextBlock << 8) | (extent.blockPointers.get(i) & 0xFF);
                }
                if (nextBlock == 0) {
                    break;
                }

                List<ByteBuffer> records = disc.readBlock(nextBlock);
                int recordsCount = records.size();

                records.stream()
                    .limit(recordsLeft)
                    .forEach(b -> printContent(b, extent.bc & 0xFF));

                recordsLeft -= recordsCount;
            }
            expectingExtentNumber++;
        }
    }

    private void printContent(ByteBuffer buffer, int extentBc) {
        buffer.position(3);
        byte[] b = new byte[Math.min(extentBc == 0 ? RECORD_BYTES : extentBc, buffer.remaining())];
        buffer.get(b);
        for (byte c : b) {
            System.out.print((char) c);
        }
    }

}
