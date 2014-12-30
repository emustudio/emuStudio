package net.sf.emustudio.intel8080.impl;

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
