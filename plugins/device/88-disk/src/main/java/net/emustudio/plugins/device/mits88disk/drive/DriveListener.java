package net.emustudio.plugins.device.mits88disk.drive;

public interface DriveListener {

    void driveSelect(boolean sel);

    void driveParamsChanged(DriveParameters parameters);
}
