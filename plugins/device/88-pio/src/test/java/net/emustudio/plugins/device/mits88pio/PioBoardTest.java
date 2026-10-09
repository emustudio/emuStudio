/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.plugins.device.mits88pio.api.PioContext;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class PioBoardTest {
    @Test public void ddrAndDataShareOddAddressesAndMixPins() {
        PioBoard board = new PioBoard(0xA0, 2);
        for (int channel = 0; channel < 4; channel++) {
            int control = 0xA0 + channel * 2;
            board.write(control + 1, (byte) 0xF0);
            assertEquals(0xF0, board.read(control + 1) & 255);
            board.write(control, (byte) 4);
            board.setInputPins(channel, 0xAB);
            board.write(control + 1, (byte) 0x56);
            assertEquals(0x5B, board.read(0x1200 | (control + 1)) & 255);
        }
        assertEquals(255, board.read(0xA8) & 255);
    }

    @Test public void edgeFlagsLatchWithoutIrqEnableAndClearOnlyOnDataRead() {
        PioBoard board = new PioBoard(0xA0, 2);
        AtomicInteger interrupts = new AtomicInteger();
        board.setInterruptHandler(interrupts::incrementAndGet);
        board.write(0xA0, (byte) 4);
        board.setControlLine1(0, false);
        board.setControlLine2(0, false);
        assertEquals(0xC4, board.read(0xA0) & 255);
        assertEquals(0, interrupts.get());
        board.write(0xA0, (byte) 0x0D);
        assertEquals(1, interrupts.get());
        board.write(0xA1, (byte) 12);
        assertEquals(0xCD, board.read(0xA0) & 255);
        board.read(0xA1);
        assertEquals(0x0D, board.read(0xA0) & 255);
        board.setControlLine1(0, true);
        board.setControlLine1(0, false);
        assertEquals(2, interrupts.get());
    }

    @Test public void risingEdgesAndSecondPiaHaveIndependentFlags() {
        PioBoard board = new PioBoard(0xA0, 2);
        board.write(0xA6, (byte) 0x16);
        board.setControlLine1(3, false);
        board.setControlLine2(3, false);
        assertEquals(0x16, board.read(0xA6) & 255);
        board.setControlLine1(3, true);
        board.setControlLine2(3, true);
        assertEquals(0xD6, board.read(0xA6) & 255);
        assertEquals(0, board.read(0xA0));
        board.read(0xA7);
        assertEquals(0x16, board.read(0xA6) & 255);
    }

    @Test public void pulseUsesAReadsAndBWritesAndHandshakeWaitsForC1() {
        PioBoard board = new PioBoard(0xA0, 1);
        List<String> edges = new ArrayList<>();
        board.attachPeripheral(new PioContext.Peripheral() {
            @Override public void outputChanged(int channel, int data, int mask) { }
            @Override public void controlOutputChanged(int channel, boolean high) { edges.add(channel + ":" + high); }
            @Override public void reset() { }
        });
        board.write(0xA0, (byte) 0x2C);
        board.write(0xA2, (byte) 0x24);
        board.write(0xA1, (byte) 42);
        board.read(0xA3);
        assertTrue(edges.isEmpty());
        board.read(0xA1);
        assertEquals(List.of("0:false", "0:true"), edges);
        edges.clear();
        board.write(0xA3, (byte) 1);
        board.write(0xA3, (byte) 2);
        assertEquals(List.of("1:false"), edges);
        board.setControlLine1(1, false);
        assertEquals(List.of("1:false", "1:true"), edges);
        board.write(0xA3, (byte) 3);
        assertEquals("1:false", edges.get(2));
    }

    @Test public void staticC2OutputIgnoresExternalC2AndDoesNotStrobe() {
        PioBoard board = new PioBoard(0xA0, 1);
        List<Boolean> edges = new ArrayList<>();
        board.attachPeripheral(new PioContext.Peripheral() {
            @Override public void outputChanged(int c, int d, int m) { }
            @Override public void controlOutputChanged(int c, boolean high) { edges.add(high); }
            @Override public void reset() { }
        });
        board.write(0xA0, (byte) 0x34);
        board.setControlLine2(0, false);
        board.read(0xA1);
        assertEquals(List.of(false), edges);
        assertEquals(0x34, board.read(0xA0) & 255);
        board.write(0xA0, (byte) 0x3C);
        assertEquals(List.of(false, true), edges);
    }

    @Test public void resetClearsDdrOutputsAndFlags() {
        PioBoard board = new PioBoard(0xA0, 4);
        board.write(0xAF, (byte) 255);
        board.write(0xAE, (byte) 5);
        board.setControlLine1(7, false);
        board.reset();
        assertEquals(0, board.read(0xAE));
        assertEquals(0, board.read(0xAF));
        assertEquals(16, board.getPortCount());
    }
}
