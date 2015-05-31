package net.sf.emustudio.devices.mits88disk.cpmfs;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.Objects;

@Immutable
public class CpmFile {
    public final String fileName;
    public final String fileExt;
    public final int status;
    public final int extentNumber;
    public final int bc;
    public final int rc;

    public CpmFile(String fileName, String fileExt, int status, int extentNumber, int bc, int rc) {
        this.status = status;
        this.extentNumber = extentNumber;
        this.bc = bc;
        this.rc = rc;
        this.fileName = Objects.requireNonNull(fileName);
        this.fileExt = Objects.requireNonNull(fileExt);
    }

    @Override
    public String toString() {
        String result = fileName.trim();

        String ext = fileExt.trim();
        if (!ext.equals("")) {
            result += "." + ext;
        }
        return result;
    }

    public static CpmFile fromEntry(ByteBuffer entry) {
        int fileStatus = entry.get() & 0xFF;

        byte[] fileNameBytes = new byte[11];
        entry.get(fileNameBytes);
        String fileName = new String(fileNameBytes);

        int extent = entry.get();
        int bc = entry.get(); // Bc
        extent = (entry.get() << 8) | extent;
        int rc = entry.get(); // Rc

        return new CpmFile(fileName.substring(0, 8), fileName.substring(8, 11), fileStatus, extent, bc, rc);
    }

}
