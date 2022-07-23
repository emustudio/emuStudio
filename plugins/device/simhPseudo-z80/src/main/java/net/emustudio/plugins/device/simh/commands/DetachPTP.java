package net.emustudio.plugins.device.simh.commands;

public class DetachPTP implements Command {
    public final static DetachPTP INS = new DetachPTP();

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
        //detach_unit( & ptp_unit);
    }
}
