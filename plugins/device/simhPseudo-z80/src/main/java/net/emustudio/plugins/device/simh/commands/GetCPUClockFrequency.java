package net.emustudio.plugins.device.simh.commands;

import java.util.concurrent.atomic.AtomicInteger;

public class GetCPUClockFrequency implements Command {
    public final static GetCPUClockFrequency INS = new GetCPUClockFrequency();

    private int getClockFrequencyPos = 0; // determines state for receiving the clock frequency
    private final AtomicInteger cpuFreq = new AtomicInteger();

    @Override
    public void reset() {
        getClockFrequencyPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getClockFrequencyPos == 0) {
            cpuFreq.set(control.getCpu().getCPUFrequency());
            result = (byte)(cpuFreq.get() & 0xff);
            getClockFrequencyPos = 1;
        } else {
            result = (byte)((cpuFreq.get() >> 8) & 0xff);
            getClockFrequencyPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        getClockFrequencyPos = 0;
    }
}
