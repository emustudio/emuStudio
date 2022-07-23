package net.emustudio.plugins.device.simh.commands;

public class HasBankedMemory implements Command {
    public final static HasBankedMemory INS = new HasBankedMemory();

    @Override
    public void reset() {

    }

    @Override
    public byte read(Control control) {
        byte result = (byte)control.getMemory().getBanksCount();
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
