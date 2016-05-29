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
package emustudio.drawing;

import java.awt.*;
import java.util.Properties;

public class CpuElement extends Element {
    private final static Color BACK_COLOR = new Color(0xffeeee);

    CpuElement(String pluginName, Properties settings, Schema schema) throws NumberFormatException {
        super(pluginName, settings, BACK_COLOR, schema);
    }

    public CpuElement(String pluginName, Point location, Schema schema) {
        super(pluginName, location, BACK_COLOR, schema);
    }

    @Override
    protected String getPluginType() {
        return "CPU";
    }

}

