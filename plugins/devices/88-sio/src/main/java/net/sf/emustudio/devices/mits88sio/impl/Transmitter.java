/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
class Transmitter {
    private final static Logger LOGGER = LoggerFactory.getLogger(Transmitter.class);

    private final Queue<Short> buffer = new ConcurrentLinkedQueue<>();
    private final Lock bufferAndStatusLock = new ReentrantLock();

    private volatile DeviceContext<Short> device;
    private volatile short status;

    void setDevice(DeviceContext<Short> device) {
        this.device = device;
        LOGGER.info("Attaching device: " + getDeviceId());
    }

    String getDeviceId() {
        DeviceContext tmpDevice = device;
        if (tmpDevice == null) {
            return "unknown";
        }
        ContextType contextType = tmpDevice.getClass().getAnnotation(ContextType.class);
        return  (contextType != null) ? contextType.id() : tmpDevice.toString();
    }

    void reset() {
        writeToStatus((short)0x03);
    }

    void writeToStatus(short value) {
        bufferAndStatusLock.lock();
        try {
            // TODO: Wrong implementation; buffer SHOULD be emptied.
            // However, it messes up the automation.
            if (value == 0x03 && buffer.isEmpty()) {
                this.status = 0x02;
            }
        } finally {
            bufferAndStatusLock.unlock();
        }
    }

    void writeFromDevice(short data) {
        bufferAndStatusLock.lock();
        try {
            buffer.add(data);
            status = (short) (status | 0x01);
        } finally {
            bufferAndStatusLock.unlock();
        }
    }

    void writeToDevice(short data) throws IOException {
        DeviceContext<Short> tmpDevice = device;
        if (tmpDevice != null) {
            tmpDevice.write(data);
        }
    }

    short readBuffer() {
        bufferAndStatusLock.lock();
        try {
            Short result = buffer.poll();
            status = buffer.isEmpty()
                    ? (short) (status & 0xFE)
                    : (short) (status | 0x01);
            return result == null ? 0 : result;
        } finally {
            bufferAndStatusLock.unlock();
        }
    }

    short readStatus() {
        return status;
    }
}
