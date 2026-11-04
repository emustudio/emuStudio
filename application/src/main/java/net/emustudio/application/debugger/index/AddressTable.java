/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Primitive address -> compact payload/list ID table. Zero means absent.
 * Each dense address table has an 8 MiB budget (2,097,152 physical cells).
 * Shared suffix exists only in bank zero. Sparse tables use 1024-cell pages,
 * a two-level directory (at most 2048 top-level entries for an int domain).
 * Uniform pages/directories hold a single ID, so a long covered range does not
 * allocate all its cells. Banks, directory leaves and pages are allocated lazily.
 * Pages with at most 16 nondefault cells retain sorted primitive offset/ID pairs;
 * their bounded binary search trades a few comparisons for sparse retained memory.
 */
final class AddressTable {
    private static final long DENSE_CELLS = 2L * 1024 * 1024;
    private static final int SHIFT = 10;
    private static final int MASK = (1 << SHIFT) - 1;
    private final int[] banks;
    private final Partition[] partitions;

    AddressTable(ProgramIndex.Layout layout, Map<Long, Integer> entries) {
        this(layout, pointSegments(entries));
    }

    AddressTable(ProgramIndex.Layout layout, List<BankEntries> entries) {
        this(layout, entries, 0);
    }

    static AddressTable bitmapWords(ProgramIndex.Layout layout, Map<Long, Integer> entries) {
        return new AddressTable(layout, pointSegments(entries), 6);
    }

    private AddressTable(ProgramIndex.Layout layout, List<BankEntries> entries, int cellShift) {
        banks = new int[entries.size()];
        partitions = new Partition[banks.length];
        int addressUnits = units(layout.addressSpaceSize(), cellShift);
        int bankUnits = units(layout.commonBoundary(), cellShift);
        long physicalCells = addressUnits + (long) (layout.bankCount() - 1) * bankUnits;
        boolean dense = physicalCells <= DENSE_CELLS;
        int i = 0;
        for (BankEntries group : entries) {
            int bank = group.bank;
            int size = bank <= 0 ? addressUnits : bankUnits;
            banks[i] = bank;
            partitions[i++] = new Partition(size, dense, group.segments);
        }
    }

    private static int units(int cells, int shift) {
        return (int) (((long) cells + (1L << shift) - 1) >>> shift);
    }

    private static List<BankEntries> pointSegments(Map<Long, Integer> entries) {
        Map<Integer, BankEntries> grouped = new TreeMap<>();
        entries.forEach((key, value) -> grouped.computeIfAbsent(bank(key), BankEntries::new)
                .segments.add(new Segment(address(key), address(key) + 1, value)));
        return new ArrayList<>(grouped.values());
    }

    static final class BankEntries {
        final int bank;
        final List<Segment> segments = new ArrayList<>();
        BankEntries(int bank) { this.bank = bank; }
    }

    static final class Segment {
        final int start, end, id;
        Segment(int start, int end, int id) { this.start = start; this.end = end; this.id = id; }
    }

    int get(int bank, int address) {
        int at = Arrays.binarySearch(banks, bank);
        return at < 0 ? 0 : partitions[at].get(address);
    }

    interface PartVisitor { void accept(int bank, int start, int end); }

    /** Splits a canonical range at the common boundary; common cells belong to physical bank zero. */
    static void split(ProgramIndex.Layout layout, ProgramIndex.Range range, PartVisitor visitor) {
        int lowerEnd = Math.min(range.end, layout.commonBoundary());
        if (range.start < lowerEnd) visitor.accept(range.bank, range.start, lowerEnd);
        int upperStart = Math.max(range.start, layout.commonBoundary());
        if (upperStart < range.end) visitor.accept(0, upperStart, range.end);
    }

    static long key(int bank, int address) { return ((long) bank << 32) | (address & 0xffffffffL); }
    static int bank(long key) { return (int) (key >>> 32); }
    static int address(long key) { return (int) key; }

    private static final class Partition {
        private final int[] dense;
        private final int[] uniformDirectories;
        private final int[][] uniformPages;
        private final int[][][] pages;

        Partition(int size, boolean useDense, List<Segment> entries) {
            if (useDense) {
                dense = new int[size];
                uniformDirectories = null;
                uniformPages = null;
                pages = null;
                for (Segment entry : entries) Arrays.fill(dense, entry.start, entry.end, entry.id);
            } else {
                dense = null;
                int directorySize = (int) (((long) size + (1L << (2 * SHIFT)) - 1) >>> (2 * SHIFT));
                uniformDirectories = new int[directorySize];
                uniformPages = new int[directorySize][];
                pages = new int[directorySize][][];
                for (Segment entry : entries) put(entry, size);
                compactPages();
            }
        }

        private void compactPages() {
            for (int upper = 0; upper < pages.length; upper++) {
                if (pages[upper] == null) continue;
                for (int lower = 0; lower < pages[upper].length; lower++) {
                    int[] page = pages[upper][lower];
                    if (page == null) continue;
                    int defaultId = uniformPages[upper][lower], count = 0;
                    for (int id : page) if (id != defaultId) count++;
                    if (count == 0) pages[upper][lower] = null;
                    else if (count <= 16) {
                        int[] exceptions = new int[count * 2];
                        int at = 0;
                        for (int offset = 0; offset < page.length; offset++) if (page[offset] != defaultId) {
                            exceptions[at] = offset;
                            exceptions[count + at++] = page[offset];
                        }
                        pages[upper][lower] = exceptions;
                    }
                }
            }
        }

        private void put(Segment entry, int size) {
            int at = entry.start;
            while (at < entry.end) {
                int upper = at >>> (2 * SHIFT);
                int directoryEnd = (int) Math.min(size, ((long) upper + 1) << (2 * SHIFT));
                if ((at & ((1 << (2 * SHIFT)) - 1)) == 0 && entry.end >= directoryEnd) {
                    uniformDirectories[upper] = entry.id;
                    uniformPages[upper] = null;
                    pages[upper] = null;
                    at = directoryEnd;
                    continue;
                }
                if (uniformPages[upper] == null) {
                    uniformPages[upper] = new int[1 << SHIFT];
                    Arrays.fill(uniformPages[upper], uniformDirectories[upper]);
                    pages[upper] = new int[1 << SHIFT][];
                }
                int limit = Math.min(entry.end, directoryEnd);
                while (at < limit) {
                    int lower = (at >>> SHIFT) & MASK;
                    int pageEnd = (int) Math.min(limit, ((long) (at >>> SHIFT) + 1) << SHIFT);
                    if ((at & MASK) == 0 && (pageEnd & MASK) == 0) {
                        uniformPages[upper][lower] = entry.id;
                        pages[upper][lower] = null;
                    } else {
                        if (pages[upper][lower] == null) {
                            pages[upper][lower] = new int[1 << SHIFT];
                            Arrays.fill(pages[upper][lower], uniformPages[upper][lower]);
                        }
                        int endOffset = (pageEnd & MASK) == 0 ? 1 << SHIFT : pageEnd & MASK;
                        Arrays.fill(pages[upper][lower], at & MASK, endOffset, entry.id);
                    }
                    at = pageEnd;
                }
            }
        }

        int get(int at) {
            if (dense != null) return dense[at];
            int upper = at >>> (2 * SHIFT);
            int[] directory = uniformPages[upper];
            if (directory == null) return uniformDirectories[upper];
            int lower = (at >>> SHIFT) & MASK;
            int[] page = pages[upper][lower];
            if (page == null) return directory[lower];
            if (page.length < 1 << SHIFT) {
                int count = page.length / 2;
                int found = Arrays.binarySearch(page, 0, count, at & MASK);
                return found < 0 ? directory[lower] : page[count + found];
            }
            return page[at & MASK];
        }
    }
}
