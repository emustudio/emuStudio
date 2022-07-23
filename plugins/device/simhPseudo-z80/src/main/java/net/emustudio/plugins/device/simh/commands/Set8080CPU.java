package net.emustudio.plugins.device.simh.commands;

public class Set8080CPU implements Command {
    public final static Set8080CPU INS = new Set8080CPU();

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
        System.out.println("SIMH: Set 8080 CPU command not supported!");
    }
}
