package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

public class InstrExprTest extends AbstractCompilerTest {

    @Test
    public void testJump() {
        compile(
                "JP EXAMPLE\n" +
                        "RST 00H\n" +
                        "EXAMPLE:\n" +
                        "ld A,01H"
        );

        assertProgram(
                0xC3, 0x04, 0x00, 0xC7, 0x3E, 0x01
        );
    }

    @Test
    public void testRelativeJumpLabel() {
        compile(
                "ld A,01H\n" +
                        "jr z, EXAMPLE\n" +
                        "RST 00H\n" +
                        "EXAMPLE:\n" +
                        "halt"
        );

        assertProgram(
                0x3E, 0x01, 0x28, 0x01, 0xC7, 0x76
        );
    }

    @Test
    public void testRelativeJumpCurrentAddress() {
        compile("halt\njr $"); // infinite loop
        assertProgram(0x76, 0x18, (byte) -2);
    }

    @Test
    public void testRelativeJumpExact() {
        compile("halt\ndjnz $+0x20"); // if it is complex expression, treat it like exact value
        assertProgram(0x76, 0x10, 0x21);
    }

    @Test
    public void testCP() {
        compile("cp '9' + 1");
        assertProgram(0xFE, '9' + 1);
    }

    @Test
    public void testForwardCall() {
        compile("call sample\n" +
                "label: db 'hello'\n" +
                "sample: halt");
        assertProgram(0xCD, 0x08, 0x00, 'h', 'e', 'l', 'l', 'o', 0x76);
    }
}
