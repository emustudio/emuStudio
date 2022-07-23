package net.emustudio.plugins.device.simh.commands;

public class GetCommon implements Command {
    public final static GetCommon INS = new GetCommon();

    private int getCommonPos = 0; // determines state for sending the 'common' register

    @Override
    public void reset() {
        getCommonPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result;
        if (getCommonPos == 0) {
            result = (byte)(control.getMemory().getCommonBoundary() & 0xff);
            getCommonPos = 1;
        } else {
            result = (byte)((control.getMemory().getCommonBoundary() >> 8) & 0xff);
            getCommonPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        getCommonPos = 0;
    }
}
