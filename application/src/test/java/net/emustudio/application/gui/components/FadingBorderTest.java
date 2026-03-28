/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.application.gui.AbstractSwingTest;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class FadingBorderTest extends AbstractSwingTest {

    @Test
    public void paintBorderReusesCachedImageUntilSizeChanges() throws Exception {
        FadingBorder border = new FadingBorder(2, new Color(10, 20, 30));
        JPanel component = onEdt(() -> new JPanel());

        paint(border, component, 6, 6);
        BufferedImage firstCache = cachedBorder(border);
        int fadedPixel = firstCache.getRGB(1, 1);

        assertNotNull(firstCache);
        assertEquals(0, firstCache.getRGB(0, 0) >>> 24);
        assertTrue((fadedPixel >>> 24) > 0);
        assertEquals(10, (fadedPixel >> 16) & 0xFF);
        assertEquals(20, (fadedPixel >> 8) & 0xFF);
        assertEquals(30, fadedPixel & 0xFF);

        paint(border, component, 6, 6);
        assertSame(firstCache, cachedBorder(border));

        paint(border, component, 7, 7);
        assertNotSame(firstCache, cachedBorder(border));
    }

    @Test
    public void insetsAndOpacityReflectBorderConfiguration() {
        FadingBorder border = new FadingBorder(4, Color.WHITE);

        JPanel component = onEdt(() -> new JPanel());

        assertEquals(new Insets(4, 4, 4, 4), border.getBorderInsets(component));
        assertFalse(border.isBorderOpaque());
    }

    private static void paint(FadingBorder border, JComponent component, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            border.paintBorder(component, graphics, 0, 0, width, height);
        } finally {
            graphics.dispose();
        }
    }

    private static BufferedImage cachedBorder(FadingBorder border) throws Exception {
        Field field = FadingBorder.class.getDeclaredField("cachedBorder");
        field.setAccessible(true);
        return (BufferedImage) field.get(border);
    }
}
