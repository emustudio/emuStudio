package net.emustudio.plugins.compiler.asZ80;

import org.junit.Test;

public class OC_ExprTest extends AbstractCompilerTest {

    @Test
    public void testRST() throws Exception {
        compile(
            "JP EXAMPLE\n" +
                "RST 0\n" +
                "EXAMPLE:\n" +
                "LD A, 1\n"
        );

        assertProgram(
            0xC3, 0x04, 0x00, 0xC7, 0x3E, 0x01
        );
    }
}
