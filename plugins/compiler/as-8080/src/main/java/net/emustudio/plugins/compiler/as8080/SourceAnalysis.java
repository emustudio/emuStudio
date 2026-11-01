/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.as8080;

import net.emustudio.emulib.plugins.compiler.SourceToken;
import net.emustudio.emulib.plugins.compiler.Token;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.*;

/** Editing analysis shared by the two assembler grammars. No desktop dependencies. */
public final class SourceAnalysis {
    private SourceAnalysis() { }

    public static List<Token> analyze(Parser parser, ParserRuleContext root, int[] tokenMap) {
        if (parser.getNumberOfSyntaxErrors() > 0) throw new IllegalArgumentException("Fix syntax errors before refactoring");
        Map<Integer, Token> semantic = new HashMap<>();
        walk(parser, root, "", semantic);
        CommonTokenStream stream = (CommonTokenStream) parser.getInputStream(); stream.fill();
        List<Token> result = new ArrayList<>();
        for (org.antlr.v4.runtime.Token token : stream.getTokens()) {
            int type = token.getType() == org.antlr.v4.runtime.Token.EOF ? Token.EOF : tokenMap[token.getType()];
            result.add(semantic.getOrDefault(token.getStartIndex(), new SourceToken(type, token.getStartIndex(), token.getText(), null, null, null)));
        }
        return result;
    }

    private static void walk(Parser parser, ParseTree tree, String scope, Map<Integer, Token> result) {
        String context = tree.getClass().getSimpleName();
        if (context.equals("PseudoMacroDefContext")) {
            org.antlr.v4.runtime.Token name = firstIdentifier(parser, tree);
            if (name == null) return;
            put(result, name, Token.SYMBOL_DEFINITION, "", "");
            String macroScope = "macro/" + name.getText().toLowerCase(Locale.ROOT);
            for (int i = 1; i < tree.getChildCount(); i++) {
                ParseTree child = tree.getChild(i);
                if (child.getClass().getSimpleName().equals("RMacroParametersContext")) {
                    parameters(parser, child, macroScope, result);
                } else walk(parser, child, macroScope, result);
            }
            return;
        }
        if (context.equals("PseudoIncludeContext")) {
            for (int i = 0; i < tree.getChildCount(); i++) {
                ParseTree child = tree.getChild(i);
                if (child instanceof TerminalNode) {
                    org.antlr.v4.runtime.Token token = ((TerminalNode) child).getSymbol();
                    String kind = parser.getVocabulary().getSymbolicName(token.getType());
                    if (kind != null && kind.startsWith("LIT_STRING")) {
                        String text = token.getText();
                        result.put(token.getStartIndex(), new SourceToken(Token.INCLUDE_PATH, token.getStartIndex(), text,
                                null, scope, null, text.substring(1, text.length() - 1)));
                    }
                }
            }
            return;
        }
        org.antlr.v4.runtime.Token declared = null;
        if (context.equals("PseudoEquContext") || context.equals("PseudoVarContext")) {
            declared = firstIdentifier(parser, tree);
            if (declared != null) put(result, declared, Token.SYMBOL_DEFINITION, scope, scope);
        }
        org.antlr.v4.runtime.Token macroCall = context.equals("PseudoMacroCallContext") ? firstIdentifier(parser, tree) : null;
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof TerminalNode) {
                org.antlr.v4.runtime.Token token = ((TerminalNode) child).getSymbol();
                String kind = parser.getVocabulary().getSymbolicName(token.getType());
                if (token == declared) continue;
                if ("ID_LABEL".equals(kind)) put(result, token, Token.SYMBOL_DEFINITION, scope, scope);
                else if ("ID_IDENTIFIER".equals(kind)) put(result, token, Token.SYMBOL_REFERENCE, scope, token == macroCall ? "" : scope);
            } else walk(parser, child, scope, result);
        }
    }
    private static void parameters(Parser parser, ParseTree tree, String scope, Map<Integer, Token> result) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            if (child instanceof TerminalNode) {
                org.antlr.v4.runtime.Token token = ((TerminalNode) child).getSymbol();
                if ("ID_IDENTIFIER".equals(parser.getVocabulary().getSymbolicName(token.getType()))) put(result, token, Token.SYMBOL_DEFINITION, scope, scope);
            }
        }
    }
    private static org.antlr.v4.runtime.Token firstIdentifier(Parser parser, ParseTree tree) {
        for (int i = 0; i < tree.getChildCount(); i++) {
            if (tree.getChild(i) instanceof TerminalNode) {
                org.antlr.v4.runtime.Token token = ((TerminalNode) tree.getChild(i)).getSymbol();
                if ("ID_IDENTIFIER".equals(parser.getVocabulary().getSymbolicName(token.getType()))) return token;
            }
        }
        return null;
    }
    private static void put(Map<Integer, Token> result, org.antlr.v4.runtime.Token token, int type, String scope, String referenceScope) {
        String name = token.getText();
        if (name.endsWith(":")) name = name.substring(0, name.length() - 1);
        result.put(token.getStartIndex(), new SourceToken(type, token.getStartIndex(), token.getText(), name, scope, referenceScope));
    }

    public static String extract(String source, int start, int end, String name, String kind,
                                 java.util.function.Function<String, List<Token>> analyze, String jump) {
        if (start < 0 || end > source.length() || start >= end || start > 0 && source.charAt(start - 1) != '\n'
                || end < source.length() && source.charAt(end - 1) != '\n') {
            throw new IllegalArgumentException("Select complete source lines to extract");
        }
        List<Token> all = analyze.apply(source);
        List<Token> nameTokens = analyze.apply(name + ":\n");
        if (nameTokens.stream().filter(t -> t.getType() == Token.SYMBOL_DEFINITION && name.equals(t.getSymbolName())).count() != 1
                || !name.matches("[a-zA-Z_?@.][a-zA-Z_?@.0-9]*")) throw new IllegalArgumentException("Choose a valid non-reserved symbol name");
        if (all.stream().anyMatch(t -> t.getSymbolName() != null && name.equalsIgnoreCase(t.getSymbolName()))) {
            throw new IllegalArgumentException("The symbol already exists");
        }
        String body = source.substring(start, end);
        boolean instruction = false;
        for (Token token : all) {
            if (token.getOffset() < start || token.getOffset() >= end) continue;
            if (token.getType() == Token.SYMBOL_DEFINITION || token.getType() == Token.BLOCK_START || token.getType() == Token.BLOCK_END
                    || token.getType() == Token.INCLUDE_PATH || token.getSymbolScope() != null && !token.getSymbolScope().isEmpty()) {
                throw new IllegalArgumentException("Extract a block outside macros without declarations, nested blocks, or includes");
            }
            instruction |= token.getType() == Token.RESERVED;
            if (!kind.equals("Macro") && (token.getType() == Token.PREPROCESSOR
                    || token.getType() == Token.REGISTER && token.getText().equalsIgnoreCase("sp")
                    || token.getType() == Token.RESERVED && token.getText().toLowerCase(Locale.ROOT).matches("j.*|ret.*|r[cnzpme].*|call|c[cnzpme].*|push|pop|rst|pchl|sphl|xthl|halt|hlt"))) {
                throw new IllegalArgumentException("Subroutine extraction requires straight-line instructions without stack or control-flow operations");
            }
        }
        if (!instruction) throw new IllegalArgumentException("Select at least one instruction");
        String replacement;
        if (kind.equals("Macro")) replacement = name + " macro\n" + body + (body.endsWith("\n") ? "" : "\n") + "endm\n" + name + "\n";
        else if (kind.equals("Subroutine (uses stack)")) {
            String after = name + "_after";
            if (all.stream().anyMatch(t -> t.getSymbolName() != null && after.equalsIgnoreCase(t.getSymbolName()))) throw new IllegalArgumentException("The generated return label already exists");
            replacement = "call " + name + "\n" + jump + " " + after + "\n" + name + ":\n" + body + (body.endsWith("\n") ? "" : "\n") + "ret\n" + after + ":\n";
        } else throw new IllegalArgumentException("Unknown extraction kind");
        String edited = source.substring(0, start) + replacement + source.substring(end);
        analyze.apply(edited);
        return edited;
    }
}
