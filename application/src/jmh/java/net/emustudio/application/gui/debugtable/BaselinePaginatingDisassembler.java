/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

// Benchmark-only baseline from 184f8d5a49fd40d99e553a80496c508e2e699618; class names only changed.

import net.jcip.annotations.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Paginating disassembler
 * <p>
 * Disassembler which holds pages of disassembled instructions as they go sequentially from current CPU position
 * onwards. One page is active at a time, which represents the content of a debug table. Debug table model reads instructions
 * from the active page.
 * <p>
 * A page is specified by its min and max location in memory. These locations tries to bound a fixed number of
 * instructions. A page is "filled" using "call flow" object, which can return disassembled instructions in given location
 * range.
 * <p>
 * A first-time fill creates first "snapshot" of instructions in memory. But there are a few situations implying the
 * need of refill of some pages. In such cases, all pages starting from changed location onwards are removed from cache.
 * The situations are as follows:
 * <p>
 * - on memory change (can change instruction size)
 * - on jump to a location which is not in the current call-flow path.
 * <p>
 * Call flow is the most important component of the paginating disassembler.
 * It is basically a map of known instruction locations pointing to next instructions. In other words, it's a set of
 * separate directed graphs. By using this graph map, one can build continuous "paths" from one location to another.
 * <p>
 * The complexity of a paginating disassembler lies in a requirement that all instructions in all pages must be reachable
 * from the current CPU position - the following ones and also previous ones. It's tricky since between the current
 * instruction and any other one can be data. The paginating disassembler doesn't know that, so if the data is "in the path"
 * of filling up the page, it is not recognized - it's treated as instructions.
 */
@ThreadSafe
public class BaselinePaginatingDisassembler {
    public final static int INSTR_PER_PAGE = 2 * 10 + 1;

    private final BaselineCallFlow callFlow; // call flow won't contain all locations, it grows only if row == middle
    private final NavigableMap<Integer, Page> bytesPerPageCache = new ConcurrentSkipListMap<>();

    private volatile int pageIndex;
    private final int lastAddress;

    private volatile Page currentPage = new Page(0, -1, -1);
    private volatile int lastKnownCurrentLocation;

    private volatile int instructionsPerPage = INSTR_PER_PAGE;
    private volatile int currentInstrRow = instructionsPerPage / 2;
    private volatile int instrPerHalfPage = instructionsPerPage / 2;

    BaselinePaginatingDisassembler(BaselineCallFlow callFlow, int addressSpaceSize) {
        this.callFlow = Objects.requireNonNull(callFlow);
        if (addressSpaceSize <= 0) {
            throw new IllegalArgumentException("Address-space size must be positive");
        }
        this.lastAddress = addressSpaceSize - 1;
        bytesPerPageCache.put(0, currentPage);
    }

    int getInstructionsPerPage() {
        return instructionsPerPage;
    }

    synchronized void setInstructionsPerPage(int value) {
        if (instructionsPerPage == value) return;
        instructionsPerPage = value;
        currentInstrRow = instructionsPerPage / 2;
        instrPerHalfPage = instructionsPerPage / 2;
        // Page extents belong to the old row count. The model retains the browsed anchor.
        bytesPerPageCache.clear();
        pageIndex = 0;
        currentPage = new Page(0, -1, -1);
        bytesPerPageCache.put(0, currentPage);
    }

    int getCurrentInstructionRow() {
        return currentInstrRow;
    }

    int getPageIndex() {
        return pageIndex;
    }

    void pagePrevious() {
        synchronized (this) {
            int previousPageIndex = pageIndex - 1;
            Page tmpPage = bytesPerPageCache.get(previousPageIndex);

            if (tmpPage == null) {
                tmpPage = currentPage;
                if (tmpPage.index != previousPageIndex + 1) {
                    // do not support concurrent page changes
                    return;
                }

                if (tmpPage.minLocation > 0) {
                    tmpPage = new Page(previousPageIndex, -1, tmpPage.minLocation);
                } else {
                    return;
                }
                Page existingPage = bytesPerPageCache.putIfAbsent(previousPageIndex, tmpPage);
                if (existingPage != null) {
                    tmpPage = existingPage;
                }
            }
            pageIndex = previousPageIndex;
            currentPage = tmpPage;
        }
    }

    void pageNext() {
        synchronized (this) {
            int nextPageIndex = pageIndex + 1;
            Page tmpPage = bytesPerPageCache.get(nextPageIndex);

            if (tmpPage == null) {
                tmpPage = currentPage;
                if (tmpPage.index != nextPageIndex - 1) {
                    // do not support concurrent page changes
                    return;
                }

                if (!tmpPage.lastPage && tmpPage.maxLocation >= 0) {
                    tmpPage = new Page(nextPageIndex, tmpPage.maxLocation, -1);
                } else {
                    return;
                }
                Page existingPage = bytesPerPageCache.putIfAbsent(nextPageIndex, tmpPage);
                if (existingPage != null) {
                    tmpPage = existingPage;
                }
            }
            pageIndex = nextPageIndex;
            currentPage = tmpPage;
        }
    }

    synchronized void pageCurrent() {
        pageIndex = 0;

        if (!bytesPerPageCache.containsKey(0)) {
            bytesPerPageCache.put(0, new Page(0, -1, -1));
        }

        currentPage = bytesPerPageCache.get(0);
    }

    void pageFirst() {
        Map.Entry<Integer, Page> firstPage = bytesPerPageCache.firstEntry();

        synchronized (this) {
            pageIndex = firstPage.getKey();
            currentPage = firstPage.getValue();

            int tmpPageIndex;
            do {
                tmpPageIndex = pageIndex;
                pagePrevious();
            } while (tmpPageIndex != pageIndex);
        }
    }

    void pageLast() {
        Map.Entry<Integer, Page> lastPage = bytesPerPageCache.lastEntry();

        synchronized (this) {
            pageIndex = lastPage.getKey();
            currentPage = lastPage.getValue();

            int tmpPageIndex;
            do {
                tmpPageIndex = pageIndex;
                pageNext();
                rowToLocation(lastKnownCurrentLocation, instructionsPerPage - 1);
            } while (tmpPageIndex != pageIndex);
        }
    }

    boolean isRowAtCurrentInstruction(int row) {
        return (pageIndex == 0) && (currentInstrRow == row);
    }

    synchronized int rowToLocation(int currentLocation, int row) {
        if (currentLocation < 0 || currentLocation > lastAddress || row < 0 || row >= instructionsPerPage) {
            return -1;
        }
        int tmpLastKnownCurrentLocation = lastKnownCurrentLocation;
        Page tmpCurrentPage = currentPage;

        if (currentLocation != tmpLastKnownCurrentLocation) {
            // delete all pages except the current one
            bytesPerPageCache.tailMap(tmpCurrentPage.index, false).clear();
            bytesPerPageCache.headMap(tmpCurrentPage.index, false).clear();

            tmpCurrentPage.setMiddleLocation(-1);
            tmpCurrentPage.setMin(-1);
            tmpCurrentPage.setMax(-1);
        }

        lastKnownCurrentLocation = currentLocation;

        int firstPageShift = boundaryRowShift(currentLocation);
        int newCurrentLocation = findCurrentLocationInPage(currentLocation, tmpCurrentPage, firstPageShift);
        boolean updateCalled = false;
        if (newCurrentLocation != currentLocation || tmpCurrentPage.middleLocation == -1) {
            tmpCurrentPage.setMiddleLocation(newCurrentLocation);
            callFlow.updateCache(currentLocation);
            updateCalled = true;
        }

        currentInstrRow = instrPerHalfPage - boundaryRowShift(newCurrentLocation);

        if (row == currentInstrRow) {
            if (!updateCalled) {
                // update cache always on current instruction
                callFlow.updateCache(currentLocation);
            }
            if (row == 0) tmpCurrentPage.setMin(newCurrentLocation);
            if (row == instructionsPerPage - 1) {
                tmpCurrentPage.setMax(newCurrentLocation);
                List<Integer> next = new ArrayList<>();
                callFlow.traverseForInstructionCount(newCurrentLocation, 1, next::add);
                tmpCurrentPage.setLastPage(next.isEmpty() || next.get(0) > lastAddress);
            }
            return newCurrentLocation;
        }

        int half = callFlow.getLongestInstructionSize() * instrPerHalfPage;

        if (row < currentInstrRow) {
            return findLocationBelowHalf(newCurrentLocation, row, half, tmpCurrentPage);
        } else {
            return findLocationAboveHalf(newCurrentLocation, row, tmpCurrentPage);
        }
    }

    void flushCache(int from, int to) {
        callFlow.flushCache(from, to + 1);
    }

    private int maxBytesPerPage() {
        return instructionsPerPage * callFlow.getLongestInstructionSize();
    }

    /** Keep valid instructions at the top when the centered page reaches address zero. */
    private int boundaryRowShift(int location) {
        int half = callFlow.getLongestInstructionSize() * instrPerHalfPage;
        if (location >= half) return 0;
        List<Integer> previous = callFlow.getLocations(0, location);
        int count = 0;
        for (int at : previous) if (at < location) count++;
        return Math.max(0, instrPerHalfPage - count);
    }

    private int findCurrentLocationInPage(int currentLocation, Page tmpCurrentPage, int firstPageShift) {
        if (tmpCurrentPage.index == 0) {
            return currentLocation;
        }

        if (tmpCurrentPage.middleLocation >= 0) {
            return tmpCurrentPage.middleLocation;
        }

        int currentPageIndex = tmpCurrentPage.index;
        if (currentPageIndex > 0) {
            return findIncreasing(currentLocation, tmpCurrentPage, currentPageIndex, firstPageShift);
        } else {
            return findDecreasing(currentLocation, tmpCurrentPage, currentPageIndex);
        }
    }

    private int findIncreasing(int currentLocation, Page tmpPage, int currentPageIndex, int firstPageShift) {
        // currentLocation is below current page. We will traverse from the last known current location
        // to the current page's one and set it to the current page.

        while (tmpPage.index > 0 && tmpPage.middleLocation < 0) {
            // find previous page from which we will traverse to the current page
            Integer prevPageIndex = bytesPerPageCache.floorKey(tmpPage.index - 1);
            if (prevPageIndex != null) {
                tmpPage = bytesPerPageCache.get(prevPageIndex);
            } else {
                break;
            }
        }
        int from = currentLocation;
        int sourcePageIndex = 0;
        if (tmpPage.middleLocation >= 0) {
            from = tmpPage.middleLocation;
            sourcePageIndex = tmpPage.index;
        }

        List<Integer> instructions = new ArrayList<>();

        int longestInstr = callFlow.getLongestInstructionSize();
        int stride = Math.max(1, instructionsPerPage - 1);
        int steps = (int) Math.min(Integer.MAX_VALUE, (long) (currentPageIndex - sourcePageIndex) * stride
                + (sourcePageIndex == 0 ? firstPageShift : 0));
        long guessedLocation = (long) currentLocation + ((long) currentPageIndex * stride + firstPageShift) * longestInstr;
        int guessUpTo = (int) Math.min(lastAddress, Math.max(0, guessedLocation));

        int result = callFlow.traverseUpTo(from, guessUpTo, instructions::add);
        if (result == guessUpTo) {
            instructions.add(guessUpTo);
        }

        int instrCount = instructions.size();
        if (instrCount <= steps) {
            // there is not enough instructions to reach currentRow. How to deal with it? Well, we can (in order):
            // 1. return the last instruction (if exists)
            // 2. If it doesn't, then return the middleLocation of tmpPage (if exists)
            // 3. If it doesn't, then return minLocation of tmpPage (if exists).
            // 4. Otherwise... return currentLocation... (what to do??)
            if (instrCount > 0) {
                return instructions.get(instrCount - 1);
            }
            if (tmpPage.middleLocation >= 0) {
                return tmpPage.middleLocation;
            }
            if (tmpPage.minLocation >= 0) {
                return tmpPage.minLocation;
            }

            return currentLocation; // if instrSize > 0, it still belongs to the "skip" group
        }

        return instructions.get(steps);
    }

    private int findDecreasing(int currentLocation, Page tmpPage, int currentPageIndex) {
        // currentLocation is above current page. So we will traverse back by maxBytesPerPage to the last known
        // location in some adjacent page (the nearer, the better).

        while (tmpPage.index < 0 && tmpPage.middleLocation < 0) {
            Integer nextPageIndex = bytesPerPageCache.ceilingKey(tmpPage.index + 1);
            if (nextPageIndex != null) {
                tmpPage = bytesPerPageCache.get(nextPageIndex);
            } else {
                break;
            }
        }
        int to = currentLocation;
        if (tmpPage.middleLocation >= 0) {
            to = tmpPage.middleLocation;
        }

        List<Integer> instructions = new ArrayList<>();
        int maxBytesPP = maxBytesPerPage();
        int longestInstr = callFlow.getLongestInstructionSize();
        int from = (int) Math.max(0, (long) currentLocation
                + (long) currentPageIndex * Math.max(longestInstr, maxBytesPP - longestInstr));

        int result = callFlow.traverseUpTo(from, to, instructions::add);
        if (result == to) {
            instructions.add(to);
        }

        int instrCount = instructions.size();
        int stride = Math.max(1, instructionsPerPage - 1);
        if (instrCount <= stride) {
            return 0;
        }
        return instructions.get(instrCount - stride - 1);
    }

    private int findLocationAboveHalf(int currentLocation, int row, Page tmpCurrentPage) {
        int following = instructionsPerPage - currentInstrRow - 1;
        int realUpTo = (int) Math.min(lastAddress, (long) currentLocation
                + (long) callFlow.getLongestInstructionSize() * following);
        int upperBound = nextPageBoundary(tmpCurrentPage);
        realUpTo = Math.min(realUpTo, upperBound);
        if (currentLocation > realUpTo) {
            return -1;
        }

        List<Integer> halfPage = callFlow.getLocations(currentLocation, realUpTo);
        int loadedHalfSize = halfPage.size();

        if (realUpTo < upperBound && loadedHalfSize > 0 && loadedHalfSize < following + 1) {
            // try to fill it up (the "half" was not enough)
            callFlow.traverseForInstructionCount(halfPage.get(loadedHalfSize - 1), following - loadedHalfSize + 1,
                    location -> { if (location <= upperBound) halfPage.add(location); });
            loadedHalfSize = halfPage.size();
        }

        if (halfPage.isEmpty()) {
            return -1;
        }
        halfPage.remove(0); // current instruction would be twice otherwise
        loadedHalfSize--;

        int lastInstructionIndex = currentInstrRow + loadedHalfSize;

        tmpCurrentPage.setLastPage(upperBound == lastAddress && lastInstructionIndex < instructionsPerPage - 1);
        tmpCurrentPage.setMax(loadedHalfSize == 0 ? currentLocation
                : halfPage.get(Math.min(following, loadedHalfSize) - 1));
        if (row > lastInstructionIndex) {
            return -1;
        }

        int rowLocation = halfPage.get(row - currentInstrRow - 1);
        return rowLocation;
    }

    private int nextPageBoundary(Page page) {
        Page nextPage = bytesPerPageCache.get(page.index + 1);
        if (nextPage != null && nextPage.minLocation >= 0) return nextPage.minLocation;
        if (page.index >= 0) return lastAddress;
        // A stepped CPU invalidates neighboring cached pages. Retain the partial first
        // page's overlap with the following centered page while its cache rebuilds.
        Page next = nextPage == null ? new Page(page.index + 1, -1, -1) : nextPage;
        int middle = findCurrentLocationInPage(lastKnownCurrentLocation, next, boundaryRowShift(lastKnownCurrentLocation));
        List<Integer> previous = precedingLocations(middle, callFlow.getLongestInstructionSize() * instrPerHalfPage);
        return previous.isEmpty() ? middle : previous.get(0);
    }

    private List<Integer> precedingLocations(int currentLocation, int half) {
        int realFrom = Math.max(0, currentLocation - half);

        List<Integer> halfPage = new ArrayList<>(callFlow.getLocations(realFrom, currentLocation));

        int loadedHalfSize = halfPage.size();
        if (realFrom > 0 && loadedHalfSize < instructionsPerPage) {
            // we do not have enough instructions. This might be caused by the "half" which is not big enough,
            // or by the fact that we just don't know how much instructions back is known.
            callFlow.traverseBackForInstructionCount(
                    realFrom,
                    instructionsPerPage - loadedHalfSize + 1,
                    loc -> halfPage.add(0, loc)
            );
            loadedHalfSize = halfPage.size();
        }

        if (!halfPage.isEmpty() && halfPage.get(loadedHalfSize - 1) == currentLocation) {
            halfPage.remove(loadedHalfSize - 1);
        }

        int i = 0;
        while (halfPage.size() > instrPerHalfPage && i < instrPerHalfPage) {
            halfPage.remove(0);
            i++;
        }
        return halfPage;
    }

    private int findLocationBelowHalf(int currentLocation, int row, int half, Page tmpCurrentPage) {
        List<Integer> halfPage = precedingLocations(currentLocation, half);
        int firstInstructionIndex = currentInstrRow - halfPage.size();
        if (row < firstInstructionIndex) {
            return -1;
        }

        int rowLocation = halfPage.get(row - firstInstructionIndex);
        if (row == firstInstructionIndex) {
            tmpCurrentPage.setMin(rowLocation);
        }

        return rowLocation;
    }

    private static final class Page {
        private final int index;
        private volatile int minLocation;
        private volatile int maxLocation;
        private volatile int middleLocation = -1;
        private volatile boolean lastPage = false;

        Page(int index, int minLocation, int maxLocation) {
            this.index = index;
            this.minLocation = minLocation;
            this.maxLocation = maxLocation;
        }

        void setMin(int location) {
            minLocation = location;
        }

        void setMax(int location) {
            maxLocation = location;
        }

        void setMiddleLocation(int middleLocation) {
            this.middleLocation = middleLocation;
        }

        void setLastPage(boolean lastPage) {
            this.lastPage = lastPage;
        }

        @Override
        public String toString() {
            return "Page{" +
                    "index=" + index +
                    ", minLocation=" + minLocation +
                    ", maxLocation=" + maxLocation +
                    ", middleLocation=" + middleLocation +
                    ", lastPage=" + lastPage +
                    '}';
        }
    }
}
