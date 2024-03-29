/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
import net.emustudio.application.gui.ExtendedDialogs;
import net.emustudio.application.gui.GuiDialogsImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.ConfigFiles;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.application.cmdline.Utils.*;
import static net.emustudio.application.gui.GuiUtils.setupLookAndFeel;
import static net.emustudio.application.settings.ConfigFiles.listConfigurationNames;

@SuppressWarnings("unused")
@CommandLine.Command(
        name = "emuStudio",
        mixinStandardHelpOptions = true,
        versionProvider = Runner.VersionProvider.class,
        description = "Universal emulation platform and framework",
        scope = CommandLine.ScopeType.INHERIT,
        subcommands = {AutomationCommand.class}
)
public class Runner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    // if a command is being run, default behavior is: do nothing
    private static boolean runsSomeCommand;
    @CommandLine.ArgGroup(heading = "Virtual computer%n")
    public Exclusive exclusive;
    @CommandLine.Option(names = {"-i", "--input-file"}, description = "input file name (source code)", paramLabel = "FILE")
    public Path inputFile;
    @CommandLine.Option(names = {"-cl", "--computers-list"}, description = "list all existing virtual computers")
    private boolean listConfigs;

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
            try {
                AtomicInteger index = new AtomicInteger();
                System.out.println("Index\tComputer name");
                listConfigurationNames().forEach(name -> System.out.println(index.getAndIncrement() + "\t" + name));
            } catch (IOException e) {
                LOGGER.error("Could not list configuration names", e);
                System.exit(1);
            }
            System.exit(0);
        }

        if (!runsSomeCommand) {
            try {
                AppSettings appConfig = loadAppSettings(true, false);
                setupLookAndFeel(appConfig);
                ExtendedDialogs dialogs = new GuiDialogsImpl();
                Optional<ComputerConfig> computerConfigOpt = (exclusive != null) ?
                        exclusive.loadConfiguration() :
                        loadComputerConfigFromGui(appConfig, dialogs);

                if (computerConfigOpt.isEmpty()) {
                    System.err.println("Virtual computer must be selected!");
                    System.exit(1);
                }

                ComputerConfig computerConfig = computerConfigOpt.get();

                LoadingDialog splash = showSplashScreen();
                ContextPoolImpl contextPool = new ContextPoolImpl(EMUSTUDIO_ID);
                DebugTableModelImpl debugTableModel = new DebugTableModelImpl();
                VirtualComputer computer = loadComputer(
                        appConfig, computerConfig, dialogs, contextPool, debugTableModel
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

    public static class Exclusive {
        @CommandLine.Option(names = {"-cn", "--computer-name"},
                description = "virtual computer name (see -cl for options)",
                paramLabel = "NAME"
        )
        public String configName;

        @CommandLine.Option(
                names = {"-cf", "--computer-file"},
                description = "virtual computer configuration file",
                paramLabel = "FILE"
        )
        public Path configFile;

        @CommandLine.Option(
                names = {"-ci", "--computer-index"},
                description = "virtual computer index (see -cl for options)",
                paramLabel = "INDEX"
        )
        public Integer configIndex;

        public Optional<ComputerConfig> loadConfiguration() throws IOException {
            if (configName != null) {
                Optional<ComputerConfig> optConfig = ConfigFiles.loadConfiguration(configName);
                if (optConfig.isPresent()) {
                    return optConfig;
                }
                System.err.println("Non-existing virtual computer: " + configFile);
                System.exit(1);
            }
            if (configFile != null) {
                return ConfigFiles.loadConfiguration(configFile);
            }
            if (configIndex != null) {
                return ConfigFiles.loadConfiguration(configIndex);
            }
            return Optional.empty();
        }
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{Resources.getVersion()};
        }
    }
}
