/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Custom border that fades from inner color to outer transparent.
 */
public class FadingBorder implements Border {
    private final int thickness;
    private final Color color;
    private BufferedImage cachedBorder;
    private int cachedWidth;
    private int cachedHeight;

    public FadingBorder(int thickness, Color color) {
        this.thickness = thickness;
        this.color = color;
    }

    private void rebuildCache(int width, int height) {
        if (cachedBorder != null && cachedWidth == width && cachedHeight == height) {
            return;
        }
        cachedWidth = width;
        cachedHeight = height;
        cachedBorder = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int rgb = (r << 16) | (g << 8) | b;

        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
                int distFromRight = width - 1 - px;
                int distFromBottom = height - 1 - py;
                int minDist = Math.min(
                        Math.min(px, distFromRight),
                        Math.min(py, distFromBottom)
                );
                if (minDist < thickness) {
                    float alpha = (float) minDist / thickness;
                    int argb = ((int) (alpha * 255) << 24) | rgb;
                    cachedBorder.setRGB(px, py, argb);
                }
            }
        }
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        rebuildCache(width, height);
        g.drawImage(cachedBorder, x, y, null);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}

