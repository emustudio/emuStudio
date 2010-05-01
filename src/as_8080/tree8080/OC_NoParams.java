/*
 * OpcodeWParams.java
 *
 * Created on Sobota, 2007, september 29, 14:57
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

package as_8080.tree8080;

import as_8080.impl.HEXFileHandler;
import as_8080.impl.compileEnv;
import as_8080.tree8080Abstract.OpCodeNode;
import plugins.compiler.IMessageReporter;

/**
 *
 * @author vbmacher
 */
public class OC_NoParams extends OpCodeNode {
    
    /** Creates a new instance of OpcodeWParamsNode */
    public OC_NoParams(String mnemo, int line, int column) {
        super(mnemo, line, column);
    }
    
    /// compile time ///

    public int getSize() { return 1; }
    public void pass1(IMessageReporter r) {
    }

    public int pass2(compileEnv parentEnv, int addr_start) throws Exception {
        return addr_start + 1;
    }

    public void pass4(HEXFileHandler hex) throws Exception {
        short opCode = 0; // nop

        if (mnemo.equals("stc")) opCode = 55;
        else if (mnemo.equals("cmc")) opCode = 63;
        else if (mnemo.equals("cma")) opCode = 47;
        else if (mnemo.equals("daa")) opCode = 39;
        else if (mnemo.equals("nop"));
        else if (mnemo.equals("rlc")) opCode = 7;
        else if (mnemo.equals("rrc")) opCode = 15;
        else if (mnemo.equals("ral")) opCode = 23;
        else if (mnemo.equals("rar")) opCode = 31;
        else if (mnemo.equals("xchg")) opCode = 235;
        else if (mnemo.equals("xthl")) opCode = 227;
        else if (mnemo.equals("sphl")) opCode = 249;
        else if (mnemo.equals("pchl")) opCode = 233;
        else if (mnemo.equals("ret")) opCode = 201;
        else if (mnemo.equals("rc")) opCode = 216;
        else if (mnemo.equals("rnc")) opCode = 208;
        else if (mnemo.equals("rz")) opCode = 200;
        else if (mnemo.equals("rnz")) opCode = 192;
        else if (mnemo.equals("rm")) opCode = 248;
        else if (mnemo.equals("rp")) opCode = 240;
        else if (mnemo.equals("rpe")) opCode = 232;
        else if (mnemo.equals("rpo")) opCode = 224;
        else if (mnemo.equals("ei")) opCode = 251;
        else if (mnemo.equals("di")) opCode = 243;
        else if (mnemo.equals("hlt")) opCode = 118;
        hex.putCode(String.format("%1$02X",opCode));
    }
    
}
