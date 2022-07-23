package net.emustudio.plugins.device.simh.commands;

public class SetBankSelect implements Command {
    public final static SetBankSelect INS = new SetBankSelect();

    @Override
    public void reset() {

    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        control.getMemory().selectBank(data);
        control.clearCommand();
    }

    @Override
    public void start(Control control) {

    }
}
