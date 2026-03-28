/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import org.junit.After;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;

public abstract class AbstractSwingTest {
    private final List<Window> windows = new ArrayList<>();

    @After
    public void tearDownWindows() {
        runOnEdt(() -> {
            for (Window window : windows) {
                if (window != null) {
                    window.dispose();
                }
            }
            windows.clear();
        });
    }

    protected JFrame showInFrame(Component component) {
        JFrame frame = onEdt(() -> {
            JFrame result = new JFrame();
            result.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            result.getContentPane().add(component);
            result.pack();
            result.doLayout();
            return result;
        });
        windows.add(frame);
        return frame;
    }

    protected JDialog showDialog(JDialog dialog) {
        runOnEdt(dialog::doLayout);
        windows.add(dialog);
        return dialog;
    }

    protected JFrame showFrame(JFrame frame) {
        runOnEdt(frame::doLayout);
        windows.add(frame);
        return frame;
    }

    protected <T> T onEdt(Callable<T> action) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw propagate(e);
            }
        }

        FutureTask<T> task = new FutureTask<>(action);
        try {
            SwingUtilities.invokeAndWait(task);
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (InvocationTargetException | ExecutionException e) {
            throw propagate(e.getCause());
        }
    }

    protected void runOnEdt(Runnable action) {
        onEdt(() -> {
            action.run();
            return null;
        });
    }

    protected <T extends Component> T findComponent(Container root, Class<T> type, Predicate<T> matcher) {
        T component = onEdt(() -> findComponentRecursive(root, type, matcher));
        if (component == null) {
            throw new AssertionError("Could not find " + type.getSimpleName() + " in " + root.getClass().getSimpleName());
        }
        return component;
    }

    protected JLabel findLabel(Container root, String text) {
        return findComponent(root, JLabel.class, label -> text.equals(label.getText()));
    }

    protected JButton findButton(Container root, String text) {
        return findComponent(root, JButton.class, button -> text.equals(button.getText()));
    }

    protected JButton findButtonByTooltip(Container root, String tooltip) {
        return findComponent(root, JButton.class, button -> tooltip.equals(button.getToolTipText()));
    }

    protected JToggleButton findToggleButtonByTooltip(Container root, String tooltip) {
        return findComponent(root, JToggleButton.class, button -> tooltip.equals(button.getToolTipText()));
    }

    protected void triggerButton(AbstractButton button) {
        runOnEdt(() -> button.doClick(0));
    }

    protected void setText(JTextComponent component, String text) {
        runOnEdt(() -> component.setText(text));
    }

    protected <T extends Window> T waitForWindow(Class<T> type, Predicate<T> matcher) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            T window = onEdt(() -> Arrays.stream(Window.getWindows())
                    .filter(type::isInstance)
                    .map(type::cast)
                    .filter(Window::isShowing)
                    .filter(matcher)
                    .findFirst()
                    .orElse(null));
            if (window != null) {
                windows.add(window);
                return window;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Could not find visible " + type.getSimpleName());
    }

    private static <T extends Component> T findComponentRecursive(Component component, Class<T> type, Predicate<T> matcher) {
        if (type.isInstance(component)) {
            T typed = type.cast(component);
            if (matcher.test(typed)) {
                return typed;
            }
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                T match = findComponentRecursive(child, type, matcher);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static RuntimeException propagate(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        return new RuntimeException(throwable);
    }
}
