/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.*;

final class DebugMemory extends AbstractMemoryContext<Byte> {
    final byte[][] values;
    int selected;
    final int common;
    final Annotations annotations = new Annotations();

    DebugMemory(int size, int banks, int common) {
        values = new byte[banks][size];
        this.common = common;
    }

    @Override
    public Byte read(int at) {
        return readBank(at, selected);
    }

    @Override
    public Byte[] read(int at, int count) {
        Byte[] v = new Byte[count];
        for (int i = 0; i < count; i++) v[i] = read(at + i);
        return v;
    }

    @Override
    public Byte readBank(int at, int bank) {
        return values[at < common ? bank : 0][at];
    }

    @Override
    public void write(int at, Byte value) {
        values[getBank(at)][at] = value;
    }

    @Override
    public void write(int at, Byte[] value, int count) {
        for (int i = 0; i < count; i++) write(at + i, value[i]);
    }

    @Override
    public int getBanksCount() {
        return values.length;
    }

    @Override
    public int getSelectedBank() {
        return selected;
    }

    @Override
    public int getCommonBoundary() {
        return common;
    }

    @Override
    public int getSize() {
        return values[0].length;
    }

    @Override
    public void clear() {
        for (byte[] bank : values) java.util.Arrays.fill(bank, (byte) 0);
    }

    @Override
    public Class<Byte> getCellTypeClass() {
        return Byte.class;
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return annotations;
    }
}
