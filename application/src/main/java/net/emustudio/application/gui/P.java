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

package net.emustudio.application.gui;

import net.emustudio.application.configuration.SchemaPoint;

import java.awt.*;

public class P {
    public volatile double x;
    public volatile double y;

    private P(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public int ix() {
        return (int) x;
    }

    public int iy() {
        return (int) y;
    }

    public void move(P p) {
        this.x = p.x;
        this.y = p.y;
    }

    public P minus(P other) {
        return new P(x - other.x, y - other.y);
    }

    public P diff(int diffX, int diffY) {
        return new P(x + diffX, y + diffY);
    }

    public P copy() {
        return new P(x, y);
    }

    public boolean isInRectangle(Point leftTop, Point rightBottom) {
        return ((x >= leftTop.x) && (x <= rightBottom.x) && (y >= leftTop.y) && (y <= rightBottom.y));
    }


    public SchemaPoint toSchemaPoint() {
        return SchemaPoint.of(ix(), iy());
    }

    public static P of(double x, double y) {
        return new P(x, y);
    }

    public static P of(int x, int y) {
        return new P(x, y);
    }

    public static P of(SchemaPoint schemaPoint) {
        return new P(schemaPoint.x, schemaPoint.y);
    }

    public static P of(Point point) {
        return new P(point.getX(), point.getY());
    }
}
