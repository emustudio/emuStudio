package net.emustudio.plugins.device.simh.commands;

public class GetHostOSPathSeparator implements Command {
    public final static GetHostOSPathSeparator INS = new GetHostOSPathSeparator();

    @Override
    public void reset() {

    }

    @Override
    public byte read(Control control) {
        return (byte) GetHostFilenames.hostPathSeparator;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {

    }
}
