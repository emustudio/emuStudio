package net.emustudio.plugins.compiler.raspc;

import net.emustudio.plugins.memory.rasp.InstructionImpl;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;
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
            new InstructionImpl(RASPInstruction.JMP),
            new NumberMemoryItem(6),
            new InstructionImpl(RASPInstruction.JMP),
            new NumberMemoryItem(2),
            new InstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        );
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
