/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Universal Asynchronous Receiver Transmitter (UART)
 *
 * <p>
 * Status port IN:
 * <p>
 * 7 - 0 - device ready; 1 - not ready
 * 6 - N/A
 * 5 - 1 - data ready (for writing to output device - CPU); 0 - not ready
 * 4 - 1 - data overflow; 0 - OK
 * 3 - 1 - framing error; 0 - OK
 * 2 - 1 - parity error; 0 - OK
 * 1 - 1 - transmitter buffer empty (i.e. ready for receive data from CPU)
 * 0 - 1 - data from input device is ready to be read
 */
@ThreadSafe
public class UART {
    private final static Logger LOGGER = LoggerFactory.getLogger(UART.class);
    private final static int OUTPUT_DEVICE_READY = 0x80;
    private final static int DATA_AVAILABLE = 0x20;
    private final static int DATA_OVERFLOW = 0x10;
    private final static int XMITTER_BUFFER_EMPTY = 0x2; // ready to receive data from device
    private final static int INPUT_DEVICE_READY = 0x1; // ready to send data to CPU

    private final static Map<Integer, Byte> RST_MAP = Map.of(
        0, (byte) 0xC7,
        1, (byte) 0xCF,
        2, (byte) 0xD7,
        3, (byte) 0xDF,
        4, (byte) 0xE7,
        5, (byte) 0xEF,
        6, (byte) 0xF7,
        7, (byte) 0xFF
    );

    private final AtomicReference<Byte> bufferFromDevice = new AtomicReference<>();
    private final Lock bufferAndStatusLock = new ReentrantLock();

    private volatile DeviceContext<Byte> device;
    private byte statusRegister = XMITTER_BUFFER_EMPTY;
    private volatile boolean interruptsSupported;
    private volatile boolean inputInterruptEnabled;
    private volatile boolean outputInterruptEnabled;
    private volatile byte[] inputRstInterrupt;
    private volatile byte[] outputRstInterrupt;
    private final Context8080 cpu;

    private final List<Observer> observers = new ArrayList<>();

    public UART(Context8080 cpu, SioUnitSettings settings) {
        this.cpu = Objects.requireNonNull(cpu);
        this.interruptsSupported = settings.getInterruptsSupported();
        this.inputRstInterrupt = new byte[] {RST_MAP.get(settings.getInputInterruptVector())};
        this.outputRstInterrupt = new byte[] {RST_MAP.get(settings.getOutputInterruptVector())};

        settings.addObserver(() -> {
            this.interruptsSupported = settings.getInterruptsSupported();
            this.inputRstInterrupt = new byte[] {RST_MAP.get(settings.getInputInterruptVector())};
            this.outputRstInterrupt = new byte[] {RST_MAP.get(settings.getOutputInterruptVector())};
        });
    }

    public void setDevice(DeviceContext<Byte> device) {
        this.device = device;
        if (device == null) {
            LOGGER.info("[88-SIO] Device disconnected");
        } else {
            LOGGER.info("[88-SIO, device={}] Device was attached", getDeviceId());
        }
    }

    public String getDeviceId() {
        DeviceContext<Byte> tmpDevice = device;
        if (tmpDevice == null) {
            return "unknown";
        }
        PluginContext pluginContext = tmpDevice.getClass().getAnnotation(PluginContext.class);
        return (pluginContext != null) ? pluginContext.id() : tmpDevice.toString();
    }

    void reset(boolean guiSupported) {
        if (guiSupported) {
            bufferFromDevice.set(null);
        }
        setStatus((byte) 0); // disable interrupts
        statusRegister = XMITTER_BUFFER_EMPTY;
    }

    public void setStatus(byte status) {
        inputInterruptEnabled = (status & 1) == 1;
        outputInterruptEnabled = (status & 2) == 2;
    }

    public void receiveFromDevice(byte data) {
        bufferAndStatusLock.lock();
        int status = statusRegister;

        try {
            if (bufferFromDevice.get() != null) {
                status |= DATA_OVERFLOW;
            } else {
                status = (byte) (status & (~DATA_OVERFLOW));
            }
            status = (byte) (status | DATA_AVAILABLE | INPUT_DEVICE_READY);
            bufferFromDevice.set(data);
            statusRegister = (byte) status;
        } finally {
            bufferAndStatusLock.unlock();

            if (interruptsSupported && inputInterruptEnabled && cpu.isInterruptSupported()) {
                cpu.signalInterrupt(inputRstInterrupt);
            }
            notifyNewData(data);
            notifyStatusChanged(status);
        }
    }

    public void sendToDevice(byte data) {
        DeviceContext<Byte> tmpDevice = device;
        if (tmpDevice != null) {
            tmpDevice.writeData(data);
            if (interruptsSupported && outputInterruptEnabled && cpu.isInterruptSupported()) {
                cpu.signalInterrupt(outputRstInterrupt);
            }
        }
    }

    public byte readBuffer() {
        Byte bufferData;
        int status = 0;
        bufferAndStatusLock.lock();
        try {
            bufferData = bufferFromDevice.get();
            bufferFromDevice.set(null);
            if (bufferData == null) {
                return 0;
            }
            statusRegister = XMITTER_BUFFER_EMPTY;
            status = statusRegister;
            return bufferData;
        } finally {
            bufferAndStatusLock.unlock();
            notifyNoData();
            notifyStatusChanged(status);
        }
    }

    public byte getStatus() {
        return statusRegister;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    private void notifyStatusChanged(int status) {
        observers.forEach(o -> o.statusChanged(status));
    }

    private void notifyNewData(byte data) {
        observers.forEach(o -> o.dataAvailable(data));
    }

    private void notifyNoData() {
        observers.forEach(Observer::noData);
    }

    public interface Observer {
        void statusChanged(int status);

        void dataAvailable(byte data);

        void noData();
    }

    public static class DeviceChannel implements DeviceContext<Byte> {
        private UART uart;

        public void setUART(UART uart) {
            this.uart = uart;
        }

        @Override
        public Byte readData() {
            return 0; // Attached device cannot read back what it already wrote
        }

        @Override
        public void writeData(Byte data) {
            if (uart != null) {
                uart.receiveFromDevice(data);
            }
        }

        @Override
        public Class<Byte> getDataType() {
            return Byte.class;
        }
    }
}
