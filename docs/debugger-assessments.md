# Debugging assessments for #454

## Reverse execution

Implemented instruction history stores before/after registers and guest access values. It cannot reconstruct a complete
machine: attempted writes may target ROM, reads can have device effects, and bus timing advances devices independently.
The existing Spectrum image loader covers CPU/RAM image import, not host tape/audio/file/socket state or deterministic
external-input replay. Restoring just PC/registers/RAM would produce an inconsistent machine.

A follow-up checkpoint contract should enumerate participating CPU, memory, bus and device components; capture them together
at a paused instruction boundary; version each state payload; preserve pending interrupts, cycle counters and bank selection;
and reject restoration when any participant lacks support. Record external inputs and irreversible host-output boundaries.
A small CPU+RAM-only computer can be the first supported target. Validate forward/replay state parity and interrupt timing,
then add tape, audio, storage and Spectrum ULA state. Until that exists, history is explicitly read-only and Move PC Back
explicitly leaves registers/memory/devices unchanged.

## Edigen flow and processor semantics

Inspected Edigen's Specification AST, JavaCC Grammar.jj and Disassembler.edt. The DSL currently defines decoder rules and
rendered mnemonic formats. The template emits disassemble/getNextInstructionPosition; it has no instruction-semantic AST.
The new optional Disassembler.getFlow default keeps generated output compatible. Opcode adapters currently provide the
structured flow required by bundled 8080/Z80 plugins.

A future DSL increment should attach flow kind, conditional fall-through, and target expressions to decoder rules. Reuse
named decoded operands and existing signed/endian transformations, represent indirect targets explicitly, and generate
InstructionFlow through a new template method. Preserve old specifications via the empty default. Cover relative branches,
prefixes, conditional calls/returns, RST vectors, malformed specs and stale-byte decoding with generator fixtures. Changes
belong to Edigen plus its Gradle plugin and CPU specifications.

Explicit register/address spaces and a machine-independent semantic IR could support data-flow and
decompilation. Supporting that would require semantic definitions for every instruction, validation against CPU implementations,
handling flag/cycle/device behavior, and stable consumers. This is a separate proposal, not a required rewrite of Edigen.

## Scripting

The initial stable surface is a bounded line-oriented STEP/RUN/ASSERT/CAPTURE runner, exposed through automation's
--debug-script option. Each step waits for CPU completion and checks its resulting state. RUN has an explicit instruction
limit. Failed operations return an error to automation. This provides repeatable experiments without a language runtime.
Assess a general scripting engine after these operations have stable asynchronous cancellation/ownership semantics; UI
and script execution must not race for the same CPU.

## Portable memory and metadata

Export a captured logical bank at an explicit base with processor/layout/image metadata and user labels, comments,
bookmarks and code/data classifications. Common addresses retain physical bank zero. Imported annotations must match
the complete emuStudio capture identity. The files have no external application dependency. They do not transfer source
maps, runtime traces, bank-switch semantics or patched binaries. Comparison captures are independent of annotation identity.

## Debugger workspace

Inspection belongs to the existing Debugger tab. The instruction-aligned listing remains the primary view, with a
resizable inspection pane beneath it. CPU status/registers, watches and peripheral-device selection share the right
column in that order. Selecting an inspection address does not alter the PC or CPU bank. Memory is a byte view rather
than a second speculative instruction listing. History remains read-only; its controls must not imply reverse execution.

## Validation

Application, 8080/Z80, byte-memory, Spectrum bus/ULA and emuLib suites passed with the local composite build.
Behavior tests cover masked/endian search and rescans, bank identity/common memory, stale source mappings, atomic annotation
imports, persistent rules, guest/debugger access separation, bounded history, flow metadata, register edit notifications and
Swing navigation/pinning. User/developer/root documentation builds and
emuLib Javadoc passed. A packaged headless 8080 experiment compiled MVI/STA, ran to address 5, asserted A/memory, and captured
64 KiB; its deliberately failing assertion returned exit status 1.

An ad hoc Java 21 NOP benchmark compared the original dispatch body with disabled-probe dispatch using warmed method handles
and median batches of one million instructions. 8080 measured 5.04 versus 5.75 ns/instruction; Z80 measured 12.63 versus 12.44.
These are local microbenchmark observations, not general throughput guarantees. Disabled engines retain the original memory
reference and allocate no instruction/register events. Enabled recording has intentionally greater overhead.

