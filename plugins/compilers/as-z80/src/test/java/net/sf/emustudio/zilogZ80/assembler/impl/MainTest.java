package net.sf.emustudio.zilogZ80.assembler.impl;

import org.junit.Test;

public class MainTest {
    @Test
    public void testCommandLinePrintHelp() throws Exception {
        Main.main("--help");
    }

    @Test
    public void testCommandLineNonexistantSourceFileDoesNotThrow() throws Exception {
        Main.main("slfjkdf");
    }

    @Test
    public void testCommandLinePrintVersion() throws Exception {
        Main.main("--version");
    }

}
