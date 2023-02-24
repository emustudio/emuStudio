/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
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

package net.emustudio.plugins.memory.rasp.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.memory.rasp.gui.Disassembler;

import java.util.List;
import java.util.Optional;

/**
 * Context of the RASP memory.
 */
@PluginContext
@SuppressWarnings("unused")
public interface RaspMemoryContext extends MemoryContext<RaspMemoryCell> {

    void setLabels(List<RaspLabel> labels);

    Optional<RaspLabel> getLabel(int address);

    List<Integer> getInputs();

    void setInputs(List<Integer> inputs);

    default Optional<String> disassemble(int opcode) {
        return Disassembler.disassemble(opcode);
    }
}
