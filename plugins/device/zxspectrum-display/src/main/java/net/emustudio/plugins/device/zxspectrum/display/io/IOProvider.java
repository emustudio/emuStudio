package net.emustudio.plugins.device.zxspectrum.display.io;

import java.io.Closeable;

public interface IOProvider extends Closeable {
    void reset();
}
