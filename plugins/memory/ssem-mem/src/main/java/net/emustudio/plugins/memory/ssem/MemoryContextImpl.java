/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem;

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;

import java.util.Arrays;
import java.util.Objects;

public class MemoryContextImpl extends AbstractMemoryContext<Byte> {
    public static final int NUMBER_OF_CELLS = 32 * 4;

    // byte type is atomic in JVM memory model
    private final Byte[] memory = new Byte[NUMBER_OF_CELLS];
    private final MemoryContextAnnotations annotations;

    public MemoryContextImpl(MemoryContextAnnotations annotations) {
        this.annotations = Objects.requireNonNull(annotations);
        Arrays.fill(memory, (byte) 0);
    }

    @Override
    public void clear() {
        Arrays.fill(memory, (byte) 0);
        notifyMemoryContentChanged(-1); // notify that all memory has changed
    }

    @Override
    public Byte read(int from) {
        return memory[from];
    }

    @Override
    public Byte[] read(int from, int count) {
        int to = Math.min(memory.length, from + count);
        return Arrays.copyOfRange(memory, from, to);
    }

    @Override
    public void write(int to, Byte value) {
        memory[to] = value;
        notifyMemoryContentChanged(to);
    }

    @Override
    public void write(int to, Byte[] values, int count) {
        System.arraycopy(values, 0, memory, to, count);
        notifyMemoryContentChanged(to, to + values.length);
    }

    @Override
    public Class<Byte> getCellTypeClass() {
        return Byte.class;
    }

    @Override
    public int getSize() {
        return memory.length;
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return annotations;
    }
}
