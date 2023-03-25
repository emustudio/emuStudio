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
package net.emustudio.application.gui.actions;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;

public class CompilerSettingsAction extends AbstractAction {
    private final JFrame parent;
    private final VirtualComputer computer;

    public CompilerSettingsAction(JFrame parent, VirtualComputer computer) {
        super("Compiler settings...");
        this.parent = Objects.requireNonNull(parent);
        this.computer = Objects.requireNonNull(computer);

        putValue(MNEMONIC_KEY, KeyEvent.VK_S);

        setEnabled(computer.getCompiler().filter(Compiler::isShowSettingsSupported).isPresent());
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        Optional<Compiler> compiler = computer.getCompiler();
        if (compiler.isPresent() && (compiler.get().isShowSettingsSupported())) {
            compiler.get().showSettings(parent);
        }
    }
}
