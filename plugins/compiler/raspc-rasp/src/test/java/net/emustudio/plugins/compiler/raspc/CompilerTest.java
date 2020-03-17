package net.emustudio.plugins.compiler.raspc;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class CompilerTest extends AbstractCompilerTest {

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }
}
