/*
 * CommandLineFactory.java
 * 
 * Copyright (C) 2012 vbmacher
 * 
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for parsing command lines.
 * 
 * @author vbmacher
 */
class CommandLineFactory {
    private final static Logger logger = LoggerFactory.getLogger(CommandLineFactory.class);

    public static class CommandLine {
        private String inputFileName = null;
        private String outputFileName = null;
        private String configName = null;
        private boolean auto = false;
        private boolean help = false;
        private boolean noGUI = false;
        
        /**
         * Create instance of the CommandLine class.
         * 
         * @param inputFileName input source file name. If not used, null should be here.
         * @param outputFileName output file name. If not used, null should be here.
         * @param configName name of virtual computer. If not used, null should be here.
         * @param auto whether to perform automatization
         * @param classToHash name of the class to create hash of
         * @param help whether to display help message
         * @param noGUI whether to not use GUI during automatization
         */
        public CommandLine(String inputFileName, String outputFileName, String configName, boolean auto,
                boolean help, boolean noGUI) {
            this.inputFileName = inputFileName;
            this.outputFileName = outputFileName;
            this.auto = auto;
            this.help = help;
            this.noGUI = noGUI;
            this.configName = configName;
        }

        public boolean helpWanted() {
            return help;
        }

        public boolean noGUIWanted() {
            return noGUI;
        }

        public String getConfigName() {
            return configName;
        }
        
        public void setConfigName(String configName) {
            this.configName = configName;
        }

        public String getInputFileName() {
            return inputFileName;
        }

        public String getOutputFileName() {
            return outputFileName;
        }

        public boolean autoWanted() {
            return auto;
        }
        
    }
    
    /**
     * Parses command line and creates CommandLine object.
     * 
     * @param args command-line arguments (raw)
     * @return CommandLine object
     * @throws InvalidCommandLineException if the command line is not parseable, or not valid.
     */
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
                if (arg.equals("--CONFIG")) {
                    i++;
                    // what configuration to load
                    if (configName != null) {
                        logger.warn(new StringBuilder().append("Config file already defined, ignoring this one: ")
                                .append(args[i]).toString());
                    } else {
                        configName = args[i];
                        logger.info(new StringBuilder().append("Loading virtual computer: ").append(configName).toString());
                    }
                } else if (arg.equals("--INPUT")) {
                    i++;
                    // what input file take to compiler
                    if (inputFileName != null) {
                        logger.warn(new StringBuilder().append("Input file already defined, ignoring this one: ")
                                .append(args[i]).toString());
                    } else {
                        inputFileName = args[i];
                        logger.info(new StringBuilder().append("Input file name for compiler: ").append(inputFileName)
                                .toString());
                    }
                } else if (arg.equals("--OUTPUT")) {
                    i++;
                    // what output file take for emuStudio messages during
                    // automation process. This option has a meaning
                    // only if the "-auto" option is set too.
                    if (outputFileName != null) {
                        logger.warn(new StringBuilder().append("Output file already defined, ignoring this one: ")
                                .append(args[i]).toString());
                    } else {
                        outputFileName = args[i];
                        logger.info(new StringBuilder().append("Output file name: ").append(outputFileName).toString());
                    }
                } else if (arg.equals("--AUTO")) {
                    auto = true;
                    logger.info("Turning automatization on.");
                } else if (arg.equals("--HELP")) {
                    help = true;
                } else if (arg.equals("--NOGUI")) {
                    logger.info("Setting GUI off.");
                    noGUI = true;
                } else {
                    throw new InvalidCommandLineException(new StringBuilder().append("Unknown command line argument (")
                            .append(arg).append(")").toString());
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InvalidCommandLineException(new StringBuilder().append("[").append(arg)
                        .append("] Missing argument").toString());
            }
        }
        return new CommandLine(inputFileName, outputFileName, configName, auto, help, noGUI);
    }
}
