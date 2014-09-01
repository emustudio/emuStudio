/*
 * KISS, YAGNI, DRY
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.drawing;

import java.awt.Color;
import java.awt.Point;
import java.util.Properties;

/**
 * This class represents a compiler element. It is used in the abstract schema
 * editor. It corresponds to a compiler object that will be used in the emulated
 * computer.
 *
 */
public class CompilerElement extends Element {
    private final static Color BACK_COLOR = new Color(0xeeefff);

    public CompilerElement(String pluginName, Properties settings, Schema schema) throws NumberFormatException {
        super(pluginName, settings, BACK_COLOR, schema);
    }

    public CompilerElement(String pluginName, Point location, Schema schema) {
        super(pluginName, location, BACK_COLOR, schema);
    }

    @Override
    protected String getPluginType() {
        return "Compiler";
    }

}
