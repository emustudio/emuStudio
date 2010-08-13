/*
 * CompilerElement.java
 *
 * KISS, YAGNI
 *
 *  Copyright (C) 2010 vbmacher
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

package emustudio.architecture.drawing;

import java.awt.Color;
import java.awt.Point;

/**
 * This class represents a compiler element. It is used in the abstract schema
 * editor. It corresponds to a compiler object that will be used in the emulated
 * computer.
 *
 * @author vbmacher
 */
public class CompilerElement extends Element {

    /**
     * Create new Compiler element object.
     *
     * @param x the X coordinate in pixels, in the schema
     * @param y the Y coordinate in pixels, in the schema
     * @param text name of the compiler
     */
    public CompilerElement(int x, int y, String text) {
        super(Color.CYAN, text, x, y);
    }

    /**
     * Create new Compiler element object.
     *
     * @param shapePoint the point where the compiler is located in the schema
     * @param newText name of the compiler
     */
    public CompilerElement(Point shapePoint, String newText) {
        this(shapePoint.x, shapePoint.y, newText);
    }

    /**
     * Get string representing the type of the plugin.
     *
     * @return "compiler" text
     */
    @Override
    protected String getPluginType() {
        return "Compiler";
    }

}
