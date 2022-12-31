/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

import net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.*;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Format;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.ENTRIES_PER_RECORD;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.DateFormat.*;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.RAW_BLOCK_POINTERS_COUNT;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmNativeDate.*;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmPlusDiscLabel.STATUS_LABEL;


@NotThreadSafe
public class CpmFileSystem {
    public final static int STATUS_UNUSED = 0xE5;
    private final static int MAX_USER_NUMBER = 0x0F;

    private final static Format format = DateTimeFormatter.ofPattern("dd-MMM-yy HH.mma").toFormat();
    public final CpmFormat cpmFormat;
    private final DriveIO driveIO;

    public CpmFileSystem(DriveIO driveIO) {
        this.driveIO = Objects.requireNonNull(driveIO);
        this.cpmFormat = driveIO.cpmFormat;
    }

    private static String getContent(ByteBuffer buffer, int extentBc) {
        byte[] b = new byte[Math.min(extentBc == 0 ? RECORD_SIZE : extentBc, buffer.remaining())];
        buffer.get(b);
        return new String(b);
    }

    public Stream<CpmFile> listExistingFiles() {
        return listValidFiles().filter(file -> file.ex == 0);
    }

    public boolean exists(String fileName) {
        return listExistingFiles()
                .anyMatch(file -> file.getFileName().toUpperCase().equals(fileName.toUpperCase(Locale.ENGLISH)));
    }

    public String getLabel() {
        return readDirectoryBlocks()
                .flatMap(i -> i.v.flatMap(this::getEntries))
                .filter(entry -> (CpmEntry.getStatus(entry) & 0xFF) == STATUS_LABEL)
                .map(entry -> CpmPlusDiscLabel.fromEntry(entry).toString())
                .findAny().orElse("");
    }

    public String readFile(String fileName) throws IOException {
        List<CpmFile> entries = listValidFiles()
                .filter(file -> file.getFileName().toUpperCase().equals(fileName.toUpperCase(Locale.ENGLISH)))
                .collect(Collectors.toList());

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("File '" + fileName + "' not found!");
        }

        StringBuilder content = new StringBuilder();
        for (CpmFile extent : entries) {
            // extent numbering can be various...
            int recordsLeft = extent.numberOfRecords;

            for (int i = 0; i < RAW_BLOCK_POINTERS_COUNT; i++) {
                int nextBlock = extent.al.get(i) & 0xFF;
                if (cpmFormat.blockPointerIsWord) {
                    nextBlock = (extent.al.get(++i) << 8) | nextBlock;
                }
                if (nextBlock == 0) {
                    continue;
                }
                if (recordsLeft <= 0) {
                    break;
                }

                List<ByteBuffer> records = driveIO.readBlock(nextBlock);
                int recordsCount = records.size();

                records.stream()
                        .limit(recordsLeft)
                        .forEach(b -> content.append(getContent(b, extent.bc & 0xFF)));

                recordsLeft -= recordsCount;
            }
        }
        return content.toString();
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
        Iterator<I<Stream<I<Stream<Integer>>>>> freeExtentsBlocks = findFreeDataExtents().iterator();
        if (!freeExtentsBlocks.hasNext()) {
            throw new IllegalStateException("No free space in directory");
        }

        I<Stream<I<Stream<Integer>>>> freeExtentsBlock = freeExtentsBlocks.next();
        Iterator<I<Stream<Integer>>> freeExtentsSectors = freeExtentsBlock.v.iterator();
        while (!freeExtentsSectors.hasNext()) {
            if (!freeExtentsBlocks.hasNext()) {
                throw new IllegalStateException("No free space in directory");
            }
            freeExtentsBlock = freeExtentsBlocks.next();
            freeExtentsSectors = freeExtentsBlock.v.iterator();
        }
        I<Stream<Integer>> freeExtentsSector = freeExtentsSectors.next();
        Iterator<Integer> freeExtents = freeExtentsSector.v.iterator();

        int extentIndex = 0;
        for (int i = 0; i < bp.size(); i++) {
            List<List<ByteBuffer>> blocksPerExtent = contentInExtents.get(i);
            List<Byte> bpPerExtent = bp.get(i);
            if (blocksPerExtent.isEmpty()) {
                // do not waste extent
                continue;
            }

            while (!freeExtents.hasNext()) {
                while (!freeExtentsSectors.hasNext()) {
                    if (!freeExtentsBlocks.hasNext()) {
                        throw new IllegalStateException("No free space in directory");
                    }
                    freeExtentsBlock = freeExtentsBlocks.next();
                    freeExtentsSectors = freeExtentsBlock.v.iterator();
                }
                freeExtentsSector = freeExtentsSectors.next();
                freeExtents = freeExtentsSector.v.iterator();
            }

            int freeExtentIndex = freeExtents.next(); // extent index in sector
            List<ByteBuffer> lastBlock = blocksPerExtent.get(blocksPerExtent.size() - 1); // assuming lastBlock.size() > 0

            byte rc = (byte) lastBlock.size(); // assuming > 0
            ByteBuffer lastRecord = lastBlock.get(lastBlock.size() - 1);
            byte bc = cpmFormat.bcInterpretsAsUnused ?
                    (byte) (RECORD_SIZE - lastRecord.remaining()) :
                    (byte) lastRecord.remaining();

            // no flags setting supported yet
            CpmFile file = new CpmFile(
                    (byte) 0, fileName, 0,
                    extentIndex++, cpmFormat.dpb.exm, bc, rc, bpPerExtent
            );

            // write extent to the directory block
            List<ByteBuffer> directoryBlock = driveIO.readBlock(freeExtentsBlock.index);
            ByteBuffer directorySector = directoryBlock.get(freeExtentsSector.index * RECORD_SIZE);
            directorySector.position(freeExtentIndex * ENTRY_SIZE);
            directorySector.put(file.toEntry());
            directorySector.position(0); // so reading is possible

            // TODO: write timestamps
            driveIO.writeBlock(freeExtentsBlock.index, directoryBlock); // no caching, never mind..
        }
    }

    /**
     * Removes a file from disk. It erases just file extents (frees block allocation), but keeps
     * data in blocks untouched.
     */
    public void removeFile(String fileName) {
        readDirectoryBlocks()
                .map(i -> new I<>(i.index, i.v.map(this::getEntries)))
                .map(i -> new I<>(i.index, i.v.map(entries -> entries.map(e -> {
                    CpmFile f = CpmFile.fromEntry(e, cpmFormat.dpb.exm);
                    boolean nameMatch = f.getFileName().toUpperCase(Locale.ENGLISH).equals(fileName.toUpperCase(Locale.ENGLISH));
                    if ((f.status & 0xFF) != STATUS_UNUSED && nameMatch) {
                        return f.toEntry((byte) STATUS_UNUSED);
                    } else {
                        e.position(0);
                    }
                    return e;
                }))))
                .map(i -> new I<>(i.index, i.v.map(this::toRecord)))
                .forEach(i -> {
                    try {
                        driveIO.writeBlock(i.index, i.v.collect(Collectors.toList()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Finds native datestamp entries
     * A native datestamp is every 4th entry
     *
     * @return native datestamps
     */
    public List<String> listNativeDates() {
        if (cpmFormat.dateFormat != NATIVE && cpmFormat.dateFormat != NATIVE2) {
            return Collections.emptyList();
        }

        List<CpmFile> previousFiles = new ArrayList<>();

        return readDirectoryBlocks()
                .flatMap(i -> i.v.flatMap(this::getEntries))
                .limit(cpmFormat.dpb.drm + 1)
                .flatMap(entry -> {
                    int status = CpmEntry.getStatus(entry) & 0xFF;
                    if (status != STATUS_DATESTAMP) {
                        // even for labels and passwords. Every extent has it's datestamp.
                        previousFiles.add(CpmFile.fromEntry(entry, cpmFormat.dpb.exm));
                        return Stream.empty();
                    } else {
                        DateStamp[][] datestamps = CpmNativeDate.fromEntry(entry, cpmFormat.dateFormat).datestamps;
                        List<String> sb = new ArrayList<>();

                        for (int j = 0; j < datestamps.length; j++) {
                            CpmFile prev = previousFiles.get(j);
                            if ((prev.status & 0xFF) <= MAX_USER_NUMBER && prev.ex == 0) {
                                String dates =
                                        dformat(datestamps[j][CREATE].dateTime) +
                                                " | " + dformat(datestamps[j][MODIFY].dateTime) +
                                                " | " + dformat(datestamps[j][ACCESS].dateTime);

                                sb.add(String.format("%12s : %s", prev.getFileName(), dates));
                            }
                        }
                        previousFiles.clear();
                        return sb.stream();
                    }
                })
                .filter(str -> !str.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String dformat(LocalDateTime time) {
        return format.format(time).replace("AM", "a").replace("PM", "p");
    }

    public List<String> listPasswords() {
        return readDirectoryBlocks()
                .flatMap(i -> i.v.flatMap(this::getEntries))
                .limit(cpmFormat.dpb.drm + 1)
                .filter(entry -> {
                    int status = CpmEntry.getStatus(entry) & 0xFF;
                    return status > 0x0F && status <= (0x0F + 15);
                })
                .map(entry -> CpmPlusPassword.fromEntry(entry).toString())
                .collect(Collectors.toList());
    }

    private List<List<Byte>> writeContent(List<List<ByteBuffer>> contentInBlocks) throws IOException {
        // will not update timestamps
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
                            extents.add((f.al.get(i + 1) << 8) | f.al.get(i));
                        }
                    } else {
                        extents.addAll(f.al.stream().mapToInt(Byte::intValue).boxed().collect(Collectors.toList()));
                    }
                    return extents.stream();
                }).filter(b -> b != 0)
                .collect(Collectors.toSet());

        // first data block is located after directory blocks
        int firstBlock = cpmFormat.directoryBlocks.stream().max(Comparator.naturalOrder()).orElse(0) + 1;
        return Stream.iterate(firstBlock, b -> b + 1).filter(b -> !reservedBlocks.contains(b));
    }

    /**
     * Returns free "data" extents per block and sector.
     * <p>
     * "Data" means that they can be used for files, label or password, but not for timestamp extents (both native
     * and date-stamper).
     *
     * <p>
     * The indexes returned are "entry indexes": they reset to 0 on each new sector
     * Note: if a native timestamps are used, then every 4th entry represents a date-time entry of previous 3 entries.
     *
     * @return free extents per directory block and sector
     */
    private Stream<I<Stream<I<Stream<Integer>>>>> findFreeDataExtents() {
        return readDirectoryBlocks().map(block -> {
            AtomicInteger sectorIndex = new AtomicInteger();
            return new I<>(block.index, block.v
                    .map(this::getEntries)
                    .map(filesInSector -> {
                        sectorIndex.set(0);
                        AtomicInteger entryIndex = new AtomicInteger();
                        return new I<>(sectorIndex.getAndIncrement(), filesInSector
                                .map(f -> new I<>(entryIndex.getAndIncrement(), f))
                                .filter(f -> (cpmFormat.dateFormat != NATIVE && cpmFormat.dateFormat != NATIVE2) || ((f.index + 1) % 4 != 0))
                                .filter(f -> cpmFormat.dateFormat != DATE_STAMPER || (f.index != 0))
                                .filter(f -> (CpmEntry.getStatus(f.v) & 0xFF) == STATUS_UNUSED)
                                .map(f -> f.index));
                    }));
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

                if (recordsInBlock == cpmFormat.recordsPerBlock) {
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
     *
     * @return directory blocks (indexed)
     */
    private Stream<I<Stream<ByteBuffer>>> readDirectoryBlocks() {
        return cpmFormat.directoryBlocks
                .stream()
                .map(d -> {
                    try {
                        return new I<>(d, driveIO.readBlock(d).stream());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    private Stream<ByteBuffer> getEntries(ByteBuffer record) {
        List<ByteBuffer> entries = new ArrayList<>();
        for (int i = 0; i < ENTRIES_PER_RECORD; i++) {
            byte[] entry = new byte[ENTRY_SIZE];
            record.get(entry);
            entries.add(ByteBuffer.wrap(entry));
        }
        return entries.stream();
    }

    private ByteBuffer toRecord(Stream<ByteBuffer> entries) {
        ByteBuffer record = ByteBuffer.allocate(RECORD_SIZE);
        entries.forEach(record::put);
        record.position(0);
        record.limit(RECORD_SIZE);
        return record;
    }

    public Stream<CpmFile> listValidFiles() {
        return readDirectoryBlocks()
                .flatMap(i -> i.v.flatMap(this::getEntries))
                .limit(cpmFormat.dpb.drm + 1)
                .filter(entry -> (CpmEntry.getStatus(entry) & 0xFF) <= MAX_USER_NUMBER)
                .map(entry -> CpmFile.fromEntry(entry, cpmFormat.dpb.exm));
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
