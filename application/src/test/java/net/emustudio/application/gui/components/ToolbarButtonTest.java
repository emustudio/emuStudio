/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.components;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import org.junit.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ToolbarButtonTest extends AbstractSwingTest {

    @Test
    public void actionConstructorUsesActionMetadataAndInvokesAction() {
        AtomicInteger invocations = new AtomicInteger();
        Action action = new AbstractAction("Run") {
            @Override
            public void actionPerformed(ActionEvent e) {
                invocations.incrementAndGet();
            }
        };
        action.putValue(Action.SHORT_DESCRIPTION, "Run emulation");

        ToolbarButton button = onEdt(() -> new ToolbarButton(action));

        showInFrame(button);
        triggerButton(button);

        assertEquals(1, invocations.get());
        assertTrue(onEdt(button::getHideActionText));
        assertEquals("Run emulation", onEdt(button::getToolTipText));
        assertFalse(onEdt(button::isFocusable));
        assertEquals("toolBarButton", onEdt(() -> button.getClientProperty("JButton.buttonType")));
    }

    @Test
    public void iconConstructorAppliesTooltipAndIconToAction() {
        AtomicInteger invocations = new AtomicInteger();
        Action action = new AbstractAction("About") {
            @Override
            public void actionPerformed(ActionEvent e) {
                invocations.incrementAndGet();
            }
        };

        ToolbarButton button = onEdt(
                () -> new ToolbarButton(action, EmuStudioGui.ICON_FAVICON, "Show About dialog", getClass())
        );

        showInFrame(button);
        triggerButton(button);

        assertEquals(1, invocations.get());
        assertEquals("Show About dialog", action.getValue(Action.SHORT_DESCRIPTION));
        assertSame(action.getValue(Action.SMALL_ICON), onEdt(button::getIcon));
        assertNotNull(onEdt(button::getIcon));
        assertEquals("Show About dialog", onEdt(button::getToolTipText));
    }

    @Test
    public void consumerConstructorCreatesAnActionFromEvent() {
        AtomicReference<ActionEvent> receivedEvent = new AtomicReference<>();

        ToolbarButton button = onEdt(
                () -> new ToolbarButton(receivedEvent::set, EmuStudioGui.ICON_FAVICON, "Open", getClass())
        );

        showInFrame(button);
        triggerButton(button);

        assertNotNull(receivedEvent.get());
        assertSame(button, receivedEvent.get().getSource());
        assertEquals("Open", onEdt(() -> button.getAction().getValue(Action.SHORT_DESCRIPTION)));
        assertNotNull(onEdt(button::getIcon));
    }
}
