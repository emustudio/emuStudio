/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Immutable paused-memory capture. Logical banks include their shared common region. */
public final class MemoryAnalysis {
    public final int common;
    private final byte[][] banks;

    public MemoryAnalysis(byte[][] banks, int common) {
        if (banks.length == 0 || common < 0 || common > banks[0].length)
            throw new IllegalArgumentException("Invalid memory layout");
        this.banks = new byte[banks.length][];
        this.common = common;
        for (int i = 0; i < banks.length; i++) {
            if (banks[i].length != banks[0].length)
                throw new IllegalArgumentException("Bank sizes differ");
            this.banks[i] = banks[i].clone();
        }
    }

    public static MemoryAnalysis capture(MemoryContext<?> memory) {
        if (memory.getCellTypeClass() != Byte.class)
            throw new IllegalArgumentException("Byte memory required");
        byte[][] banks = new byte[memory.getBanksCount()][memory.getSize()];
        for (int b = 0; b < banks.length; b++)
            for (int at = 0; at < banks[b].length; at++) {
                checkCancelled();
                banks[b][at] = (Byte) memory.readBank(at, b);
            }
        return new MemoryAnalysis(banks, memory.getCommonBoundary());
    }

    static byte[] readBinary(Path file, int size) throws IOException {
        if (Files.size(file) != size) throw new IOException("Binary size must match memory size");
        try (InputStream input = Files.newInputStream(file)) {
            return readBinary(input, size);
        }
    }

    static byte[] readBinary(InputStream input, int size) throws IOException {
        byte[] bytes = new byte[size];
        int offset = 0;
        while (offset < size) {
            checkCancelled();
            int count = input.read(bytes, offset, size - offset);
            if (count < 0) throw new IOException("Binary size must match memory size");
            offset += count;
        }
        if (input.read() != -1) throw new IOException("Binary size must match memory size");
        return bytes;
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    public int size() {
        return banks[0].length;
    }

    public int bankCount() {
        return banks.length;
    }

    public int physicalBank(int at, int bank) {
        check(at, bank);
        return at < common ? bank : 0;
    }

    public int value(int at, int bank) {
        check(at, bank);
        return banks[bank][at] & 255;
    }

    private void check(int at, int bank) {
        if (bank < 0 || bank >= banks.length || at < 0 || at >= size())
            throw new IllegalArgumentException("Address/bank outside memory");
    }

    public byte[] bytes(int bank) {
        check(0, bank);
        return banks[bank].clone();
    }

    public String identity() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(
                    (bankCount() + ":" + size() + ":" + common + ":")
                            .getBytes(StandardCharsets.US_ASCII));
            for (byte[] bank : banks) digest.update(bank);
            StringBuilder s = new StringBuilder();
            for (byte b : digest.digest()) s.append(String.format("%02x", b & 255));
            return s.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public void requireSameLayout(MemoryAnalysis other) {
        if (other.bankCount() != bankCount() || other.size() != size() || other.common != common)
            throw new IllegalArgumentException("Memory layout differs from capture");
    }

    public List<Integer> differences(MemoryAnalysis newer, int bank) {
        requireSameLayout(newer);
        List<Integer> result = new ArrayList<>();
        for (int at = 0; at < size(); at++) {
            checkCancelled();
            if (value(at, bank) != newer.value(at, bank)) result.add(at);
        }
        return result;
    }

    List<Integer> differences(byte[] bytes, int bank) {
        if (bytes.length != size()) throw new IllegalArgumentException("Binary size must match memory size");
        List<Integer> result = new ArrayList<>();
        for (int at = 0; at < bytes.length; at++) {
            checkCancelled();
            if ((bytes[at] & 255) != value(at, bank)) result.add(at);
        }
        return result;
    }

    /** Hex bytes, ?? wildcards, or str:literal (UTF-8). */
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
            for (int j = 0; j < 2; j++)
                if (words[i].charAt(j) != '?') {
                    int shift = (1 - j) * 4;
                    masks[i] |= (byte) (15 << shift);
                    bytes[i] |= (byte) (Character.digit(words[i].charAt(j), 16) << shift);
                }
        }
        return new byte[][] {bytes, masks};
    }

    public List<Integer> search(int bank, byte[] bytes, byte[] masks) {
        if (bytes.length == 0 || bytes.length != masks.length)
            throw new IllegalArgumentException("Invalid pattern");
        List<Integer> result = new ArrayList<>();
        for (int at = 0; at <= size() - bytes.length; at++) {
            checkCancelled();
            boolean match = true;
            for (int i = 0; i < bytes.length; i++)
                if ((value(at + i, bank) & (masks[i] & 255)) != (bytes[i] & masks[i] & 255)) {
                    match = false;
                    break;
                }
            if (match) result.add(at);
        }
        return result;
    }

    public static byte[] integer(long value, int width, boolean little) {
        if (width < 1 || width > 8) throw new IllegalArgumentException("Width must be 1..8");
        if (width < 8 && (value < 0 || (value >>> (width * 8)) != 0))
            throw new IllegalArgumentException("Value exceeds width");
        byte[] result = new byte[width];
        for (int i = 0; i < width; i++)
            result[little ? i : width - 1 - i] = (byte) (value >>> (8 * i));
        return result;
    }

    public long number(int bank, int at, int width, boolean little) {
        if (width < 1 || width > 8 || at < 0 || at > size() - width)
            throw new IllegalArgumentException("Value outside memory");
        long value = 0;
        for (int i = 0; i < width; i++)
            value |= (long) value(at + i, bank) << (8 * (little ? i : width - 1 - i));
        return value;
    }

    public List<Integer> rescan(
            MemoryAnalysis newer,
            int bank,
            List<Integer> addresses,
            int width,
            boolean little,
            String mode) {
        requireSameLayout(newer);
        List<Integer> result = new ArrayList<>();
        for (int at : addresses) {
            checkCancelled();
            long old = number(bank, at, width, little), now = newer.number(bank, at, width, little);
            int cmp = Long.compareUnsigned(now, old);
            boolean keep;
            switch (mode) {
                case "Changed":
                    keep = cmp != 0;
                    break;
                case "Equal":
                    keep = cmp == 0;
                    break;
                case "Increased":
                    keep = cmp > 0;
                    break;
                case "Decreased":
                    keep = cmp < 0;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown rescan mode");
            }
            if (keep) result.add(at);
        }
        return result;
    }

    public Map<Integer, String> strings(int bank, int minimum) {
        if (minimum < 1) throw new IllegalArgumentException("Minimum length must be positive");
        Map<Integer, String> result = new LinkedHashMap<>();
        int start = 0;
        for (int at = 0; at <= size(); at++) {
            checkCancelled();
            if (at < size() && value(at, bank) >= 32 && value(at, bank) <= 126) continue;
            if (at - start >= minimum)
                result.put(
                        start,
                        new String(banks[bank], start, at - start, StandardCharsets.US_ASCII));
            start = at + 1;
        }
        return result;
    }
}
