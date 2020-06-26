/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application;

import net.emustudio.application.emulation.Automation;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import java.nio.file.Path;
import java.util.Optional;

@SuppressWarnings("unused")
public class CommandLine {

    @Option(name = "-i", aliases = "-input", metaVar = "filename", usage = "use the source code given by the file name")
    private String inputFileName;

    @Option(name = "-c", aliases = "--config", metaVar = "filename", usage = "load configuration with file name")
    private String configFileName;

    @Option(name = "-a", aliases = "--auto", usage = "run the emulation automation")
    private boolean auto;

    @Option(name = "-w", aliases = "--waitmax", metaVar = "X", usage = "wait for emulation finish max X milliseconds", depends = "--auto")
    private int waitForFinishMillis = Automation.DONT_WAIT;

    @Option(name = "-h", aliases = "--help", help = true, usage = "output this message")
    private boolean help;

    @Option(name = "-n", aliases = "--nogui", usage = "try to not show GUI in automation", depends = "--auto")
    private boolean noGUI;

    @Option(name = "-s", aliases = "--programStart", usage = "set program start address", metaVar = "X")
    private String programStart = "0";

    public boolean isAuto() {
        return auto;
    }

    public Optional<Path> getConfigFileName() {
        return Optional.ofNullable(configFileName).map(Path::of);
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public boolean isNoGUI() {
        return noGUI;
    }

    public int getWaitForFinishMillis() {
        return waitForFinishMillis;
    }

    public int getProgramStart() {
        return RadixUtils.getInstance().parseRadix(programStart);
    }

    public static CommandLine parse(String[] args) throws CmdLineException {
        CommandLine commandLine = new CommandLine();

        CmdLineParser parser = new CmdLineParser(commandLine, ParserProperties.defaults().withUsageWidth(120));
        try {
            parser.parseArgument(args);

            if (commandLine.help) {
                parser.printUsage(System.err);
                System.exit(0);
            }

            return commandLine;
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            throw e;
        }
    }
}
