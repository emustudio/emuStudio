package net.sf.emustudio.cpu.testsuite;

import emulib.plugins.cpu.CPU;

public class RunStateListenerStub implements CPU.CPUListener {
    public CPU.RunState runState;

    @Override
    public void runStateChanged(CPU.RunState runState) {
        this.runState = runState;
    }

    @Override
    public void internalStateChanged() {

    }
}
