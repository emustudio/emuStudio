/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.debugtable;

import net.jcip.annotations.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

@ThreadSafe
public class PaginatingDisassembler {
    public final static int INSTR_PER_PAGE = 2 * 10 + 1;
    final static int CURRENT_INSTR_ROW = INSTR_PER_PAGE / 2; // do not touch this; the algorithm depends on it
    final static int INSTR_PER_HALF_PAGE = INSTR_PER_PAGE / 2;

    private final CallFlow callFlow;
    private final NavigableMap<Integer, Page> bytesPerPageCache = new ConcurrentSkipListMap<>();

    private int memorySize;
    private volatile int pageIndex;

    private volatile Page currentPage = new Page(0, -1, -1);
    private volatile int lastKnownCurrentLocation;

    private final class Page {
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
            return "P{" +
                "index=" + index +
                ", minLocation=" + minLocation +
                ", maxLocation=" + maxLocation +
                ", middleLocation=" + middleLocation +
                ", lastPage=" + lastPage +
                '}';
        }
    }

    PaginatingDisassembler(CallFlow callFlow, int memorySize) {
        if (memorySize < 0) {
            throw new IllegalArgumentException("Memory size < 0");
        }

        this.memorySize = memorySize;
        this.callFlow = Objects.requireNonNull(callFlow);
        bytesPerPageCache.put(0, currentPage);
    }

    private int maxBytesPerPage() {
        return INSTR_PER_PAGE * callFlow.getLongestInstructionSize();
    }

    int getPageIndex() {
        return pageIndex;
    }

    void pagePrevious() {
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
            bytesPerPageCache.putIfAbsent(previousPageIndex, tmpPage);
        }
        synchronized (this) {
            pageIndex = previousPageIndex;
            currentPage = tmpPage;
        }
    }

    void pageNext() {
        int nextPageIndex = pageIndex + 1;
        Page tmpPage = bytesPerPageCache.get(nextPageIndex);
        int maxMemoryIndex = memorySize - 1;

        if (tmpPage == null) {
            tmpPage = currentPage;
            if (tmpPage.index != nextPageIndex - 1) {
                // do not support concurrent page changes
                return;
            }

            if (!tmpPage.lastPage && tmpPage.maxLocation >= 0 && tmpPage.maxLocation + 1 < maxMemoryIndex) {
                tmpPage = new Page(nextPageIndex, tmpPage.maxLocation, -1);
            } else {
                return;
            }
            bytesPerPageCache.putIfAbsent(nextPageIndex, tmpPage);
        }
        synchronized (this) {
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
                rowToLocation(lastKnownCurrentLocation, INSTR_PER_PAGE - 1);
            } while (tmpPageIndex != pageIndex);
        }
    }

    boolean isRowAtCurrentInstruction(int row) {
        return (pageIndex == 0) && (CURRENT_INSTR_ROW == row);
    }

    void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    int rowToLocation(int currentLocation, int row) {
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

        int newCurrentLocation = findCurrentLocationInPage(currentLocation, tmpCurrentPage);
        if (newCurrentLocation != currentLocation || tmpCurrentPage.middleLocation == -1) {
            tmpCurrentPage.setMiddleLocation(newCurrentLocation);
            callFlow.updateCache(currentLocation);
        }

        if (row == CURRENT_INSTR_ROW) {
            return newCurrentLocation;
        }

        int half = callFlow.getLongestInstructionSize() * INSTR_PER_HALF_PAGE;

        if (row < INSTR_PER_HALF_PAGE) {
            return findLocationBelowHalf(newCurrentLocation, row, half, tmpCurrentPage);
        } else {
            return findLocationAboveHalf(newCurrentLocation, row, half, tmpCurrentPage);
        }
    }

    private int findCurrentLocationInPage(int currentLocation, Page tmpCurrentPage) {
        if (tmpCurrentPage.index == 0) {
            return currentLocation;
        }

        if (tmpCurrentPage.middleLocation >= 0) {
            return tmpCurrentPage.middleLocation;
        }

        int currentPageIndex = tmpCurrentPage.index;
        if (currentPageIndex > 0) {
            return findIncreasing(currentLocation, tmpCurrentPage, currentPageIndex);
        } else {
            return findDecreasing(currentLocation, tmpCurrentPage, currentPageIndex);
        }
    }

    private int findIncreasing(int currentLocation, Page tmpPage, int currentPageIndex) {
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
        if (tmpPage.middleLocation >= 0) {
            from = tmpPage.middleLocation;
        }

        List<Integer> instructions = new ArrayList<>();

        int maxBytesPP = maxBytesPerPage();
        int longestInstr = callFlow.getLongestInstructionSize();
        int guessUpTo = Math.min(memorySize - 1, currentLocation + currentPageIndex * (maxBytesPP - longestInstr));

        int result = callFlow.traverseUpTo(from, guessUpTo, instructions::add);
        if (result == guessUpTo) {
            instructions.add(guessUpTo);
        }

        int instrCount = instructions.size();
        if (instrCount < INSTR_PER_PAGE) {
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

        return instructions.listIterator(INSTR_PER_PAGE - 1).next();
    }

    private int findDecreasing(int currentLocation, Page tmpPage, int currentPageIndex) {
        // currentLocation is above current page. So we will traverse back by maxBytesPerPage to the last known
        // location in some adjacent page (the nearer the better).

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
        int from = Math.max(0, currentLocation + currentPageIndex * (maxBytesPP - longestInstr));

        int result = callFlow.traverseUpTo(from, to, instructions::add);
        if (result == to) {
            instructions.add(to);
        }

        int instrCount = instructions.size();
        if (instrCount < INSTR_PER_PAGE) {
            return 0;
        }
        return instructions.listIterator(instrCount - INSTR_PER_PAGE).next();
    }


    private int findLocationAboveHalf(int currentLocation, int row, int half, Page tmpCurrentPage) {
        int lastMemoryIndex = memorySize - 1;
        if (lastMemoryIndex < 0) {
            return -1;
        }

        int realUpTo = Math.min(lastMemoryIndex, currentLocation + half);
        if (currentLocation > realUpTo) {
            return -1;
        }

        List<Integer> halfPage = callFlow.getLocations(currentLocation, realUpTo);
        int loadedHalfSize = halfPage.size();

        if (realUpTo < lastMemoryIndex && loadedHalfSize < INSTR_PER_HALF_PAGE) {
            // try to fill it up (the "half" was not enough)
            callFlow.traverseForInstructionCount(realUpTo, INSTR_PER_HALF_PAGE - loadedHalfSize + 1, halfPage::add);
            loadedHalfSize = halfPage.size();
        }

        if (halfPage.isEmpty()) {
            return -1;
        }
        halfPage.remove(0); // current instruction would be twice otherwise
        loadedHalfSize--;

        int lastInstructionIndex = INSTR_PER_HALF_PAGE + loadedHalfSize;

        tmpCurrentPage.setLastPage(lastInstructionIndex < INSTR_PER_PAGE - 1);
        if (row > lastInstructionIndex) {
            return -1;
        }

        int rowLocation = halfPage.get(row - INSTR_PER_HALF_PAGE - 1);
        if (row == Math.min(INSTR_PER_PAGE - 1, lastInstructionIndex)) {
            tmpCurrentPage.setMax(rowLocation);
        }

        return rowLocation;
    }

    private int findLocationBelowHalf(int currentLocation, int row, int half, Page tmpCurrentPage) {
        int realFrom = Math.max(0, currentLocation - half);

        List<Integer> halfPage = callFlow.getLocations(realFrom, currentLocation);

        int loadedHalfSize = halfPage.size();
        if (realFrom > 0 && loadedHalfSize < INSTR_PER_PAGE) {
            // we do not have enough instructions. This might be caused by the "half" which is not big enough,
            // or by the fact that we just don't know how much instructions back is known.
            callFlow.traverseBackForInstructionCount(
                realFrom,
                INSTR_PER_PAGE - loadedHalfSize + 1,
                loc -> halfPage.add(0, loc)
            );
            loadedHalfSize = halfPage.size();
        }

        if (!halfPage.isEmpty() && halfPage.get(loadedHalfSize - 1) == currentLocation) {
            halfPage.remove(loadedHalfSize - 1);
        }

        int i = 0;
        while (halfPage.size() > INSTR_PER_HALF_PAGE && i < INSTR_PER_HALF_PAGE) {
            halfPage.remove(0);
            i++;
        }

        int firstInstructionIndex = INSTR_PER_HALF_PAGE - halfPage.size();
        if (row < firstInstructionIndex) {
            return -1;
        }

        int rowLocation = halfPage.get(row - firstInstructionIndex);
        if (row == firstInstructionIndex) {
            tmpCurrentPage.setMin(rowLocation);
        }

        return rowLocation;
    }

    void flushCache(int from, int to) {
        callFlow.flushCache(from, to + 1);
    }
}
