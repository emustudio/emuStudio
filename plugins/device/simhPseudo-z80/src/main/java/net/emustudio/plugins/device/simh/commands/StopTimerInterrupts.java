package net.emustudio.plugins.device.simh.commands;

public class StopTimerInterrupts implements Command {
    public final static StopTimerInterrupts INS = new StopTimerInterrupts();

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
        StartTimerInterrupts.INS.reset();
    }
}
