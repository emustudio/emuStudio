/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;

import java.util.*;
import java.util.stream.Collectors;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.RAW_BLOCK_POINTERS_COUNT;

/**
 * Represents a CP/M disk format configuration.
 * <p>
 * A CP/M format defines the logical layout of a disk, including how sectors are organized into blocks,
 * how directory entries are stored, and how sector interleaving (skew) is applied. It bridges the gap
 * between the physical disk geometry (tracks, sectors, sector size) and the logical CP/M filesystem
 * structure (blocks, extents, directory entries).
 * <p>
 * Instances are typically constructed from a TOML configuration file via {@link #fromConfig(Config)}.
 *
 * @see <a href="https://manpages.debian.org/testing/cpmtools/cpm.5.en.html">cpm(5) man page</a>
 * @see <a href="https://www.seasip.info/Cpm/format22.html">CP/M 2.2 directory format</a>
 * @see DiskParameterBlock
 */
public class CpmFormat {
    /**
     * Size of a single CP/M logical record (always 128 bytes in all CP/M versions).
     */
    public static final int RECORD_SIZE = 128;

    /**
     * Number of 32-byte directory entries that fit into one 128-byte record.
     */
    public static final int ENTRIES_PER_RECORD = RECORD_SIZE / ENTRY_SIZE;

    /**
     * Human-readable identification string for this disk format (e.g. "mits", "imsai").
     */
    public final String id;

    /**
     * The CP/M Disk Parameter Block describing the logical disk geometry.
     */
    public final DiskParameterBlock dpb;

    /**
     * Block size in bytes, computed as {@code RECORD_SIZE * (BLM + 1)}.
     * Common values are 1024, 2048, 4096, 8192, or 16384.
     */
    public final int blockSize;

    /**
     * List of block numbers allocated to the directory area, derived from the AL0/AL1 allocation bitmap
     * in the Disk Parameter Block.
     */
    public final List<Integer> directoryBlocks;

    /**
     * Whether block pointers in directory entries are stored as 16-bit words ({@code true})
     * or 8-bit bytes ({@code false}). This is determined by whether the highest block number
     * (DSM) exceeds 255.
     */
    public final boolean blockPointerIsWord;

    /**
     * Number of 128-byte logical records that fit into one block ({@code BLM + 1}).
     */
    public final int recordsPerBlock;

    /**
     * Number of 32-byte directory entries that fit into one block.
     */
    public final int entriesPerBlock;

    /**
     * Number of usable block pointers per directory entry. Each entry has 16 raw bytes for block pointers;
     * if pointers are words (16-bit), this is 8; if bytes (8-bit), this is 16.
     */
    public final int blockPointersCount;

    /**
     * Total number of tracks on the disk, including reserved (system/boot) tracks.
     * Computed from the total data capacity and the number of reserved tracks.
     */
    public final int tracks;

    /**
     * Physical (raw) sector size in bytes as used by the drive hardware.
     * This may differ from the CP/M logical record size of 128 bytes.
     */
    public final int sectorSize;

    /**
     * Sector skew factor, or {@code null} if an explicit skew table is used instead.
     * The skew determines how logical sector numbers are interleaved on the physical track
     * to allow time for the controller to process data between consecutive sector reads.
     */
    public final Integer sectorSkew;

    /**
     * Sector skew table mapping logical sector indices to physical sector numbers.
     * Either computed from {@link #sectorSkew} or provided directly by configuration.
     */
    public final int[] sectorSkewTable;

    /**
     * Sector operations (encode/decode) performed by the CP/M BIOS when reading/writing sectors.
     * For example, Altair floppy drives use a special sector format.
     */
    public final SectorOps sectorOps;

    /**
     * If {@code true}, the Bc (byte count) field in directory entries is interpreted as the number
     * of <em>unused</em> bytes in the last record, rather than the number of <em>used</em> bytes.
     * This variant is used by the ISX format.
     *
     * @see <a href="https://manpages.debian.org/testing/cpmtools/cpm.5.en.html">cpm(5) man page</a>
     */
    public final boolean bcInterpretsAsUnused;

    /**
     * The date/time stamping format used on this disk (e.g. native CP/M 2.2 timestamps,
     * P2DOS timestamps, DateStamper, or none).
     */
    public final DateFormat dateFormat;

    /**
     * Constructs a new CP/M disk format.
     *
     * @param id                   format identifier string
     * @param dpb                  the Disk Parameter Block
     * @param sectorSize           physical sector size in bytes
     * @param sectorSkew           sector skew factor (must be {@code null} if skew table is non-empty)
     * @param sectorSkewTable      explicit skew table (must be empty if sectorSkew is non-null)
     * @param sectorOps            sector encode/decode operations
     * @param bcInterpretsAsUnused whether the Bc field counts unused bytes (ISX format)
     * @param dateFormat           date stamping format
     * @throws IllegalArgumentException if both sectorSkew and a non-empty sectorSkewTable are provided
     */
    public CpmFormat(String id,
                     DiskParameterBlock dpb,
                     int sectorSize,
                     Integer sectorSkew,
                     List<Integer> sectorSkewTable,
                     SectorOps sectorOps,
                     boolean bcInterpretsAsUnused,
                     DateFormat dateFormat) {
        Objects.requireNonNull(sectorSkewTable, "sector skew table must be defined");
        if (sectorSkew != null && !sectorSkewTable.isEmpty()) {
            throw new IllegalArgumentException("sector skew and skew table cannot be defined at the same time");
        }

        this.id = Objects.requireNonNull(id, "Unknown CP/M disk format ID");
        this.dpb = Objects.requireNonNull(dpb, "Unknown CP/M disk parameter block");
        this.sectorOps = Objects.requireNonNull(sectorOps, "Unknown disk sector ops");
        this.dateFormat = Objects.requireNonNull(dateFormat, "Unknown date format");
        this.sectorSkew = sectorSkew;

        // Compute or use the provided sector skew table
        if (sectorSkew != null) {
            this.sectorSkewTable = computeSectorSkewTable(sectorSkew, dpb.spt);
        } else if (!sectorSkewTable.isEmpty()) {
            this.sectorSkewTable = NumberUtils.listToNativeInts(sectorSkewTable);
        } else {
            // Default: no interleave (skew = 1)
            this.sectorSkewTable = computeSectorSkewTable(1, dpb.spt);
        }

        // Block size = number of records per block * record size
        this.blockSize = RECORD_SIZE * (dpb.blm + 1);

        // Decode the AL0/AL1 directory allocation bitmap into a sorted list of block numbers.
        // AL0 and AL1 form a 16-bit bitmap where bit 15 (MSB of AL0) corresponds to block 0,
        // bit 14 to block 1, etc. A set bit means that block is reserved for the directory.
        //
        // Example:
        //   al0              al1
        //   b7b6b5b4b3b2b1b0 b7b6b5b4b3b2b1b0
        //    1 1 1 1 0 0 0 0  0 0 0 0 0 0 0 0
        // -> directory blocks = [0, 1, 2, 3]
        List<Integer> dblocks = new ArrayList<>();
        int tmpAl01 = ((dpb.al0 << 8) | dpb.al1) & 0xFFFF;
        for (int i = 0; i < 16; i++) {
            if ((tmpAl01 & 1) == 1) {
                dblocks.add(15 - i);
            }
            tmpAl01 = tmpAl01 >>> 1;
        }
        dblocks.sort(Integer::compareTo);

        this.directoryBlocks = Collections.unmodifiableList(dblocks);
        // Block pointers are 16-bit words if the highest block number doesn't fit in a single byte
        this.blockPointerIsWord = dpb.dsm > 255;
        this.recordsPerBlock = (dpb.blm + 1);
        this.entriesPerBlock = blockSize / ENTRY_SIZE;
        this.blockPointersCount = RAW_BLOCK_POINTERS_COUNT / (blockPointerIsWord ? 2 : 1);
        this.sectorSize = sectorSize;

        // Total tracks = data tracks + reserved (system/boot) tracks
        this.tracks = (dpb.dsm + 1) * blockSize / (dpb.spt * RECORD_SIZE) + dpb.ofs;
        this.bcInterpretsAsUnused = bcInterpretsAsUnused;
    }

    /**
     * Computes a sector skew table using the given skew factor and number of sectors per track.
     * <p>
     * The skew table maps logical sector indices (0-based) to physical sector numbers. Sector
     * interleaving is used so that after reading one sector, the disk has rotated past enough
     * sectors to give the controller time to process the data before the next logical sector
     * arrives under the read head.
     *
     * @param sectorSkew      the interleave factor (e.g. 1 = no interleave, 2 = every other sector)
     * @param sectorsPerTrack total number of sectors on one track
     * @return the computed skew table
     */
    private static int[] computeSectorSkewTable(int sectorSkew, int sectorsPerTrack) {
        int[] skewTable = new int[sectorsPerTrack];
        int currentSkew = 0;
        for (int sector = 0; sector < sectorsPerTrack; sector++) {
            // Find the next unused physical sector number
            while (true) {
                int k = 0;
                while (k < sector && skewTable[k] != currentSkew) {
                    k++;
                }
                if (k < sector) {
                    // currentSkew is already assigned to an earlier logical sector; try the next one
                    currentSkew = (currentSkew + 1) % sectorsPerTrack;
                } else {
                    break;
                }
            }
            skewTable[sector] = currentSkew;
            // Advance by the skew factor for the next logical sector
            currentSkew = (currentSkew + sectorSkew) % sectorsPerTrack;
        }
        return skewTable;
    }

    /**
     * Parses a list of CP/M format definitions from a TOML configuration.
     * <p>
     * Expects a top-level {@code [[format]]} array of tables, each containing keys such as
     * {@code id}, {@code sectorSize}, {@code sectorSkew}, {@code sectorSkewTable},
     * {@code bcInterpretsAsUnused}, {@code dateFormat}, {@code sectorOps}, and a nested
     * {@code [format.dpb]} table for the Disk Parameter Block.
     *
     * @param config the parsed TOML configuration
     * @return list of parsed CpmFormat instances (empty if no formats are defined)
     */
    public static List<CpmFormat> fromConfig(Config config) {
        Optional<List<Config>> formats = config.getOptional("format");
        return formats
                .orElse(Collections.emptyList())
                .stream()
                .map(c -> {
                    String id = c.get("id");
                    int sectorSize = c.get("sectorSize");
                    Optional<Integer> sectorSkew = c.getOptional("sectorSkew");
                    Optional<List<Integer>> sectorSkewTable = c.getOptional("sectorSkewTable");
                    boolean bcInterpretsAsUnused = c.<Boolean>getOptional("bcInterpretsAsUnused").orElse(false);
                    DateFormat dateFormat = c.getEnumOrElse("dateFormat", DateFormat.class, () -> DateFormat.NOT_USED);
                    SectorOps sectorOps = c
                            .<String>getOptional("sectorOps")
                            .map(SectorOps::fromString)
                            .orElse(SectorOps.DUMMY);

                    DiskParameterBlock dpb = DiskParameterBlock.fromConfig(c.get("dpb"));

                    return new CpmFormat(
                            id, dpb, sectorSize, sectorSkew.orElse(null),
                            sectorSkewTable.orElse(Collections.emptyList()), sectorOps, bcInterpretsAsUnused, dateFormat
                    );
                }).collect(Collectors.toList());
    }

    /**
     * Converts a logical track/sector position to a byte offset within the disk image.
     * <p>
     * The offset accounts for the physical sector size, the number of physical sectors per track
     * (driveSpt), and sector skew (interleaving).
     *
     * @param position the track and (logical) sector position
     * @return absolute byte offset in the disk image
     */
    public long positionToOffset(Position position) {
        return (long) dpb.driveSpt * sectorSize * position.track + (long) sectorSize * sectorSkewTable[position.sector];
    }

    /**
     * Converts a CP/M block number to a track/sector position.
     * <p>
     * Block 0 is the first data block (after the reserved/system tracks). The method computes
     * the linear sector number from the block, accounts for the reserved track offset, and
     * returns the corresponding track and sector.
     *
     * @param blockNumber the CP/M block number (0-based, must not exceed DSM)
     * @return the track/sector position
     * @throws IllegalArgumentException if blockNumber exceeds the maximum block number (DSM)
     */
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

    @Override
    public String toString() {
        return "CP/M Format:\n" +
                "  ID: " + id + "\n" +
                "  tracks: " + tracks + "\n" +
                "  sectors per track (cp/m): " + dpb.spt + "\n" +
                "  sectors per track (drive): " + dpb.driveSpt + "\n" +
                "  sector size: " + sectorSize + "\n" +
                "  sector skew:" + sectorSkew + "\n" +
                "  sector skew table:" + Arrays.toString(sectorSkewTable) + "\n" +
                "  block size: " + blockSize + "\n" +
                "  block pointer is word: " + blockPointerIsWord + "\n" +
                "  records per block: " + recordsPerBlock + "\n" +
                "  entries per block: " + entriesPerBlock + "\n" +
                "  directory blocks: " + directoryBlocks + "\n" +
                "  date format: " + dateFormat;
    }
}
