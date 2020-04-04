---
layout: default
title: Memory standard-mem
nav_order: 5
parent: MITS Altair8800
permalink: /altair8800/standard-mem
---

# Operating memory "standard-mem"

This plugin emulates an operating memory, in a quite broad meaning. It can be used for any virtual computer which can benefit from the following basic properties:

- A memory cell has size of 1 byte (8 bits)
- Memory cells are linearly ordered (sequential)
- Each memory cell is accessible by unique address, representing the index of the memory cell, if the memory is imagined as an array of cells

Besides, the memory supports these additional features:

- Setting up ROM (read only memory) ranges
- Changing memory size; by default it is 64kB
- Support of bank switching

There are also some "interactive" features:

- Manual loading/saving memory images in either binary or Intel HEX format
- Ability to automatically load a memory image at startup
- Intuitive control using keystrokes, and nice visual presentation of the data

This operating memory is (for now) used only in 8-bit emulator of MITS Altair8800. However it is possible to develop an emulator which can benefit from it.

## GUI overview

To open the memory GUI (graphical user interface), click at the right-most icon in the debug toolbar, on the Emulator panel. The window is shown, as in the following image:

![Standard operating memory]({{ site.baseurl }}/assets/altair8800/standard-mem.png)

- *A*: Open a memory image. Current memory content, if it does not interfere with the loaded data will be kept.
- *B*: Dump (save) whole memory content into a file.
- *C*: Go to address. The address can be either in decimal, hexadecimal (prefix `0x`) or octal (prefix `0`) format.
- *D*: Find a sequence. A dialog shows up where user can find either a plain text or sequence of bytes in the memory.
- *E*: Erases all memory content.
- *F*: Shows memory settings
- *G*: By double-clicking on the memory cell it is possible to edit it and change its value. The value format is the same as the input to "go to address" dialog (see *C*).
- *H*: Page of the memory view. The whole memory cannot be shown in single window, because it can be quite large (64 kB by default), so it was divided into pages.
- *I*: If the memory has set up <<STANDARD-MEM_BANKS,memory banks>>, it is possible to change the view to different bank. Switching in here has no effect on the emulator and on the active bank.
- *J*: Displays the data of the selected cell in various forms.

Generally, it is possible to move around the cells using keystrokes (arrows). If user presses some other letter/number key, a small text field appears allowing to edit the current value. When editing
is finished, user can press ENTER key to confirm it, or ESC key to discard the editing.

## Memory settings

Settings window can be opened by clicking on "settings" icon in the main GUI window:

![Memory settings]({{ site.baseurl }}/assets/altair8800/standard-mem-settings.png)

- *A*: Settings for memory bank-switching
- *B*: Settings for ROM areas
- *C*: If checked, settings for ROM areas will be saved to the configuration file
- *D*: List of memory images which will be loaded at startup
- *E*: The button will apply the settings

ROM areas and memory bank-switching is explained in the following sections.

### ROM areas

Some "controllers" - used as embedded devices - usually logically organize memory into areas, some of which are read only, which usually contains the firmware, and some are rewritable. Physically, these memories are wired to specific addresses, so the programmer can access them.

Standard operating memory plugin emulates this behavior. It allows to define ROM areas which represent read only memory. There can be set up multiple ROM areas, and they can overlap. Effectively it means that memory cells in ROM area cannot be changed from software running on the emulator. All writes to the memory will be ignored.

Manually, as a user it is possible to change the values, but only by loading new memory image. Editing the value will not work.

If a ROM range is defined, it is possible to remove only a part of it, effectively splitting the range and correcting their boundaries. For example, if there is defined a ROM range from `0x0A - 0x64` (see the image above), then it is possible to remove a range e.g. `0x32 - 0x46`, which is the part of defined ROM area. Then, the original ROM area is split into two parts - first will be a range from `0x0A - 0x31`, and the second from `0x47 - 0x64`.

### Memory bank-switching

This technique was invented as a workaround for a problem when the address space of a processor was smaller than memory size. In order to overcome this issue, memory was logically split into many regions of size equal to the processor address space. These regions are called "banks".

Physically, banks could refer to the same memory, but they could be also different memories (e.g. external cartridges), and the bank-switching involved switching the active memory.

Selecting a bank from a programming perspective was usually done by writing some code to some I/O port using some I/O instruction of a CPU. But it can be implemented in various ways, e.g. some memory addresses can be used for selecting a bank.

Also, it was very common that some part of the address space still kept some common memory part which was never switched out. This part is called a "common" part. In emuStudio, common part starts with the `Common` address (as it can be seen in the Settings dialog image above) and ends till the rest of the CPU address space (or memory end).

To summarize, let's consider an example. If a CPU is 8-bit, it means it has address space of size 2^8 - i.e. it can access memory from address 0 to (2^8 - 1). If the memory was larger, CPU just doesn't allow to access higher memory cells. So memory bank-switching is coming for the rescue. If the memory has 2 MB, we require `2^log2(2MB) = 2^21` addresses. So, if we won't have any common address space, we require `ceil(21 / 8) = 3` banks:

- bank 0: maps from 0 - (2^8 - 1)
- bank 1: maps from 2^8 - (2^16 - 1)
- bank 2: maps from 2^16 - (2^21 - 1)

## Configuration file

The following table shows all the possible settings of Standard operating memory plugin:

|---
|Name              | Default value        | Valid values          | Description
|-|-|-|-
|`banksCount`      | 0                    | >= 0                  | Number of memory banks
|`commonBoundary`  | 0                    | >= 0 and < mem size   | Address from which the banks are shared
|`memorySize`      | 65536                | > 0                   | Memory size in bytes
|`ROMfrom`(i)      | N/A                  | >= 0 and < mem size   | Start of the i-th ROM area
|`ROMto`(i)        | N/A                  | >= `ROMfrom`(i) and < mem size   | End of the i-th ROM area
|`imageName`(i)    | N/A                  | file path             | The i-th memory image file name. If it ends with `.hex` suffix, it will be loaded as Intel HEX format, otherwise as binary
|`imageAddress`(i) | N/A                  | >= 0 and < mem size   | The i-th memory image load address
|---

## Using memory in custom computers

This section is for developers of emulators. If you do not plan to create custom virtual computers, you can safely skip this section. In order to get started with developing plugins for emuStudio, please read tutorial "Developing emuStudio Plugins".

As it was mentioned in the earlier sections, the Standard operating memory plugin can be used in other computers, too. Besides standard operations which are provided by `net.emustudio.emulib.plugins.memory.MemoryContext` interface, it provides custom context API, enabling to use more features - e.g. bank-switching.

You can obtain the context in [Plugin.initialize()][pluginInitialize]{:target="_blank"} method. The context is named `net.emustudio.plugins.memory.standard.api.StandardMemoryContext`:

```java
...

public void initialize(SettingsManager settings) {
    StandardMemoryContext mem = contextPoolImpl.getMemoryContext(pluginID, StandardMemoryContext.class);
    ...
}
```

The memory context has the following content:

```java
package net.emustudio.plugins.memory.standard.api;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.List;

/**
 * Extended memory context.
 * <p>
 * Supports bank switching, ROM ranges, and loading HEX/BIN files.
 */
@PluginContext(id = "Standard memory")
public interface StandardMemoryContext extends MemoryContext<Short> {

    /**
     * This interface represents a range of addresses in the memory.
     */
    interface AddressRange {
        int getStartAddress();

        int getStopAddress();
    }

    /**
     * Determine whether specified memory position is read-only.
     *
     * @param address memory position
     * @return true if the memory position is read only, false otherwise
     */
    boolean isReadOnly(int address);

    /**
     * Get list of ranges of read-only addresses.
     *
     * @return list of ROM memory addresses
     */
    List<? extends AddressRange> getReadOnly();

    /**
     * Set specified memory range as RAM (Random Access Memory).
     *
     * @param range address range
     */
    void setReadWrite(AddressRange range);

    /**
     * Set specified memory range as ROM (Read Only Memory).
     *
     * @param range address range
     */
    void setReadOnly(AddressRange range);

    /**
     * Get number of available memory banks.
     *
     * @return count of memory banks
     */
    int getBanksCount();

    /**
     * Get index of the selected memory bank.
     *
     * @return index of active (selected) memory bank
     */
    short getSelectedBank();

    /**
     * Select (set as active) a memory bank.
     *
     * @param bankIndex index (number) of a bank which should be selected
     */
    void selectBank(short bankIndex);

    /**
     * Return an address in the memory which represents a boundary from which
     * the memory banks have the same content. Before this address all banks
     * can have different content.
     *
     * @return common boundary address
     */
    int getCommonBoundary();
}
```

[pluginInitialize]: /documentation/developer/emulib_javadoc/net/emustudio/emulib/plugins/Plugin.html#initialize()
