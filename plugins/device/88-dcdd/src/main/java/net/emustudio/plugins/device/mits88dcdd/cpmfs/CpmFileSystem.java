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

import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CPM:
 * <p>
 * +------------------+------------------+------------------+---
 * |block0            |block1            |block2            |...
 * +---------------------------+---------------------------+---+
 * |track0                     |track1                     |...|
 * +-------+-------+---+-------+-------+-------+---+-------+---+
 * |sector0|sector1|...|sectorN|sector0|sector1|...|sectorN|...|
 * <p>
 * <p>
 * <p>
 * Each CP/M disk format is described by the following specific sizes:
 * - Sector size in bytes
 * - Number of tracks
 * - Number of sectors
 * - Block size
 * - Number of directory entries
 * - Logical sector skew
 * - Number of reserved system tracks
 * <p>
 * A block is the smallest allocatable storage unit. CP/M supports block sizes of 1024, 2048, 4096, 8192 and 16384 bytes.
 * Unfortunately, this format specification is not stored on the disk and there are lots of formats. Accessing a block
 * is performed by accessing its sectors, which are stored with the given software skew.
 */
@NotThreadSafe
public class CpmFileSystem {
    public final static int ENTRY_SIZE = 32;
    public final static int DIRECTORY_ENTRIES = 256;
    private final static int RECORD_BYTES = 128;
    public final static int DIRECTORY_TRACK = 6; // or number of system tracks
    public final static int BLOCK_LENGTH = 1024; // 2048, 4096, 8192 and 16384
    public final static int BLOCKS_COUNT = 255; // number of blocks on disk

    private final DriveIO driveIO;
    private final int directoryTrack;
    private final int blockLength;
    private final int blockCount;

    public CpmFileSystem(DriveIO driveIO, int directoryTrack, int blockLength, int blockCount) {
        this.driveIO = Objects.requireNonNull(driveIO);
        this.directoryTrack = directoryTrack;
        this.blockLength = blockLength;
        this.blockCount = blockCount;
    }

    public List<CpmFile> listFiles() throws IOException {
        List<ByteBuffer> directorySectors = readBlock(0);
        List<ByteBuffer> entries = getEntries(directorySectors);
        return getFilesFromEntries(entries);
    }


    public List<CpmFile> listExistingFiles() throws IOException {
        return listFiles().stream()
            .filter(file -> file.status < 32)
            .filter(file -> file.extentNumber == 0)
            .collect(Collectors.toList());
    }

    public String getLabel() throws IOException {
        for (CpmFile file : listFiles()) {
            if (file.status == 32) {
                return file.fileName + file.fileExt;
            }
        }
        return "";
    }

    public Optional<String> readContent(String fileName) throws IOException {
        List<CpmFile> foundExtents = listFiles().stream()
            .filter(file -> file.status < 32)
            .filter(file -> file.toString().toUpperCase().equals(fileName.toUpperCase(Locale.ENGLISH)))
            .collect(Collectors.toList());

        if (foundExtents.isEmpty()) {
            return Optional.empty();
        }

        boolean blocksAreTwoBytes = blockCount >= 256;

        int maxBlocksCount = 16;
        if (blocksAreTwoBytes) {
            maxBlocksCount /= 2;
        }

        StringBuilder result = new StringBuilder();

        foundExtents.sort(Comparator.comparingInt(o -> o.extentNumber));
        int expectingExtentNumber = 0;
        for (CpmFile extent : foundExtents) {
            if (extent.extentNumber != expectingExtentNumber) {
                System.err.printf("[file=%s, extent=%d, expectingExtent=%d] ERROR: Expecting different extent number!%n",
                    fileName, extent.extentNumber, expectingExtentNumber);
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

                List<ByteBuffer> records = readBlock(nextBlock);
                int recordsCount = records.size();

                records.stream()
                    .limit(recordsLeft)
                    .forEach(b -> result.append(getContent(b, extent.bc & 0xFF)));

                recordsLeft -= recordsCount;
            }
            expectingExtentNumber++;
        }
        return Optional.of(result.toString());
    }

    public void writeContent(String fileName, String content) throws IOException {
        if (exists(fileName)) {
            throw new IOException("File cpm://" + fileName + " already exists. Overwrites are not supported");
        }

        // At first write content
        boolean blocksAreTwoBytes = blockCount >= 256;
        List<List<ByteBuffer>> contentInBlocks = splitToBlocks(content);
        Iterator<Integer> freeBlocks = findFreeBlocks().iterator();

        List<Byte> blockPointersPerExtent = new ArrayList<>();
        List<List<Byte>> blockPointers = new ArrayList<>(List.of(blockPointersPerExtent)); // needed for extents
        int bpIndex = 1;

        for (List<ByteBuffer> records : contentInBlocks) {
            if (!freeBlocks.hasNext()) {
                throw new IOException("Not enough free space!");
            }
            int blockNumber = freeBlocks.next();
            writeBlock(blockNumber, records);

            if (bpIndex == 16) {
                blockPointersPerExtent = new ArrayList<>();
                blockPointers.add(blockPointersPerExtent);
                bpIndex = 0;
            }

            if (blocksAreTwoBytes) {
                blockPointersPerExtent.add((byte)(blockNumber >>> 8));
                blockPointersPerExtent.add((byte)(blockNumber & 0xFF));
            } else {
                blockPointersPerExtent.add((byte)(blockNumber & 0xFF));
            }
            bpIndex++;
        }
        for (; bpIndex < 16; bpIndex++) {
            blockPointersPerExtent.add((byte)0);
        }


        // Now write file extent(s)
        Iterator<Integer> freeExtents = findFreeExtents().iterator();
        List<ByteBuffer> directoryBlock = readBlock(0);

        // create a file
        int lastDot = fileName.lastIndexOf('.');

        String fn = fileName.substring(0, (lastDot == -1) ? fileName.length() : lastDot).toUpperCase();
        String fnExt = (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toUpperCase();

        List<ByteBuffer> lastBlock = contentInBlocks.get(contentInBlocks.size() - 1);
        int recordsInLastBlock = lastBlock.size();
        ByteBuffer dataOfLastRecord = lastBlock.get(lastBlock.size() - 1);
        dataOfLastRecord.flip();
        int dataSizeOfLastRecord = dataOfLastRecord.remaining() - 3;

        int extentsRequired = Math.max(1, blockPointers.size() / 16);
        int extentsPerSector = driveIO.sectorSize / ENTRY_SIZE;
        for (int extent = 0; extent < extentsRequired; extent++) {
            if (!freeExtents.hasNext()) {
                throw new IOException("Not enough free extents!");
            }
            boolean isLastExtent = (extent == extentsRequired - 1);

            int extentIndex = freeExtents.next();
            List<Byte> bp = blockPointers.get(extent);

            CpmFile file = new CpmFile(
                fn, fnExt, 0, extent,
                (byte) (isLastExtent ? dataSizeOfLastRecord : 0),
                (byte) (isLastExtent ? recordsInLastBlock : 16),
                bp
            );

            ByteBuffer directorySector = directoryBlock.get(extentIndex / extentsPerSector);
            directorySector.position(3 + (extentIndex % extentsPerSector) * ENTRY_SIZE);
            directorySector.put(file.toEntry());
            directorySector.position(0); // so reading is possible
        }
        writeBlock(0, directoryBlock);
    }

    public boolean exists(String fileName) throws IOException {
        return listFiles().stream()
            .filter(file -> file.status < 32)
            .anyMatch(file -> file.toString().toUpperCase().equals(fileName.toUpperCase(Locale.ENGLISH)));
    }

    private String getContent(ByteBuffer buffer, int extentBc) {
        buffer.position(3);
        byte[] b = new byte[Math.min(extentBc == 0 ? RECORD_BYTES : extentBc, buffer.remaining())];
        buffer.get(b);
        StringBuilder result = new StringBuilder();
        for (byte c : b) {
            result.append((char) c);
        }
        return result.toString();
    }

    /**
     * Reads a block
     *
     * @param blockNumber block number
     * @return records
     * @throws IOException unexpected
     */
    private List<ByteBuffer> readBlock(int blockNumber) throws IOException {
        int sectorSize = driveIO.sectorSize;
        int sectorsPerTrack = driveIO.sectorsPerTrack;

        int sectorsPerBlock = blockLength / sectorSize;
        final int sector = (blockNumber * sectorsPerBlock + sectorsPerTrack * directoryTrack) % sectorsPerTrack;
        final int track = (blockNumber * sectorsPerBlock + sectorsPerTrack * directoryTrack) / sectorsPerTrack;

        Position position = new Position(track, sector);
        List<ByteBuffer> block = new ArrayList<>();
        for (int counter = 0; counter < sectorsPerBlock; counter++) {
            block.add(driveIO.readSector(position));
            position.next(sectorsPerTrack);
        }
        return block;
    }

    private void writeBlock(int blockNumber, List<ByteBuffer> records) throws IOException {
        if (records.size() * RECORD_BYTES > BLOCK_LENGTH) {
            throw new IOException("Too many records per block");
        }

        int sectorSize = driveIO.sectorSize;
        int sectorsPerTrack = driveIO.sectorsPerTrack;

        int sectorsPerBlock = blockLength / sectorSize;
        final int sector = (blockNumber * sectorsPerBlock + sectorsPerTrack * directoryTrack) % sectorsPerTrack;
        final int track = (blockNumber * sectorsPerBlock + sectorsPerTrack * directoryTrack) / sectorsPerTrack;

        Position position = new Position(track, sector);
        for (ByteBuffer record : records) {
            driveIO.writeSector(position, record);
            position.next(sectorsPerTrack);
        }
    }

    private Stream<Integer> findFreeBlocks() throws IOException {
        boolean blocksAreTwoBytes = blockCount >= 256;

        Set<Integer> reservedBlocks = listFiles()
            .stream()
            .filter(file -> file.status < 32)
            .flatMap(f -> {
                List<Integer> bp = new ArrayList<>();
                if (blocksAreTwoBytes) {
                    for (int i = 0; i < 16; i += 2) {
                        bp.add((f.blockPointers.get(i) << 8) | f.blockPointers.get(i + 1));
                    }
                } else {
                    bp.addAll(f.blockPointers.stream().mapToInt(Byte::intValue).boxed().collect(Collectors.toList()));
                }
                return bp.stream();
            }).filter(b -> b != 0)
            .collect(Collectors.toSet());

        return Stream.iterate(1, b -> b + 1).filter(b -> !reservedBlocks.contains(b));
    }

    private Stream<Integer> findFreeExtents() throws IOException {

        class P<T> {
            final int index;
            final T t;
            P(int index, T t) {
                this.index = index;
                this.t = t;
            }
        }

        AtomicInteger index = new AtomicInteger();
        Set<Integer> reservedExtents = listFiles()
            .stream()
            .map(f -> new P<>(index.getAndIncrement(), f))
            .filter(p -> p.t.status < 32)
            .map(p -> p.index)
            .collect(Collectors.toSet());

        index.set(0);
        return Stream.iterate(0, e -> e + 1).filter(e -> !reservedExtents.contains(e));
    }

    private List<List<ByteBuffer>> splitToBlocks(String content) {
        ByteBuffer contentData = ByteBuffer.wrap(content.getBytes());
        int recordsPerBlock = blockLength / RECORD_BYTES;

        ByteBuffer record = ByteBuffer.allocate(driveIO.rawSectorSize);
        record.position(3);
        List<ByteBuffer> block = new ArrayList<>(List.of(record));
        List<List<ByteBuffer>> blocks = new ArrayList<>();
        blocks.add(block);

        int byteIndex = 0;
        int recordsInBlock = 1;
        while (contentData.remaining() > 0) {
            if (byteIndex == RECORD_BYTES) {
                record.flip(); // prepare for reading

                if (recordsInBlock == recordsPerBlock) {
                    block = new ArrayList<>();
                    blocks.add(block);
                    recordsInBlock = 0;
                }
                record = ByteBuffer.allocate(driveIO.rawSectorSize);
                record.position(3);

                block.add(record);
                byteIndex = 0;
                recordsInBlock++;
            }
            record.put(contentData.get());
            byteIndex++;
        }
        record.flip();
        return blocks;
    }

    private static List<ByteBuffer> getEntries(List<ByteBuffer> directorySectors) {
        List<ByteBuffer> entries = new ArrayList<>();

        for (ByteBuffer sector : directorySectors) {
            sector.position(3); // why needed??? part of checksum
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
}
