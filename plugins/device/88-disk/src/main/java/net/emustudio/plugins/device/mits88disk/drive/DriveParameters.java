package net.emustudio.plugins.device.mits88disk.drive;

import net.jcip.annotations.Immutable;

import java.nio.file.Path;

@Immutable
public final class DriveParameters {
    public final short port1status;
    public final short port2status;

    public final int track;
    public final int sector;
    public final int sectorOffset;

    public final Path mountedFloppy;

    public DriveParameters(short port1status, short port2status, int track, int sector, int sectorOffset,
                           Path mountedFloppy) {
        this.port1status = port1status;
        this.port2status = port2status;
        this.track = track;
        this.sector = sector;
        this.sectorOffset = sectorOffset;
        this.mountedFloppy = mountedFloppy;
    }
}
