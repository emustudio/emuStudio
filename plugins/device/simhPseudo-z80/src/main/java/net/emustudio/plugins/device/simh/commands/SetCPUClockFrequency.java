package net.emustudio.plugins.device.simh.commands;

public class SetCPUClockFrequency implements Command {
    public final static SetCPUClockFrequency INS = new SetCPUClockFrequency();

    private int newClockFrequency;
    private int setClockFrequencyPos = 0; // determines state for sending the clock frequency

    @Override
    public void reset() {
        setClockFrequencyPos = 0;
    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setClockFrequencyPos == 0) {
            newClockFrequency = data;
            setClockFrequencyPos = 1;
        } else {
            control.getCpu().setCPUFrequency((data << 8) | newClockFrequency);
            setClockFrequencyPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        setClockFrequencyPos = 0;
    }
}
