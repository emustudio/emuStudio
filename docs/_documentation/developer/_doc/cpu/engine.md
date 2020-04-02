---
layout: default
title: Emulator engine
nav_order: 3
parent: Writing a CPU
permalink: /cpu/engine
---

# Emulator engine

Emulator engine is the core of the emulator. It interprets binary-encoded instructions stored in a memory (emuStudio assumes it's a von-Neumann-like CPU). Execution of one instruction
involves four basic steps: fetch, decode and execute, and store, executed in order. Those steps can overlap in the implementation.

A pseudo-algorithm for emulator engine can look as follows:

```java
public class EmulatorEngine {
    private final CPU cpu;
    private final MemoryContext<Byte> memory;

    // internal CPU registers
    private int currentInstruction;

    EmulatorEngine(MemoryContext<Byte> memory, CPU cpu) {
        this.memory = Objects.requireNonNull(memory);
        this.cpu = Objects.requireNonNull(cpu);
    }

    CPU.RunState step(CPU.RunState currentRunState) {
        int instruction = memory.read(currentInstruction);
        currentInstruction = currentInstruction + 1;

        switch (instruction) {
            case 0: // ADD
                ...
                return CPU.RunState.STATE_STOPPED_BREAK;
            case 4: // JMP
                ...
                return CPU.RunState.STATE_STOPPED_BREAK;
            case 99: // HLT
                return CPU.RunState.STATE_STOPPED_NORMAL;
        }
    }

    CPU.RunState run(CPU.RunState currentRunState) {
        while (currentRunState == CPU.RunState.STATE_STOPPED_BREAK) {
            try {
                if (cpu.isBreakpointSet(currentInstruction)) {
                    return currentRunState;
                }
                currentRunState = step();
            } catch (...) {
                currentRunState = CPU.RunState.STATE_STOPPED_XXX;
                break;
            }
        }
        return currentRunState;
    }

    ...
}
```

It uses interpretation emulation technique (the simplest one). Note that breakpoints must be manually handled - after execution of each instruction it should be checked if the current instruction hasn't a breakpoint, and if yes, return. 
