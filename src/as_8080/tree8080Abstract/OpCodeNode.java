/*
 * OpCode.java
 *
 * Created on Sobota, 2007, september 22, 9:15
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

package as_8080.tree8080Abstract;

/**
 *
 * @author vbmacher
 */
public abstract class OpCodeNode extends CodeNode {
    protected String mnemo;
    
    protected String getRegMnemo(byte reg) {
        switch (reg) {
            case 0: return "b";
            case 1: return "c";
            case 2: return "d";
            case 3: return "e";
            case 4: return "h";
            case 5: return "l";
            case 6: return "m";
            case 7: return "a";
        }
        return "";
    }
    
    protected String getRegpairMnemo(byte regpair, boolean psw) {
        switch (regpair) {
            case 0: return "bc";
            case 1: return "de";
            case 2: return "hl";
            case 3: if (psw == false) return "sp";
                    else return "psw";
        }
        return "";
    }
    
    /** Creates a new instance of OpCode */
    public OpCodeNode(String mnemo, int line, int column) {
        super(line,column);
        this.mnemo = mnemo;
    }
    
}
