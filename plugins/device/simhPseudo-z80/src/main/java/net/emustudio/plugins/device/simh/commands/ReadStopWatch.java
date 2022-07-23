package net.emustudio.plugins.device.simh.commands;

public class ReadStopWatch implements Command {
    public final static ReadStopWatch INS = new ReadStopWatch();

    private int getStopWatchDeltaPos = 0; // determines the state for receiving stopWatchDelta
    private long stopWatchDelta = 0; // stores elapsed time of stop watch
    public long stopWatchNow = 0; // stores starting time of stop watch


    @Override
    public void reset() {
        getStopWatchDeltaPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getStopWatchDeltaPos == 0) {
            result = (byte) (stopWatchDelta & 0xff);
            getStopWatchDeltaPos = 1;
        } else {
            result = (byte) ((stopWatchDelta >> 8) & 0xff);
            getStopWatchDeltaPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        getStopWatchDeltaPos = 0;
        stopWatchDelta = System.currentTimeMillis() - stopWatchNow;
    }
}
