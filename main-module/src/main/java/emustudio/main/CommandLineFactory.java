/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for parsing command lines.
 */
public class CommandLineFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(CommandLineFactory.class);

    public static class CommandLine {
        private String inputFileName = null;
        private String outputFileName = null;
        private String configName = null;
        private boolean auto = false;
        private boolean help = false;
        private boolean noGUI = false;

        CommandLine(String inputFileName, String outputFileName, String configName, boolean auto,
                    boolean help, boolean noGUI) {
            this.inputFileName = inputFileName;
            this.outputFileName = outputFileName;
            this.auto = auto;
            this.help = help;
            this.noGUI = noGUI;
            this.configName = configName;
        }

        boolean helpWanted() {
            return help;
        }

        public boolean noGUIWanted() {
            return noGUI;
        }

        String getConfigName() {
            return configName;
        }

        void setConfigName(String configName) {
            this.configName = configName;
        }

        String getInputFileName() {
            return inputFileName;
        }

        String getOutputFileName() {
            return outputFileName;
        }

        public boolean autoWanted() {
            return auto;
        }

    }

    static CommandLine parseCommandLine(String[] args) throws InvalidCommandLineException {
        String configName = null;
        String inputFileName = null;
        String outputFileName = null;
        boolean auto = false;
        boolean noGUI = false;
        boolean help = false;

        // process arguments
        int size = args.length;
        for (int i = 0; i < size; i++) {
            String arg = args[i].toUpperCase();
            try {
                switch (arg) {
                    case "--CONFIG":
                        i++;
                        // what configuration to load
                        if (configName != null) {
                            LOGGER.warn("Config file already defined, ignoring this one: " +
                                args[i]);
                        } else {
                            configName = args[i];
                            LOGGER.info("Loading virtual computer: " + configName);
                        }   break;
                    case "--INPUT":
                        i++;
                        // what input file take to compiler
                        if (inputFileName != null) {
                            LOGGER.warn("Input file already defined, ignoring this one: " +
                                args[i]);
                        } else {
                            inputFileName = args[i];
                            LOGGER.info("Input file name for compiler: " + inputFileName);
                        }   break;
                    case "--OUTPUT":
                        i++;
                        // what output file take for emuStudio messages during automatization process. This option has a
                        // meaning only if the "--auto" option is set.
                        if (outputFileName != null) {
                            LOGGER.warn("Output file already defined, ignoring this one: " +
                                args[i]);
                        } else {
                            outputFileName = args[i];
                            LOGGER.info("Output file name: " + outputFileName);
                        }   break;
                    case "--AUTO":
                        auto = true;
                        LOGGER.info("Turning automatization on.");
                        break;
                    case "--HELP":
                        help = true;
                        break;
                    case "--NOGUI":
                        LOGGER.info("Setting GUI off.");
                        noGUI = true;
                        break;
                    default:
                        throw new InvalidCommandLineException("Unknown command line argument (" +
                            arg + ")");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidCommandLineException("[" + arg +
                    "] Missing argument");
            }
        }
        return new CommandLine(inputFileName, outputFileName, configName, auto, help, noGUI);
    }
}
