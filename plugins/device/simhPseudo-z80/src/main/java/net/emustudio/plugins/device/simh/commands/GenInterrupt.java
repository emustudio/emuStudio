package net.emustudio.plugins.device.simh.commands;

public class GenInterrupt implements Command {
    public final static GenInterrupt INS = new GenInterrupt();

    private int genInterruptPos = 0; // determines state for receiving interrupt vector and data
    private int genInterruptVec = 0; // stores interrupt vector

    @Override
    public void reset() {

    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (genInterruptPos == 0) {
            genInterruptVec = data; // interrupt vector is not used.
            genInterruptPos = 1;
            System.out.println("genInterruptVec=" + genInterruptVec + " genInterruptPos=" + genInterruptPos);
        } else {
            control.getCpu().signalInterrupt(control.getDevice(), new byte[]{data});
            genInterruptPos = 0;
            control.clearCommand();
            System.out.printf(
                "genInterruptVec=%d vectorInterrupt=%X dataBus=%02X genInterruptPos=%d\n",
                genInterruptVec, 1 << genInterruptVec, data, genInterruptPos);
        }
    }

    @Override
    public void start(Control control) {

    }
}
