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

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFile.ENTRY_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFile.RAW_BLOCK_POINTERS_COUNT;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.ENTRIES_PER_RECORD;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;


@NotThreadSafe
public class CpmFileSystem {
    private final static int STATUS_LABEL = 0x20;
    private final DriveIO driveIO;
    private final CpmFormat cpmFormat;

    public CpmFileSystem(DriveIO driveIO) {
        this.driveIO = Objects.requireNonNull(driveIO);
        this.cpmFormat = driveIO.cpmFormat;
    }

    public Stream<CpmFile> listExistingFiles() {
        return listValidFiles().filter(file -> file.entryNumber == 0);
    }

    public boolean exists(String fileName) {
        return listValidFiles()
            .anyMatch(file -> file.toString().toUpperCase().equals(fileName.toUpperCase(Locale.ENGLISH)));
    }

    public String getLabel() {
        return listValidFiles()
            .map(f -> f.fileName + f.fileExt)
            .findAny()
            .orElse("");
    }

    public Optional<String> readFile(String fileName) throws IOException {
        List<CpmFile> entries = listValidFiles()
            .filter(file -> file.toString().toUpperCase().equals(fileName.toUpperCase(Locale.ENGLISH)))
            .sorted(Comparator.comparingInt(o -> o.entryNumber))
            .collect(Collectors.toList());

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder content = new StringBuilder();

        int expectingExtentNumber = 0;
        for (CpmFile extent : entries) {
            if (extent.entryNumber != expectingExtentNumber) {
                throw new IllegalStateException(String.format(
                    "[file=%s, extent=%d, expectingExtent=%d] ERROR: Expecting different extent number!%n",
                    fileName, extent.entryNumber, expectingExtentNumber));
            }

            int recordsLeft = extent.rc & 0xFF;
            for (int i = 0; i < RAW_BLOCK_POINTERS_COUNT; i = i + (cpmFormat.blockPointerIsWord ? 2 : 1)) {
                int nextBlock = extent.bp.get(i) & 0xFF;
                if (cpmFormat.blockPointerIsWord) {
                    nextBlock = (extent.bp.get(i + 1) << 8) | nextBlock;
                }
                if (nextBlock == 0) {
                    break;
                }

                List<ByteBuffer> records = driveIO.readBlock(nextBlock);
                int recordsCount = records.size();

                records.stream()
                    .limit(recordsLeft)
                    .forEach(b -> content.append(getContent(b, extent.bc & 0xFF)));

                recordsLeft -= recordsCount;
            }
            expectingExtentNumber++;
        }
        return Optional.of(content.toString());
    }

    public void writeFile(String fileName, String content) throws IOException {
        if (exists(fileName)) {
            throw new IOException("File cpm://" + fileName + " already exists. Overwrites are not supported");
        }

        // At first write content
        List<List<ByteBuffer>> contentInBlocks = splitToBlocks(content);
        List<List<Byte>> bp = writeContent(contentInBlocks);
        if (contentInBlocks.size() != bp.size()) {
            throw new IllegalStateException("Mismatch in number of used blocks");
        }

        // split blocks to extents
        List<List<List<ByteBuffer>>> contentInExtents = splitToExtents(contentInBlocks);
        if (contentInExtents.size() != bp.size()) {
            throw new IllegalStateException("Mismatch in number of used extents");
        }

        // write extents
        Iterator<I<Stream<Integer>>> freeExtents = findFreeExtents().iterator();
        if (!freeExtents.hasNext()) {
            throw new IllegalStateException("No free space in directory");
        }

        I<Stream<Integer>> currentDirectoryBlock = freeExtents.next();
        Iterator<Integer> currentBlockFreeExtents = currentDirectoryBlock.v.iterator();

        int extentIndex = 0;
        for (int i = 0; i < bp.size(); i++) {
            List<List<ByteBuffer>> blocksPerExtent = contentInExtents.get(i);
            List<Byte> bpPerExtent = bp.get(i);
            if (blocksPerExtent.isEmpty()) {
                // do not waste extent
                continue;
            }

            while (!currentBlockFreeExtents.hasNext()) {
                if (!freeExtents.hasNext()) {
                    throw new IllegalStateException("No free space in directory");
                }
                currentDirectoryBlock = freeExtents.next();
                currentBlockFreeExtents = currentDirectoryBlock.v.iterator();
            }

            int rawExtentIndex = currentBlockFreeExtents.next(); // raw index in directory block
            List<ByteBuffer> lastBlock = blocksPerExtent.get(blocksPerExtent.size() - 1); // assume lastBlock.size() > 0

            byte rc = (byte)lastBlock.size(); // assume > 0
            ByteBuffer lastRecord = lastBlock.get(lastBlock.size() - 1);
            byte bc = (byte)lastRecord.remaining();

            CpmFile file = new CpmFile(
                (byte)0, fileName, false, false, false,
                extentIndex++, cpmFormat.dpb.exm, bc, rc, bpPerExtent
            );

            // write extent to the directory block
            List<ByteBuffer> directoryBlock = driveIO.readBlock(currentDirectoryBlock.index);
            ByteBuffer directorySector = directoryBlock.get(rawExtentIndex / ENTRIES_PER_RECORD);
            directorySector.position( (rawExtentIndex % ENTRIES_PER_RECORD) * ENTRY_SIZE);
            directorySector.put(file.toEntry());
            directorySector.position(0); // so reading is possible
            driveIO.writeBlock(currentDirectoryBlock.index, directoryBlock); // no caching, never mind..
        }
    }

    private List<List<Byte>> writeContent(List<List<ByteBuffer>> contentInBlocks) throws IOException {
        Iterator<Integer> freeBlocks = findFreeBlocks().iterator();

        List<Byte> bpPerEntry = new ArrayList<>();
        List<List<Byte>> bp = new ArrayList<>(List.of(bpPerEntry));
        int bpIndex = 1;

        for (List<ByteBuffer> records : contentInBlocks) {
            if (!freeBlocks.hasNext()) {
                throw new IOException("Not enough free space!");
            }
            int blockNumber = freeBlocks.next();
            driveIO.writeBlock(blockNumber, records);

            if (bpIndex == RAW_BLOCK_POINTERS_COUNT) {
                bpPerEntry = new ArrayList<>();
                bp.add(bpPerEntry);
                bpIndex = 0;
            }

            if (cpmFormat.blockPointerIsWord) {
                bpPerEntry.add((byte) (blockNumber & 0xFF));
                bpPerEntry.add((byte) (blockNumber >>> 8));
            } else {
                bpPerEntry.add((byte) (blockNumber & 0xFF));
            }
            bpIndex++;
        }
        for (; bpIndex < RAW_BLOCK_POINTERS_COUNT; bpIndex++) {
            bpPerEntry.add((byte) 0);
        }
        return bp;
    }

    private <T> List<List<T>> splitToExtents(List<T> contentInBlocks) {
        List<T> blocksPerExtent = new ArrayList<>();
        List<List<T>> extents = new ArrayList<>(List.of(blocksPerExtent));

        int bpIndex = 0;
        for (T block : contentInBlocks) {
            if (bpIndex == cpmFormat.blockPointersCount) {
                blocksPerExtent = new ArrayList<>();
                extents.add(blocksPerExtent);
                bpIndex = 0;
            }
            blocksPerExtent.add(block);
            bpIndex++;
        }
        return extents;
    }


    private Stream<Integer> findFreeBlocks() {
        Set<Integer> reservedBlocks = listValidFiles()
            .flatMap(f -> {
                List<Integer> extents = new ArrayList<>();
                if (cpmFormat.blockPointerIsWord) {
                    for (int i = 0; i < RAW_BLOCK_POINTERS_COUNT; i += 2) {
                        extents.add((f.bp.get(i + 1) << 8) | f.bp.get(i));
                    }
                } else {
                    extents.addAll(f.bp.stream().mapToInt(Byte::intValue).boxed().collect(Collectors.toList()));
                }
                return extents.stream();
            }).filter(b -> b != 0)
            .collect(Collectors.toSet());

        // first data block is located after directory blocks
        int firstBlock = cpmFormat.directoryBlocks.stream().max(Comparator.naturalOrder()).orElse(0) + 1;
        return Stream.iterate(firstBlock, b -> b + 1).filter(b -> !reservedBlocks.contains(b));
    }

    /**
     * Returns free extents per block
     *
     * The indexes returned are "entry indexes": they reset to 0 on new block
     *
     * @return free extents per directory block
     */
    private Stream<I<Stream<Integer>>> findFreeExtents() {
        return readDirectoryBlocks().map(block -> {
            AtomicInteger entryIndex = new AtomicInteger();
            Set<Integer> reservedExtents = block.v
                .flatMap(CpmFileSystem::getEntries)
                .map(e -> CpmFile.fromEntry(e, driveIO.cpmFormat.dpb.exm))
                .map(f -> new I<>(entryIndex.getAndIncrement(), f))
                .filter(p -> p.v.status < STATUS_LABEL)
                .map(p -> p.index)
                .collect(Collectors.toSet());

            return new I<>(block.index, Stream.iterate(0, e -> e + 1).filter(e -> !reservedExtents.contains(e)));
        });
    }

    private List<List<ByteBuffer>> splitToBlocks(String content) {
        ByteBuffer contentData = ByteBuffer.wrap(content.getBytes());

        ByteBuffer record = ByteBuffer.allocate(RECORD_SIZE);
        List<ByteBuffer> block = new ArrayList<>(List.of(record));
        List<List<ByteBuffer>> blocks = new ArrayList<>();
        blocks.add(block);

        int byteIndex = 0;
        int recordsInBlock = 1;
        while (contentData.remaining() > 0) {
            if (byteIndex == RECORD_SIZE) {
                record.flip(); // prepare for reading

                if (recordsInBlock == driveIO.cpmFormat.recordsPerBlock) {
                    block = new ArrayList<>();
                    blocks.add(block);
                    recordsInBlock = 0;
                }
                record = ByteBuffer.allocate(RECORD_SIZE);
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

    /**
     * Read directory blocks, indexed by real block index
     * @return directory blocks (indexed)
     */
    private Stream<I<Stream<ByteBuffer>>> readDirectoryBlocks() {
        return driveIO.cpmFormat.directoryBlocks
            .stream()
            .map(d -> {
                try {
                    return new I<>(d, driveIO.readBlock(d).stream());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    private static Stream<ByteBuffer> getEntries(ByteBuffer record) {
        List<ByteBuffer> entries = new ArrayList<>();
        for (int i = 0; i < ENTRIES_PER_RECORD; i++) {
            byte[] entry = new byte[ENTRY_SIZE];
            record.get(entry);
            entries.add(ByteBuffer.wrap(entry));
        }
        return entries.stream();
    }

    private static String getContent(ByteBuffer buffer, int extentBc) {
        byte[] b = new byte[Math.min(extentBc == 0 ? RECORD_SIZE : extentBc, buffer.remaining())];
        buffer.get(b);
        return new String(b);
    }

    private Stream<CpmFile> listValidFiles() {
        return readDirectoryBlocks()
            .flatMap(i -> i.v.flatMap(CpmFileSystem::getEntries))
            .map(e -> CpmFile.fromEntry(e, driveIO.cpmFormat.dpb.exm))
            .filter(file -> (file.status & 0xFF) < STATUS_LABEL);
    }

    /**
     * Indexed value
     *
     * @param <T> value type
     */
    static class I<T> {
        public final int index;
        public final T v;

        public I(int index, T v) {
            this.index = index;
            this.v = v;
        }
    }
}
