package net.emustudio.plugins.device.simh.commands;

public class ResetPTR implements Command {
    public final static ResetPTR INS = new ResetPTR();

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
        //ptr_reset( & ptr_dev);
    }
}
