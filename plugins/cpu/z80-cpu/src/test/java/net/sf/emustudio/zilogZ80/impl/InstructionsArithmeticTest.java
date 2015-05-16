/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.zilogZ80.impl;

import org.junit.Test;

public class InstructionsArithmeticTest extends InstructionsTest {
    
    public void testADD_A__IX_plus_d() {
        
    }
    
    public void testADD_A__IY_plus_d() {
        
    }
    
    public void testADC_A__IX_plus_d() {
        
    }
    
    public void testADC_A__IY_plus_d() {
        
    }
    
    public void testSUB_A__IX_plus_d() {
        
    }
    
    public void testSUB_A__IY_plus_d() {
        
    }

    public void testSBC_A__IX_plus_d() {
        
    }
    
    public void testSBC_A__IY_plus_d() {
        
    }
    
    public void testINC__IX() {
        
    }
    
    public void testINC__IY() {
        
    }

    public void testDEC__IX() {
        
    }
    
    public void testDEC__IY() {
        
    }
    
    public void testINC__IX_plus_d() {
        
    }

    public void testINC__IY_plus_d() {
        
    }

    public void testDEC__IX_plus_d() {
        
    }

    public void testDEC__IY_plus_d() {
        
    }
    
    public void testADD_IX__ss() {
        
    }
    
    public void testADD_IY__ss() {
        
    }
    
    @Test
    public void testADC_HL__ss() throws Exception {
        resetProgram(0xED, 0x4A, 0xED, 0x5A, 0xED, 0x6A, 0xED, 0x7A);

    }
    
    @Test
    public void testSBC_HL__ss() throws Exception {

    }

}
