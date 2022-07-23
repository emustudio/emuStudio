package net.emustudio.plugins.device.simh.commands;

public class SetTimerInterruptAdr implements Command {
    public static final SetTimerInterruptAdr INS = new SetTimerInterruptAdr();

    private int setTimerInterruptAdrPos = 0; // determines state for receiving timerInterruptHandler
    public int timerInterruptHandler = 0x0fc00; // default address of interrupt handling routine

    @Override
    public void reset() {
        setTimerInterruptAdrPos = 0;
    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setTimerInterruptAdrPos == 0) {
            timerInterruptHandler = data;
            setTimerInterruptAdrPos = 1;
        } else {
            timerInterruptHandler |= (data << 8);
            setTimerInterruptAdrPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        reset();
    }
}
