/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Pio8255Test {
    private Pio8255 pio;

    @Before
    public void setUp() {
        pio = new Pio8255();
    }

    @Test
    public void testResetConfiguresAllPortsAsInput() {
        assertEquals(0x9B, pio.getControl());
        assertTrue(pio.isInput(0));
        assertTrue(pio.isInput(1));
        assertTrue(pio.isInput(2));
    }

    @Test
    public void testModeSetControlsDirectionsAndOutputLatches() {
        pio.write(Pio8255.CONTROL_PORT, (byte) 0x80);
        pio.write(Pio8255.PORT_A, (byte) 0xA5);
        pio.write(Pio8255.PORT_B, (byte) 0x5A);

        assertFalse(pio.isInput(0));
        assertFalse(pio.isInput(1));
        assertEquals(0xA5, pio.read(Pio8255.PORT_A) & 0xFF);
        assertEquals(0x5A, pio.getChannel(1).readData() & 0xFF);
    }

    @Test
    public void testInputValuesComeFromDeviceContexts() {
        pio.getChannel(0).writeData((byte) 0x42);
        pio.getChannel(1).writeData((byte) 0x24);

        assertEquals(0x42, pio.read(Pio8255.PORT_A) & 0xFF);
        assertEquals(0x24, pio.read(Pio8255.PORT_B) & 0xFF);
    }

    @Test
    public void testPortCMixesInputAndOutputNibbles() {
        pio.write(Pio8255.CONTROL_PORT, (byte) 0x88); // upper input, lower output
        pio.getChannel(2).writeData((byte) 0xA0);
        pio.write(Pio8255.PORT_C, (byte) 0x0B);

        assertEquals(0xAB, pio.read(Pio8255.PORT_C) & 0xFF);
    }

    @Test
    public void testBitSetResetChangesPortCLatch() {
        pio.write(Pio8255.CONTROL_PORT, (byte) 0x80);
        pio.write(Pio8255.CONTROL_PORT, (byte) 0x07); // set bit 3
        assertEquals(0x08, pio.read(Pio8255.PORT_C) & 0xFF);
        pio.write(Pio8255.CONTROL_PORT, (byte) 0x06); // reset bit 3
        assertEquals(0, pio.read(Pio8255.PORT_C) & 0xFF);
    }

    @Test
    public void testUnsupportedModesAreNormalizedToModeZero() {
        pio.write(Pio8255.CONTROL_PORT, (byte) 0xE2);
        assertEquals(0x82, pio.getControl());
    }
}
