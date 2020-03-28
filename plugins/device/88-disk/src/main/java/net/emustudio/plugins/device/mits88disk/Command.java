package net.emustudio.plugins.device.mits88disk;

import net.emustudio.plugins.device.mits88disk.cpmfs.DriveIO;

import java.io.IOException;

public interface Command {

    void execute(DriveIO driveIO) throws IOException;
}
