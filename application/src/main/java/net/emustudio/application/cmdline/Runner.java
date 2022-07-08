/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.cmdline;

import net.emustudio.application.Resources;
import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.application.gui.ExtendedDialogs;
import net.emustudio.application.gui.GuiDialogsImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Optional;

import static net.emustudio.application.cmdline.Utils.*;
import static net.emustudio.application.gui.GuiUtils.setupLookAndFeel;

@CommandLine.Command(
    name = "emuStudio",
    mixinStandardHelpOptions = true,
    versionProvider = Runner.VersionProvider.class,
    description = "Universal emulation platform and framework",
    subcommands = {AutomationCommand.class}
)
public class Runner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    @CommandLine.Option(names = {"-c", "--config"},
        description = "computer configuration name (file name without suffix)",
        required = true,
        paramLabel = "FILE"
    )
    public String config;

    @CommandLine.Option(names = {"-l", "--list-configs"}, description = "list all existing computer configurations")
    private boolean listConfigs;

    @CommandLine.Parameters(description = "input file name (source code)", paramLabel = "FILE")
    public Path inputFile;

    // if a command is being run, default behavior is: do nothing
    private static boolean runsSomeCommand;

    public static void main(String[] args) {
        CommandLine cmdline = new CommandLine(new Runner());
        cmdline.registerConverter(Path.class, Path::of);
        cmdline.getCommandSpec().parser().collectErrors(true);

        CommandLine.ParseResult result = cmdline.parseArgs(args);
        runsSomeCommand = result.hasSubcommand();

        try {
            cmdline.execute(args);
        } catch (Exception e) {
            result.errors().forEach(System.err::println);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        if (listConfigs) {
            // TODO
            System.exit(0);
        }

        if (!runsSomeCommand) {
            try (ApplicationConfig appConfig = loadApplicationConfig(true, false)) {
                setupLookAndFeel(appConfig);
                ExtendedDialogs dialogs = new GuiDialogsImpl();
                LoadingDialog splash = showSplashScreen();

                ComputerConfig computerConfig = loadComputerConfig(config, appConfig, dialogs, ConfigFiles.DEFAULT);

                ContextPoolImpl contextPool = new ContextPoolImpl(EMUSTUDIO_ID);
                DebugTableModelImpl debugTableModel = new DebugTableModelImpl();
                VirtualComputer computer = loadVirtualComputer(
                    appConfig, computerConfig, dialogs, contextPool, debugTableModel, ConfigFiles.DEFAULT
                );

                splash.dispose();

                showMainWindow(
                    computer, appConfig, dialogs, debugTableModel, contextPool, Optional.ofNullable(inputFile)
                );
            } catch (Exception e) {
                LOGGER.error("Unexpected error", e);
                System.exit(1);
            }
        }
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{Resources.getVersion()};
        }
    }
}
