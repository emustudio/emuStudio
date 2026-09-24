/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.util.Arrays;

/** Intel 8255 mode-0 parallel interface. */
public final class Pio8255 implements Context8080.CpuPortDevice {
    public static final int PORT_A = 0x08;
    public static final int PORT_B = 0x09;
    public static final int PORT_C = 0x0A;
    public static final int CONTROL_PORT = 0x0B;

    private static final int POWER_ON_CONTROL = 0x9B;
    private final byte[] outputLatches = new byte[3];
    private final byte[] inputPins = new byte[3];
    private final PortChannel[] channels = {
            new PortChannel(0), new PortChannel(1), new PortChannel(2)
    };
    private int control;

    public Pio8255() {
        reset();
    }

    @Override
    public synchronized byte read(int portAddress) {
        int register = (portAddress & 0xFF) - PORT_A;
        if (register >= 0 && register < 3) {
            return readPort(register);
        }
        return register == 3 ? (byte) control : (byte) 0xFF;
    }

    @Override
    public synchronized void write(int portAddress, byte data) {
        int register = (portAddress & 0xFF) - PORT_A;
        if (register >= 0 && register < 3) {
            writePort(register, data);
        } else if (register == 3) {
            writeControl(data & 0xFF);
        }
    }

    @Override
    public String getName() {
        return "MITS 88-PIO";
    }

    public synchronized void reset() {
        Arrays.fill(outputLatches, (byte) 0);
        Arrays.fill(inputPins, (byte) 0xFF);
        control = POWER_ON_CONTROL;
    }

    public DeviceContext<Byte> getChannel(int port) {
        if (port < 0 || port >= channels.length) {
            throw new IllegalArgumentException("Port index must be 0, 1, or 2");
        }
        return channels[port];
    }

    public synchronized int getControl() {
        return control;
    }

    public synchronized boolean isInput(int port) {
        switch (port) {
            case 0:
                return (control & 0x10) != 0;
            case 1:
                return (control & 0x02) != 0;
            case 2:
                return (control & 0x09) != 0;
            default:
                throw new IllegalArgumentException("Port index must be 0, 1, or 2");
        }
    }

    private byte readPort(int port) {
        if (port == 2) {
            int result = outputLatches[2] & 0xFF;
            if ((control & 0x08) != 0) {
                result = (result & 0x0F) | (inputPins[2] & 0xF0);
            }
            if ((control & 0x01) != 0) {
                result = (result & 0xF0) | (inputPins[2] & 0x0F);
            }
            return (byte) result;
        }
        return isInput(port) ? inputPins[port] : outputLatches[port];
    }

    private void writePort(int port, byte data) {
        if (port == 2) {
            int writableMask = 0;
            if ((control & 0x08) == 0) {
                writableMask |= 0xF0;
            }
            if ((control & 0x01) == 0) {
                writableMask |= 0x0F;
            }
            outputLatches[2] = (byte) ((outputLatches[2] & ~writableMask) | (data & writableMask));
        } else if (!isInput(port)) {
            outputLatches[port] = data;
        }
    }

    private void writeControl(int value) {
        if ((value & 0x80) != 0) {
            // Modes 1 and 2 are deliberately not emulated; their mode bits are normalized to mode 0.
            control = (value & 0x9B) | 0x80;
            Arrays.fill(outputLatches, (byte) 0);
            return;
        }

        int bit = (value >>> 1) & 0x07;
        int mask = 1 << bit;
        int portC = outputLatches[2] & 0xFF;
        outputLatches[2] = (byte) ((value & 1) != 0 ? portC | mask : portC & ~mask);
    }

    private final class PortChannel implements DeviceContext<Byte> {
        private final int port;

        private PortChannel(int port) {
            this.port = port;
        }

        @Override
        public Byte readData() {
            synchronized (Pio8255.this) {
                return readPort(port);
            }
        }

        @Override
        public void writeData(Byte data) {
            synchronized (Pio8255.this) {
                inputPins[port] = data;
            }
        }

        @Override
        public Class<Byte> getDataType() {
            return Byte.class;
        }
    }
}
