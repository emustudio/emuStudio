package net.emustudio.plugins.device.zxspectrum.display.io;

import net.jcip.annotations.Immutable;

@Immutable
class DisplayParameters {
    final int maxWidth;
    final int maxHeight;
    final int charWidth;

    DisplayParameters(int maxWidth, int maxHeight, int charWidth) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.charWidth = charWidth;
    }
}
