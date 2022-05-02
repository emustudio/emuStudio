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
            0x3E, 0x01, 0x28, 0x03, 0xC7, 0x76
        );
    }

    @Test
    public void testRelativeJumpCurrentAddress() {
        compile("halt\njr $"); // infinite loop
        assertProgram(0x76, 0x18, 0);
    }

    @Test
    public void testRelativeJumpExact() {
        compile("halt\ndjnz $+0x20"); // if it is complex expression, treat it like exact value
        assertProgram(0x76, 0x10, 0x21);
    }
}
