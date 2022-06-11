package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFile.RAW_BLOCK_POINTERS_COUNT;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFile.ENTRY_SIZE;

public class CpmFormat {

    public static final int RECORD_SIZE = 128;
    public static final int ENTRIES_PER_RECORD = RECORD_SIZE / ENTRY_SIZE;

    public final DiskParameterBlock dpb;

    public final int blockSize;
    public final List<Integer> directoryBlocks;
    public final boolean blockPointerIsWord; // if block pointer is DW or DB
    public final int recordsPerBlock;
    public final int entriesPerBlock;
    public final int blockPointersCount;

    public final int tracks;
    public final int sectorSize; // raw sector size
    public final int sectorSkew;
    public final int[] sectorSkewTable;

    // https://manpages.debian.org/testing/cpmtools/cpm.5.en.html
    // ISX records the number of unused instead of used bytes in Bc
    public final boolean bcInterpretsAsUnused;
    public final DateStampFormat dateStampFormat;
    public final SectorOps sectorOps;

    public enum DateStampFormat {
        Z80DOS, DOSPLUS, CPM3, NONE
    }

    public interface SectorOps {

        /**
         * Converts record of max RECORD_SIZE bytes to raw sector.
         * the record size might be less than RECORD_SIZE.
         * @param record CP/M record
         * @param position position
         * @return raw sector with correct length
         */
        ByteBuffer toSector(ByteBuffer record, Position position);

        /**
         * Converts sector of sectorSize to RECORD_SIZE record.
         * @param sector sector
         * @return record
         */
        ByteBuffer toRecord(ByteBuffer sector);
    }

    public CpmFormat(DiskParameterBlock dpb, int sectorSize, int sectorSkew,
                     boolean bcInterpretsAsUnused, DateStampFormat dateStampFormat, SectorOps sectorOps) {
        this.dpb = Objects.requireNonNull(dpb);
        this.sectorOps = Objects.requireNonNull(sectorOps);

        this.blockSize = RECORD_SIZE * (dpb.blm + 1);

        List<Integer> dblocks = new ArrayList<>();
        int tmpAl01 = dpb.al01;
        for (int i = 0; i < 16; i++) {
            if ((tmpAl01 & 1) == 1) {
                dblocks.add(i);
            }
            tmpAl01 = tmpAl01 >>> 1;
        }

        this.directoryBlocks = Collections.unmodifiableList(dblocks);
        this.blockPointerIsWord = dpb.dsm > 255; // if block index doesn't fit in one byte
        this.recordsPerBlock = blockSize / RECORD_SIZE;
        this.entriesPerBlock = blockSize / ENTRY_SIZE;
        this.blockPointersCount = RAW_BLOCK_POINTERS_COUNT / (blockPointerIsWord ? 2 : 1);
        this.sectorSize = sectorSize;
        this.sectorSkew = sectorSkew;
        this.sectorSkewTable = computeSectorSkewTable(sectorSkew, dpb.spt);

        this.tracks = dpb.drm * blockSize / (dpb.spt * RECORD_SIZE);
        this.bcInterpretsAsUnused = bcInterpretsAsUnused;
        this.dateStampFormat = Objects.requireNonNull(dateStampFormat);
    }

    public long positionToOffset(Position position) {
        return (long) dpb.spt * sectorSize * position.track + (long) sectorSize * sectorSkewTable[position.sector];
    }

    // so far it is not allowed to write to system/boot tracks
    // block 0 is basically the first data block
    public Position blockToPosition(int blockNumber) {
        if (blockNumber > dpb.dsm) {
            throw new IllegalArgumentException("Too big block number");
        }
        int linearSector = blockNumber * recordsPerBlock + dpb.spt * dpb.ofs;
        int sector = linearSector % dpb.spt;
        int track = linearSector / dpb.spt;
        if (track >= tracks) {
            throw new IllegalArgumentException("Too big track number: " + track);
        }
        return new Position(track, sector);
    }


    private static int[] computeSectorSkewTable(int sectorSkew, int sectorsPerTrack) {
        int[] skewTable = new int[sectorsPerTrack];
        int currentSkew = 0;
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            while (true) {
                int k = 0;
                while (k < sector && skewTable[k] != currentSkew) {
                    k++;
                }
                if (k < sector) {
                    currentSkew = (currentSkew + 1) % sectorsPerTrack;
                } else {
                    break;
                }
            }
            skewTable[sector] = currentSkew;
            currentSkew = (currentSkew + sectorSkew) % sectorsPerTrack;
        }
        return skewTable;
    }
}
