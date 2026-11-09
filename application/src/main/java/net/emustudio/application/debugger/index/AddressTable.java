/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Primitive address -> compact payload ID table. Zero means absent.
 * Each dense address table has an 8 MiB budget (2,097,152 cells).
 * Sparse tables use 1024-cell pages,
 * a two-level directory (at most 2048 top-level entries for an int domain).
 * Uniform pages/directories hold a single ID, so a long covered range does not
 * allocate all its cells. Directory leaves and pages are allocated lazily.
 * Pages with at most 16 nondefault cells retain sorted primitive offset/ID pairs;
 * their bounded binary search trades a few comparisons for sparse retained memory.
 */
final class AddressTable {
    private static final long DENSE_CELLS = 2L * 1024 * 1024;
    private static final int SHIFT = 10;
    private static final int MASK = (1 << SHIFT) - 1;
    private final Partition partition;

    AddressTable(ProgramIndex.Layout layout, Map<Integer, Integer> entries) {
        this(layout, pointSegments(entries));
    }

    AddressTable(ProgramIndex.Layout layout, List<Segment> entries) {
        int addressUnits = layout.addressSpaceSize();
        partition = entries.isEmpty() ? null : new Partition(addressUnits, addressUnits <= DENSE_CELLS, entries);
    }

    private static List<Segment> pointSegments(Map<Integer, Integer> entries) {
        List<Segment> segments = new ArrayList<>();
        entries.forEach((address, value) -> segments.add(new Segment(address, address + 1, value)));
        return segments;
    }

    static final class Segment {
        final int start, end, id;
        Segment(int start, int end, int id) { this.start = start; this.end = end; this.id = id; }
    }

    int get(int address) {
        return partition == null ? 0 : partition.get(address);
    }

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
