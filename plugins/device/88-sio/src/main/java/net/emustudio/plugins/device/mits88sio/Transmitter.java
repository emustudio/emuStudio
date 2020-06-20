/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
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
public class Transmitter {
    private final static Logger LOGGER = LoggerFactory.getLogger(Transmitter.class);

    private final Queue<Short> buffer = new ConcurrentLinkedQueue<>();
    private final Lock bufferAndStatusLock = new ReentrantLock();

    private volatile DeviceContext<Short> device;
    private volatile short status = 0x2;
    private volatile boolean inputInterruptEnabled;
    private volatile boolean outputInterruptEnabled;

    private final List<Observer> observers = new ArrayList<>();

    void setDevice(DeviceContext<Short> device) {
        this.device = device;
        LOGGER.info("[device={}] Device was attached to 88-SIO", getDeviceId());
    }

    String getDeviceId() {
        DeviceContext<Short> tmpDevice = device;
        if (tmpDevice == null) {
            return "unknown";
        }
        PluginContext pluginContext = tmpDevice.getClass().getAnnotation(PluginContext.class);
        return (pluginContext != null) ? pluginContext.id() : tmpDevice.toString();
    }

    void reset(boolean noGUI) {
        if (!noGUI) {
            buffer.clear();
            writeToStatus((short) 0); // disable interrupts
        }
    }

    public void writeToStatus(short value) {
        int newStatus = status;

        bufferAndStatusLock.lock();
        try {
            inputInterruptEnabled = (value & 1) == 1;
            outputInterruptEnabled = (value & 2) == 2;

            if (buffer.isEmpty()) {
                this.status = 2;
            } else {
                this.status = 3;
            }
            newStatus = this.status;
        } finally {
            bufferAndStatusLock.unlock();
            notifyStatusChanged(newStatus);
        }
    }

    public void writeFromDevice(short data) {
        boolean wasEmpty = false;
        int newStatus = status;

        bufferAndStatusLock.lock();
        try {
            if (buffer.isEmpty()) {
                wasEmpty = true;
            }
            buffer.add(data);
            status = (short) (status | 1);
            newStatus = status;
        } finally {
            bufferAndStatusLock.unlock();

            if (wasEmpty) {
                notifyNewData(data);
            }
            notifyStatusChanged(newStatus);
        }
    }

    public void writeToDevice(short data) throws IOException {
        DeviceContext<Short> tmpDevice = device;
        if (tmpDevice != null) {
            tmpDevice.writeData(data);
        }
    }

    public short readBuffer() {
        int newData = 0;
        boolean isNotEmpty = false;
        int newStatus = status; // what to do..

        bufferAndStatusLock.lock();
        try {
            Short result = buffer.poll();

            isNotEmpty = !buffer.isEmpty();
            status = isNotEmpty ? (short) (status | 1) : (short) (status & 0xFE);
            newStatus = status;

            if (isNotEmpty) {
                newData = buffer.peek();
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

    public short readStatus() {
        return status;
    }


    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    private void notifyStatusChanged(int status) {
        observers.forEach(o -> o.statusChanged(status));
    }

    private void notifyNewData(int data) {
        observers.forEach(o -> o.dataAvailable(data));
    }

    private void notifyNoData() {
        observers.forEach(Observer::noData);
    }

    public interface Observer {
        void statusChanged(int status);

        void dataAvailable(int data);

        void noData();
    }
}
