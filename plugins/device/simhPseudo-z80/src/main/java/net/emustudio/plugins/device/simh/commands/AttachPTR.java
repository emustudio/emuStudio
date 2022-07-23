package net.emustudio.plugins.device.simh.commands;

public class AttachPTR implements Command {
    public final static AttachPTR INS = new AttachPTR();

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
        //attachCPM( & ptr_unit);
    }
}
