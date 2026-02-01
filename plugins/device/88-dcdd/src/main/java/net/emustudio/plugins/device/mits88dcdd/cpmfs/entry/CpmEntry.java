/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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
