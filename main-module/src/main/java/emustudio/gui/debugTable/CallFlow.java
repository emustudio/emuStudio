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
class CallFlow {

    private final Disassembler disassembler;
    private final NavigableMap<Integer, List<Integer>> flowGraph = new ConcurrentSkipListMap<>();
    private int longestInstructionSize = 2;


    CallFlow(Disassembler disassembler) {
        this.disassembler = Objects.requireNonNull(disassembler);
    }
    
    void updateCache(int currentLocation) {
        SortedMap<Integer, List<Integer>> tail = flowGraph.tailMap(currentLocation);

        // determine if currentLocation is already present in the cache
        if (!tail.containsKey(currentLocation)) {
            insertNewLocation(currentLocation);
        } else {
            checkAndFixTail(currentLocation, tail);
        }
    }

    private void checkAndFixTail(int currentLocation, SortedMap<Integer, List<Integer>> tail) {
        List<Integer> oldList = tail.get(currentLocation);

        int checkTo = oldList.get(oldList.size() - 1);
        do {
            // merge adjacent positions
            List<Integer> instructionsInTheInterval = new ArrayList<>();

            int singleBytesCount;
            int checkFrom = currentLocation;

            for (; checkFrom < checkTo; checkFrom += singleBytesCount) {
                int nextPosition = disassembler.getNextInstructionPosition(checkFrom);
                if (nextPosition - checkFrom > longestInstructionSize) {
                    longestInstructionSize = nextPosition - checkFrom;
                }

                instructionsInTheInterval.add(nextPosition);

                singleBytesCount = (nextPosition - checkFrom);
            }

            // solves two cases:
            //  1. checkFrom != checkTo
            //  2. checkFrom = checkTo and lists do not equal
            if (checkFrom != checkTo || !flowGraph.get(currentLocation).equals(instructionsInTheInterval)) {
                flowGraph.subMap(
                        currentLocation, false, Math.max(checkFrom, checkTo), true
                ).clear();
                flowGraph.put(currentLocation, instructionsInTheInterval);
            } else {
                break;
            }

            checkTo = instructionsInTheInterval.get(instructionsInTheInterval.size() - 1);
        } while (tail.containsKey(checkTo));
    }

    private void insertNewLocation(int currentLocation) {
        int nextLocation = disassembler.getNextInstructionPosition(currentLocation);
        if (nextLocation - currentLocation > longestInstructionSize) {
            longestInstructionSize = nextLocation - currentLocation;
        }
        flowGraph.put(
                currentLocation, Collections.singletonList(nextLocation)
        );
    }

    List<Integer> getLocationsInPage(int from, int to, int currentLocationInPage, int instructionsToLoad) {
        SortedMap<Integer, List<Integer>> page = flowGraph.subMap(from, true, to, true);
        List<Integer> locations = new ArrayList<>();

        int lastDecodedLocation = -1;
        for (Map.Entry<Integer, List<Integer>> currentDecodedLocation : page.entrySet()) {
            locations.add(currentDecodedLocation.getKey());
            locations.addAll(currentDecodedLocation.getValue());

            if (lastDecodedLocation != -1) {
                while (lastDecodedLocation < currentDecodedLocation.getKey()) {
                    int nextLocation = disassembler.getNextInstructionPosition(lastDecodedLocation);
                    if (nextLocation - lastDecodedLocation > longestInstructionSize) {
                        longestInstructionSize = nextLocation - lastDecodedLocation;
                    }
                    lastDecodedLocation = nextLocation;
                    
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

        while (locations.size() < (indexOfCurrentLocation + instructionsToLoad)) {
            try {
                int location = locations.get(locations.size() - 1);
                int nextLocation = disassembler.getNextInstructionPosition(location);
                if (nextLocation - location > longestInstructionSize) {
                    longestInstructionSize = nextLocation - location;
                }

                locations.add(nextLocation);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }

        return locations;
    }

    void flushCache(int fromLocationInclusive, int toLocationExclusive) {
        flowGraph.subMap(fromLocationInclusive, toLocationExclusive).clear();
    }

    int getLongestInstructionSize() {
        return longestInstructionSize;
    }
}
