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
