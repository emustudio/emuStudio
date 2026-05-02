/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus.api;

/**
 * ZX Spectrum model-specific timing and layout constants used by emuStudio.
 *
 * <p>Each enum entry stores the raw machine timings taken from the target model and precomputes
 * a few derived values that the bus and ULA use often enough to make caching worthwhile. The raw
 * values describe one frame worth of contention, floating-bus sampling, and screen layout; the
 * derived values keep the call sites free from repeating the same arithmetic.
 */
public enum TimingProfile {

    /**
     * ZX Spectrum 48K timings.
     *
     * <p>Constructor arguments, in order:
     * <ol>
     * <li>contended RAM start address</li>
     * <li>contended RAM end address</li>
     * <li>border lines before the visible screen</li>
     * <li>border lines after the visible screen</li>
     * <li>T-states per raster line</li>
     * <li>length of the frame interrupt pulse in T-states</li>
     * <li>frame T-state where the first contended screen fetch starts</li>
     * <li>extra T-states between that fetch and the first floating-bus sample</li>
     * <li>8-T-state contention pattern repeated across each visible line</li>
     * </ol>
     */
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

    /**
     * Inclusive start of the RAM range slowed down by ULA screen fetches.
     */
    public final int contendedMemoryStart;
    /**
     * Inclusive end of the RAM range slowed down by ULA screen fetches.
     */
    public final int contendedMemoryEnd;
    /**
     * Border lines before the visible bitmap area starts.
     */
    public final int preScreenLines;
    /**
     * Border lines after the visible bitmap area ends.
     */
    public final int postScreenLines;
    /**
     * Number of T-states in one full raster line, including borders and blanking.
     */
    public final int displayLineTstates;
    /**
     * Duration of the frame interrupt pulse asserted by the ULA.
     */
    public final int interruptTstates;
    /**
     * First frame T-state where visible-line contention begins.
     */
    public final int firstContendedTstate;
    /**
     * Offset from {@link #firstContendedTstate} to the first floating-bus sample point.
     */
    public final int floatingBusSampleOffsetTstates;
    /**
     * Total frame height in raster lines, including top and bottom borders.
     */
    public final int frameLineCount;
    /**
     * Total number of T-states in one complete frame.
     */
    public final int displayFrameTstates;
    /**
     * Visible-line fetch window in T-states, covering bitmap and attribute reads.
     */
    public final int screenFetchCycles;
    /**
     * Absolute frame T-state of the first floating-bus sample on the first visible line.
     */
    public final long firstFloatingBusTstate;
    /**
     * Delay pattern repeated every 8 T-states while the ULA fetches screen data.
     */
    private final int[] contentionPattern;
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
        this.contentionPattern = contentionPattern.clone();
        this.frameLineCount = preScreenLines + ZxSpectrumBus.SCREEN_HEIGHT_PIXELS + postScreenLines;
        this.displayFrameTstates = frameLineCount * displayLineTstates;
        this.screenFetchCycles = ZxSpectrumBus.ATTRIBUTES_WIDTH * 4;
        this.firstFloatingBusTstate = (long) firstContendedTstate + floatingBusSampleOffsetTstates;
        this.contentionDelays = buildContentionDelays();
    }

    /**
     * Returns whether the address lies in the model's contended RAM window.
     */
    public boolean isContendedMemoryAddress(int location) {
        return location >= contendedMemoryStart && location <= contendedMemoryEnd;
    }

    /**
     * Returns the ULA-imposed wait-state penalty for a frame T-state.
     *
     * <p>The lookup wraps around frame boundaries so callers can pass absolute cycle counters.
     */
    public int contentionDelayAt(long cycle) {
        long normalized = cycle % displayFrameTstates;
        if (normalized < 0) {
            normalized += displayFrameTstates;
        }
        return contentionDelays[(int) normalized];
    }

    /**
     * Returns the extra wait states for an I/O access at the given frame T-state.
     *
     * <p>The rules match the 48K ULA behaviour:
     * <ul>
     * <li>contended odd ports sample four consecutive contention slots</li>
     * <li>contended even ports sample two slots</li>
     * <li>non-contended even ports still incur one delayed slot</li>
     * <li>non-contended odd ports do not stall</li>
     * </ul>
     */
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

    /**
     * Returns whether the floating bus is driven by screen fetch data at this line-local cycle.
     */
    public boolean isFloatingBusDrivenAtCycle(int cycleInLine) {
        return cycleInLine >= 0 && cycleInLine < screenFetchCycles && (cycleInLine & 7) < 4;
    }

    /**
     * Returns whether the driven floating-bus byte comes from attributes rather than bitmap data.
     */
    public boolean isFloatingBusAttributePhase(int cycleInLine) {
        return (cycleInLine & 1) == 1;
    }

    /**
     * Maps a driven floating-bus cycle to the screen or attribute column being fetched.
     */
    public int floatingBusColumnAt(int cycleInLine) {
        return (cycleInLine / 8) * 2 + ((cycleInLine & 2) >>> 1);
    }

    private int[] buildContentionDelays() {
        int[] contentionDelays = new int[displayFrameTstates];
        int patternSize = contentionPattern.length;

        for (int line = 0; line < ZxSpectrumBus.SCREEN_HEIGHT_PIXELS; line++) {
            int lineStart = firstContendedTstate + line * displayLineTstates;
            for (int cycle = 0; cycle < screenFetchCycles; cycle += patternSize) {
                for (int phase = 0; phase < patternSize; phase++) {
                    int delay = contentionPattern[phase];
                    contentionDelays[lineStart + cycle + phase] = delay;
                }
            }
        }

        return contentionDelays;
    }

    /**
     * Returns the Spectrum bitmap memory address for a pixel row and 8-pixel column.
     */
    public int screenAddressAt(int line, int column) {
        int lineOffset = ((line & 0xC0) << 5) | ((line & 7) << 8) | ((line & 0x38) << 2);
        return ZxSpectrumBus.SCREEN_MEMORY_BASE + lineOffset + column;
    }

    /**
     * Returns the Spectrum attribute memory address for a pixel row and attribute column.
     */
    public int attributeAddressAt(int line, int column) {
        int attributeOffset = ((line >>> 3) << 5) | column;
        return ZxSpectrumBus.ATTRIBUTE_MEMORY_BASE + attributeOffset;
    }

    /**
     * Convenience wrapper that accepts an 8-pixel character row instead of a pixel row.
     */
    public int attributeAddressAtRow(int row, int column) {
        return attributeAddressAt(row << 3, column);
    }
}
