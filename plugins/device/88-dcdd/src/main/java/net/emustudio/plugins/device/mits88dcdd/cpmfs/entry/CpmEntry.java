package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import java.nio.ByteBuffer;

public interface CpmEntry {

    static byte getStatus(ByteBuffer entry) {
        try {
            return entry.get();
        } finally {
            entry.position(0);
        }
    }
}
