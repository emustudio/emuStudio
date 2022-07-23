package net.emustudio.plugins.device.simh.commands;

public class SIMHSleep implements Command {
    public final static SIMHSleep INS = new SIMHSleep();
    private final static int SIMHSleepMillis = 1;

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
        do_SIMH_sleep();
    }

    private void do_SIMH_sleep() {
        // TODO:
        // Do not sleep when timer interrupts are pending or are about to be created.
        // Otherwise there is the possibility that such interrupts are skipped.

        // time to sleep and SIO not attached to a file.
        try {
            Thread.sleep(SIMHSleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
