/*
 * CpuElement.java
 *
 * Created on 3.7.2008, 8:28:14
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.architecture.drawing;

import java.awt.Color;
import java.awt.Point;

/**
 *
 * @author vbmacher
 */
public class CpuElement extends Element {

    public CpuElement(int x, int y, String text) {
        super(Color.WHITE, text, x, y);
        //super(new Color(0x6D8471), text, x, y);
    }

    public CpuElement(Point e1, String text) {
        this((int)e1.getX(),(int)e1.getY(), text);
    }

    @Override
    protected String getPluginType() {
        return "CPU";
    }
    
}
