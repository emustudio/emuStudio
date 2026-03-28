/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema.mode;

import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ModeSelectorTest {

    @Test
    public void selectSwitchesBetweenAllConcreteModes() {
        ModeSelector selector = new ModeSelector(new DrawingPanel(mock(Schema.class)), new DrawingModel());

        selector.select(ModeSelector.SelectMode.MOVING);
        assertEquals("MovingMode", selector.get().getClass().getSimpleName());

        selector.select(ModeSelector.SelectMode.MODELING);
        assertEquals("ModelingMode", selector.get().getClass().getSimpleName());

        selector.select(ModeSelector.SelectMode.RESIZING);
        assertEquals("ResizingMode", selector.get().getClass().getSimpleName());

        selector.select(ModeSelector.SelectMode.SELECTING);
        assertEquals("SelectingMode", selector.get().getClass().getSimpleName());
    }
}
