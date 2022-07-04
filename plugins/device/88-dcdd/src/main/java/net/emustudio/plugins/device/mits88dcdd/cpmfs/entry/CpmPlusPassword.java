package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import java.nio.ByteBuffer;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;

// https://manpages.debian.org/testing/cpmtools/cpm.5.en.html
//CP/M Plus supports passwords, which are stored in an arbitrary directory entry. The structure of these entries is:
//
//1 byte status (user number plus 16)
//F0-E2 are the file name and its extension.
//1 byte password mode: bit 7 means password required for reading, bit 6 for writing and bit 5 for deleting.
//1 byte password decode byte: To decode the password, xor this byte with the password bytes in reverse order.
// To encode a password, add its characters to get the decode byte.
//2 reserved bytes
//8 password bytes
public class CpmPlusPassword implements CpmEntry {
    public final byte status; // (user number plus 16)
    public final String fileName;
    public final String fileExt;

    public final byte mode;
    public final byte passwordDecodeByte;
    public final byte[] password;
    public final String passwordString;


    public CpmPlusPassword(byte status, String fileName, String fileExt, byte mode, byte passwordDecodeByte,
                           byte[] password) {
        this.status = status;
        this.fileName = Objects.requireNonNull(fileName);
        this.fileExt = Objects.requireNonNull(fileExt);
        this.mode = mode;
        this.passwordDecodeByte = passwordDecodeByte;
        this.password = Objects.requireNonNull(password);
        this.passwordString = new String(password);
    }

    public static CpmPlusPassword fromEntry(ByteBuffer entry) {
        byte status = entry.get();

        byte[] fileNameBytes = new byte[11];
        entry.get(fileNameBytes);

        String fileNameExt = new String(fileNameBytes);
        String fileName = fileNameExt.substring(0, 8);
        String fileExt = fileNameExt.substring(8);

        byte mode = entry.get();
        byte passwordDecodeByte = entry.get();

        entry.get();
        entry.get(); // 2 bytes reserved

        byte[] password = new byte[8];
        entry.get(password);

        return new CpmPlusPassword(status, fileName, fileExt, mode, passwordDecodeByte, password);
    }

    public ByteBuffer toEntry() {
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

        entry.put(fileNameBytes);
        entry.put(mode);
        entry.put(passwordDecodeByte);
        entry.put((byte) 0);
        entry.put((byte) 0); // 2 bytes reserved
        entry.put(password);

        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    public String getFileName() {
        String result = fileName.trim();

        String ext = fileExt.trim();
        if (!ext.equals("")) {
            result += "." + ext;
        }
        return result;
    }

    @Override
    public String toString() {
        return "'" + getFileName() + "'; mode=" + Integer.toHexString(mode) + "; passwordByte=" +
            Integer.toHexString(passwordDecodeByte) + "; password='" + passwordString + "'";
    }
}
