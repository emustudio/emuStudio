/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.adm3a.impl;

import net.jcip.annotations.Immutable;

@Immutable
class DisplayParameters {
    final int charHeight;
    final int charWidth;
    final int startY;
    final int maxWidth;
    final int maxHeight;

    DisplayParameters(int charHeight, int charWidth, int startY, int maxWidth, int maxHeight) {
        this.charHeight = charHeight;
        this.charWidth = charWidth;
        this.startY = startY;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
}
