/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;

/** CPU-visible live byte reads. No memory cells are retained; stale operations fail. */
public final class LiveMemory {
    private final MemoryContext<?> memory;
    private final int size;
    private final Runnable checkCurrent;

    LiveMemory(MemoryContext<?> memory, int size, Runnable checkCurrent) {
        this.memory = Objects.requireNonNull(memory);
        this.size = size;
        this.checkCurrent = Objects.requireNonNull(checkCurrent);
        if (memory.getCellTypeClass() != Byte.class) throw new IllegalArgumentException("Byte memory required");
    }

    public void requireCurrent() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
        checkCurrent.run();
    }

    public int size() { return size; }
    public int value(int at) {
        if (at < 0 || at >= size()) throw new IllegalArgumentException("Address outside memory");
        requireCurrent();
        int value = ((Byte) memory.peek(at)) & 255;
        requireCurrent();
        return value;
    }

    /** Hex bytes, ?? wildcards, or str:literal (UTF-8). Pattern buffers contain only user input. */
    public static byte[][] pattern(String text) {
        if (text.startsWith("str:")) {
            byte[] bytes = text.substring(4).getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0) throw new IllegalArgumentException("Empty pattern");
            byte[] masks = new byte[bytes.length];
            Arrays.fill(masks, (byte) 255);
            return new byte[][] {bytes, masks};
        }
        String[] words = text.trim().split("\\s+");
        byte[] bytes = new byte[words.length], masks = new byte[words.length];
        for (int i = 0; i < words.length; i++) {
            if (!words[i].matches("[0-9a-fA-F?]{2}"))
                throw new IllegalArgumentException("Use hex byte pairs, ??, or str:text");
            for (int j = 0; j < 2; j++) if (words[i].charAt(j) != '?') {
                int shift = (1 - j) * 4;
                masks[i] |= (byte) (15 << shift);
                bytes[i] |= (byte) (Character.digit(words[i].charAt(j), 16) << shift);
            }
        }
        return new byte[][] {bytes, masks};
    }

    public List<Integer> search(byte[] bytes, byte[] masks) {
        if (bytes.length == 0 || bytes.length != masks.length) throw new IllegalArgumentException("Invalid pattern");
        requireCurrent();
        List<Integer> result = new ArrayList<>();
        for (int at = 0; at <= size() - bytes.length; at++) {
            boolean match = true;
            for (int i = 0; i < bytes.length; i++)
                if ((value(at + i) & (masks[i] & 255)) != (bytes[i] & masks[i] & 255)) {
                    match = false;
                    break;
                }
            if (match) result.add(at);
        }
        requireCurrent();
        return result;
    }

    public static byte[] integer(long value, int width, boolean little) {
        if (width < 1 || width > 8) throw new IllegalArgumentException("Width must be 1..8");
        if (width < 8 && (value < 0 || (value >>> (width * 8)) != 0))
            throw new IllegalArgumentException("Value exceeds width");
        byte[] result = new byte[width];
        for (int i = 0; i < width; i++) result[little ? i : width - 1 - i] = (byte) (value >>> (8 * i));
        return result;
    }

    public long number(int at, int width, boolean little) {
        if (width < 1 || width > 8 || at < 0 || at > size() - width)
            throw new IllegalArgumentException("Value outside memory");
        long value = 0;
        for (int i = 0; i < width; i++)
            value |= (long) value(at + i) << (8 * (little ? i : width - 1 - i));
        requireCurrent();
        return value;
    }

    /** Result text is derived output, not a saved memory image. */
    public Map<Integer, String> strings(int minimum) {
        if (minimum < 1) throw new IllegalArgumentException("Minimum length must be positive");
        requireCurrent();
        Map<Integer, String> result = new LinkedHashMap<>();
        StringBuilder text = new StringBuilder();
        int start = 0;
        for (int at = 0; at < size(); at++) {
            int value = value(at);
            if (value >= 32 && value <= 126) text.append((char) value);
            else {
                if (text.length() >= minimum) result.put(start, text.toString());
                text.setLength(0);
                start = at + 1;
            }
        }
        if (text.length() >= minimum) result.put(start, text.toString());
        requireCurrent();
        return result;
    }
}
