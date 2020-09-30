package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

@NotThreadSafe
public class CpmFileSystem {
    private final static int ENTRY_SIZE = 32;
    //    public final static int DIRECTORY_ENTRIES = 256;
    private final static int RECORD_BYTES = 128;
    public final static boolean BLOCKS_ARE_TWO_BYTES = false;
    public final static int DIRECTORY_TRACK = 6;
    public final static int BLOCK_LENGTH = 1024; // 2048, 4096, 8192 and 16384

    private final DriveIO driveIO;
    private final int directoryTrack;
    private final int blockLength;
    private final boolean blocksAreTwoBytes;

    public CpmFileSystem(DriveIO driveIO, int directoryTrack, int blockLength, boolean blocksAreTwoBytes) {
        this.driveIO = Objects.requireNonNull(driveIO);
        this.directoryTrack = directoryTrack;
        this.blockLength = blockLength;
        this.blocksAreTwoBytes = blocksAreTwoBytes;
    }

    public List<CpmFile> listFiles() throws IOException {
        List<ByteBuffer> directorySectors = readBlock(0);
        List<ByteBuffer> entries = getEntries(directorySectors);
        return getFilesFromEntries(entries);
    }


    public List<CpmFile> listValidFiles() throws IOException {
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

        int maxBlocksCount = 16;
        if (blocksAreTwoBytes) {
            maxBlocksCount /= 2;
        }

        StringBuilder result = new StringBuilder();

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
}
