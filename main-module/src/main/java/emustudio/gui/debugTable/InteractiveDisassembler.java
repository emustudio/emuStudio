package emustudio.gui.debugTable;

import emulib.plugins.cpu.Disassembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class InteractiveDisassembler {
    public final static int INSTRUCTIONS_IN_GAP = 8;
    private final static int AVG_INSTRUCTION_SIZE = 2;
    public final static int BYTES_PER_PAGE = 2 * INSTRUCTIONS_IN_GAP * AVG_INSTRUCTION_SIZE;

    private final Disassembler disassembler;
    private final int memorySize;
    private final NavigableMap<Integer, List<Integer>> flowGraph = new TreeMap<>();

    private volatile int addressOffset;

    public InteractiveDisassembler(Disassembler disassembler, int memorySize) {
        if (memorySize < 0) {
            throw new IllegalArgumentException("Memory size < 0");
        }

        this.disassembler = Objects.requireNonNull(disassembler);
        this.memorySize = memorySize;
    }

    public void pagePrevious() {
        // do not go over "backwards maximum"
        if (addressOffset - BYTES_PER_PAGE < memorySize) {
            addressOffset -= BYTES_PER_PAGE;
        } else {
            addressOffset = -memorySize;
        }
    }

    public void pageNext() {
        // do not go over "forwards maximum"
        if (addressOffset + BYTES_PER_PAGE < memorySize) {
            addressOffset += BYTES_PER_PAGE;
        } else {
            addressOffset = memorySize;
        }
    }

    public void pageCurrent() {
        addressOffset = 0;
    }

    public void pageFirst() {
        addressOffset = -memorySize;
    }

    public void pageLast() {
        addressOffset = memorySize;
    }

    private void updateCache(int currentLocation) {
        SortedMap<Integer, List<Integer>> tail = flowGraph.tailMap(currentLocation);

        // determine if currentLocation is already present in the cache
        if (!tail.containsKey(currentLocation)) {
            flowGraph.put(
                    currentLocation, Arrays.asList(disassembler.getNextInstructionPosition(currentLocation))
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

        while (locations.size() < (indexOfCurrentLocation + INSTRUCTIONS_IN_GAP)) {
            try {
                locations.add(disassembler.getNextInstructionPosition(locations.get(locations.size() - 1)));
            } catch (IndexOutOfBoundsException e) {
                break;
            }
        }

        return locations;
    }

    public int rowToLocation(int currentLocation, int row) {
        updateCache(currentLocation);

        int currentLocationInPage = currentLocation + addressOffset;
        int instructionGap = INSTRUCTIONS_IN_GAP * AVG_INSTRUCTION_SIZE;

        // recompute current page
        int from = Math.max(0, currentLocationInPage - instructionGap);
        int to = Math.min(memorySize - 1, currentLocationInPage + instructionGap);

        if (from > to) {
            to = from;
        }

        SortedMap<Integer, List<Integer>> interval = flowGraph.subMap(from, true, to, true);

        List<Integer> locationsInPage = getLocationsInPage(interval, currentLocationInPage);
        int indexOfCurrentLocation = locationsInPage.indexOf(currentLocationInPage);

        if (indexOfCurrentLocation == -1) {
            indexOfCurrentLocation = 0;
        }

        int index;
        if (row == INSTRUCTIONS_IN_GAP) {
            return Math.max(0, currentLocationInPage);
        } else if (row < INSTRUCTIONS_IN_GAP) {
            index = indexOfCurrentLocation - (INSTRUCTIONS_IN_GAP - row);
        } else {
            index = indexOfCurrentLocation + (row - INSTRUCTIONS_IN_GAP);
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
