---
layout: default
title: Run states
nav_order: 2
parent: Writing a CPU
permalink: /cpu/runstate
---

# Run states

Emulator "life" is a state machine. A state machine reacts on asynchronous events, which make the machine to transition the current state to another state. In emuStudio, whole emulation "state" depends on CPU run state. The run state is a name for the following states: `breakpoint` (starting state), `running`, `stopped` (more variants).

The state machine, how it should work, can be seen in the following diagram:

![runstate]({{ site.baseurl }}/cpu/images/runstate.svg)

The states of the state machine are encoded into an enum [CPU.RunState][runstate]{:target="_blank"} in emuLib:


```java
public static enum RunState {
    STATE_STOPPED_NORMAL("stopped"),
    STATE_STOPPED_BREAK("breakpoint"),
    STATE_STOPPED_ADDR_FALLOUT("stopped (address fallout)"),
    STATE_STOPPED_BAD_INSTR("stopped (instruction fallout)"),
    STATE_RUNNING("running");

    ...
}
```

Implementation of the state machine is a sole responsibility of CPU plugin. emuStudio has some expectations of it, like:

- initial run state should be `STATE_STOPPED_BREAK`
- calling `reset()` should set the run state to `STATE_STOPPED_BREAK`
- calling `pause()` if the current state is not one of `STATE_STOPPED_(how)` variant, it should set the run state to `STATE_STOPPED_BREAK`. Otherwise, do nothing.
- calling `step()` if the current state is one of `STATE_STOPPED_(how)` (except `STATE_STOPPED_BREAK`), it should do nothing. Otherwise, it should set the run state to:
    - `STATE_STOPPED_BREAK`, if the execution of the current instruction did not cause error, or it wasn't a "halt" instruction.
    - `STATE_STOPPED_(how)` state, where `(how)` should be replaced by:
        - `BAD_INSTR` - if unknown instruction was encountered
        - `ADDR_FALLOUT` - if instruction pointed to unknown or forbidden memory location
        - `NORMAL` - if the instruction was "halt" causing CPU to "halt" 
- calling `run()` should set the state to `STATE_RUNNING` and run instructions "infinitely", upon external event or some error, in which case it should set the state to:
    - `STATE_STOPPED_BREAK` - if external call `pause()` method
    - `STATE_STOPPED_(how)` state, where `(how)` should be replaced by:
        - `BAD_INSTR` - if unknown instruction was encountered
        - `ADDR_FALLOUT` - if instruction pointed to unknown or forbidden memory location
        - `NORMAL` - if the instruction was "halt" causing CPU to "halt"
- calling `stop()` if the current state is one of `STATE_STOPPED_(how)` (except `STATE_STOPPED_BREAK`), it should do nothing. Otherwise it should set the state to `STATE_STOPPED_NORMAL`.


If the CPU plugin root class implements [CPU][cpu]{:target="_blank"} interface, it is its responsibility to notify CPU run state changes and manage run state "listeners". But if the plugin root class extends [AbstractCPU][abstractCPU]{:target="_blank"}, it does not have care about listeners and run state notifications, because the class implements it. 



[runstate]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/CPU.RunState.html
[cpu]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/CPU.html
[abstractCPU]: {{ site.baseurl }}/emulib_javadoc/net/emustudio/emulib/plugins/cpu/AbstractCPU.html
