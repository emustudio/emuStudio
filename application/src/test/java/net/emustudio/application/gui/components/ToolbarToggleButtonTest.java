/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioUI;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ToolbarToggleButtonTest extends AbstractSwingTest {

    @Test
    public void actionConstructorUsesTooltipFromAction() {
        AtomicInteger invocations = new AtomicInteger();
        Action action = new AbstractAction("Toggle") {
            @Override
            public void actionPerformed(ActionEvent e) {
                invocations.incrementAndGet();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Toggle tracing");

        ToolbarToggleButton button = onEdt(() -> new ToolbarToggleButton(action));

        showInFrame(button);
        triggerButton(button);

        assertEquals(1, invocations.get());
        assertTrue(onEdt(button::isSelected));
        assertTrue(onEdt(button::getHideActionText));
        assertEquals("Toggle tracing", onEdt(button::getToolTipText));
        assertFalse(onEdt(button::isFocusable));
        assertEquals("toolBarButton", onEdt(() -> button.getClientProperty("JButton.buttonType")));
    }

    @Test
    public void iconConstructorAddsTooltipAndIconToAction() {
        AtomicInteger invocations = new AtomicInteger();
        Action action = new AbstractAction("Breakpoint") {
            @Override
            public void actionPerformed(ActionEvent e) {
                invocations.incrementAndGet();
            }
        };

        ToolbarToggleButton button = onEdt(
                () -> new ToolbarToggleButton(action, EmuStudioUI.ICON_BREAKPOINT, "Toggle breakpoint", getClass())
        );

        showInFrame(button);
        triggerButton(button);

        assertEquals(1, invocations.get());
        assertEquals("Toggle breakpoint", action.getValue(Action.SHORT_DESCRIPTION));
        assertSame(action.getValue(Action.SMALL_ICON), onEdt(button::getIcon));
        assertNotNull(onEdt(button::getIcon));
        assertEquals("Toggle breakpoint", onEdt(button::getToolTipText));
    }

    @Test
    public void consumerConstructorInvokesActionAndItemListener() {
        AtomicReference<ActionEvent> receivedActionEvent = new AtomicReference<>();
        AtomicReference<ItemEvent> receivedItemEvent = new AtomicReference<>();
        AtomicInteger itemEvents = new AtomicInteger();

        ToolbarToggleButton button = onEdt(() -> new ToolbarToggleButton(
                receivedActionEvent::set,
                event -> {
                    receivedItemEvent.set(event);
                    itemEvents.incrementAndGet();
                },
                EmuStudioUI.ICON_BREAKPOINT,
                "Inspect",
                getClass()
        ));

        showInFrame(button);
        triggerButton(button);
        triggerButton(button);

        assertNotNull(receivedActionEvent.get());
        assertSame(button, receivedActionEvent.get().getSource());
        assertNotNull(receivedItemEvent.get());
        assertTrue(itemEvents.get() >= 2);
        assertFalse(onEdt(button::isSelected));
    }
}
