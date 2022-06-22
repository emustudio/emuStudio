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
public class CpmPlusDiscLabel {
    public final static int STATUS_LABEL = 0x20;

    public final byte status; // 0x20
    public final String label;
    public final byte mode;
    public final byte passwordDecodeByte;

    public CpmPlusDiscLabel(byte status, String label, byte mode, byte passwordDecodeByte) {
        this.status = status;
        this.label = Objects.requireNonNull(label);
        this.mode = mode;
        this.passwordDecodeByte = passwordDecodeByte;
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
        return new CpmPlusDiscLabel(status, label, mode, passwordDecodeByte);
    }

    public ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(status);

        byte[] labelBytes = new byte[11];
        int i;
        for (i = 0; i < label.length(); i++) {
            labelBytes[i] = (byte) label.charAt(i);
        }
        for (; i < 11; i++) {
            labelBytes[i] = 0x20; // space
        }
        entry.put(labelBytes);
        entry.put(mode);
        entry.put(passwordDecodeByte);

        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    @Override
    public String toString() {
        return "'" + label + "'; mode=" + Integer.toHexString(mode) + "; passwordByte=" +
            Integer.toHexString(passwordDecodeByte);
    }
}
