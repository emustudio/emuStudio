/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

/** Original 88-PIO: separate Intel 8212 input/output latches and two handshake flags. */
public final class PioUnit implements Context8080.CpuPortDevice, DeviceContext<Byte> {
    private final int basePort;
    private Runnable interrupt = () -> { };
    private int inputLatch;
    private int outputLatch;
    private int enables;
    private boolean inputReady;
    private boolean outputReady;
    private boolean interruptPending;

    public PioUnit(int basePort) {
        if (basePort < 0 || basePort > 254 || (basePort & 1) != 0) {
            throw new IllegalArgumentException("88-PIO basePort must be an even address from 00h to FEh");
        }
        this.basePort = basePort;
    }

    public synchronized void setInterruptHandler(Runnable interrupt) {
        this.interrupt = interrupt;
        updateInterrupt();
    }

    @Override
    public synchronized byte read(int portAddress) {
        int port = portAddress & 0xFF;
        if (port == basePort) {
            return (byte) ((inputReady ? 2 : 0) | (outputReady ? 1 : 0));
        }
        if (port == basePort + 1) {
            inputReady = false;
            updateInterrupt();
            return (byte) inputLatch;
        }
        return (byte) 0xFF;
    }

    @Override
    public synchronized void write(int portAddress, byte data) {
        int port = portAddress & 0xFF;
        if (port == basePort) {
            enables = data & 3;
        } else if (port == basePort + 1) {
            outputLatch = data & 0xFF;
            outputReady = false;
        }
        updateInterrupt();
    }

    /** Peripheral consumes output and requests another byte. */
    @Override
    public synchronized Byte readData() {
        outputReady = true;
        updateInterrupt();
        return (byte) outputLatch;
    }

    /** Peripheral strobes one byte into the input latch; there is no input queue. */
    @Override
    public synchronized void writeData(Byte data) {
        inputLatch = data & 0xFF;
        inputReady = true;
        updateInterrupt();
    }

    public synchronized void setOutputReady(boolean ready) {
        outputReady = ready;
        updateInterrupt();
    }

    public synchronized int getOutputLatch() { return outputLatch; }
    public synchronized int getInputLatch() { return inputLatch; }
    public synchronized int getEnables() { return enables; }

    public synchronized void reset() {
        inputLatch = 0;
        outputLatch = 0;
        enables = 0;
        inputReady = false;
        outputReady = false;
        interruptPending = false;
    }

    private void updateInterrupt() {
        boolean pending = (inputReady && (enables & 2) != 0) || (outputReady && (enables & 1) != 0);
        boolean signal = pending && !interruptPending;
        interruptPending = pending;
        if (signal) { interrupt.run(); }
    }

    @Override
    public Class<Byte> getDataType() { return Byte.class; }

    @Override
    public String getName() { return "MITS 88-PIO"; }
}
