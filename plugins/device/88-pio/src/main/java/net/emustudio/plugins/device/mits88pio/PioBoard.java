/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88pio.api.PioContext;

/** 88-4PIO address wiring places control at even and DDR/data at odd addresses. */
public final class PioBoard implements Context8080.CpuPortDevice, PioContext {
    private final int basePort;
    private final Pia6820[] pias;
    private Peripheral peripheral;
    private Runnable interrupt = () -> { };
    private boolean irqPending;

    public PioBoard(int basePort, int piaCount) {
        if (basePort < 0 || basePort > 0xF0 || (basePort & 15) != 0) {
            throw new IllegalArgumentException("88-4PIO basePort must be aligned to 16 ports, from 00h to F0h");
        }
        if (piaCount < 1 || piaCount > 4) {
            throw new IllegalArgumentException("piaCount must be between 1 and 4");
        }
        this.basePort = basePort;
        pias = new Pia6820[piaCount];
        for (int i = 0; i < piaCount; i++) {
            final int chip = i;
            pias[i] = new Pia6820(new Pia6820.Signals() {
                @Override public void outputChanged(int side, int data, int mask) {
                    if (peripheral != null) { peripheral.outputChanged(chip * 2 + side, data, mask); }
                }
                @Override public void controlOutputChanged(int side, boolean high) {
                    if (peripheral != null) { peripheral.controlOutputChanged(chip * 2 + side, high); }
                }
                @Override public void interruptChanged() { updateInterrupt(); }
            });
        }
    }

    @Override
    public synchronized byte read(int portAddress) {
        int offset = (portAddress & 255) - basePort;
        if (offset < 0 || offset >= getPortCount()) { return (byte) 255; }
        Pia6820 pia = pias[offset / 4];
        int side = (offset / 2) & 1;
        return (byte) ((offset & 1) == 0 ? pia.readControl(side) : pia.readData(side));
    }

    @Override
    public synchronized void write(int portAddress, byte data) {
        int offset = (portAddress & 255) - basePort;
        if (offset < 0 || offset >= getPortCount()) { return; }
        Pia6820 pia = pias[offset / 4];
        int side = (offset / 2) & 1;
        if ((offset & 1) == 0) { pia.writeControl(side, data & 255); }
        else { pia.writeData(side, data & 255); }
    }

    public int getPortCount() { return pias.length * 4; }
    @Override public int getChannelCount() { return pias.length * 2; }
    @Override public String getName() { return "MITS 88-4PIO"; }

    @Override
    public synchronized void attachPeripheral(Peripheral peripheral) {
        if (peripheral == null) { throw new NullPointerException("peripheral"); }
        if (this.peripheral != null && this.peripheral != peripheral) {
            throw new IllegalStateException("88-4PIO already has a peripheral attached");
        }
        this.peripheral = peripheral;
        for (int channel = 0; channel < getChannelCount(); channel++) {
            peripheral.outputChanged(channel, getOutputPins(channel), getOutputMask(channel));
        }
        peripheral.reset();
    }

    @Override
    public synchronized void detachPeripheral(Peripheral peripheral) {
        if (this.peripheral == peripheral) { this.peripheral = null; }
    }

    @Override public synchronized void setInputPins(int channel, int data) {
        checkChannel(channel); pias[channel / 2].setInput(channel & 1, data);
    }
    @Override public synchronized void setControlLine1(int channel, boolean high) {
        checkChannel(channel); pias[channel / 2].setC1(channel & 1, high);
    }
    @Override public synchronized void setControlLine2(int channel, boolean high) {
        checkChannel(channel); pias[channel / 2].setC2(channel & 1, high);
    }
    @Override public synchronized int getOutputPins(int channel) {
        checkChannel(channel); return pias[channel / 2].getOutput(channel & 1);
    }
    @Override public synchronized int getOutputMask(int channel) {
        checkChannel(channel); return pias[channel / 2].getDirection(channel & 1);
    }

    public synchronized void setInterruptHandler(Runnable interrupt) { this.interrupt = interrupt; }

    public synchronized int getControl(int channel) {
        checkChannel(channel); return pias[channel / 2].readControl(channel & 1);
    }

    public synchronized int getInputPins(int channel) {
        checkChannel(channel); return pias[channel / 2].getInput(channel & 1);
    }

    synchronized boolean getControlLine1(int channel) {
        checkChannel(channel); return pias[channel / 2].getC1(channel & 1);
    }

    synchronized boolean getControlLine2(int channel) {
        checkChannel(channel); return pias[channel / 2].getC2(channel & 1);
    }

    synchronized boolean hasPeripheral() { return peripheral != null; }
    synchronized Peripheral getPeripheral() { return peripheral; }

    public synchronized void reset() {
        for (Pia6820 pia : pias) { pia.reset(); }
        irqPending = false;
        if (peripheral != null) { peripheral.reset(); }
    }

    private void checkChannel(int channel) {
        if (channel < 0 || channel >= getChannelCount()) { throw new IllegalArgumentException("Invalid PIA channel"); }
    }

    private void updateInterrupt() {
        boolean pending = false;
        for (Pia6820 pia : pias) { pending |= pia != null && pia.isInterruptPending(); }
        boolean signal = pending && !irqPending;
        irqPending = pending;
        if (signal) { interrupt.run(); }
    }
}
