---
layout: default
title: CPU "ram-cpu"
nav_order: 3
parent: RAM
permalink: /ram/ram-cpu
---

# CPU "ram-cpu"

This plugin is the core of the emulation/simulation. Even if we're supposed to talk about RAM simulator, because emulation is connected more with imitation of real hardware than abstract machine, there is a plugin which calls itself a RAM CPU. It is really not accurate, but CPU nowadays means something as the
main or core engine of the computation which the machine does. So the name is stick rather with this convention.

The plugin strictly requires a `ram-mem`, and three instances of `abstractTape-ram` plugins, representing the tapes. After boot, the CPU assigns the specific meaning to each tape.

## Status panel

In the following image, you can see the status panel of `ram-cpu`.

![RAM CPU status panel]({{ site.baseurl }}/assets/ram/ram-cpu-status.png)

It is split into three parts. Within 'Internal status' part, there is shown content of registers `R0` (accumulator) and `IP`. Register `IP` is the position of the program memory head. It stands for "instruction pointer". It is pointing at the next instruction being executed.

The input/output part shows the next unread symbol ("next input"), and the last symbol written to the output tape ("last output"). This is just for the convenience; it is possible to see the same values in particular tape devices.

The last part, "Run state", shows in which state the whole emulation is, and it is common to all emulators in emuStudio. The state "breakpoint" means that the emulation is paused.

