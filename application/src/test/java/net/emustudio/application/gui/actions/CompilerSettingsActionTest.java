/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;
import org.junit.Test;

import javax.swing.*;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CompilerSettingsActionTest {

    @Test
    public void actionIsDisabledWhenCompilerSettingsAreUnsupported() {
        JFrame parent = new JFrame();
        VirtualComputer computer = mock(VirtualComputer.class);
        Compiler compiler = mock(Compiler.class);
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(compiler.isShowSettingsSupported()).thenReturn(false);

        CompilerSettingsAction action = new CompilerSettingsAction(parent, computer);
        action.actionPerformed(null);

        assertFalse(action.isEnabled());
        verify(compiler, never()).showSettings(any());
    }

    @Test
    public void actionIsDisabledWhenCompilerIsMissing() {
        JFrame parent = new JFrame();
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getCompiler()).thenReturn(Optional.empty());

        CompilerSettingsAction action = new CompilerSettingsAction(parent, computer);
        action.actionPerformed(null);

        assertFalse(action.isEnabled());
    }

    @Test
    public void actionShowsCompilerSettingsWhenSupported() {
        JFrame parent = new JFrame();
        VirtualComputer computer = mock(VirtualComputer.class);
        Compiler compiler = mock(Compiler.class);
        when(computer.getCompiler()).thenReturn(Optional.of(compiler));
        when(compiler.isShowSettingsSupported()).thenReturn(true);

        CompilerSettingsAction action = new CompilerSettingsAction(parent, computer);
        action.actionPerformed(null);

        assertTrue(action.isEnabled());
        verify(compiler).showSettings(parent);
    }
}
