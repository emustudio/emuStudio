/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus.api;

/**
 * Immutable ZX Spectrum hardware definitions used by the bus, ULA, display, and beeper.
 *
 * <p>Each entry defines one model. Runtime state, such as cycle counters and selected memory
 * banks, belongs to the bus and devices, never to these shared enum instances. The CPU context
 * remains authoritative for the current clock; {@link #cpuFrequencyKHz} describes the nominal clock.
 */
public enum ModelProfile {

    /**
     * ZX Spectrum 48K with Issue 3 beeper levels.
     */
    ZX_SPECTRUM_48K(
            "ZX Spectrum 48K", 3500,
            256, 192, 48,                 // bitmap width, height, horizontal border pixels
            0x4000, 0x5800,               // bitmap and attribute addresses
            7, 16, 0xFF,                  // reset border, FLASH period, undriven bus byte
            new double[]{0.34, 0.66, 3.56, 3.70}, // EAR/MIC voltages: 00, 01, 10, 11
            0x4000, 0x7FFF,               // contended RAM range, inclusive
            64, 56,                       // lines before and after visible bitmap
            224, 32,                      // raster line and interrupt pulse T-states
            14335, 3,                     // first contention T-state and floating-bus offset
            new int[]{6, 5, 4, 3, 2, 1, 0, 0}
    );

    /** Model used when the bus configuration omits its model setting. */
    public static final ModelProfile DEFAULT = ZX_SPECTRUM_48K;

    /** Model name used in device descriptions. */
    public final String displayName;
    /** Nominal CPU clock in kHz; the CPU context supplies the live clock. */
    public final int cpuFrequencyKHz;
    /** Visible bitmap dimensions in pixels. */
    public final int screenWidthPixels;
    /** Visible bitmap height in pixels. */
    public final int screenHeightPixels;
    /** Number of 8-pixel attribute cells per row. */
    public final int attributesWidth;
    /** Number of 8-pixel attribute rows. */
    public final int attributeHeight;
    /** Displayed border width on each horizontal side, in pixels. */
    public final int borderWidthPixels;
    /** CPU address of the bitmap in the current unbanked model. */
    public final int screenMemoryBase;
    /** CPU address of the attribute area in the current unbanked model. */
    public final int attributeMemoryBase;
    /** Border color selected on reset. */
    public final int defaultBorderColor;
    /** Number of frames between FLASH attribute swaps. */
    public final int flashSwapFrameCount;
    /** Value returned by an undriven bus and during interrupt acknowledge. */
    public final int undrivenBusDataByte;
    private final double[] beeperLevels;

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

    ModelProfile(
            String displayName,
            int cpuFrequencyKHz,
            int screenWidthPixels,
            int screenHeightPixels,
            int borderWidthPixels,
            int screenMemoryBase,
            int attributeMemoryBase,
            int defaultBorderColor,
            int flashSwapFrameCount,
            int undrivenBusDataByte,
            double[] beeperVoltages,
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
        this.displayName = displayName;
        this.cpuFrequencyKHz = cpuFrequencyKHz;
        this.screenWidthPixels = screenWidthPixels;
        this.screenHeightPixels = screenHeightPixels;
        this.attributesWidth = screenWidthPixels / 8;
        this.attributeHeight = screenHeightPixels / 8;
        this.borderWidthPixels = borderWidthPixels;
        this.screenMemoryBase = screenMemoryBase;
        this.attributeMemoryBase = attributeMemoryBase;
        this.defaultBorderColor = defaultBorderColor;
        this.flashSwapFrameCount = flashSwapFrameCount;
        this.undrivenBusDataByte = undrivenBusDataByte;
        this.beeperLevels = normalizeBeeperVoltages(beeperVoltages);
        this.contendedMemoryStart = contendedMemoryStart;
        this.contendedMemoryEnd = contendedMemoryEnd;
        this.preScreenLines = preScreenLines;
        this.postScreenLines = postScreenLines;
        this.displayLineTstates = displayLineTstates;
        this.interruptTstates = interruptTstates;
        this.firstContendedTstate = firstContendedTstate;
        this.floatingBusSampleOffsetTstates = floatingBusSampleOffsetTstates;
        this.contentionPattern = contentionPattern.clone();
        this.frameLineCount = preScreenLines + screenHeightPixels + postScreenLines;
        this.displayFrameTstates = frameLineCount * displayLineTstates;
        this.screenFetchCycles = attributesWidth * 4;
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
     * <li>contended odd ports apply up to four successive per-T-state delays</li>
     * <li>contended even ports apply one delay before T1 and one before T2</li>
     * <li>non-contended even ports still incur one delayed slot before T2</li>
     * <li>non-contended odd ports do not stall</li>
     * </ul>
     */
    public int portContentionDelay(long frameCycle, int portAddress) {
        long cycle = frameCycle;
        int delay = 0;
        boolean contendedHighByte = isContendedMemoryAddress(portAddress);

        if (contendedHighByte) {
            int currentDelay = contentionDelayAt(cycle);
            delay += currentDelay;
            cycle += currentDelay;
        }

        cycle++; // T1

        if (isUlaPort(portAddress)) {
            return delay + contentionDelayAt(cycle);
        }

        if (!contendedHighByte) {
            return 0;
        }

        for (int i = 0; i < 3; i++) {
            int currentDelay = contentionDelayAt(cycle);
            delay += currentDelay;
            cycle += currentDelay + 1;
        }
        return delay;
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

        for (int line = 0; line < screenHeightPixels; line++) {
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
        return screenMemoryBase + lineOffset + column;
    }

    /**
     * Returns the Spectrum attribute memory address for a pixel row and attribute column.
     */
    public int attributeAddressAt(int line, int column) {
        int attributeOffset = ((line >>> 3) << 5) | column;
        return attributeMemoryBase + attributeOffset;
    }

    /**
     * Convenience wrapper that accepts an 8-pixel character row instead of a pixel row.
     */
    public int attributeAddressAtRow(int row, int column) {
        return attributeAddressAt(row << 3, column);
    }

    /** Returns whether the ULA handles this I/O address. */
    public boolean isUlaPort(int portAddress) {
        return (portAddress & 1) == 0;
    }

    /** Preserves the current keyboard read decoding, including port aliases. */
    public boolean isKeyboardReadPort(int portAddress) {
        return (portAddress & 0xFE) == 0xFE;
    }

    /** Idle ULA read value with the EAR input bit cleared. */
    public byte ulaIdleReadData() {
        return (byte) (undrivenBusDataByte & ~0x40);
    }

    public int borderColorFrom(byte data) {
        return data & 7;
    }

    public boolean earOutputFrom(byte data) {
        return (data & 0x10) != 0;
    }

    public boolean micOutputFrom(byte data) {
        return (data & 0x08) == 0;
    }

    public byte earInputFrom(byte tapeData) {
        return (byte) ((tapeData & 1) << 6);
    }

    /** Hardware beeper level centered and scaled to [-1, 1], independent of host PCM format. */
    public double beeperLevel(boolean earOn, boolean micOn) {
        return beeperLevels[(earOn ? 2 : 0) | (micOn ? 1 : 0)];
    }

    private static double[] normalizeBeeperVoltages(double[] voltages) {
        double min = voltages[0];
        double max = voltages[voltages.length - 1];
        double mid = (min + max) / 2.0;
        double halfRange = (max - min) / 2.0;
        double[] levels = new double[voltages.length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = (voltages[i] - mid) / halfRange;
        }
        return levels;
    }
}
