/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus.api;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ZX Spectrum model-specific timing and layout constants used by emuStudio.
 */
public enum TimingProfile {
    ZX_SPECTRUM_48K(
            0x4000,
            0x7FFF,
            64,
            56,
            224,
            32,
            14335,
            3,
            new int[]{6, 5, 4, 3, 2, 1, 0, 0}
    );

    public final int contendedMemoryStart;
    public final int contendedMemoryEnd;
    public final int preScreenLines;
    public final int postScreenLines;
    public final int displayLineTstates;
    public final int interruptTstates;
    public final int firstContendedTstate;
    public final int floatingBusSampleOffsetTstates;
    public final List<Integer> contentionPattern;
    public final int frameLineCount;
    public final int displayFrameTstates;
    public final int screenFetchCycles;
    public final long firstFloatingBusTstate;
    private final int[] contentionDelays;

    TimingProfile(
            int contendedMemoryStart,
            int contendedMemoryEnd,
            int preScreenLines,
            int postScreenLines,
            int displayLineTstates,
            int interruptTstates,
            int firstContendedTstate,
            int floatingBusSampleOffsetTstates,
            int[] contentionPattern
    ) {
        this.contendedMemoryStart = contendedMemoryStart;
        this.contendedMemoryEnd = contendedMemoryEnd;
        this.preScreenLines = preScreenLines;
        this.postScreenLines = postScreenLines;
        this.displayLineTstates = displayLineTstates;
        this.interruptTstates = interruptTstates;
        this.firstContendedTstate = firstContendedTstate;
        this.floatingBusSampleOffsetTstates = floatingBusSampleOffsetTstates;
        this.contentionPattern = Arrays.stream(contentionPattern)
                .boxed()
                .collect(Collectors.toUnmodifiableList());
        this.frameLineCount = preScreenLines + ZxSpectrumBus.SCREEN_HEIGHT_PIXELS + postScreenLines;
        this.displayFrameTstates = frameLineCount * displayLineTstates;
        this.screenFetchCycles = ZxSpectrumBus.ATTRIBUTES_WIDTH * 4;
        this.firstFloatingBusTstate = (long) firstContendedTstate + floatingBusSampleOffsetTstates;
        this.contentionDelays = buildContentionDelays();
    }

    public boolean isContendedMemoryAddress(int location) {
        return location >= contendedMemoryStart && location <= contendedMemoryEnd;
    }

    public int contentionDelayAt(long cycle) {
        long normalized = cycle % displayFrameTstates;
        if (normalized < 0) {
            normalized += displayFrameTstates;
        }
        return contentionDelays[(int) normalized];
    }

    public int portContentionDelay(long frameCycle, int portAddress) {
        if (isContendedMemoryAddress(portAddress)) {
            if ((portAddress & 1) == 0) {
                return contentionDelayAt(frameCycle) + contentionDelayAt(frameCycle + 1);
            }
            return contentionDelayAt(frameCycle)
                    + contentionDelayAt(frameCycle + 1)
                    + contentionDelayAt(frameCycle + 2)
                    + contentionDelayAt(frameCycle + 3);
        }
        if ((portAddress & 1) == 0) {
            return contentionDelayAt(frameCycle + 1);
        }
        return 0;
    }

    public boolean isFloatingBusDrivenAtCycle(int cycleInLine) {
        return cycleInLine >= 0 && cycleInLine < screenFetchCycles && (cycleInLine & 7) < 4;
    }

    public boolean isFloatingBusAttributePhase(int cycleInLine) {
        return (cycleInLine & 1) == 1;
    }

    public int floatingBusColumnAt(int cycleInLine) {
        return (cycleInLine / 8) * 2 + ((cycleInLine & 2) >>> 1);
    }

    private int[] buildContentionDelays() {
        int[] contentionDelays = new int[displayFrameTstates];
        int patternSize = contentionPattern.size();

        for (int line = 0; line < ZxSpectrumBus.SCREEN_HEIGHT_PIXELS; line++) {
            int lineStart = firstContendedTstate + line * displayLineTstates;
            for (int cycle = 0; cycle < screenFetchCycles; cycle += patternSize) {
                for (int phase = 0; phase < patternSize; phase++) {
                    int delay = contentionPattern.get(phase);
                    contentionDelays[lineStart + cycle + phase] = delay;
                }
            }
        }

        return contentionDelays;
    }

    public int screenAddressAt(int line, int column) {
        int lineOffset = ((line & 0xC0) << 5) | ((line & 7) << 8) | ((line & 0x38) << 2);
        return ZxSpectrumBus.SCREEN_MEMORY_BASE + lineOffset + column;
    }

    public int attributeAddressAt(int line, int column) {
        int attributeOffset = ((line >>> 3) << 5) | column;
        return ZxSpectrumBus.ATTRIBUTE_MEMORY_BASE + attributeOffset;
    }

    public int attributeAddressAtRow(int row, int column) {
        return attributeAddressAt(row << 3, column);
    }
}
