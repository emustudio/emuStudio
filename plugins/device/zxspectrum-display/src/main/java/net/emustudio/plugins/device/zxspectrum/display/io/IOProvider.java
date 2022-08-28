package net.emustudio.plugins.device.zxspectrum.display.io;

import java.io.Closeable;

public interface IOProvider extends Closeable {
    int EOF = 0;

    void reset();
}
