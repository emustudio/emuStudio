/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

// https://manpages.debian.org/testing/cpmtools/cpm.5.en.html
//CP/M Plus support disc labels, which are stored in an arbitrary directory entry. The structure of disc labels is:
//
//1 byte status 0x20
//F0-E2 are the disc label
//
//1 byte mode: bit 7 activates password protection, bit 6 causes time stamps on access, but 5 causes time stamps on
// modifications, bit 4 causes time stamps on creation and bit 0 is set when a label exists. Bit 4 and 6 are exclusively
// set.
//
//1 byte password decode byte: To decode the password, xor this byte with the password bytes in reverse order.
// To encode a password, add its characters to get the decode byte.

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;

//2 reserved bytes
//8 password bytes
//4 bytes label creation time stamp
//4 bytes label modification time stamp
@Immutable
public class CpmPlusDiscLabel implements CpmEntry {
    public final static byte STATUS_LABEL = 0x20;

    public static final int MODE_LABEL_EXISTS = 0x01;
    public static final int MODE_TIMESTAMPS_ON_CREATION = 0x10;
    public static final int MODE_TIMESTAMPS_ON_MODIFICATION = 0x20;
    public static final int MODE_TIMESTAMPS_ON_ACCESS = 0x40;
    public static final int MODE_PASSWORD_PROTECTION = 0x80;

    public final String label;
    public final byte mode;
    public final byte passwordDecodeByte;
    public final byte[] password; // 8 bytes
    public final DateStamp createStamp;
    public final DateStamp modifyStamp;

    public CpmPlusDiscLabel(String label, byte mode, byte passwordDecodeByte, byte[] password,
                            DateStamp createStamp, DateStamp modifyStamp) {
        this.label = Objects.requireNonNull(label);
        this.mode = mode;
        this.passwordDecodeByte = passwordDecodeByte;
        this.password = Objects.requireNonNull(password);
        this.createStamp = Objects.requireNonNull(createStamp);
        this.modifyStamp = Objects.requireNonNull(modifyStamp);
    }

    public static CpmPlusDiscLabel fromEntry(ByteBuffer entry) {
        byte status = entry.get();
        if (status != STATUS_LABEL) {
            throw new IllegalArgumentException("Invalid entry status (expected " + STATUS_LABEL + ")");
        }
        byte[] labelBytes = new byte[11];
        entry.get(labelBytes);
        for (int i = 0; i < labelBytes.length; i++) {
            labelBytes[i] = (byte) (labelBytes[i] & 0x7F);
        }
        String label = new String(labelBytes);

        byte mode = entry.get();
        byte passwordDecodeByte = entry.get();

        // 2 reserved bytes
        entry.get();
        entry.get();

        // 8 password bytes
        byte[] password = new byte[8];
        entry.get(password);

        // 4 bytes creation timestamp (days low, days high, hour BCD, minute BCD)
        DateStamp createStamp = readDateStamp(entry);
        // 4 bytes modification timestamp
        DateStamp modifyStamp = readDateStamp(entry);

        return new CpmPlusDiscLabel(label, mode, passwordDecodeByte, password, createStamp, modifyStamp);
    }

    private static DateStamp readDateStamp(ByteBuffer entry) {
        int days = (entry.get() & 0xFF) | ((entry.get() & 0xFF) << 8);
        int hour = net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin(entry.get() & 0xFF);
        int minute = net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin(entry.get() & 0xFF);
        return new DateStamp(days, hour, minute);
    }

    private static void writeDateStamp(ByteBuffer entry, DateStamp stamp) {
        entry.put((byte) (stamp.days & 0xFF));
        entry.put((byte) (stamp.days >>> 8));
        entry.put((byte) net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd(stamp.hour & 0xFF));
        entry.put((byte) net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd(stamp.minute & 0xFF));
    }

    public ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(STATUS_LABEL);

        byte[] labelBytes = new byte[11];
        int i;
        for (i = 0; i < label.length() && i < 11; i++) {
            labelBytes[i] = (byte) label.charAt(i);
        }
        for (; i < 11; i++) {
            labelBytes[i] = 0x20; // space
        }
        entry.put(labelBytes);
        entry.put(mode);
        entry.put(passwordDecodeByte);

        // 2 reserved bytes
        entry.put((byte) 0);
        entry.put((byte) 0);

        // 8 password bytes
        byte[] pw = new byte[8];
        System.arraycopy(password, 0, pw, 0, Math.min(password.length, 8));
        entry.put(pw);

        // timestamps
        writeDateStamp(entry, createStamp);
        writeDateStamp(entry, modifyStamp);

        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(label.trim()).append("'");
        sb.append("; mode=").append(Integer.toHexString(mode & 0xFF));
        if ((mode & MODE_LABEL_EXISTS) != 0) sb.append(" [label]");
        if ((mode & MODE_TIMESTAMPS_ON_CREATION) != 0) sb.append(" [create-ts]");
        if ((mode & MODE_TIMESTAMPS_ON_MODIFICATION) != 0) sb.append(" [modify-ts]");
        if ((mode & MODE_TIMESTAMPS_ON_ACCESS) != 0) sb.append(" [access-ts]");
        if ((mode & MODE_PASSWORD_PROTECTION) != 0) sb.append(" [password]");
        sb.append("; created=").append(createStamp);
        sb.append("; modified=").append(modifyStamp);
        return sb.toString();
    }
}
