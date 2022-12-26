package net.emustudio.plugins.compiler.as8080.e2e;

import org.junit.Test;

public class InstrExprTest extends AbstractCompilerTest {

    @Test
    public void testRST() throws Exception {
        compile(
                "JMP EXAMPLE\n" +
                        "RST 00H\n" +
                        "EXAMPLE:\n" +
                        "MVI A,01H"
        );

        assertProgram(
                0xC3, 0x04, 0x00, 0xC7, 0x3E, 0x01
        );
    }

    @Test
    public void testCPI() throws Exception {
        compile("cpi '9' + 1");
        assertProgram(0xFE, '9' + 1);
    }

    @Test
    public void testForwardCall() throws Exception {
        compile("call sample\n" +
                "label: db 'hello'\n" +
                "sample: hlt");
        assertProgram(0xCD, 0x08, 0x00, 'h', 'e', 'l', 'l', 'o', 0x76);
    }
}
