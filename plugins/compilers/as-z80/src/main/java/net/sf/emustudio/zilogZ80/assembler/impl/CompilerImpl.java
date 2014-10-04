/*
 * Created on Piatok, 2007, august 10, 8:22
 *
 * Copyright (C) 2007-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.plugins.compiler.AbstractCompiler;
import emulib.plugins.compiler.LexicalAnalyzer;
import emulib.plugins.compiler.SourceFileExtension;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.HEXFileManager;
import java.io.FileReader;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.zilogZ80.assembler.tree.Program;

/**
 * Implementation class of the Zilog Z80 assembler.
 * @author Peter Jakubčo
 */
@PluginType(type=PLUGIN_TYPE.COMPILER,
        title="Zilog Z80 Assembler",
        copyright="\u00A9 Copyright 2007-2013, Peter Jakubčo",
        description="Custom version of the assembler. For syntax look at users manual.")
public class CompilerImpl extends AbstractCompiler {
    private LexerImpl lex;
    private ParserImpl par;
    private final SourceFileExtension[] suffixes;

    public CompilerImpl(Long pluginID) {
        super(pluginID);
        lex = new LexerImpl((Reader) null);
        par = new ParserImpl(lex);
        suffixes = new SourceFileExtension[1];
        suffixes[0] = new SourceFileExtension("asm", "Z80 assembler source");
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.zilogZ80.assembler.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void destroy() {
        lex = null;
        par = null;
    }

     /**
     * Compile the source code into HEXFileHadler
     *
     * @return HEXFileManager object
     */
    public HEXFileManager compile(Reader in) throws Exception {
        if (in == null) {
            return null;
        }

        Object parsedProgram;
        HEXFileManager hex = new HEXFileManager();

        notifyInfo(CompilerImpl.class.getAnnotation(PluginType.class).title() + ", version " + getVersion());
        notifyCompileStart();
        lex.reset(in, 0, 0, 0);
        parsedProgram = par.parse().value;

        if (parsedProgram == null) {
            notifyError("Unexpected end of file");
            return null;
        }
        if (par.errorCount != 0) {
            notifyCompileFinish(1);
            return null;
        }

        // do several passes for compiling
        Program stat = (Program) parsedProgram;
        Namespace env = new Namespace();
        stat.pass1(env); // create symbol table
        stat.pass2(0); // try to evaluate all expressions + compute relative addresses
        while (stat.pass3(env) == true) {
            // don't worry about deadlock
        }
        if (env.getPassNeedCount() != 0) {
            notifyError("Error: can't evaulate all expressions");
            notifyCompileFinish(1);
            return null;
        }
        stat.pass4(hex, env);
        notifyCompileFinish(0);
        return hex;
    }

    @Override
    public boolean compile(String fileName, Reader in) {
        try {
            HEXFileManager hex = compile(in);
            if (hex == null) {
                return false;
            }
            // Remove ".*" suffix and add ".hex" suffix to the filename
            int i = fileName.lastIndexOf(".");
            if (i >= 0) {
                fileName = fileName.substring(0, i);
            }
            fileName += ".hex"; // the output suffix

            hex.generateFile(fileName);
            notifyInfo("Compile was sucessfull. Output: " + fileName);

            MemoryContext memory = ContextPool.getInstance().getMemoryContext(pluginID, MemoryContext.class);
            if (memory != null) {
                if (hex.loadIntoMemory(memory)) {
                    notifyInfo("Compiled file was loaded into operating memory.");
                } else {
                    notifyError("Compiled file couldn't be loaded into operating"
                            + "memory due to an error.");
                }
            }
            programStart = hex.getProgramStart();
            return true;
        } catch (Exception e) {
            notifyError(e.getMessage());
            return false;
        }
    }

    @Override
    public LexicalAnalyzer getLexer(Reader in) {
        return new LexerImpl(in);
    }

    @Override
    public void showSettings() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public SourceFileExtension[] getSourceSuffixList() {
        return suffixes;
    }

    private static void printHelp() {
        System.out.println("Syntax: as-z80 [-o outputFile] inputFile\nOptions:");
        System.out.println("\t--output, -o\tfile: name of the output file");
        System.out.println("\t--version, -v\t: print version");
        System.out.println("\t--help, -h\t: this help");
    }

    public static void main(String[] args) {
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
                System.out.println(new CompilerImpl(0L).getVersion());
                return;
            } else {
              break;
            }
        }
        if (i >= args.length) {
            System.out.println("Error: expected input file name");
            return;
        }
        inputFile = args[i];
        if (outputFile == null) {
          outputFile = inputFile.substring(0, inputFile.lastIndexOf('.')) + ".hex";
        }

        CompilerImpl compiler = new CompilerImpl(0L);
        try {
          HEXFileManager hex = compiler.compile(new FileReader(inputFile));
          hex.generateFile(outputFile);
          System.out.println("Output saved to: " + outputFile);
        } catch (Exception e) {
          System.out.println(e.getMessage());
        }
    }

}
