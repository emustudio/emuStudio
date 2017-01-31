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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

@ThreadSafe
class CallFlow {

    private final Disassembler disassembler;
    private final NavigableMap<Integer, Integer> flowGraph = new ConcurrentSkipListMap<>();
    private int longestInstructionSize = 2;

    CallFlow(Disassembler disassembler) {
        this.disassembler = Objects.requireNonNull(disassembler);
    }
    
    void updateCache(int currentLocation) {
        flowGraph.put(currentLocation, disassembler.getNextInstructionPosition(currentLocation));
    }

    private int traverseTo(int knownFrom, int to, Consumer<Integer> consumer) {
        int lastKnownFrom;
        do {
            lastKnownFrom = knownFrom;

            consumer.accept(lastKnownFrom);

            knownFrom = disassembler.getNextInstructionPosition(knownFrom);
            if (knownFrom - lastKnownFrom > longestInstructionSize) {
                longestInstructionSize = knownFrom - lastKnownFrom;
            }
        } while (knownFrom < to);
        return (knownFrom == to) ? knownFrom : lastKnownFrom;
    }

    private int findGreatestPreviousLocation(int unknownLocation, SortedMap<Integer, Integer> knownLocations) {
        if (knownLocations.isEmpty() || knownLocations.firstKey() > unknownLocation) {
            Integer previousKnownLocation = flowGraph.lowerKey(unknownLocation);
            if (previousKnownLocation != null) {
                return traverseTo(previousKnownLocation, unknownLocation, i -> {});
            }
        }
        return knownLocations.isEmpty() ? unknownLocation : knownLocations.firstKey();
    }

    List<Integer> getLocationsInterval(int from, int to) {
        SortedMap<Integer, Integer> knownInterval = flowGraph.subMap(from, true, to, true);
        List<Integer> locations = new ArrayList<>();

        int lastLocation = -1;
        if (!knownInterval.containsKey(from)) {
            from = findGreatestPreviousLocation(from, knownInterval);
            if (!knownInterval.isEmpty() && from < knownInterval.firstKey()) {
                lastLocation = traverseTo(from, knownInterval.firstKey(), locations::add);
                if (lastLocation != knownInterval.firstKey()) {
                    lastLocation = disassembler.getNextInstructionPosition(lastLocation);
                }
            }
        }

        // keep locations sorted!
        boolean skipNext = false;
        List<Integer> invalidLocations = new ArrayList<>();
        for (Map.Entry<Integer, Integer> currentLocation : knownInterval.entrySet()) {
            int currentDecodedLocation = currentLocation.getKey();
            if (skipNext) {
                skipNext = false;
                if (lastLocation > currentDecodedLocation) {
                    invalidLocations.add(currentDecodedLocation);
                    continue;
                }
            }

            if (lastLocation != -1 && lastLocation < currentDecodedLocation) {
                lastLocation = traverseTo(lastLocation, currentDecodedLocation, locations::add);

                if (lastLocation < currentDecodedLocation) {
                    invalidLocations.add(currentDecodedLocation);
                    // move ahead because we will try to traverse to the end from the lastLocation
                    // after the loop
                    lastLocation = disassembler.getNextInstructionPosition(lastLocation);
                    break;
                } else if (lastLocation == to) {
                    // we fit
                    locations.add(currentDecodedLocation);
                }
            } else {
                if (lastLocation > currentDecodedLocation) {
                    skipNext = true;
                } else {
                    locations.add(currentDecodedLocation);
                    lastLocation = currentLocation.getValue();
                }
            }
        }
        invalidLocations.forEach(flowGraph::remove);

        if (lastLocation == -1) {
            lastLocation = from;
        }

        if (lastLocation < to) {
            int newTo = traverseTo(lastLocation, to, locations::add);
            if (newTo == to) {
                locations.add(newTo);
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
