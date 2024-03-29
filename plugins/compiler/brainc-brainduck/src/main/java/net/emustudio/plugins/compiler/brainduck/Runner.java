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
package net.emustudio.plugins.compiler.brainduck;

import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.nio.file.Path;
import java.util.Optional;

public class Runner {

    public static void main(String... args) {
        String inputFile;
        String outputFile = null;

        int i;
        for (i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if ((arg.equals("--output") || arg.equals("-o")) && ((i + 1) < args.length)) {
                outputFile = args[++i];
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return;
            } else if (arg.equals("--version") || arg.equals("-v")) {
                System.out.println(new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE).getVersion());
                return;
            } else {
                break;
            }
        }
        if (i >= args.length) {
            System.err.println("Error: expected input file name");
            return;
        }
        inputFile = args[i];

        CompilerBrainduck compiler = new CompilerBrainduck(0L, ApplicationApi.UNAVAILABLE, PluginSettings.UNAVAILABLE);
        compiler.addCompilerListener(new CompilerListener() {

            @Override
            public void onStart() {
            }

            @Override
            public void onMessage(CompilerMessage message) {
                System.err.println(message.getFormattedMessage());
            }

            @Override
            public void onFinish() {
            }
        });

        try {
            compiler.compile(Path.of(inputFile), Optional.ofNullable(outputFile).map(Path::of));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Syntax: java -jar brainc-brainduck.jar [-o outputFile] inputFile\nOptions:");
        System.out.println("\t--output, -o\tfile: name of the output file");
        System.out.println("\t--version, -v\t: print version");
        System.out.println("\t--help, -h\t: this help");
    }
}
