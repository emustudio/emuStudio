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
package net.emustudio.plugins.compiler.asZ80.visitors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CreateVisitors {
    private final static Map<String, CreatePseudoVisitor> PSEUDO_CACHE = new ConcurrentHashMap<>();
    private final static Map<String, CreateExprVisitor> EXPR_CACHE = new ConcurrentHashMap<>();
    private final static Map<String, CreateInstrVisitor> INSTR_CACHE = new ConcurrentHashMap<>();
    private final static Map<String, CreateDataVisitor> DATA_CACHE = new ConcurrentHashMap<>();
    private final static Map<String, CreateLineVisitor> LINE_CACHE = new ConcurrentHashMap<>();

    static CreatePseudoVisitor pseudo(String sourceFileName) {
        PSEUDO_CACHE.putIfAbsent(sourceFileName, new CreatePseudoVisitor(sourceFileName));
        return PSEUDO_CACHE.get(sourceFileName);
    }

    static CreateExprVisitor expr(String sourceFileName) {
        EXPR_CACHE.putIfAbsent(sourceFileName, new CreateExprVisitor(sourceFileName));
        return EXPR_CACHE.get(sourceFileName);
    }

    static CreateInstrVisitor instr(String sourceFileName) {
        INSTR_CACHE.putIfAbsent(sourceFileName, new CreateInstrVisitor(sourceFileName));
        return INSTR_CACHE.get(sourceFileName);
    }

    static CreateDataVisitor data(String sourceFileName) {
        DATA_CACHE.putIfAbsent(sourceFileName, new CreateDataVisitor(sourceFileName));
        return DATA_CACHE.get(sourceFileName);
    }

    static CreateLineVisitor line(String sourceFileName) {
        LINE_CACHE.putIfAbsent(sourceFileName, new CreateLineVisitor(sourceFileName));
        return LINE_CACHE.get(sourceFileName);
    }
}
