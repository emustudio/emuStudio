package net.emustudio.plugins.device.zxspectrum.bus.api;

/**
 * Constants related to the ZX Spectrum's display and timing parameters.
 */
public class ZxParameters {
    /**
     * The ZX Spectrum's bitmap memory (display) height in pixels.
     */
    public static final int SCREEN_HEIGHT_PIXELS = 192;

    /**
     * The ZX Spectrum's bitmap memory (display) width in pixels.
     */
    public static final int SCREEN_WIDTH_PIXELS = 256;

    /**
     * The ZX Spectrum's attribute memory width (number of columns = number of bytes).
     * <p/>
     * Attributes (color, style) of pixel bitmap are shared per 8x8 pixel blocks. That means that each 8x8 block has the
     * same attribute. Hence, attribute memory can be much shorter than bitmap memory:
     * - width = 1 byte per 8 pixels gives 256 / 8  = 32 bytes (or attribute columns)
     * - height = 8 bitmap rows belongs to the same attribute; that gives 192 / 8 = 24 attribute rows
     */
    public static final int ATTRIBUTES_WIDTH = SCREEN_WIDTH_PIXELS / 8;

    /**
     * The ZX Spectrum's attribute memory height (number of rows).
     * <p/>
     * Attributes (color, style) of pixel bitmap are shared per 8x8 pixel blocks. That means that each 8x8 block has the
     * same attribute. Hence, attribute memory can be much shorter than bitmap memory:
     * - width = 1 byte per 8 pixels gives 256 / 8  = 32 bytes (or attribute columns)
     * - height = 8 bitmap rows belongs to the same attribute; that gives 192 / 8 = 24 attribute rows
     */
    public static final int ATTRIBUTE_HEIGHT = SCREEN_HEIGHT_PIXELS / 8;

    // https://en.wikipedia.org/wiki/ZX_Spectrum_graphic_modes writes:
    //
    // The original ZX Spectrum does not conform strictly to the PAL standard frame rate of 50 Hz. ZX Spectrum outputs
    // one video line in exactly 224 CPU clock cycles, where the CPU clock rate equals 3.5 MHz. This exactly matches
    // the PAL standard 64 μs line time. However, the ZX Spectrum produces only 312 lines to form one display frame,
    // while the 625-line PAL standard recommends 312.5 lines. As a consequence, the frame rate of the ZX Spectrum
    // is approximately 50.08 frames per second.
    //
    // The 312 lines are divided into three sections:
    // - 64 lines (14336 T-states) for the upper border and vertical retrace
    // - 192 lines (43008 T-states) for the visible display
    // - 56 lines (12544 T-states) for the lower border and vertical retrace

    /**
     * Number of lines for the upper border and vertical retrace
     */
    public static final int PRE_SCREEN_LINES = 64;

    /**
     * Number of lines for the lower border and vertical retrace
     */
    public static final int POST_SCREEN_LINES = 56;

    /**
     * Number of CPU cycles (T-states) to output one line in the ZX Spectrum's display. On 3.5 MHZ CPU it takes 64 μs.
     */
    public static final int DISPLAY_LINE_TSTATES = 224;

    /**
     * Number of CPU cycles (T-states) to refresh one frame in the ZX Spectrum's display.
     * It's 69888 t-states, which means that the frame rate is 3.5MHz/69888=50.08 Hz.
     */
    public static final int DISPLAY_FRAME_TSTATES = (PRE_SCREEN_LINES + SCREEN_HEIGHT_PIXELS + POST_SCREEN_LINES) * DISPLAY_LINE_TSTATES;

    /**
     * Number of CPU cycles (T-states) that the ULA holds the INT signal low at each frame boundary.
     */
    public static final int INTERRUPT_TSTATES = 32;
}
