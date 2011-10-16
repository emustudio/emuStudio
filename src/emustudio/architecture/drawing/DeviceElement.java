/*
 * DeviceElement.java
 *
 * Created on 4.7.2008, 8:11:24
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2011 Peter Jakubčo <pjakubco at gmail.com>
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
public class DeviceElement extends Element {

    /**
     * Creates an instance of the DeviceElement class. It needs to know the
     * location of the element and the description text.
     *
     * @param x X location of the element
     * @param y Y location of the element
     * @param detials description text
     */
    public DeviceElement(int x, int y, String detials) {
        super("device", detials, Color.WHITE, x, y);
        //super(new Color(0xFFFEFF), text, x, y);
    }

    /**
     * Creates an instance of the DeviceElement class. It needs to know the
     * location of the element and the description text.
     *
     * @param e1 Point representing the location of the new element
     * @param text description text
     */
    public DeviceElement(Point e1, String text) {
        this((int)e1.getX(),(int)e1.getY(), text);
    }

    @Override
    protected String getPluginType() {
        return "device";
    }

}
