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
package emustudio.gui.debugTable;

import java.util.List;
import java.util.Objects;

public class PaginatingDisassembler {
    final static int INSTRUCTIONS_PER_PAGE = 2 * 15 + 1;
    final static int CURRENT_INSTRUCTION = 6;

    private final CallFlow callFlow;

    private int memorySize;
    private volatile int page;

    public PaginatingDisassembler(CallFlow callFlow, int memorySize) {
        if (memorySize < 0) {
            throw new IllegalArgumentException("Memory size < 0");
        }

        this.memorySize = memorySize;
        this.callFlow = Objects.requireNonNull(callFlow);
    }

    int bytesPerPage() {
        return INSTRUCTIONS_PER_PAGE * callFlow.getLongestInstructionSize();
    }

    void pagePrevious() {
        int tmpMemorySize = memorySize;

        // do not go over "backwards maximum"
        int bytesPP = bytesPerPage();
        if (page - bytesPP < tmpMemorySize) {
            page -= bytesPP;
        } else {
            page = -tmpMemorySize;
        }
    }

    void pageNext() {
        int tmpMemorySize = memorySize;

        // do not go over "forwards maximum"
        int bytesPP = bytesPerPage();
        if (page + bytesPP < tmpMemorySize) {
            page += bytesPP;
        } else {
            page = tmpMemorySize;
        }
    }

    void pageCurrent() {
        page = 0;
    }

    void pageFirst() {
        page = -memorySize;
    }

    void pageLast() {
        page = memorySize;
    }

    public boolean isRowAtCurrentInstruction(int row, int currentLocation) {
        boolean isAtExpectedRow = CURRENT_INSTRUCTION == row;

        if (page < 0 && currentLocation < bytesPerPage()) {
            return isAtExpectedRow;
        }
        if (page == 0) {
            return isAtExpectedRow;
        }

        return page > 0 && memorySize <= page + currentLocation && isAtExpectedRow;
    }

    void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    int rowToLocation(int currentLocation, int row) {
        int tmpMemorySize = memorySize;

        if (tmpMemorySize <= 0) {
            return -1;
        }

        callFlow.updateCache(currentLocation);

        int longestInstructionSize = callFlow.getLongestInstructionSize();
        int currentLocationInPage = Math.min(tmpMemorySize - 1, Math.max(currentLocation, currentLocation + page));
        int instructionGapBefore = CURRENT_INSTRUCTION * longestInstructionSize;
        int instructionGapAfter = (INSTRUCTIONS_PER_PAGE - CURRENT_INSTRUCTION) * longestInstructionSize;

        // recompute current page
        int from = Math.max(0, currentLocationInPage - instructionGapBefore);
        int to = Math.min(tmpMemorySize - 1, currentLocationInPage + instructionGapAfter);


        List<Integer> locationsInPage = callFlow.getLocationsInterval(from, to);
        int indexOfCurrentLocation = locationsInPage.indexOf(currentLocationInPage);

        if (indexOfCurrentLocation == -1) {
            indexOfCurrentLocation = 0;
        }

        int index;
        if (row == CURRENT_INSTRUCTION) {
            return Math.max(0, currentLocationInPage);
        } else {
            index = indexOfCurrentLocation + row - CURRENT_INSTRUCTION;
        }

        if (index < 0) {
            return -1;
        }
        if (index >= locationsInPage.size()) {
            return -1;
        }

        // assuming size of the page is big enough
        return locationsInPage.get(index);
    }

    void flushCache(int from, int to) {
        callFlow.flushCache(from, to + 1);
    }
}
