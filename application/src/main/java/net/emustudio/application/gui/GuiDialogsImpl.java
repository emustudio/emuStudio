/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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

package net.emustudio.application.gui;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;

import javax.swing.*;
import java.util.Optional;

public class GuiDialogsImpl implements Dialogs {
    private final RadixUtils radixUtils = RadixUtils.getInstance();

    @Override
    public void showError(String message) {
        showError(message, "Error");
    }

    @Override
    public void showError(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String message) {
        showInfo(message, "Information");
    }

    @Override
    public void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public Optional<Integer> readInteger(String message) {
        return readInteger(message, NoGuiDialogsImpl.INPUT_MESSAGE);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title) {
        return readInteger(message, title, 0);
    }

    @Override
    public Optional<Integer> readInteger(String message, String title, int initial) {
        Object inputValue = JOptionPane.showInputDialog(
            null, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial
        );
        return Optional.ofNullable(inputValue).map(String::valueOf).map(radixUtils::parseRadix);
    }

    @Override
    public Optional<String> readString(String message) {
        return readString(message, NoGuiDialogsImpl.INPUT_MESSAGE);
    }

    @Override
    public Optional<String> readString(String message, String title) {
        return readString(message, title, "");
    }

    @Override
    public Optional<String> readString(String message, String title, String initial) {
        Object inputValue = JOptionPane.showInputDialog(
            null, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial
        );
        return Optional.ofNullable(inputValue).map(String::valueOf);
    }

    @Override
    public Optional<Double> readDouble(String message) {
        return readDouble(message, NoGuiDialogsImpl.INPUT_MESSAGE);
    }

    @Override
    public Optional<Double> readDouble(String message, String title) {
        return readDouble(message, title, 0);
    }

    @Override
    public Optional<Double> readDouble(String message, String title, double initial) {
        Object inputValue = JOptionPane.showInputDialog(
            null, message, title, JOptionPane.QUESTION_MESSAGE, null, null, initial
        );
        return Optional.ofNullable(inputValue).map(String::valueOf).map(Double::parseDouble);
    }

    @Override
    public DialogAnswer ask(String message) {
        return ask(message, "Confirmation");
    }

    @Override
    public DialogAnswer ask(String message, String title) {
        int answer = JOptionPane.showConfirmDialog(
            null, message, title, JOptionPane.YES_NO_CANCEL_OPTION
        );

        switch (answer) {
            case JOptionPane.YES_OPTION:
                return DialogAnswer.ANSWER_YES;
            case JOptionPane.NO_OPTION:
                return DialogAnswer.ANSWER_NO;
        }
        return DialogAnswer.ANSWER_CANCEL;
    }
}
