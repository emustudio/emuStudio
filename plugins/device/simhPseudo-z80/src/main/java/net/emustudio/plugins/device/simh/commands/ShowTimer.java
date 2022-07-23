package net.emustudio.plugins.device.simh.commands;

public class ShowTimer implements Command {
    public final static ShowTimer INS = new ShowTimer();

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
        if (StartTimer.INS.markTimeSP > 0) {
            long delta = System.currentTimeMillis() - StartTimer.INS.markTime[StartTimer.INS.markTimeSP - 1];
            System.out.printf("SIMH: Timer running. Elapsed in milliseconds = %d.\n", delta);
        } else {
            System.out.println("SIMH: No timer active.");
        }
    }
}
