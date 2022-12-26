/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.adm3a;

import net.emustudio.plugins.device.adm3a.gui.DisplayFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Utils {
    private final static Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static FontRenderContext DEFAULT_FRC;

    public static FontRenderContext getDefaultFrc() {
        if (DEFAULT_FRC == null) {
            AffineTransform tx;
            if (GraphicsEnvironment.isHeadless()) {
                tx = new AffineTransform();
            } else {
                tx = GraphicsEnvironment
                        .getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration()
                        .getDefaultTransform();
            }
            DEFAULT_FRC = new FontRenderContext(tx, false, false);
        }
        return DEFAULT_FRC;
    }

    public static Font loadFont(DisplayFont displayFont) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);

        try (InputStream fin = Utils.class.getResourceAsStream(displayFont.path)) {
            Font font = Font
                    .createFont(Font.TRUETYPE_FONT, Objects.requireNonNull(fin))
                    .deriveFont(Font.PLAIN, displayFont.fontSize)
                    .deriveFont(attrs);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            LOGGER.error("Could not load custom font, using default monospaced font", e);
            return new Font(Font.MONOSPACED, Font.PLAIN, 12);
        }
    }
}
