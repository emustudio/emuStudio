package net.emustudio.plugins.device.mits88dcdd.cpmfs;

// https://www.idealine.info/sharpmz/succpminfo06.htm#bshblm
public class DiskParameterBlock {

    /**
     * No. of Logical (128-byte) Sectors per Logical Track
     *
     * This may be used to find the capacity of a disk - but the calculation must allow for the fact that in some
     * cases a 'Logical Track' is limited to ONE SIDE of a disk, but in other cases it extends over BOTH SIDES of a
     * disk.
     *
     * For example in Xtal's '35-track' CP/M for the MZ-80K a 'Logical Track' is limited to ONE side of the disk;
     * there are therefore 35 'Logical tracks' on each side of a disk, and 70 'Logical Tracks' on the disk as a whole.
     * The capacity of a disk is therefore
     * 128 * 16 * 70 = 143,360 Bytes ( 140K ).
     *
     * But in M-T's '35-track' CP/M 2.2 for the MZ-80B a 'Logical Track' extends over BOTH sides of a disk; there are
     * therefore 35 'Logical Tracks' on the whole of a disk, and the disk capacity is
     * 128 * 80 * 35 = 358,400 ( 350K ).
     */
    public final int spt;

    /**
     * Block Shift - Block Size is given by 128 * 2^(BSH)
     * Either one of BSH / BLM parameters may be used to calculate the CP/M block size
     */
    public final int bsh;

    /**
     * Block Mask - Block Size is given by 128 * (BLM +1)
     * Either one of BSH / BLM parameters may be used to calculate the CP/M block size
     */
    public final int blm;

    /**
     * Extent Mask
     *
     * Large CP/M files are divided into EXTENTS of 16K. In early versions of CP/M 'Logical and 'Physical' Extents
     * were both 16K; but in CP/M 2.2 and later a 'Logical' extent is 16K but a 'Physical' extent can be 16K, 32K, 64K,
     * 128K or 256K, and this is indicated by setting EXM to either 0, 1, 3, 7, or 15 ( i.e. EXM is set to 1 less than
     * the number of 'Logical' extents in a 'Physical' extent ).
     *
     * It is used for computing total number of records: (EX & exm) * 128 + RC
     * Also, extent number = ((32*S2)+EX) / (exm + 1)
     */
    public final byte exm;

    /**
     * Highest Block Number
     * This indicates the HIGHEST PERMISSIBLE BLOCK NUMBER; numbering starts at 0, so the TOTAL NUMBER OF BLOCKS is
     * 1 more than the value shown.
     */
    public final int dsm;

    /**
     * Highest Directory Entry Number
     * This indicates the HIGHEST PERMISSIBLE DIRECTORY NUMBER; numbering starts at 0 so the
     * MAXIMUM NUMBER OF DIRECTORY ENTRIES is 1 more than the value shown.
     */
    public final int drm;

    /**
     * AL0 - AL1	16-bit Directory Allocation Pattern
     *
     * These parameters, taken BITWISE, indicate which of the first 16 CP/M blocks ( numbered 00 - 15 ) are allocated
     * to the Directory. For example:-
     *
     * $80 00 = 10000000 00000000 i.e. the Directory is allocated BLOCK 00 only
     * $C0 00= 11000000 00000000 i.e. the Directory is allocated BLOCKS 00 & 01
     */
    public final int al01;

    /**
     * No. of reserved tracks
     * This shows the location of CP/M BLOCK 00 and also, by implication, how many tracks are reserved for
     * bootstrap & system files. Thus, '5' on the MZ-80K shows that Block 00 is at the start of track 5, and
     * Tracks 0 - 4 are used for bootstrap & system files; whereas '1' on the MZ-80B shows that Block 00 is at the
     * start of Track 1, and Track 0 is used for bootstrap & system files.
     */
    public final int ofs;

    public DiskParameterBlock(int spt, int bsh, int blm, int dsm, int drm, int al01, int ofs) {
        if (bsh < 3 || bsh > 7) {
            throw new IllegalArgumentException("Invalid BSH");
        }
        if (blm < 7 || blm > 127) {
            throw new IllegalArgumentException("Invalid BLM");
        }

        if (dsm <= 255) {
            this.exm = (byte)((1 << (bsh - 3)) - 1);
        } else {
            if (bsh == 3) {
                throw new IllegalArgumentException("For given BSH, DSM must be < 256");
            }
            this.exm = (byte)((1 << (bsh - 4)) - 1);
        }

        this.spt = spt;
        this.bsh = bsh;
        this.blm = blm;
        this.dsm = dsm;
        this.drm = drm;
        this.al01 = al01;
        this.ofs = ofs;
    }

    public static DiskParameterBlock fromBSH(int spt, int bsh, int dsm, int drm, int al01, int ofs) {
        return new DiskParameterBlock(
            spt, bsh, (1 << bsh) - 1, dsm, drm, al01, ofs
        );
    }

    public static DiskParameterBlock fromBLM(int spt, int blm, int dsm, int drm, int al01, int ofs) {
        return new DiskParameterBlock(
            spt, (int)(Math.log(blm + 1) / Math.log(2)), blm, dsm, drm, al01, ofs
        );
    }

    @Override
    public String toString() {
        return "DPB:\n" +
            "  spt=" + spt + "\n" +
            "  bsh=" + bsh + "\n" +
            "  blm=" + blm + "\n" +
            "  exm=" + exm + "\n" +
            "  dsm=" + dsm + "\n" +
            "  drm=" + drm + "\n" +
            "  al0=" + (Integer.toBinaryString(al01 & 0xFF)) + "\n" +
            "  al1=" + (Integer.toBinaryString(al01 >>> 8)) + "\n" +
            "  ofs=" + ofs + "\n";
    }
}
