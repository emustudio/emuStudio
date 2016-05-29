/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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

import emulib.plugins.cpu.Disassembler;
import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@ThreadSafe
class InteractiveDisassembler {
    final static int INSTRUCTIONS_PER_PAGE = 2 * 15 + 1;
    final static int CURRENT_INSTRUCTION = 4;
    private final static int AVG_INSTRUCTION_SIZE = 2;
    final static int BYTES_PER_PAGE = INSTRUCTIONS_PER_PAGE * AVG_INSTRUCTION_SIZE;

    private final Disassembler disassembler;
    private volatile int memorySize;
    private final NavigableMap<Integer, List<Integer>> flowGraph = new ConcurrentSkipListMap<>();

    private volatile int addressOffset;

    InteractiveDisassembler(Disassembler disassembler, int memorySize) {
        if (memorySize < 0) {
            throw new IllegalArgumentException("Memory size < 0");
        }

        this.disassembler = Objects.requireNonNull(disassembler);
        this.memorySize = memorySize;
    }

    void pagePrevious() {
        int tmpMemorySize = memorySize;

        // do not go over "backwards maximum"
        if (addressOffset - BYTES_PER_PAGE < tmpMemorySize) {
            addressOffset -= BYTES_PER_PAGE;
        } else {
            addressOffset = -tmpMemorySize;
        }
    }

    void pageNext() {
        int tmpMemorySize = memorySize;

        // do not go over "forwards maximum"
        if (addressOffset + BYTES_PER_PAGE < tmpMemorySize) {
            addressOffset += BYTES_PER_PAGE;
        } else {
            addressOffset = tmpMemorySize;
        }
    }

    void pageCurrent() {
        addressOffset = 0;
    }

    void pageFirst() {
        addressOffset = -memorySize;
    }

    void pageLast() {
        addressOffset = memorySize;
    }

    private void updateCache(int currentLocation) {
        SortedMap<Integer, List<Integer>> tail = flowGraph.tailMap(currentLocation);

        // determine if currentLocation is already present in the cache
        if (!tail.containsKey(currentLocation)) {
            flowGraph.put(
                    currentLocation, Collections.singletonList(disassembler.getNextInstructionPosition(currentLocation))
            );
        } else {
            List<Integer> oldList = tail.get(currentLocation);

            int higherBound = oldList.get(oldList.size() - 1);
            do {
                // merge adjacent positions
                List<Integer> instructionsInTheInterval = new ArrayList<>();

                int singleBytesCount;
                int lowerBound = currentLocation;

                for (; lowerBound < higherBound; lowerBound += singleBytesCount) {
                    int nextPosition = disassembler.getNextInstructionPosition(lowerBound);
                    instructionsInTheInterval.add(nextPosition);

                    singleBytesCount = (nextPosition - lowerBound);
                }

                // solves two cases:
                //  1. lowerBound != higherBound
                //  2. lowerBound = higherBound and lists do not equal
                if (lowerBound != higherBound || !flowGraph.get(currentLocation).equals(instructionsInTheInterval)) {
                    flowGraph.subMap(
                            currentLocation, false, Math.max(lowerBound, higherBound), true
                    ).clear();
                    flowGraph.put(currentLocation, instructionsInTheInterval);
                } else {
                    break;
                }

                higherBound = instructionsInTheInterval.get(instructionsInTheInterval.size() - 1);
            } while (tail.containsKey(higherBound));
        }
    }

    private List<Integer> getLocationsInPage(SortedMap<Integer, List<Integer>> page, int currentLocationInPage) {
        List<Integer> locations = new ArrayList<>();

        int lastDecodedLocation = -1;
        for (Map.Entry<Integer, List<Integer>> currentDecodedLocation : page.entrySet()) {
            locations.add(currentDecodedLocation.getKey());
            locations.addAll(currentDecodedLocation.getValue());

            if (lastDecodedLocation != -1) {
                while (lastDecodedLocation < currentDecodedLocation.getKey()) {
                    lastDecodedLocation = disassembler.getNextInstructionPosition(lastDecodedLocation);
                    locations.add(lastDecodedLocation);
                }
            }

            int decodedSize = currentDecodedLocation.getValue().size();
            lastDecodedLocation = currentDecodedLocation.getValue().get(decodedSize - 1);
        }

        locations = locations.stream().sorted().distinct().collect(Collectors.toList());

        if (locations.isEmpty()) {
            locations.add(currentLocationInPage);
        }
        int indexOfCurrentLocation = locations.indexOf(currentLocationInPage);

        int instructionsToLoad = INSTRUCTIONS_PER_PAGE / 2;
        while (locations.size() < (indexOfCurrentLocation + instructionsToLoad)) {
            try {
                locations.add(disassembler.getNextInstructionPosition(locations.get(locations.size() - 1)));
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }

        return locations;
    }

    void flushCache(int fromLocationInclusive, int toLocationExclusive) {
        flowGraph.subMap(fromLocationInclusive, toLocationExclusive).clear();
    }

    void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    int rowToLocation(int currentLocation, int row) {
        int tmpMemorySize = memorySize;

        if (tmpMemorySize <= 0) {
            return -1;
        }

        updateCache(currentLocation);

        int currentLocationInPage = Math.min(tmpMemorySize - 1, Math.max(currentLocation, currentLocation + addressOffset));
        int instructionGapBefore = CURRENT_INSTRUCTION * AVG_INSTRUCTION_SIZE;
        int instructionGapAfter = (INSTRUCTIONS_PER_PAGE - CURRENT_INSTRUCTION) * AVG_INSTRUCTION_SIZE;

        // recompute current page
        int from = Math.max(0, currentLocationInPage - instructionGapBefore);
        int to = Math.min(tmpMemorySize - 1, currentLocationInPage + instructionGapAfter);

        SortedMap<Integer, List<Integer>> interval = flowGraph.subMap(from, true, to, true);

        List<Integer> locationsInPage = getLocationsInPage(interval, currentLocationInPage);
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

}
