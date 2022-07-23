package net.emustudio.plugins.device.simh.commands;

public class ResetStopWatch implements Command {
    public final static ResetStopWatch INS = new ResetStopWatch();

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
        ReadStopWatch.INS.stopWatchNow = System.currentTimeMillis();
    }
}
