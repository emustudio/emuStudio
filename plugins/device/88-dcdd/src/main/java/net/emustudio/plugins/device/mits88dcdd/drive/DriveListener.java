package net.emustudio.plugins.device.mits88dcdd.drive;

public interface DriveListener {

    void driveSelect(boolean sel);

    void driveParamsChanged(DriveParameters parameters);
}
