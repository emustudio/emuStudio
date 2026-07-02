/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import net.emustudio.application.Resources;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.framework.DialogsGui;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;

import static net.emustudio.application.cmdline.Utils.*;
import static net.emustudio.application.settings.ConfigFiles.listConfigurationNames;

@SuppressWarnings("unused")
@CommandLine.Command(
        name = "emuStudio",
        mixinStandardHelpOptions = true,
        versionProvider = Runner.VersionProvider.class,
        description = "Universal emulation platform and framework",
        subcommands = {AutomationCommand.class}
)
public class Runner implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    private static final Set<String> PARENT_OPTIONS_WITH_VALUE = new HashSet<>(Arrays.asList(
            "-i", "--input-file",
            "-cn", "--computer-name",
            "-cf", "--computer-file",
            "-ci", "--computer-index"
    ));
    private static final Set<String> PARENT_FLAGS = new HashSet<>(Arrays.asList(
            "-cl", "--computers-list"
    ));

    @CommandLine.ArgGroup(heading = "Virtual computer%n")
    public Exclusive exclusive;

    @CommandLine.Option(names = {"-i", "--input-file"}, description = "input file name (source code)", paramLabel = "FILE")
    public Path inputFile;

    @CommandLine.Option(names = {"-cl", "--computers-list"}, description = "list all existing virtual computers")
    private boolean listConfigs;

    public static void main(String[] args) {
        int exitCode = executeArgs(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int executeArgs(String... args) {
        return createCommandLine(new Runner()).execute(normalizeArgs(args));
    }

    static CommandLine createCommandLine(Runner runner) {
        CommandLine cmdline = new CommandLine(runner);
        cmdline.registerConverter(Path.class, Path::of);
        return cmdline;
    }

    static String[] normalizeArgs(String... args) {
        int subcommandIndex = findSubcommandIndex(args);
        if (subcommandIndex < 0) {
            return args;
        }

        List<String> parentArgs = new ArrayList<>(Arrays.asList(args).subList(0, subcommandIndex));
        List<String> subcommandArgs = new ArrayList<>();
        String subcommand = args[subcommandIndex];

        for (int i = subcommandIndex + 1; i < args.length; i++) {
            String arg = args[i];
            if (isParentOptionWithValue(arg)) {
                parentArgs.add(arg);
                if (!arg.contains("=") && i + 1 < args.length) {
                    parentArgs.add(args[++i]);
                }
            } else if (isParentFlag(arg)) {
                parentArgs.add(arg);
            } else {
                subcommandArgs.add(arg);
            }
        }

        List<String> normalized = new ArrayList<>(parentArgs.size() + subcommandArgs.size() + 1);
        normalized.addAll(parentArgs);
        normalized.add(subcommand);
        normalized.addAll(subcommandArgs);
        return normalized.toArray(new String[0]);
    }

    private static int findSubcommandIndex(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("automation".equals(args[i]) || "auto".equals(args[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isParentOptionWithValue(String arg) {
        return PARENT_OPTIONS_WITH_VALUE.contains(arg)
                || PARENT_OPTIONS_WITH_VALUE.stream().anyMatch(option -> arg.startsWith(option + "="));
    }

    private static boolean isParentFlag(String arg) {
        return PARENT_FLAGS.contains(arg);
    }

    @Override
    public Integer call() {
        if (listConfigs) {
            try {
                AtomicInteger index = new AtomicInteger();
                System.out.println("Index\tComputer name");
                listConfigurationNames().forEach(name -> System.out.println(index.getAndIncrement() + "\t" + name));
            } catch (IOException e) {
                LOGGER.error("Could not list configuration names", e);
                return 1;
            }
            return 0;
        }

        try {
            AppSettings appConfig = loadAppSettings(true, false);
            EmuStudioGui gui = new EmuStudioGui();
            gui.initialize(appConfig);
            DialogsGui dialogs = new DialogsGui(gui);
            Optional<ComputerConfig> computerConfigOpt = (exclusive != null) ?
                    exclusive.loadConfiguration() :
                    loadComputerConfigFromGui(appConfig, dialogs, gui);

            if (computerConfigOpt.isEmpty()) {
                System.err.println("Virtual computer must be selected!");
                return 1;
            }

            ComputerConfig computerConfig = computerConfigOpt.get();

            LoadingDialog splash = showSplashScreen(gui);
            ContextPoolImpl contextPool = new ContextPoolImpl(EMUSTUDIO_ID);
            DebugTableModelImpl debugTableModel = new DebugTableModelImpl();
            VirtualComputer computer = loadComputer(
                    appConfig, computerConfig, dialogs, contextPool, debugTableModel, gui
            );
            splash.dispose();

            showMainWindow(
                    computer, appConfig, dialogs, debugTableModel, contextPool, inputFile, gui
            );
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected error", e);
            return 1;
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
                throw new IOException("Non-existing virtual computer: " + configName);
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
