/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DisplayPanelTest {
    @Test
    @SuppressWarnings("unchecked")
    public void capturedFramesKeepOverlayScaleAndIndependentPixels() {
        AtomicReference<Byte> value = new AtomicReference<>((byte) 0xFF);
        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        expect(memory.read(anyInt())).andAnswer(value::get).anyTimes();
        replay(memory);
        DisplayPanel panel = new DisplayPanel(memory, new SpaceInvadersHardware(), 2, true);
        BufferedImage first = panel.captureFrame(448, 512);
        assertEquals(new Color(255, 80, 80).getRGB(), first.getRGB(0, 0));
        assertEquals(Color.WHITE.getRGB(), first.getRGB(0, 200));
        assertEquals(new Color(80, 255, 80).getRGB(), first.getRGB(0, 511));
        value.set((byte) 0);
        panel.setSize(700, 800);
        BufferedImage second = panel.captureFrame(448, 512);
        assertEquals(448, second.getWidth());
        assertEquals(512, second.getHeight());
        assertEquals(Color.BLACK.getRGB(), second.getRGB(0, 0));
        assertEquals(new Color(255, 80, 80).getRGB(), first.getRGB(0, 0));
    }
}
