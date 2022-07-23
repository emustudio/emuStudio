package net.emustudio.plugins.device.simh.commands;

public class AttachPTP implements Command {
    public final static AttachPTP INS = new AttachPTP();
    private int lastCPMStatus = 0; // result of last attachCPM command

    @Override
    public void reset() {
        lastCPMStatus = 0;
    }

    @Override
    public byte read(Control control) {
        byte result = (byte) lastCPMStatus;
        control.clearCommand();
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
       // attachCPM( & ptp_unit);
    }



}
