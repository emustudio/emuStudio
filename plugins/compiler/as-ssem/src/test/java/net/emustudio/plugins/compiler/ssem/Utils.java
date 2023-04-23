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
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.ssem.ast.Instruction;
import net.emustudio.plugins.compiler.ssem.ast.Program;
import net.emustudio.plugins.compiler.ssem.ast.ProgramParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class Utils {

    public static List<Token> getTokens(String variation) {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(variation));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        stream.fill();
        return stream.getTokens();
    }

    public static Program parseProgram(String program) {
        SSEMLexer lexer = new SSEMLexer(CharStreams.fromString(program));
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SSEMParser parser = new SSEMParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ParserErrorListener(""));

        ParseTree tree = parser.start();
        ProgramParser programParser = new ProgramParser("");
        programParser.visit(tree);
        return programParser.getProgram();
    }


    public static void assertTokenTypes(String variation, int... expectedTypes) {
        List<Token> tokens = getTokens(variation);
        assertTokenTypes(tokens, expectedTypes);
    }

    public static void assertTokenTypes(List<Token> tokens, int... expectedTypes) {
        assertEquals(expectedTypes.length, tokens.size());
        for (int i = 0; i < expectedTypes.length; i++) {
            Token token = tokens.get(i);
            assertEquals(expectedTypes[i], token.getType());
        }
    }

    public static void assertTokenTypesForCaseVariations(String base, int... expectedTypes) {
        Random r = new Random();
        List<String> variations = new ArrayList<>();
        variations.add(base);
        variations.add(base.toLowerCase());
        variations.add(base.toUpperCase());
        for (int i = 0; i < 5; i++) {
            byte[] chars = base.getBytes();
            for (int j = 0; j < base.length(); j++) {
                if (r.nextBoolean()) {
                    chars[j] = Character.valueOf((char) chars[j]).toString().toUpperCase().getBytes()[0];
                } else {
                    chars[j] = Character.valueOf((char) chars[j]).toString().toLowerCase().getBytes()[0];
                }
            }
            variations.add(new String(chars));
        }
        for (String variation : variations) {
            assertTokenTypes(variation, expectedTypes);
        }
    }

    public static void assertInstructions(Program program, ParsedInstruction... instructions) {
        Map<Integer, Instruction> pinstr = program.getInstructions();
        assertEquals(instructions.length, pinstr.size());
        for (ParsedInstruction instruction : instructions) {
            assertEquals(
                    new Instruction(instruction.opcode, instruction.operand, new SourceCodePosition(0, 0, ""), Optional.empty()),
                    pinstr.get(instruction.line)
            );
        }
    }

    public static class ParsedInstruction {
        public final int line;
        public final int opcode;
        public final int operand;

        public ParsedInstruction(int line, int opcode, int operand) {
            this.line = line;
            this.opcode = opcode;
            this.operand = operand;
        }
    }
}
