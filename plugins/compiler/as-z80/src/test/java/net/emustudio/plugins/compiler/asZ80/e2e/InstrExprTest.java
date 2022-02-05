package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

public class InstrExprTest extends AbstractCompilerTest {

    @Test
    public void testRST() throws Exception {
        compile(
            "JP EXAMPLE\n" +
                "RST 00H\n" +
                "EXAMPLE:\n" +
                "ld A,01H"
        );

        assertProgram(
            0xC3, 0x04, 0x00,  0xC7, 0x3E, 0x01
        );
    }
}
