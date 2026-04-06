/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DialogsGuiTest extends AbstractSwingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void messageAndConfirmationDialogsReturnExpectedAnswers() throws Exception {
        DialogsGui dialogs = new DialogsGui(new EmuStudioGui());
        dialogs.setParent(showFrame(onEdt(() -> new JFrame("parent"))));

        FutureTask<Void> errorTask = startDialogCall(() -> {
            dialogs.showError("Broken");
            return null;
        });
        closeDialog(waitForWindow(JDialog.class, dialog -> "Error".equals(dialog.getTitle())));
        errorTask.get(5, TimeUnit.SECONDS);

        FutureTask<Void> infoTask = startDialogCall(() -> {
            dialogs.showInfo("FYI");
            return null;
        });
        closeDialog(waitForWindow(JDialog.class, dialog -> "Information".equals(dialog.getTitle())));
        infoTask.get(5, TimeUnit.SECONDS);

        FutureTask<Dialogs.DialogAnswer> yesTask = startDialogCall(() -> dialogs.ask("Proceed?", "Confirm"));
        answerOptionPane(waitForWindow(JDialog.class, dialog -> "Confirm".equals(dialog.getTitle())), JOptionPane.YES_OPTION);
        assertEquals(Dialogs.DialogAnswer.ANSWER_YES, yesTask.get(5, TimeUnit.SECONDS));

        FutureTask<Dialogs.DialogAnswer> noTask = startDialogCall(() -> dialogs.ask("Proceed?"));
        answerOptionPane(waitForWindow(JDialog.class, dialog -> "Confirmation".equals(dialog.getTitle())), JOptionPane.NO_OPTION);
        assertEquals(Dialogs.DialogAnswer.ANSWER_NO, noTask.get(5, TimeUnit.SECONDS));

        FutureTask<Dialogs.DialogAnswer> cancelTask = startDialogCall(() -> dialogs.ask("Proceed?", "Cancel"));
        answerOptionPane(waitForWindow(JDialog.class, dialog -> "Cancel".equals(dialog.getTitle())), JOptionPane.CANCEL_OPTION);
        assertEquals(Dialogs.DialogAnswer.ANSWER_CANCEL, cancelTask.get(5, TimeUnit.SECONDS));
    }

    @Test
    public void inputAndChooserDialogsReturnParsedValuesAndSelections() throws Exception {
        DialogsGui dialogs = new DialogsGui(new EmuStudioGui());
        Path baseDirectory = temporaryFolder.newFolder("chooser").toPath();
        FileExtensionsFilter filter = new FileExtensionsFilter("Text", "txt");

        FutureTask<Optional<String>> stringTask = startDialogCall(() -> dialogs.readString("Name?"));
        JDialog stringDialog = waitForWindow(JDialog.class, dialog -> DialogsNoGui.INPUT_MESSAGE.equals(dialog.getTitle()));
        setText(findComponent(stringDialog, JTextField.class, field -> true), "emu");
        triggerButton(findButton(stringDialog, "OK"));
        assertEquals(Optional.of("emu"), stringTask.get(5, TimeUnit.SECONDS));

        FutureTask<Optional<Integer>> integerTask = startDialogCall(() -> dialogs.readInteger("Value?"));
        JDialog integerDialog = waitForWindow(JDialog.class, dialog -> DialogsNoGui.INPUT_MESSAGE.equals(dialog.getTitle()));
        setText(findComponent(integerDialog, JTextField.class, field -> true), "0x10");
        triggerButton(findButton(integerDialog, "OK"));
        assertEquals(Optional.of(16), integerTask.get(5, TimeUnit.SECONDS));

        FutureTask<Optional<Double>> doubleTask = startDialogCall(() -> dialogs.readDouble("Rate?"));
        JDialog doubleDialog = waitForWindow(JDialog.class, dialog -> DialogsNoGui.INPUT_MESSAGE.equals(dialog.getTitle()));
        setText(findComponent(doubleDialog, JTextField.class, field -> true), "12.5");
        triggerButton(findButton(doubleDialog, "OK"));
        assertEquals(12.5, doubleTask.get(5, TimeUnit.SECONDS).orElseThrow(AssertionError::new), 0.0);

        FutureTask<Optional<Path>> saveTask = startDialogCall(() ->
                dialogs.chooseFile("Save file", "Save", baseDirectory, true, filter)
        );
        approveFileChooser(
                waitForWindow(JDialog.class, dialog -> "Save file".equals(dialog.getTitle())),
                baseDirectory.resolve("program")
        );
        assertEquals(Optional.of(baseDirectory.resolve("program.txt")), saveTask.get(5, TimeUnit.SECONDS));

        FutureTask<Optional<Path>> listTask = startDialogCall(() ->
                dialogs.chooseFile("Save with list", "Save", baseDirectory, true, Collections.singletonList(filter))
        );
        approveFileChooser(
                waitForWindow(JDialog.class, dialog -> "Save with list".equals(dialog.getTitle())),
                baseDirectory.resolve("already.txt")
        );
        assertEquals(Optional.of(baseDirectory.resolve("already.txt")), listTask.get(5, TimeUnit.SECONDS));

        FutureTask<Optional<Path>> cancelTask = startDialogCall(() ->
                dialogs.chooseFile("Open file", "Open", false, Collections.singletonList(filter))
        );
        cancelFileChooser(waitForWindow(JDialog.class, dialog -> "Open file".equals(dialog.getTitle())));
        assertFalse(cancelTask.get(5, TimeUnit.SECONDS).isPresent());

        FutureTask<Optional<Path>> directoryTask = startDialogCall(() ->
                dialogs.chooseDirectory("Choose directory", "Choose", baseDirectory)
        );
        approveDirectoryChooser(
                waitForWindow(JDialog.class, dialog -> "Choose directory".equals(dialog.getTitle())),
                baseDirectory
        );
        assertEquals(Optional.of(baseDirectory), directoryTask.get(5, TimeUnit.SECONDS));

        FutureTask<Optional<Path>> defaultDirectoryTask = startDialogCall(() ->
                dialogs.chooseDirectory("Default directory", "Choose")
        );
        cancelFileChooser(waitForWindow(JDialog.class, dialog -> "Default directory".equals(dialog.getTitle())));
        assertFalse(defaultDirectoryTask.get(5, TimeUnit.SECONDS).isPresent());
    }

    private <T> FutureTask<T> startDialogCall(java.util.concurrent.Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        Thread thread = new Thread(task, "gui-dialog-test");
        thread.setDaemon(true);
        thread.start();
        return task;
    }

    private void closeDialog(JDialog dialog) {
        runOnEdt(dialog::dispose);
    }

    private void answerOptionPane(JDialog dialog, Object value) {
        JOptionPane optionPane = findComponent(dialog, JOptionPane.class, pane -> true);
        runOnEdt(() -> {
            optionPane.setValue(value);
            dialog.dispose();
        });
    }

    private void approveFileChooser(JDialog dialog, Path file) {
        JFileChooser chooser = findComponent(dialog, JFileChooser.class, component -> true);
        runOnEdt(() -> {
            chooser.setSelectedFile(file.toFile());
            chooser.approveSelection();
        });
    }

    private void approveDirectoryChooser(JDialog dialog, Path directory) {
        JFileChooser chooser = findComponent(dialog, JFileChooser.class, component -> true);
        runOnEdt(() -> {
            chooser.setSelectedFile(directory.toFile());
            chooser.approveSelection();
        });
    }

    private void cancelFileChooser(JDialog dialog) {
        JFileChooser chooser = findComponent(dialog, JFileChooser.class, component -> true);
        runOnEdt(chooser::cancelSelection);
    }
}
