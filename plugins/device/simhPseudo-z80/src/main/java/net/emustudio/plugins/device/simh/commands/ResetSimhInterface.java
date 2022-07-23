package net.emustudio.plugins.device.simh.commands;

public class ResetSimhInterface implements Command {
    public final static ResetSimhInterface INS = new ResetSimhInterface();

    @Override
    public void reset() {

    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        StartTimer.INS.markTimeSP = 0;
        control.clearCommand();
        GetHostFilenames.INS.reset();
    }
}
