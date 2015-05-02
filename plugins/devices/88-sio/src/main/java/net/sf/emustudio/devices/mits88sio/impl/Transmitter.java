package net.sf.emustudio.devices.mits88sio.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
public class Transmitter {
    private final static Logger LOGGER = LoggerFactory.getLogger(Transmitter.class);

    private final Queue<Short> buffer = new ConcurrentLinkedQueue<>();
    private final Lock bufferAndStatusLock = new ReentrantLock();

    private volatile DeviceContext<Short> device;
    private volatile short status;

    public void setDevice(DeviceContext<Short> device) {
        this.device = device;
        LOGGER.info("Attaching device: " + getDeviceId());
    }

    public String getDeviceId() {
        DeviceContext tmpDevice = device;
        if (tmpDevice == null) {
            return "unknown";
        }
        ContextType contextType = tmpDevice.getClass().getAnnotation(ContextType.class);
        return  (contextType != null) ? contextType.id() : tmpDevice.toString();
    }

    public void reset() {
        writeToStatus((short)0x03);
    }

    public void writeToStatus(short value) {
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

    public void writeFromDevice(short data) {
        bufferAndStatusLock.lock();
        try {
            buffer.add(data);
            status = (short) (status | 0x01);
        } finally {
            bufferAndStatusLock.unlock();
        }
    }

    public void writeToDevice(short data) {
        DeviceContext<Short> tmpDevice = device;
        if (tmpDevice != null) {
            tmpDevice.write(data);
        }
    }

    public short readBuffer() {
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

    public short readStatus() {
        return status;
    }
}
