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
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
    private final static int SEND_DATA_READY = 2;
    private final static int RECEIVE_DATA_READY = 1;

    private final Queue<Byte> bufferFromDevice = new ConcurrentLinkedQueue<>();
    private final Lock bufferAndStatusLock = new ReentrantLock();

    private volatile DeviceContext<Byte> device;
    private byte statusRegister = SEND_DATA_READY;
    private volatile boolean inputInterruptEnabled;
    private volatile boolean outputInterruptEnabled;
    private final Context8080 cpu;

    private final List<Observer> observers = new ArrayList<>();

    public UART(Context8080 cpu) {
        this.cpu = Objects.requireNonNull(cpu);
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
            bufferFromDevice.clear();
        }
        setStatus((byte) 0); // disable interrupts
    }

    public void setStatus(byte value) {
        bufferAndStatusLock.lock();
        int newStatus = statusRegister;
        boolean isEmpty = true;
        try {
            inputInterruptEnabled = (value & 1) == 1;
            outputInterruptEnabled = (value & 2) == 2;

            isEmpty = bufferFromDevice.isEmpty();
            if (isEmpty) {
                this.statusRegister = SEND_DATA_READY;
            } else {
                this.statusRegister = SEND_DATA_READY | RECEIVE_DATA_READY;
            }
            newStatus = this.statusRegister;
        } finally {
            bufferAndStatusLock.unlock();
            if (!isEmpty && inputInterruptEnabled && cpu.isInterruptSupported()) {
                cpu.signalInterrupt(new byte[] { (byte)0xFF });  // RST 7 (in Z80 RST 0x38)
            }
            if (outputInterruptEnabled && cpu.isInterruptSupported()) {
                cpu.signalInterrupt(new byte[] { (byte)0xFF });  // RST 7 (in Z80 RST 0x38)
            }

            notifyStatusChanged(newStatus);
        }
    }

    public void receiveFromDevice(byte data) {
        boolean wasEmpty = false;
        int newStatus = statusRegister;

        bufferAndStatusLock.lock();
        try {
            if (bufferFromDevice.isEmpty()) {
                wasEmpty = true;
            }
            bufferFromDevice.add(data); // TODO: here
            statusRegister = (byte) (statusRegister | 1);
            newStatus = statusRegister;
        } finally {
            bufferAndStatusLock.unlock();

            if (wasEmpty) {
                if (inputInterruptEnabled && cpu.isInterruptSupported()) {
                    cpu.signalInterrupt(new byte[] { (byte)0xFF });  // RST 7 (in Z80 RST 0x38)
                }
                notifyNewData(data);
            }
            notifyStatusChanged(newStatus);
        }
    }

    public void sendToDevice(byte data) {
        DeviceContext<Byte> tmpDevice = device;
        if (tmpDevice != null) {
            tmpDevice.writeData(data);
        }
    }

    public byte readBuffer() {
        byte newData = 0;
        boolean isNotEmpty = false;
        int newStatus = statusRegister; // what to do..

        bufferAndStatusLock.lock();
        try {
            Byte result = bufferFromDevice.poll();

            isNotEmpty = !bufferFromDevice.isEmpty();
            statusRegister = (byte) (isNotEmpty ? (statusRegister | RECEIVE_DATA_READY) : (statusRegister & 0xFE));
            newStatus = statusRegister;

            if (isNotEmpty) {
                newData = bufferFromDevice.peek();
            }

            return result == null ? 0 : result;
        } finally {
            bufferAndStatusLock.unlock();

            if (isNotEmpty) {
                notifyNewData(newData);
            } else {
                notifyNoData();
            }
            notifyStatusChanged(newStatus);
        }
    }

    public byte readStatus() {
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
