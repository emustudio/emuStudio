package net.emustudio.plugins.device.simh.commands;

public class SetZ80CPU implements Command {
    public final static SetZ80CPU INS = new SetZ80CPU();

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
        System.out.println("SIMH: Set Z80 CPU command not supported!");
    }
}
