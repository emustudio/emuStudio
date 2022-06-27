package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;

import java.util.*;
import java.util.stream.Collectors;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.RAW_BLOCK_POINTERS_COUNT;

public class CpmFormat {

    // https://www.seasip.info/Cpm/dosses.html

    enum DateFormat {
        NOT_USED,
        NATIVE, // P2DOS  and CP/M Plus; every 4th entry
        DATE_STAMPER // !!!TIME&.DAT; in 1st entry
    }

    public static final int RECORD_SIZE = 128;
    public static final int ENTRIES_PER_RECORD = RECORD_SIZE / ENTRY_SIZE;

    public final String id; // identification
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
    public final SectorOps sectorOps;

    // https://manpages.debian.org/testing/cpmtools/cpm.5.en.html
    // ISX records the number of unused instead of used bytes in Bc
    public final boolean bcInterpretsAsUnused;
    public final DateFormat dateFormat;

    public CpmFormat(String id, DiskParameterBlock dpb, int sectorSize, int sectorSkew,
                     SectorOps sectorOps, boolean bcInterpretsAsUnused, DateFormat dateFormat) {
        this.id = Objects.requireNonNull(id);
        this.dpb = Objects.requireNonNull(dpb);
        this.sectorOps = Objects.requireNonNull(sectorOps);
        this.blockSize = RECORD_SIZE * (dpb.blm + 1);

        //al0              al1
        //b7b6b5b4b3b2b1b0 b7b6b5b4b3b2b1b0
        // 1 1 1 1 0 0 0 0  0 0 0 0 0 0 0 0

        List<Integer> dblocks = new ArrayList<>();
        int tmpAl01 = ((dpb.al0 << 8) | (dpb.al1)) & 0xFF00;
        for (int i = 0; i < 16; i++) {
            if ((tmpAl01 & 1) == 1) {
                dblocks.add(15 - i);
            }
            tmpAl01 = tmpAl01 >>> 1;
        }
        dblocks.sort(Integer::compareTo);

        this.directoryBlocks = Collections.unmodifiableList(dblocks);
        this.blockPointerIsWord = dpb.dsm > 255; // if block index doesn't fit in one byte
        this.recordsPerBlock = (dpb.blm + 1);
        this.entriesPerBlock = blockSize / ENTRY_SIZE;
        this.blockPointersCount = RAW_BLOCK_POINTERS_COUNT / (blockPointerIsWord ? 2 : 1);
        this.sectorSize = sectorSize;
        this.sectorSkew = sectorSkew;
        this.sectorSkewTable = computeSectorSkewTable(sectorSkew, dpb.spt);

        this.tracks = dpb.drm * blockSize / (dpb.spt * RECORD_SIZE);
        this.bcInterpretsAsUnused = bcInterpretsAsUnused;
        this.dateFormat = Objects.requireNonNull(dateFormat);

    }

    public long positionToOffset(Position position) {
        return (long) dpb.driveSpt * sectorSize * position.track + (long) sectorSize * sectorSkewTable[position.sector];
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

    public static List<CpmFormat> fromConfig(Config config) {
        Optional<List<Config>> formats = config.getOptional("format");
        return formats
            .orElse(Collections.emptyList())
            .stream()
            .map(c -> {
                String id = c.get("id");
                int sectorSize = c.get("sectorSize");
                int sectorSkew = c.get("sectorSkew");
                boolean bcInterpretsAsUnused = c.<Boolean>getOptional("bcInterpretsAsUnused").orElse(false);
                DateFormat dateFormat = c.getEnum("dateFormat", DateFormat.class);
                SectorOps sectorOps = c
                    .<String>getOptional("sectorOps")
                    .map(SectorOps::fromString)
                    .orElse(SectorOps.DUMMY);

                DiskParameterBlock dpb = DiskParameterBlock.fromConfig(c.get("dpb"));

                return new CpmFormat(id, dpb, sectorSize, sectorSkew, sectorOps, bcInterpretsAsUnused, dateFormat);
            }).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "CP/M Format:\n" +
            "  sector size: " + sectorSize + "\n" +
            "  skew table:" + Arrays.deepToString(new int[][]{sectorSkewTable}) + "\n" +
            "  block size: " + blockSize + "\n" +
            "  block pointer is word: " + blockPointerIsWord + "\n" +
            "  tracks: " + tracks + "\n" +
            "  records per block: " + recordsPerBlock + "\n" +
            "  entries per block: " + entriesPerBlock + "\n" +
            "  directory blocks: " + directoryBlocks + "\n" +
            "  date format: " + dateFormat;
    }
}
