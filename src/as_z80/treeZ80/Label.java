/*
 * Label.java
 *
 * Created on Streda, 2007, október 10, 16:52
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

package as_z80.treeZ80;

/**
 *
 * @author vbmacher
 */
public class Label {
    private String name;
    private Integer address;
    
    private int line;
    private int column;
    
    /** Creates a new instance of Label */
    public Label(String name, int line, int column) {
        this.name = name;
        this.address = null;
        
        this.line = line;
        this.column = column;
    }
    
    public void setAddress(Integer address) { this.address = address; }
    public Integer getAddress() { return this.address; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    
    public String getName() { return name; }
}
