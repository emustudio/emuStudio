/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.zilogZ80.assembler.impl;

import emulib.annotations.PluginType;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.Message;
import emulib.runtime.ContextPool;

public class Main {

    public static void main(String... args) {
        System.out.println(CompilerImpl.class.getAnnotation(PluginType.class).title());

        String inputFile;
        String outputFile = null;

        int i;
        for (i = 0; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if ((arg.equals("--output") || arg.equals("-o")) && ((i+1) < args.length)) {
                outputFile = args[++i];
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return;
            } else if (arg.equals("--version") || arg.equals("-v")) {
                System.out.println(new CompilerImpl(0L, new ContextPool()).getVersion());
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
        if (outputFile == null) {
            int index = inputFile.lastIndexOf('.');
            if (index != -1) {
                outputFile = inputFile.substring(0, index);
            } else {
                outputFile = inputFile;
            }
            outputFile += ".hex";
        }

        CompilerImpl compiler = new CompilerImpl(0L, new ContextPool());
        compiler.addCompilerListener(new Compiler.CompilerListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onMessage(Message message) {
                System.err.println(message.toString());
            }

            @Override
            public void onFinish(int errorCode) {
                System.err.println("Compilation finished (error code: " + errorCode + ")");

            }
        });
        try {
            compiler.compile(inputFile, outputFile);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Syntax: java -jar as-z80.jar [-o outputFile] inputFile\nOptions:");
        System.out.println("\t--output, -o\tfile: name of the output file");
        System.out.println("\t--version, -v\t: print version");
        System.out.println("\t--help, -h\t: this help");
    }


}
