/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class PioUnitTest {
    @Test public void inputStrobeOverwritesLatchAndReadClearsReady() {
        PioUnit pio = new PioUnit(4);
        pio.writeData((byte) 0x42);
        pio.writeData((byte) 0xA5);
        assertEquals(2, pio.read(4));
        assertEquals(2, pio.read(4));
        assertEquals(0xA5, pio.read(0x1205) & 255);
        assertEquals(0, pio.read(4));
        assertEquals(0xA5, pio.read(5) & 255);
    }

    @Test public void outputAcknowledgementRequestsNextByte() {
        PioUnit pio = new PioUnit(4);
        pio.setOutputReady(true);
        assertEquals(1, pio.read(4));
        pio.write(5, (byte) 0x5A);
        assertEquals(0, pio.read(4));
        assertEquals(0x5A, pio.readData() & 255);
        assertEquals(1, pio.read(4));
    }

    @Test public void interruptsRequireEnabledReadyFlagAndRearmAfterAcknowledge() {
        PioUnit pio = new PioUnit(4);
        AtomicInteger interrupts = new AtomicInteger();
        pio.setInterruptHandler(interrupts::incrementAndGet);
        pio.writeData((byte) 1);
        assertEquals(0, interrupts.get());
        pio.write(4, (byte) 2);
        assertEquals(1, interrupts.get());
        pio.writeData((byte) 2);
        assertEquals(1, interrupts.get());
        pio.read(5);
        pio.writeData((byte) 3);
        assertEquals(2, interrupts.get());
        pio.reset();
        assertEquals(0, pio.read(4));
        pio.writeData((byte) 4);
        assertEquals(2, interrupts.get());
    }

    @Test(expected = IllegalArgumentException.class) public void baseMustBeEven() { new PioUnit(5); }
}
