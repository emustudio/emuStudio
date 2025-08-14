/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class Position {
    public int track;
    public int sector;

    Position(int track, int sector) {
        this.track = track;
        this.sector = sector;
    }

    void next(int sectorsPerTrack) {
        sector++;
        if (sector >= sectorsPerTrack) {
            track++;
            sector = 0;
        }
    }

    @Override
    public String toString() {
        return "T=" + track + " S=" + sector;
    }

    public String toString(CpmFormat format) {
        return "T=" + track + " S=" + format.sectorSkewTable[sector];
    }
}
