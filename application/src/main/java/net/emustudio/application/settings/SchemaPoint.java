/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import net.jcip.annotations.Immutable;

import java.util.Objects;

@Immutable
public class SchemaPoint {
    public final int x;
    public final int y;

    private SchemaPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static SchemaPoint parse(String value) {
        String[] xy = value.split(",");
        int x = Integer.decode(xy[0].trim());
        int y = Integer.decode(xy[1].trim());

        return new SchemaPoint(x, y);
    }

    public static SchemaPoint of(int x, int y) {
        return new SchemaPoint(x, y);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaPoint that = (SchemaPoint) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
