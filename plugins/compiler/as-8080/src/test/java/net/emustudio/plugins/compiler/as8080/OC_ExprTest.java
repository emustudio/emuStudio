package net.emustudio.plugins.compiler.as8080;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class OC_ExprTest extends AbstractCompilerTest {

    @Test
    public void testRST() throws Exception {
        compile(
            "JMP EXAMPLE\n" +
                "RST 00H\n" +
                "EXAMPLE:\n" +
                "MVI A,01H"
        );

        assertProgram(
            0xC3, 0x04, 0x00,  0xC7, 0x3E, 0x01
        );
    }
}
