/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import net.emustudio.application.Resources;
import net.emustudio.application.settings.ComputerConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Optional;

import static org.junit.Assert.*;
import picocli.CommandLine;

public class RunnerTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void versionProviderReturnsApplicationVersion() throws Exception {
        assertArrayEquals(new String[]{Resources.getVersion()}, new Runner.VersionProvider().getVersion());
    }

    @Test
    public void exclusiveLoadsConfigurationFromExplicitFileAndReturnsEmptyWithoutOptions() throws Exception {
        File configFile = temporaryFolder.newFile("computer.toml");
        configFile.delete();
        try (ComputerConfig ignored = ComputerConfig.create("Test computer", configFile.toPath())) {
            Runner.Exclusive exclusive = new Runner.Exclusive();
            exclusive.configFile = configFile.toPath();

            Optional<ComputerConfig> loaded = exclusive.loadConfiguration();
            assertTrue(loaded.isPresent());
            assertEquals("Test computer", loaded.orElseThrow(AssertionError::new).getName());
            loaded.orElseThrow(AssertionError::new).close();
        }

        Runner.Exclusive empty = new Runner.Exclusive();
        assertFalse(empty.loadConfiguration().isPresent());
    }

    @Test
    public void executeArgsReturnsSuccessForHelp() {
        assertEquals(0, Runner.executeArgs("--help"));
        assertEquals(0, Runner.executeArgs("automation", "--help"));
    }

    @Test
    public void commandLineParsesInheritedOptionsBeforeAndAfterSubcommand() {
        CommandLine cmdline = Runner.createCommandLine(new Runner());

        CommandLine.ParseResult before = cmdline.parseArgs(Runner.normalizeArgs("-cn", "BrainDuck", "automation", "--no-gui"));
        assertTrue(before.hasSubcommand());
        assertEquals("BrainDuck", ((Runner) before.commandSpec().userObject()).exclusive.configName);

        CommandLine.ParseResult after = cmdline.parseArgs(Runner.normalizeArgs("automation", "--no-gui", "-cn", "BrainDuck"));
        assertTrue(after.hasSubcommand());
        assertEquals("BrainDuck", ((Runner) after.commandSpec().userObject()).exclusive.configName);
    }
}
