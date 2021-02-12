package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.DriveIO;

import java.io.IOException;

public interface Command {

    void execute(DriveIO driveIO) throws IOException;
}
