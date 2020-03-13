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
package net.emustudio.application.configuration;

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

    @Override
    public String toString() {
        return x + "," + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaPoint that = (SchemaPoint) o;
        return x == that.x &&  y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public static SchemaPoint parse(String value) {
        String[] xy = value.split(",");
        int x = Integer.decode(xy[0].trim());
        int y = Integer.decode(xy[1].trim());

        return new SchemaPoint(x, y);
    }

    public static SchemaPoint create(int x, int y) {
        return new SchemaPoint(x, y);
    }
}
