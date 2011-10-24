/*
 * CpuElement.java
 *
 * Created on 3.7.2008, 8:28:14
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
import java.util.Properties;

/**
 *
 * @author vbmacher
 */
public class CpuElement extends Element {
    private final static Color BACK_COLOR = new Color(0xffeeee);

    /**
     * This constructor creates the CpuElement instance. 
     *
     * @param pluginName file name of this plug-in, without '.jar' extension.
     * @param settings settings of this element from virtual configuration
     */
    public CpuElement(String pluginName, Properties settings) throws Exception {
        super(pluginName, settings, BACK_COLOR);
    }

    /**
     * Creates an instance of the CpuElement.
     *
     * @param e1 Point representing the element location
     * @param text description text (name of the plug-in)
     */
    public CpuElement(String pluginName, Point location) {
        super(pluginName, location, BACK_COLOR);
    }

    /**
     * Get the plug-in type string.
     *
     * @return "CPU" string
     */
    @Override
    protected String getPluginType() {
        return "CPU";
    }
    
}

