/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.plugins.device.zxspectrum.bus.api.ModelProfile;
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
    public void borderChangesLatchInEightPixelCellsWithinOneLine() {
        ModelProfile profile = ModelProfile.DEFAULT;
        ULA ula = createNiceMock(ULA.class);
        replay(ula);
        DisplayCanvas canvas = new DisplayCanvas(ula, profile, new KeyboardCanvas(profile, 0));
        List<BufferedImage> frames = new ArrayList<>();
        canvas.setFrameListener(frames::add);
        canvas.drawBorderCycles(0, profile.displayFrameTstates, 0);
        long lineStart = 16L * profile.displayLineTstates - profile.borderWidthPixels / 2;
        canvas.drawBorderCycles(lineStart, 2, 2); // color register changes before latch
        canvas.drawBorderCycles(lineStart + 2, 1, 2); // latch first cell: red
        canvas.drawBorderCycles(lineStart + 3, 3, 1); // blue waits for next latch
        canvas.drawBorderCycles(lineStart + 6, 1, 1); // latch next cell: blue
        canvas.frameReady();
        BufferedImage frame = frames.get(0);
        for (int x = 0; x < 8; x++) {
            assertEquals(0xD80000, frame.getRGB(x, 16) & 0xFFFFFF);
            assertEquals(0x0000D8, frame.getRGB(x + 8, 16) & 0xFFFFFF);
        }
        assertEquals(0, frame.getRGB(16, 16) & 0xFFFFFF);
        assertEquals(0, frame.getRGB(0, 15) & 0xFFFFFF);
        assertEquals(0, frame.getRGB(0, 17) & 0xFFFFFF);
    }

    @Test
    public void timedBorderWrapsToNextLineWithoutPaintingPaper() {
        ModelProfile profile = ModelProfile.DEFAULT;
        ULA ula = createNiceMock(ULA.class);
        replay(ula);
        DisplayCanvas canvas = new DisplayCanvas(ula, profile, new KeyboardCanvas(profile, 0));
        List<BufferedImage> frames = new ArrayList<>();
        canvas.setFrameListener(frames::add);
        canvas.drawBorderCycles(0, profile.displayFrameTstates, 0);
        canvas.drawBorderCycles(profile.preScreenLines * (long) profile.displayLineTstates
                - profile.borderWidthPixels / 2, profile.displayLineTstates, 2);
        canvas.frameReady();
        BufferedImage frame = frames.get(0);
        int y = profile.preScreenLines;
        assertEquals(0xD80000, frame.getRGB(0, y) & 0xFFFFFF);
        assertEquals(0xD80000, frame.getRGB(frame.getWidth() - 1, y) & 0xFFFFFF);
        assertEquals(0, frame.getRGB(profile.borderWidthPixels, y) & 0xFFFFFF);
        assertEquals(0, frame.getRGB(0, y + 1) & 0xFFFFFF);
        canvas.drawBorderCycles(profile.displayFrameTstates - 24, 24, 1);
        canvas.frameReady();
        assertEquals(0x0000D8, frames.get(1).getRGB(0, 0) & 0xFFFFFF);
        assertEquals(0, frames.get(0).getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    public void capturesNativeCompletedFramesWithoutDependingOnPaintOrResize() {
        for (ModelProfile profile : ModelProfile.values()) {
            AtomicInteger border = new AtomicInteger(1);
            ULA ula = createNiceMock(ULA.class);
            expect(ula.getBorderColor()).andStubAnswer(border::get);
            replay(ula);
            DisplayCanvas canvas = new DisplayCanvas(ula, profile, new KeyboardCanvas(profile, 0));
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
            assertEquals((profile.frameLineCount + 1) & ~1, frames.get(0).getHeight());
            assertEquals(frames.get(0).getWidth(), frames.get(1).getWidth());
            assertEquals(0x0000D8, frames.get(0).getRGB(0, 0) & 0xFFFFFF);
            assertEquals(0xD80000, frames.get(1).getRGB(0, 0) & 0xFFFFFF);
            canvas.setFrameListener(null);
            canvas.frameReady();
            assertEquals(2, frames.size());
        }
    }
}
