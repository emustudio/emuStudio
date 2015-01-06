package net.sf.emustudio.intel8080.impl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstructionsControlTest extends InstructionsTest {

    @Test
    public void testEI_DI() throws Exception {
        resetProgram(0xFB, 0xF3);

        cpu.step();
        assertTrue(cpu.getEngine().INTE);

        cpu.step();
        assertFalse(cpu.getEngine().INTE);
    }
}
