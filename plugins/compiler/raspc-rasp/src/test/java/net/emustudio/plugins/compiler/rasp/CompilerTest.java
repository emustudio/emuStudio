package net.emustudio.plugins.compiler.rasp;

import net.emustudio.plugins.compiler.rasp.ast.Program.RASPMemoryCellImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class CompilerTest extends AbstractCompilerTest {

    @Test
    public void testJmpInstruction() throws Exception {
        compile(
            "org 2\n" +
                "START: jmp HERE\n" +
                "jmp START\n" +
                "HERE: halt"
        );

        assertProgram(
            null,
            null,
            new RASPMemoryCellImpl(true, 15, 2),
            new RASPMemoryCellImpl(false, 6, 3),
            new RASPMemoryCellImpl(true, 15, 4),
            new RASPMemoryCellImpl(false, 2, 5),
            new RASPMemoryCellImpl(true, 18, 6)
        );
    }

    @Test(expected = Exception.class)
    public void testNonExistingLabel() throws Exception {
        compile("jmp hahaha");
    }

    @Test(expected = Exception.class)
    public void testAlreadyDefinedLabel() throws Exception {
        compile("label:\nlabel:");
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }
}
