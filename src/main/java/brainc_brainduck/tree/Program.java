/**
 * Program.java
 * 
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2009-2012 Peter Jakubčo <pjakubco@gmail.com>
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
package brainc_brainduck.tree;

import java.util.ArrayList;
import emulib.plugins.compiler.HEXFileHandler;

public class Program {
    private ArrayList<Row> list; // zoznam všetkých inštrukcií

    public Program() { 
        list = new ArrayList<Row>();
    }
    
    public void addRow(Row node) {
    	if (node != null)
            list.add(node);
    }

    public int pass1(int addr_start) throws Exception {
        int curr_addr = addr_start;
        
        for (int i = 0; i < list.size(); i++)
      		curr_addr = list.get(i).pass1(curr_addr);
        return curr_addr;
    }
 
    public void pass2(HEXFileHandler hex) throws Exception {
        for (int i = 0; i < list.size(); i++)
            list.get(i).pass2(hex);
    }

}
