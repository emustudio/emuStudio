/*
 * ColumnInfo.java
 *
 * Created on Piatok, 2007, október 26, 10:51
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package cpu_8080.impl;

import plugins.cpu.*;
/**
 *
 * @author vbmacher
 */
public class ColumnInfo implements IDebugColumn {
    private String name;
    private Class<?> type;
    private boolean editable;
    
    /** Creates a new instance of debugInteraction */
    public ColumnInfo(String name, Class<?> cl, boolean editable) {
        this.name = name;
        this.type = cl;
        this.editable = editable;
    }

    public Class<?> getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEditable() {
        return this.editable;
    }

}
