/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import net.emustudio.application.settings.SchemaPoint;

import java.awt.*;

public class P {
    public volatile double x;
    public volatile double y;

    private P(double x, double y) {
        this.x = x;
        this.y = y;
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
}
