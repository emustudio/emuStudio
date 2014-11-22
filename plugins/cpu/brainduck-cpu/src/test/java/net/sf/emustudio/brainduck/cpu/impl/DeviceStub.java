package net.sf.emustudio.brainduck.cpu.impl;

import emulib.annotations.ContextType;
import emulib.plugins.device.DeviceContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@ContextType
public class DeviceStub implements DeviceContext<Short> {
    private final List<Short> output = new ArrayList<>();
    private final Queue<Short> input = new LinkedList<>();

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
