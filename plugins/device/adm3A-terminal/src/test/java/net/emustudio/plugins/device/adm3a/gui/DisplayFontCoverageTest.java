/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DisplayFontCoverageTest {

    @Test
    public void testOriginalFontSupportsRepresentativeLatin1Characters() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path,
                DisplayFontCoverageTest.class,
                DisplayFont.FONT_ORIGINAL.fontSize
        );

        String sample = "ÀÄÇÑØßàäçñøÿ£©±¿´¹";
        assertEquals(-1, font.canDisplayUpTo(sample));
        FontRenderContext context = new FontRenderContext(null, true, true);
        for (char character : sample.toCharArray()) {
            assertTrue("Visible extension: " + (int) character,
                    !font.createGlyphVector(context, new char[]{character}).getGlyphOutline(0).getBounds2D().isEmpty());
        }
    }

    @Test
    public void testOriginalFontLeavesSpaceInsideEveryPrintableCharacterCell() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, DisplayFont.FONT_ORIGINAL.fontSize
        );
        FontRenderContext context = new FontRenderContext(null, true, false);
        float cellWidth = font.createGlyphVector(context, "W").getGlyphMetrics(0).getAdvance();
        assertEquals(9.0f, cellWidth, 0.0f);

        for (char character = ' '; character <= '\u00ff'; character++) {
            if (Character.isISOControl(character)) {
                continue;
            }
            GlyphVector glyph = font.createGlyphVector(context, new char[]{character});
            assertEquals("Cell width for " + (int) character, cellWidth, glyph.getGlyphMetrics(0).getAdvance(), 0.0f);
            Rectangle2D bounds = glyph.getGlyphOutline(0).getBounds2D();
            if (!bounds.isEmpty()) {
                assertTrue("Left bearing for " + (int) character, bounds.getMinX() > 0);
                assertTrue("Right bearing for " + (int) character, bounds.getMaxX() < cellWidth);
            }
        }
    }

    @Test
    public void testOriginalFontRendersAdjacentWideCharactersWithoutTouching() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, DisplayFont.FONT_ORIGINAL.fontSize
        );
        String sample = "MWMW0123456789COPYRIGHT[]_@";
        int cellWidth = 9;
        BufferedImage image = new BufferedImage(sample.length() * cellWidth, 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DisplayFont.FONT_ORIGINAL.textAntiAliasing);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, DisplayFont.FONT_ORIGINAL.antiAliasing);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, DisplayFont.FONT_ORIGINAL.fractionalMetrics);
            graphics.translate(-DisplayFont.FONT_ORIGINAL.xCursorOffset, 0);
            DisplayCanvas.drawCharacters(graphics, DisplayFont.FONT_ORIGINAL,
                    sample.toCharArray(), 0, sample.length(), 18);
        } finally {
            graphics.dispose();
        }

        for (int cell = 0; cell < sample.length(); cell++) {
            boolean visible = false;
            for (int y = 0; y < image.getHeight(); y++) {
                assertEquals("Gap before cell " + cell, 0, image.getRGB(cell * cellWidth, y) & 0xffffff);
                for (int x = cell * cellWidth; x < (cell + 1) * cellWidth; x++) {
                    visible |= (image.getRGB(x, y) & 0xffffff) != 0;
                }
            }
            assertTrue("Visible glyph in cell " + cell, visible);
        }
    }

    @Test
    public void testOriginalFontKeepsLastColumnAlignedAtDifferentDesktopScales() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, DisplayFont.FONT_ORIGINAL.fontSize
        );
        char[] sample = (" ".repeat(79) + "W").toCharArray();
        for (double scale : new double[]{1, 1.25, 1.5, 2}) {
            BufferedImage image = new BufferedImage((int) (725 * scale), (int) (24 * scale), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            int lastCell;
            int cellEnd;
            try {
                graphics.scale(scale, scale);
                graphics.setFont(font);
                graphics.setColor(Color.WHITE);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, DisplayFont.FONT_ORIGINAL.antiAliasing);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, DisplayFont.FONT_ORIGINAL.fractionalMetrics);
                int cellWidth = graphics.getFontMetrics().charWidth('W');
                lastCell = (int) Math.floor((DisplayFont.FONT_ORIGINAL.xCursorOffset + 79 * cellWidth) * scale);
                cellEnd = (int) Math.ceil((DisplayFont.FONT_ORIGINAL.xCursorOffset + 80 * cellWidth) * scale);
                DisplayCanvas.drawCharacters(graphics, DisplayFont.FONT_ORIGINAL, sample, 0, sample.length, 18);
            } finally {
                graphics.dispose();
            }
            boolean visible = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y) & 0xffffff;
                    if (x < lastCell || x >= cellEnd) {
                        assertEquals("Outside last cell at scale " + scale, 0, pixel);
                    } else {
                        visible |= pixel != 0;
                    }
                }
            }
            assertTrue("Visible last column at scale " + scale, visible);
        }
    }

    @Test
    public void testOriginalFontBrightensEdgesWithoutDistortingScanlines() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, DisplayFont.FONT_ORIGINAL.fontSize
        );
        char[] sample = "MWAa".toCharArray();
        BufferedImage image = new BufferedImage(40, 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        Shape outline;
        try {
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, DisplayFont.FONT_ORIGINAL.antiAliasing);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, DisplayFont.FONT_ORIGINAL.textAntiAliasing);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, DisplayFont.FONT_ORIGINAL.fractionalMetrics);
            outline = font.createGlyphVector(graphics.getFontRenderContext(), sample)
                    .getOutline(DisplayFont.FONT_ORIGINAL.xCursorOffset, 18);
            DisplayCanvas.drawCharacters(graphics, DisplayFont.FONT_ORIGINAL, sample, 0, sample.length, 18);
        } finally {
            graphics.dispose();
        }

        // Partially covered pixels should be brighter than raw area coverage, while the
        // fully lit strokes and empty gaps still follow the vector's original geometry.
        int brighterEdges = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int covered = 0;
                for (int sy = 0; sy < 16; sy++) {
                    for (int sx = 0; sx < 16; sx++) {
                        if (outline.contains(x + (sx + 0.5) / 16, y + (sy + 0.5) / 16)) {
                            covered++;
                        }
                    }
                }
                double brightness = (image.getRGB(x, y) & 0xff) / 255.0;
                double coverage = covered / 256.0;
                if (coverage == 0 || coverage == 1) {
                    assertEquals("Stroke or gap at " + x + "," + y, coverage, brightness, 0.1);
                } else if (coverage >= 0.25 && coverage <= 0.75) {
                    assertTrue("Brighter edge at " + x + "," + y, brightness > coverage + 0.1);
                    brighterEdges++;
                }
            }
        }
        assertTrue("Sample contains partially covered edges", brighterEdges > 0);
    }

    @Test
    public void testOriginalFontRendersContinuousVerticalStrokesAtDifferentDesktopScales() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, DisplayFont.FONT_ORIGINAL.fontSize
        );
        for (double scale : new double[]{1, 1.25, 1.5, 2}) {
            BufferedImage image = new BufferedImage((int) (16 * scale), (int) (24 * scale), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.scale(scale, scale);
                graphics.setFont(font);
                graphics.setColor(Color.WHITE);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, DisplayFont.FONT_ORIGINAL.antiAliasing);
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, DisplayFont.FONT_ORIGINAL.fractionalMetrics);
                DisplayCanvas.drawCharacters(graphics, DisplayFont.FONT_ORIGINAL, new char[]{'H'}, 0, 1, 18);
            } finally {
                graphics.dispose();
            }

            double fontScale = font.getSize2D() / 1680.0;
            int stemX = (int) Math.floor((DisplayFont.FONT_ORIGINAL.xCursorOffset + 1.5 * 137 * fontScale) * scale);
            int top = (int) Math.ceil((18 - 6.5 * 240 * fontScale) * scale);
            int bottom = (int) Math.floor((18 - 0.5 * 240 * fontScale) * scale);
            for (int y = top; y <= bottom; y++) {
                assertTrue("Continuous stem at scale " + scale + " row " + y,
                        (image.getRGB(stemX, y) & 0xff) >= 200);
            }
        }
    }

    @Test
    public void testOriginalFontMatchesAllCharacterGeneratorRows() throws Exception {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, 1680
        );
        FontRenderContext context = new FontRenderContext(null, true, true);
        int characters = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/adm3a-charset.txt")), StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                String[] values = line.split(" ");
                char character = (char) Integer.parseInt(values[0], 16);
                assertTrue("Character retained: " + values[0], font.canDisplay(character));
                characters++;
                GlyphVector glyph = font.createGlyphVector(context, new char[]{character});
                assertEquals("Cell width: " + values[0], 7 * 137, glyph.getGlyphMetrics(0).getAdvance(), 0.0f);
                Shape outline = glyph.getGlyphOutline(0);
                for (int row = 0; row < 8; row++) {
                    int mask = Integer.parseInt(values[row + 1], 16);
                    for (int column = 0; column < 5; column++) {
                        boolean lit = (mask & (1 << (4 - column))) != 0;
                        for (double position : new double[]{0.15, 0.5, 0.85}) {
                            assertEquals("Dot in " + values[0] + " row " + row + " column " + column,
                                    lit, outline.contains((column + 1 + position) * 137, -(7.5 - row) * 240));
                        }
                        if (row > 0 && lit) {
                            int previous = Integer.parseInt(values[row], 16);
                            for (int above = Math.max(0, column - 1); above <= Math.min(4, column + 1); above++) {
                                if ((previous & (1 << (4 - above))) != 0) {
                                    for (double position : new double[]{0.15, 0.5, 0.85}) {
                                        assertTrue("Joined dots in " + values[0] + " row " + row,
                                                outline.contains(((column + above) / 2.0 + 1 + position) * 137,
                                                        -(8 - row) * 240));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertEquals(95, characters);
    }

    @Test
    public void testOriginalFontUsesManualCharacterProportionsAndTwoBlankColumns() {
        Font font = GUI.loadFontResource(
                DisplayFont.FONT_ORIGINAL.path, DisplayFontCoverageTest.class, 1680
        );
        FontRenderContext context = new FontRenderContext(null, true, true);
        GlyphVector glyph = font.createGlyphVector(context, "W");
        Rectangle2D bounds = glyph.getGlyphOutline(0).getBounds2D();
        assertEquals(0.075 / 0.18, bounds.getWidth() / bounds.getHeight(), 0.0001);
        assertEquals(137, bounds.getMinX(), 0.0);
        assertEquals(137, glyph.getGlyphMetrics(0).getAdvance() - bounds.getMaxX(), 0.0);
    }
}
