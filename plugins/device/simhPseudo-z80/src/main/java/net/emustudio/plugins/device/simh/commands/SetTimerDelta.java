package net.emustudio.plugins.device.simh.commands;

public class SetTimerDelta implements Command {
    public final static SetTimerDelta INS = new SetTimerDelta();

    private final static int DEFAULT_TIMER_DELTA = 100; // default value for timer delta in ms

    private int setTimerDeltaPos = 0; // determines state for receiving timerDelta
    public int timerDelta = DEFAULT_TIMER_DELTA;  // interrupt every 100 ms

    @Override
    public void reset() {
        setTimerDeltaPos = 0;
    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setTimerDeltaPos == 0) {
            timerDelta = data;
            setTimerDeltaPos = 1;
        } else {
            timerDelta |= (data << 8);
            setTimerDeltaPos = 0;
            control.clearCommand();
            if (timerDelta == 0) {
                timerDelta = DEFAULT_TIMER_DELTA;
                System.out.println("SIMH: Timer delta set to 0 ms ignored. Using " + DEFAULT_TIMER_DELTA + " ms instead.");
            }
        }
    }

    @Override
    public void start(Control control) {
        reset();
    }
}
