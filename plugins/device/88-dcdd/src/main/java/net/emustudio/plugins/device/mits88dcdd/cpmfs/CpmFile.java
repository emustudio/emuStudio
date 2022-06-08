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

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.*;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;


/**
 * CP/M file entry.
 *
 * @see <a href="https://www.seasip.info/Cpm/format22.html">CP/M directory format</a>
 */
@Immutable
public class CpmFile {
    public final static int ENTRY_SIZE = 32;
    public final static int RAW_BLOCK_POINTERS_COUNT = 16;
    private final static String INVALID_CHARS_REGEX = "[<>.,;:=?*\\[\\]]";

    public final byte status;

    public final String fileName;
    public final String fileExt;
    public final boolean readOnly;
    public final boolean invisible;
    public final boolean archived;

    public final byte ex; // 0-4 bits
    public final byte s2; // 0-5 bits
    public final int entryNumber; // ((32*S2)+EX) / (exm+1)  ; the same as (s2 << 5 | ex) / (exm+1)

    public final byte rc; // the number of 128 byte records of the last used logical extent.
    public final byte bc; // the number of bytes in the last used record
    public final int numberOfRecords; // number of records used in this extent (EX & exm) * 128 + RC
    public final List<Byte> bp; // allocation

    /**
     * Constructs a CP/M file using raw extent number
     *
     * @param status       user number of file status
     * @param fileName     file name
     * @param readOnly     read only?
     * @param invisible    invisible?
     * @param archived     archived?
     * @param extentNumber raw extent number - i.e. raw entry index across all directory blocks
     * @param exm          extent mask
     * @param bc           bytes count in this extent
     * @param rc           records count in this extent
     * @param bp           extents
     */
    CpmFile(byte status, String fileName, boolean readOnly, boolean invisible, boolean archived,
            int extentNumber, byte exm, byte bc, byte rc, List<Byte> bp) {
        if (fileName.matches(INVALID_CHARS_REGEX)) {
            // https://linux.die.net/man/5/cpm
            throw new IllegalArgumentException("File name contains invalid chars!");
        }

        this.bp = Collections.unmodifiableList(Objects.requireNonNull(bp));
        int lastDot = fileName.lastIndexOf('.');
        this.fileName = fileName.substring(0, (lastDot == -1) ? fileName.length() : lastDot).toUpperCase(Locale.ENGLISH);
        this.fileExt = (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toUpperCase(Locale.ENGLISH);

        this.ex = (byte) (extentNumber & 0x1F);
        this.s2 = (byte) ((extentNumber >> 5) & 0x3F);

        this.status = status;
        this.readOnly = readOnly;
        this.invisible = invisible;
        this.archived = archived;
        this.entryNumber = ((32 * s2) + ex) / (exm + 1);
        this.bc = bc;
        this.rc = rc;
        this.numberOfRecords = (ex & exm) * RECORD_SIZE + rc;
    }

    private CpmFile(byte status, String fileName, String fileExt, boolean readOnly, boolean invisible, boolean archived,
                    byte ex, byte s2, byte exm, byte bc, byte rc, List<Byte> bp) {
        if (fileName.matches(INVALID_CHARS_REGEX) || fileExt.matches(INVALID_CHARS_REGEX)) {
            // https://linux.die.net/man/5/cpm
            throw new IllegalArgumentException("File name contains invalid chars!");
        }
        this.bp = Collections.unmodifiableList(Objects.requireNonNull(bp));

        this.status = status;
        this.fileName = Objects.requireNonNull(fileName);
        this.fileExt = Objects.requireNonNull(fileExt);
        this.readOnly = readOnly;
        this.invisible = invisible;
        this.archived = archived;

        this.ex = ex;
        this.s2 = s2;
        this.entryNumber = ((32 * s2) + ex) / (exm + 1);

        this.bc = bc;
        this.rc = rc;
        this.numberOfRecords = (ex & exm) * RECORD_SIZE + rc;
    }

    public String getFileName() {
        String result = fileName.trim();

        String ext = fileExt.trim();
        if (!ext.equals("")) {
            result += "." + ext;
        }
        return result;
    }

    static CpmFile fromEntry(ByteBuffer entry, byte exm) {
        byte status = (byte) (entry.get() & 0xFF);

        byte[] fileNameBytes = new byte[11];
        entry.get(fileNameBytes);
        boolean readOnly = (fileNameBytes[8] & 0x80) == 0x80;
        boolean invisible = (fileNameBytes[9] & 0x80) == 0x80;
        boolean archived = (fileNameBytes[10] & 0x80) == 0x80;

        fileNameBytes[8] = (byte) (fileNameBytes[8] & 0x7F);
        fileNameBytes[9] = (byte) (fileNameBytes[9] & 0x7F);
        fileNameBytes[10] = (byte) (fileNameBytes[10] & 0x7F);
        String fileNameExt = new String(fileNameBytes);
        String fileName = fileNameExt.substring(0, 8);
        String fileExt = fileNameExt.substring(8);

        byte ex = (byte) (entry.get() & 0x1F); // only bits 0-4 are valid
        byte bc = entry.get();
        byte s2 = (byte) (entry.get() & 0x3F); // only bits 0-5 are valid
        byte rc = entry.get();

        List<Byte> extents = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            byte bp = entry.get();
            extents.add(bp);
        }

        return new CpmFile(status, fileName, fileExt, readOnly, invisible, archived, ex, s2, exm, bc, rc, extents);
    }

    ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(status);

        byte[] fileNameBytes = new byte[11];
        int i;
        for (i = 0; i < fileName.length(); i++) {
            fileNameBytes[i] = (byte) (fileName.charAt(i) & 0x7F);
        }
        for (; i < 8; i++) {
            fileNameBytes[i] = 0x20; // space
        }
        for (; i < fileExt.length(); i++) {
            fileNameBytes[i] = (byte) (fileExt.charAt(i) & 0x7F);
        }
        for (; i < 11; i++) {
            fileNameBytes[i] = 0x20; // space
        }
        if (readOnly) {
            fileNameBytes[8] |= 0x80;
        }
        if (invisible) {
            fileNameBytes[9] |= 0x80;
        }
        if (archived) {
            fileNameBytes[10] |= 0x80;
        }
        entry.put(fileNameBytes);

        entry.put(ex);
        entry.put(bc);
        entry.put(s2);
        entry.put(rc);

        for (byte bPointer : bp) {
            entry.put(bPointer);
        }
        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    @Override
    public String toString() {
        return "  status=" + status + "\n" +
            "filename='" + getFileName() + "'\n" +
            "   flags=" + (readOnly ? "R" : "") + (invisible ? "I" : "") + (archived ? "A" : "") + "\n" +
            "      ex=" + ex + "\n" +
            "      s2=" + s2 + "\n" +
            "   entry=" + entryNumber + "\n" +
            "      rc=" + rc + "\n" +
            "      bc=" + bc + "\n" +
            "      bp=" + bp + "\n";
    }
}
