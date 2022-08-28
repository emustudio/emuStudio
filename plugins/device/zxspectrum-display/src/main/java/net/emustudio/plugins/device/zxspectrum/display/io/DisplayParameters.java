package net.emustudio.plugins.device.zxspectrum.display.io;

import net.jcip.annotations.Immutable;

@Immutable
class DisplayParameters {
    final int maxWidth;
    final int maxHeight;

    DisplayParameters(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
}
