---
layout: default
title: Abstract tape
nav_order: 5
parent: RAM
permalink: /ram/abstract-tape
---

# Abstract tape

Abstract tapes, in general, are used in various abstract machines. Probably the best known are Turing machine, RAM machine and RASP machine. plugin of the abstract tape for emuStudio is called `abstractTape-ram`.

There are several properties which an abstract tape might have:

- Bounded, one-side bounded or unbounded
- Random access (allowing to move head in both directions) or linear access (allowing to move head only in one direction)
- Specific or any cell content type (e.g. cells are integers, or strings, or can be any value?)
- Read only, or read-write cells
- Purpose of the tape (title)

This plugin allows to set up such properties, but those are set up by the virtual computer which uses it, not by
the user. For more information, please see the programming section.

Currently, there are just two virtual computers utilizing this plugin:

- RAM machine
- RASP machine

After emuStudio is run, RAM CPU (or RASP CPU) sets up properties for all used tapes. So the tape "purpose" and behavior
is set in run time.

## Graphical user interface (GUI)

The graphical user interface of the abstract tape is very simple. In order to open it, select the tape in the peripheral devices list in the Emulator panel. Then, click on the "Show" button.

![Abstract tape window (Input tape of the RAM machine)]({{ site.baseurl }}/assets/ram/abstractTape-ram.png)

The symbol, highlighted with the blue color is the current head position, in this case. In order to manipulate with particular symbols, one must _select_ the symbol, which appears in *bold*, as in the following image:

![Selected symbol in the abstract tape]({{ site.baseurl }}/assets/ram/abstractTape-ram-selection.png)

- *A*: If the tape allows it, one can add new symbol before the selected one in the tape. In the image, the tape does not allow it.
- *B*: The tape content area. Usually, each row consists of the symbol "index" or position within the tape, followed by the symbol itself.
- *C*: If the tape allows it, one can add new symbol after the last one in the tape. In the image, the tape allows it.
- *D*: Removes selected symbol from the tape.
- *E*: Edits the tape symbol. The symbol must be selected.
- *F*: Clears the tape content

## Settings

The tape allows to edit some settings from the graphical mode; to open the settings window click on the "Settings" button below the peripheral devices list in the Emulator panel. The window can be seen in the following image:

![Abstract tape settings]({{ site.baseurl }}/assets/ram/abstractTape-ram-settings.png)

- *A*: Do not allow the tape to fall behind other window
- *B*: Show the tape right after emuStudio start


## Configuration file

The following table shows all the possible settings of Abstract tape plugin:

|---
|Name              | Default value        | Valid values          | Description
|-|-|-|-
|`showAtStartup`   | false                | true, false           | If the tape should be shown automatically after emuStudio is started
|`alwaysOnTop`     | false                | true, false           | Whether the tape GUI should not allow to fall behind other windows
|---

## Automatic emulation

Abstract tape supports automatic emulation. It means, that every change to it is being written to a file. The file name is devised from the title of the tape, by the following algorithm:

- At first, all spaces in the title are replaced with underscore (`_`)
- Then, all "unwanted" characters are also replaced with underscore
- Every character is converted to lower-case
- Finally, the `.out` extension is added at the end.

Unwanted characters are the following: `*`, `.`, `#`, `%`, `&`, `+`, `!`, `~`, `/`, `?`, `<`, `>`, `,`, `|`, `{`, `}`, `[`, `]`, `"`, ```, `=`

## Using abstract tapes in your emulator

NOTE: This section is for developers of emulators.

The Abstract tape plugin can be used in various computers. Besides standard operations which are provided by `net.emustudio.emulib.plugins.device.DeviceContext` interface, it provides custom context API.

Usually, the tapes are used by CPU plugins, but it is of course possible to use it in any other plugin. You can obtain the context during the [Plugin.initialize()][pluginInitialize]{:target="_blank"} method of the plugin root class. The context is named `net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext`:

```java
@PluginRoot(...)
public class YourPlugin {

    ...

    public void initialize() throws PluginInitializationException {
        AbstractTapeContext tape = applicationApi.getContextPool().getDeviceContext(pluginID, AbstractTapeContext.class);
        ...
    }

    ...
}
```

The tape context interface has the following content:

```java
package net.emustudio.plugins.device.abstracttape.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;

/**
 * Public API of the abstract tape.
 */
@SuppressWarnings("unused")
@PluginContext
public interface AbstractTapeContext extends DeviceContext<String> {

    /**
     * Clear content of the tape.
     */
    void clear();

    /**
     * Set this tape to left-bounded or unbounded.
     *
     * @param bounded true if the tape should be left-bounded,
     *                false if unbounded.
     */
    void setBounded(boolean bounded);

    /**
     * Determine if the tape is left-bounded.
     *
     * @return true - left-bounded, false - unbounded.
     */
    boolean isBounded();

    /**
     * Move the tape one symbol to the left.
     * <p>
     * If the tape is left-bounded and the old position is 0, tape won't move. Otherwise the tape
     * will expand to the left - add new empty symbol to position 0 and shift the rest of the content to the right.
     *
     * @return true if the tape has been moved; false otherwise (if it is left-bounded and the position is 0).
     */
    boolean moveLeft();

    /**
     * Move tape to the right. If the tape is too short, it is expanded to the right (added new empty symbol).
     */
    void moveRight();

    /**
     * Allow or disallow to edit the tape.
     * <p>
     * If the tape is editable, the user (in GUI) can add, modify or remove symbols from the tape.
     * Otherwise it is driven only by the CPU.
     *
     * @param editable true if yes, false if not.
     */
    void setEditable(boolean editable);

    /**
     * Get symbol at the specified position.
     *
     * @param pos position in the tape, starting from 0
     * @return symbol at given position; if the position is out of bounds, then empty string is returned.
     */
    String getSymbolAt(int pos);

    /**
     * Set symbol at the specified position.
     * <p>
     * If the position is < 0, then no symbol will be set.
     * <p>
     * If the position is > tape size, empty symbols will be added until the required tape size is ensured.
     * Then, the symbol is added at the specified position.
     * <p>
     * This method should be used only when loading some initial content to the tape.
     *
     * @param pos    position in the tape, starting from 0
     * @param symbol symbol value
     */
    void setSymbolAt(int pos, String symbol);

    /**
     * Sets whether the symbol at which the head is pointing should be "highlighted" in GUI.
     *
     * @param visible true if yes; false otherwise.
     */
    void setHighlightHeadPosition(boolean visible);

    /**
     * Seths whether the tape should be cleared at emulation reset.
     *
     * @param clear true if yes; false otherwise.
     */
    void setClearAtReset(boolean clear);

    /**
     * Set title (purpose) of the tape.
     *
     * @param title title of the tape
     */
    void setTitle(String title);

    /**
     * Determines if the symbol positions should be displayed in GUI.
     *
     * @return true if yes; false otherwise
     */
    boolean showPositions();

    /**
     * Set whether the symbol positions should be displayed in GUI.
     *
     * @param showPositions true if yes; false otherwise.
     */
    void setShowPositions(boolean showPositions);

    /**
     * Get the tape head position.
     *
     * @return current position in the tape; starts from 0
     */
    int getHeadPosition();

    /**
     * Get the size of the tape
     *
     * @return tape size
     */
    int getSize();

    /**
     * Determine if the tape is empty.
     *
     * @return true if the tape is empty; false otherwise.
     */
    boolean isEmpty();

}
```


[pluginInitialize]: /documentation/developer/emulib_javadoc/net/emustudio/emulib/plugins/Plugin.html#initialize()
