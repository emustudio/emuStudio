package net.emustudio.plugins.compiler.asZ80.e2e;

import org.junit.Test;

public class JumpTest extends AbstractCompilerTest {

    @Test
    public void testDjnzWithLabelOverOneByte() {
        String program = "ORG 990\nDL: LD IX,0\n" +
                "DJNZ DL\n";
        compile(program);
        assertProgramWithStart(
                990, 0xDD, 0x21, 0x00, 0x00, 0x10, -6
        );
    }


    @Test
    public void testJrWithLabelOverOneByte() {
        String program = "ORG 990\nDL: LD IX,0\n" +
                "jr DL\n";
        compile(program);
        assertProgramWithStart(
                990, 0xDD, 0x21, 0x00, 0x00, 0x18, -6
        );
    }
}
