package net.emustudio.plugins.device.simh.commands;

public class PrintTime implements Command {
    public final static PrintTime INS = new PrintTime();

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
        System.out.printf("SIMH: Current time in milliseconds = %d.\n", System.currentTimeMillis());
    }
}
