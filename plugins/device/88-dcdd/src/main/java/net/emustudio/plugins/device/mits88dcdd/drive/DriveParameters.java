package net.emustudio.plugins.device.mits88dcdd.drive;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.jcip.annotations.Immutable;

import java.nio.file.Path;

@Immutable
public final class DriveParameters {
    public final short port1status;
    public final String port1statusString;

    public final short port2status;
    public final String port2statusString;

    public final int track;
    public final int sector;
    public final int sectorOffset;

    public final Path mountedFloppy;

    public DriveParameters(short port1status, short port2status, int track, int sector, int sectorOffset,
                           Path mountedFloppy) {
        this.port1status = port1status;
        this.port1statusString = port1StatusString(port1status);

        this.port2status = port2status;
        this.port2statusString = port2StatusString(port2status);

        this.track = track;
        this.sector = sector;
        this.sectorOffset = sectorOffset;
        this.mountedFloppy = mountedFloppy;
    }

    public static String port1StatusString(int status) {
        String sb = "";

        if ((status & 128) == 0) sb += "R ";
        else sb += ". ";

        if ((status & 64) == 0) sb += "Z ";
        else sb += ". ";

        if ((status & 32) == 0) sb += "I ";
        else sb += ". ";

        if ((status & 4) == 0) sb += "H ";
        else sb += ". ";

        if ((status & 2) == 0) sb += "M ";
        else sb += ". ";

        if ((status & 1) == 0) sb += "W ";
        else sb += ". ";

        return sb;
    }

    public static String port2StatusString(int status) {
        boolean sr0 = (status & 1) == 0;
        int offset = (status >>> 1) & 0b11111;

        return  (sr0 ? "(SR0) " : "      ") + "0x" + RadixUtils.formatByteHexString(offset);
    }

    @Override
    public String toString() {
        return
            "T=" + RadixUtils.formatByteHexString(track) +
            "; S=" + RadixUtils.formatByteHexString(sector) +
            "; O=" + RadixUtils.formatByteHexString(sectorOffset) +
            "; port1=[" + port1statusString + "]" +
            "; port2=[" + port2statusString + "]";
    }
}
