package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@ContextType
public class DeviceStub implements DeviceContext<Short> {
    private final List<Short> output = new CopyOnWriteArrayList<>();
    private final Queue<Short> input = new ConcurrentLinkedQueue<>();

    public void setInput(byte[] input) {
        for (byte value : input) {
            this.input.add((short)value);
        }
    }

    @Override
    public Short read() {
        return input.poll();
    }

    @Override
    public void write(Short val) {
        output.add(val);
    }

    @Override
    public Class getDataType() {
        return Short.class;
    }

    public boolean wasInputRead() {
        return input.isEmpty();
    }

    public List<Short> getOutput() {
        return output;
    }
}
