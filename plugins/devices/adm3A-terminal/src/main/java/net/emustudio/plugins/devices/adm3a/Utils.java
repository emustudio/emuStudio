/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.devices.adm3a;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

public class Utils {
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
}
