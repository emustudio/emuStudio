/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.bus.api.TimingProfile;
import net.emustudio.plugins.device.zxspectrum.ula.ULA;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DisplayCanvasTest {
    @Test
    public void capturesNativeCompletedFramesWithoutDependingOnPaintOrResize() {
        for (TimingProfile timing : TimingProfile.values()) {
            AtomicInteger border = new AtomicInteger(1);
            ULA ula = createNiceMock(ULA.class);
            expect(ula.getBorderColor()).andStubAnswer(border::get);
            replay(ula);
            DisplayCanvas canvas = new DisplayCanvas(ula, timing, new KeyboardCanvas(timing, 0));
            List<BufferedImage> frames = new ArrayList<>();
            canvas.setFrameListener(frames::add);
            canvas.drawNextLine(0);
            canvas.frameReady();
            border.set(2);
            canvas.drawNextLine(0);
            canvas.setSize(1000, 1000);
            canvas.frameReady();
            BufferedImage painted = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
            var graphics = painted.createGraphics();
            canvas.paint(graphics);
            graphics.dispose();
            assertEquals(2, frames.size());
            assertEquals(canvas.getRecordingSize().width, frames.get(0).getWidth());
            assertEquals((timing.frameLineCount + 1) & ~1, frames.get(0).getHeight());
            assertEquals(frames.get(0).getWidth(), frames.get(1).getWidth());
            assertEquals(0x0000D8, frames.get(0).getRGB(0, 0) & 0xFFFFFF);
            assertEquals(0xD80000, frames.get(1).getRGB(0, 0) & 0xFFFFFF);
            canvas.setFrameListener(null);
            canvas.frameReady();
            assertEquals(2, frames.size());
        }
    }
}
