package net.emustudio.plugins.device.simh.commands;

public class GetBankSelect implements Command {
    public final static GetBankSelect INS = new GetBankSelect();

    @Override
    public void reset() {

    }

    @Override
    public byte read(Control control) {
        byte result = (byte)control.getMemory().getSelectedBank();
        control.clearCommand();
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {

    }
}
