package net.emustudio.application.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;

import java.awt.*;

public interface ExtendedDialogs extends Dialogs {

    default void setParent(Component component) {

    }
}
