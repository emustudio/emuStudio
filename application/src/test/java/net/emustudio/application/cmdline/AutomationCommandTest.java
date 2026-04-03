/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import net.emustudio.application.emulation.Automation;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AutomationCommandTest {

    private CommandLine cmdline;

    @Before
    public void setUp() {
        cmdline = new CommandLine(new Runner());
        cmdline.registerConverter(Path.class, Path::of);
        cmdline.getCommandSpec().parser().collectErrors(true);
    }

    private AutomationCommand parseAutomationCommand(String... args) {
        CommandLine.ParseResult result = cmdline.parseArgs(args);
        assertTrue("Expected automation subcommand", result.hasSubcommand());
        return (AutomationCommand) result.subcommand().commandSpec().userObject();
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object obj, String fieldName, Class<T> type) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

    // --- Default values ---

    @Test
    public void defaultWaitForFinishMillisIsDontWait() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation");
        int waitForFinishMillis = getField(command, "waitForFinishMillis", Integer.class);
        assertEquals(Automation.DONT_WAIT, waitForFinishMillis);
    }

    @Test
    public void defaultGuiIsTrue() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation");
        boolean gui = getField(command, "gui", Boolean.class);
        assertTrue(gui);
    }

    @Test
    public void defaultProgramLocationIsMinusOne() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation");
        String programLocation = getField(command, "programLocation", String.class);
        assertEquals("-1", programLocation);
    }

    // --- Wait max option ---

    @Test
    public void parsesShortWaitMaxOption() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "-w", "5000");
        int waitForFinishMillis = getField(command, "waitForFinishMillis", Integer.class);
        assertEquals(5000, waitForFinishMillis);
    }

    @Test
    public void parsesLongWaitMaxOption() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "--waitmax", "3000");
        int waitForFinishMillis = getField(command, "waitForFinishMillis", Integer.class);
        assertEquals(3000, waitForFinishMillis);
    }

    @Test
    public void parsesZeroWaitMax() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "-w", "0");
        int waitForFinishMillis = getField(command, "waitForFinishMillis", Integer.class);
        assertEquals(0, waitForFinishMillis);
    }

    // --- GUI option ---

    @Test
    public void parsesNoGuiOption() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "--no-gui");
        boolean gui = getField(command, "gui", Boolean.class);
        assertFalse(gui);
    }

    @Test
    public void parsesExplicitGuiOption() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "--gui");
        boolean gui = getField(command, "gui", Boolean.class);
        assertTrue(gui);
    }

    // --- Program location option ---

    @Test
    public void parsesShortProgramLocationOption() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "-p", "0x100");
        String programLocation = getField(command, "programLocation", String.class);
        assertEquals("0x100", programLocation);
    }

    @Test
    public void parsesLongProgramLocationOption() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "--program-location", "0xFF");
        String programLocation = getField(command, "programLocation", String.class);
        assertEquals("0xFF", programLocation);
    }

    @Test
    public void parsesDecimalProgramLocation() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation", "-p", "256");
        String programLocation = getField(command, "programLocation", String.class);
        assertEquals("256", programLocation);
    }

    // --- Command alias ---

    @Test
    public void autoAliasIsRecognized() {
        CommandLine.ParseResult result = cmdline.parseArgs("auto");
        assertTrue("Expected 'auto' to be recognized as automation subcommand", result.hasSubcommand());
        Object subcommand = result.subcommand().commandSpec().userObject();
        assertNotNull(subcommand);
        assertTrue(subcommand instanceof AutomationCommand);
    }

    @Test
    public void automationCommandNameIsRecognized() {
        CommandLine.ParseResult result = cmdline.parseArgs("automation");
        assertTrue(result.hasSubcommand());
        assertTrue(result.subcommand().commandSpec().userObject() instanceof AutomationCommand);
    }

    // --- Parent command ---

    @Test
    public void parentCommandIsSetToRunner() throws Exception {
        AutomationCommand command = parseAutomationCommand("automation");
        Runner runner = getField(command, "runner", Runner.class);
        assertNotNull(runner);
    }

    // --- Combined options ---

    @Test
    public void parsesMultipleOptions() throws Exception {
        AutomationCommand command = parseAutomationCommand(
                "automation", "-w", "10000", "--no-gui", "-p", "0x200"
        );
        assertEquals(10000, (int) getField(command, "waitForFinishMillis", Integer.class));
        assertFalse(getField(command, "gui", Boolean.class));
        assertEquals("0x200", getField(command, "programLocation", String.class));
    }

    @Test
    public void parsesInputFileFromParentCommand() throws Exception {
        CommandLine.ParseResult result = cmdline.parseArgs("-i", "test.asm", "automation", "--no-gui");
        assertTrue(result.hasSubcommand());

        Runner runner = (Runner) result.commandSpec().userObject();
        assertNotNull(runner.inputFile);
        assertEquals(Path.of("test.asm"), runner.inputFile);

        AutomationCommand command = (AutomationCommand) result.subcommand().commandSpec().userObject();
        assertFalse(getField(command, "gui", Boolean.class));
    }

    @Test
    public void parsesComputerNameWithAutomation() throws Exception {
        CommandLine.ParseResult result = cmdline.parseArgs("-cn", "myComputer", "automation", "-w", "5000");
        assertTrue(result.hasSubcommand());

        Runner runner = (Runner) result.commandSpec().userObject();
        assertNotNull(runner.exclusive);
        assertEquals("myComputer", runner.exclusive.configName);

        AutomationCommand command = (AutomationCommand) result.subcommand().commandSpec().userObject();
        assertEquals(5000, (int) getField(command, "waitForFinishMillis", Integer.class));
    }

    @Test
    public void parsesComputerFileWithAutoAlias() throws Exception {
        CommandLine.ParseResult result = cmdline.parseArgs("-cf", "/path/to/config.toml", "auto");
        assertTrue(result.hasSubcommand());

        Runner runner = (Runner) result.commandSpec().userObject();
        assertNotNull(runner.exclusive);
        assertEquals(Path.of("/path/to/config.toml"), runner.exclusive.configFile);
    }

    @Test
    public void autoAliasAcceptsAllOptions() throws Exception {
        AutomationCommand command = parseAutomationCommand(
                "auto", "-w", "7500", "--no-gui", "--program-location", "42"
        );
        assertEquals(7500, (int) getField(command, "waitForFinishMillis", Integer.class));
        assertFalse(getField(command, "gui", Boolean.class));
        assertEquals("42", getField(command, "programLocation", String.class));
    }
}
