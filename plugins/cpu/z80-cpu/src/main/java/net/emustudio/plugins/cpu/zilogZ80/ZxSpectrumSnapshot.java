/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import java.util.Arrays;

final class ZxSpectrumSnapshot {
    static final int RAM_START = 0x4000;
    static final int RAM_SIZE = 0xC000;

    final int[] registers = new int[8];
    final int[] alternateRegisters = new int[8];
    final Byte[] ram = new Byte[RAM_SIZE];

    int flags;
    int alternateFlags;
    int programCounter;
    int stackPointer;
    int indexX;
    int indexY;
    int interrupt;
    int refresh;
    int interruptMode;
    int border;
    boolean iff1;
    boolean iff2;

    ZxSpectrumSnapshot() {
        Arrays.fill(ram, (byte) 0);
    }
}
