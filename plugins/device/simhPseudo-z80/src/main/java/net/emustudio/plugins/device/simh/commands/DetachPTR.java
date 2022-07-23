package net.emustudio.plugins.device.simh.commands;

public class DetachPTR implements Command {
    public final static DetachPTR INS = new DetachPTR();

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
        //detach_unit( & ptr_unit);
    }
}
