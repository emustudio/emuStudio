# Debugging assessments for #454

## Reverse execution

Implemented instruction history stores before/after registers and guest access values. It cannot reconstruct a complete
machine: attempted writes may target ROM, reads can have device effects, and bus timing advances devices independently.
Restoring just PC/registers/RAM would omit host tape/audio/file/socket state and deterministic external-input replay,
producing an inconsistent machine.

History remains read-only. Move PC Back changes only the program counter; it leaves registers, memory and devices
unchanged. Whole-memory checkpoint capture and restoration are not supported.

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

The initial stable surface is a bounded line-oriented STEP/RUN/ASSERT runner, exposed through automation's
--debug-script option. Each step waits for CPU completion and checks its resulting state. RUN has an explicit instruction
limit. Failed operations return an error to automation. This provides repeatable experiments without a language runtime.
Assess a general scripting engine after these operations have stable asynchronous cancellation/ownership semantics; UI
and script execution must not race for the same CPU.

## Live memory and user metadata

Memory inspection, byte/integer/text search, string discovery and flow decoding read the paused live memory context.
Workers retain derived addresses, display strings or instruction metadata and reject results after session, content or
memory revisions change. The debugger has no memory-image capture, comparison, restoration or binary snapshot export.
Compiler input text and instruction-recording events retain their existing provenance.

Native memory windows retain program-image loading, editing and erasing; memory-dump actions and RAM/RASP snapshot
models are removed. Z80 machine-state `.sna`/`.z80` loading and its CPU/RAM restoration path are also removed.
Compiler-produced program files, HEX/BIN loaders, tape formats and SSEM's final automation output remain available.

Annotation metadata persists independently of memory cells. Its stable identity is a SHA-256 digest of CPU title,
normalized computer-configuration URI and memory layout. Reopening that configuration preserves annotations without
reading or hashing memory. Loading another program with the same layout retains the metadata; users keep its labels
appropriate to the loaded program. Import requires matching configured-machine/layout identity. Former memory-content
identities and memory-snapshot exports are unsupported, with no compatibility migration or deletion of user files.
Metadata export transfers labels, comments and code/data classifications at CPU-visible addresses. Compiler mappings and runtime traces remain separate.

## Debugger workspace

Inspection belongs to the existing Debugger tab. Instructions and Memory share one full-height table; Symbols,
Annotations, Breakpoints, References and History provide additional views. CPU status/registers, watches and
peripheral-device selection remain in the right column. Selecting an inspection address does not alter the PC. Memory is a byte view rather than a second speculative instruction listing. History remains read-only; its
controls must not imply reverse execution.

After a successful compilation, source caret, bookmark and editor-history navigation follow mapped addresses. Selecting
an instruction, memory byte or compiled symbol follows its source position without switching the outer workspace tab
or closing an editor split. Explicit debugger and symbol visits join the existing source-editor navigation history
from #453; automatic following adds no source-history entries. The table context menu's Pin inspection stops automatic
source/PC following while allowing explicit debugger
navigation. A breakpoint hit inspects its address. Breakpoints have only enabled/disabled state; they do not hold the
inspection address during later steps. Manual view pinning remains independent of breakpoint settings.

Source synchronization uses compiled source text, include closure, annotations and CPU-visible addresses.
Changed open buffers or saved files, failed compilations and replaced annotations cannot reuse old mappings.
Recompile to establish new mappings. Symbols include source-only declarations from included files; ambiguous addresses
remain unmapped. Only proven instruction starts align the disassembly; data and operand bytes do not create speculative
instruction starts. Existing sidecar annotations alone do not establish source-text provenance.

The context row shows the inspected address, following/pinned state and source-map status. Changed sources
disable symbol interaction and explain that recompilation is required; editor compile outcomes remain visible beside the
compiler output. Symbols can be filtered by name, source file or address. Constants and macros are marked source-only;
labels whose address is no longer available are distinguished from those declarations.

The instruction table uses an independent PC triangle, theme selection and keyboard focus border. Valid instructions
start at the top near memory boundaries. Memory strings are optional, while labels and comments share the available
width. Execution breakpoints use the CPU-visible address only;
conditions and hit counts remain advanced options. The breakpoint dialog accepts only a CPU address and Set/Unset actions.
The caller gates dialog creation and its toolbar action on the CPU's `DebugCPU` execution hooks;
the dialog itself handles only breakpoint state and address validation.

History lists changed registers first in a stable order and can reveal unchanged values. Its label context comes from
current annotations, not a reconstructed historical source map. Flow nodes distinguish selection from the live PC and
support keyboard inspection without changing execution state. Split-editor headers identify file, side and active pane;
search shows the target pane, and file replacement previews include surrounding code.

Find opens a single search row; Replace or its toolbar toggle reveals replacement controls. File search remains available
in either mode. Healthy source-map status is available in the compile-status tooltip; stale-source and other actionable
messages stay visible. Memory's live string scan includes the optional String column. Annotation editing address,
filter and actions share a row, and History keeps one read-only warning above the recorded values.

## Persistent instruction recording (#457)

The existing `DebugCPU` events supply one recording path for interactive History and automation. Events preserve a
sequence number, execution-time opcode/disassembly text, before/after registers, CPU-visible addresses, guest accesses and
completion status. The host owns the versioned file format, bounded recent buffer, continuous writer, offline reader
and text export. CPU-specific printers and their settings are removed.

Continuous output uses synchronous buffered writes on the execution thread. This keeps memory bounded and applies
backpressure rather than silently dropping events. Clean closure flushes the file and writes a completion marker;
readers distinguish incomplete files and do not declare completeness until they reach that marker. Offline inspection
uses bounded pages and the captured values instead of current source annotations or memory. Filters and previous-write
navigation operate within the retained recent buffer or displayed page. Repeated-row folding affects presentation only.

Automation's `--record-instructions FILE` attaches the same recorder after optional compilation/reset and before the
first instruction, including `--no-gui` and `--debug-script` runs. Recording remains opt-in. Reverse execution,
full-machine checkpoints, deterministic input replay, trace comparison and bus/cycle tracing remain separate work.

## Validation

### ProgramIndex (#454)

The application owns one `debugger.index.ProgramIndex` per configured machine/layout. This class provides the query
and publication seam; plugin objects never depend on application classes. Source navigation, user annotation editing,
execution-rule presence and the References graph use this index. Persistence, expression evaluation, hit counters,
source-rule binding and analysis scheduling remain in their adapters. User annotation import checks the configured
machine/layout identity instead of a captured memory-content hash. Compiler owner 2 can replace its mappings/labels without
removing user metadata (owners 1 and 3) or other owners.

An immutable layout validates CPU-visible cell addresses. Addresses count cells,
including Integer and RAM instruction cells. `CPU.getAddressSpaceSize()` supplies the logical address capacity, including before a sparse program is loaded.
`MemoryContext.getSize()` retains the provider-defined size of its current storage. Primitive address tables hold IDs into immutable payload/posting pools.
Each table has an 8 MiB dense-table budget; larger layouts use bounded two-level page directories with lazy pages and
uniform-page compression. Bitmap word tables use the smaller word-address domain.
Mapping coverage uses an endpoint sweep and persistent primitive posting trees, sharing unchanged branches instead of
copying nested active sets or expanding ranges into per-cell references. Annotation ranges and per-document/revision source
spans use array-backed centered interval trees. Annotation queries cover starts and individual cells; label search remains
a linear user-metadata adapter operation. The graph uses dense node IDs and incoming/outgoing primitive CSR adjacency,
including known targets without decoded payloads. Predecessors include every explicit edge kind; call sites filter CALL.
`FlowGraph` also owns the paginator's primitive sequential-boundary storage; production `CallFlow` is removed.
Its boundary store accepts only proven cell coordinates. Decoder calls and discovery orchestration stay in
`PaginatingDisassembler.BoundaryDecoder`, and mutable pagination discoveries never enter published immutable snapshots.
`debugger.InstructionIndex` exposes an immutable CPU-addressed instruction view. The table model and paginator see logical
addresses and exclusive instruction ends. View identity changes on image, content or memory revisions, invalidating cached
pages and discovered boundaries; breakpoint-only edits retain the view. Plugins report observable wiring changes through
memory notifications or the session mutation seam. Missing/stale coverage or a CPU address beyond indexed memory uses the existing
logical decoder fallback. The view does not freeze that live decoder; the paused/execution lifecycle remains external.
The small centered `IntervalTable` remains: SDK ordered maps do not index overlapping/nested intervals, while disjoint
range maps/merged range sets would lose annotation identities. Replacing it requires equivalent overlap/multiplicity and
allocation behavior, not merely an ordered predecessor lookup.

One atomically published immutable root contains content, breakpoint and memory revisions. `snapshot()` returns that
root directly. Content edits compare content plus memory revisions; breakpoint edits compare only their own revision,
sharing the latest unrelated state on publication. Same-part stale edits fail, failed/closed edits publish nothing,
and old snapshots retain their original meaning. Unchanged specialized indexes are shared. A metadata edit rebuilds
its affected index; this first version does not patch interval trees or CSR incrementally.

Compiler positions are points, not generated extents. The source adapter therefore publishes exactly one generated cell
per mapping. It uses the exact captured token span when available, otherwise one valid UTF-16 code unit. It never fills
an address gap, guesses an instruction/line extent, clamps EOF/invalid coordinates, or identifies a file by basename.
Bundled ANTLR compiler positions use one-based lines and zero-based code-point columns; editor tokens/offsets use UTF-16.
Compilers declare their column unit through `getSourcePositionColumnUnit()`; unknown conventions produce diagnostics
instead of address bindings. The adapter converts against captured raw text, including supplementary characters, CRLF and tabs.
Unsupported positions are excluded and reported. Compiler publication runs after compilation's CPU reset/mutation exit,
retaining the compiler-provided CPU addresses. Existing source-file freshness checks remain in the navigation adapter.

`memoryChanged()` makes mappings historical and current graph queries unavailable. The session invokes it before/after
machine mutations, before execution and on observable memory writes. Notifications disabled during execution therefore
cannot leave a graph current. Observed writes advance the index before waiting for the session monitor. Source rules
resolve current mappings into address targets while paused; execution uses those already resolved targets without
rebinding from historical mappings. Actual notified writes, compilation, editor rule refreshes and image replacement
discard those bindings. Conditional/global/source and watchpoint evaluation remain adapter work; execution rules use
one address bitmap and rule postings at each portable instruction boundary. Execution/source rules and read/write access
rules use CPU-visible addresses. Source bindings deduplicate resolved addresses.
The application owns all execution breakpoints, including enabled state and persistence. `DebugSession` publishes
them into `ProgramIndex` and decides whether an instruction should stop through `DebugCPU.Listener.beforeInstruction`.
CPU plugins invoke the hook and honor its result; `CPU` and `AbstractCPU` expose no breakpoint store or CRUD methods.
The application supplies the breakpoint table column. The CPU's custom columns describe instructions only.
An enabled breakpoint attaches the debug probe; removing or disabling the last rule detaches it when no recording is active.
Debugger settings use version 3; older settings are archived by the existing incompatible-version handling.

Flow analysis uses the CPU's disassembler with its plugin-provided memory wiring. Decoders and disassemblers read logical
addresses through the current mapping supplied by the connected plugins. Analysis runs off Swing's event thread while paused, checks generation and index
identity/revisions, and rejects late results after changes/disposal. It retains derived metadata rather than copied cells.
An immutable index snapshot does not freeze live memory.
Raw paused writes made with notifications explicitly disabled, or changes through exported raw arrays, remain outside
the notification contract: callers must use the session mutation seam or explicitly invalidate the index. Global
invalidation and paused rebuilding are deliberate v1 choices; no per-instruction tracing is added for invalidation.

The instruction table retains discovered boundaries and materializes visible row addresses once per page. Cache identity
includes image/layout, content/memory revisions and the current CPU mapping; edits to any instruction cell invalidate cached
boundaries. Current indexed instruction lengths can serve pagination without another decode. Table selection and repeated
column reads reuse the materialized addresses. This addresses the redundant predecessor decoding reproduced in
`/tmp/emustudio-perf-20261103/analysis.md`; it does not establish the cause of a whole-host freeze.
Repeated CPU notifications coalesce into one pending paused refresh; controller executors/latches retain their existing
ordering. Live searches and static discovery use paused lifecycle guards and revision checks; their results become
unavailable after memory or metadata changes.

Benchmarks live in the isolated `application/src/jmh` source set; JMH never enters production runtime dependencies.
`./gradlew :application:jmh -PjmhArgs='...'` accepts normal JMH arguments. Point/predecessor baselines freeze the actual
pre-change implementations at `184f8d5a4`; interval/source/bitmap scan oracles are explicitly identified as such. Reused
callbacks and varying inputs measure query allocation separately from caller-created capturing lambdas. Full rebuild,
single-entry rebuild and root publication are separate operations. `:application:indexFootprint` uses a benchmark-only
instrumentation agent to count reachable index metadata; this is not a heap-dominator retained-size measurement. Benchmark results are workload/environment evidence, not a universal speed claim.

#### Live-memory verification on 2026-11-03

The composite `build :emuLib:test :application:jmhClasses :application:indexBenchmarkParity` passed with JDK 21 and
the Java 11 project toolchains: 4,501 emuStudio tests (zero failures, 57 skipped), including 781 application tests
(one existing skip), and 427 emuLib tests (zero failures, 21 skipped). All 41 index benchmark parity datasets passed.
The final build log is `/tmp/emustudio-live-memory-full.log`.

Coverage includes metadata-only startup with no memory-cell reads, live mapped-memory inspection, invalidation after
operand writes, original-revision result guards, cancellation/disposal and concurrent machine/metadata changes. Blocked
live reads and decoder calls do not hold the session monitor or block paused-state checks on Swing's event thread.
A deterministic blocked-read test closes the session before completion and verifies that neither results nor the
inspection-ready callback are published.
Native compiler program serialization/loading still passes its tests after removing copied-memory transfer models.
All 53 packaged JARs were inspected: removed snapshot models, dump actions, snapshot loader and capture/export classes
are absent. Website lint/build and generated-output checks passed for all 14 affected pages; see
`/tmp/emustudio-live-memory-site-final.log`. These are automated/virtual Swing checks; native desktop interaction was not run.

#### Historical verification on 2026-11-03

The checks and measurements in this subsection predate removal of memory captures and their copied-memory analysis
setup. They record the earlier ProgramIndex implementation and benchmark fixtures, not verification of the current
live-only inspection workflow. Raw timings remain useful for the index algorithms; current integration checks must
be run against the live-memory implementation.


The composite build used JDK 21.0.11-amzn, local emuLib/cpu-testsuite/Edigen/Edigen Gradle plugin checkouts, and Temurin
11.0.32.1 for the application Swing tests. The ProgramIndex composite `build :emuLib:test :application:jmhClasses
:application:indexFootprint :application:indexBenchmarkParity` passed in 42 seconds: 4,545 emuStudio tests (zero failures,
57 skipped), including 797 application tests (one existing skip), and 427 emuLib tests (zero failures, 21 skipped).
Two stale breakpoint-manager tests also failed in the unchanged `184f8d5a4` baseline; their expectations and native CPU
mock were updated with the related breakpoint UI changes. No obsolete production behavior was restored.
The subsequent dialog capability cleanup passed all 29 dialog, emulator-panel and debugger-panel tests with no skips;
`/tmp/program-index-breakpoint-caller-gate.log` records that focused run. Unsupported access is tested at the caller,
while native and portable toolbar support and dialog address validation retain their existing coverage.
Merging the facade and implementation into one final `ProgramIndex` class subsequently passed all 797 application tests
(one existing skip), Java 11 strict compilation, JMH compilation and all 41 benchmark parity datasets. See
`/tmp/program-index-class-merge.log`. Timing matrices below predate this structural cleanup and were not rerun for it.

Focused tests cover the CPU-address-only dialog, absence of breakpoint-driven
inspection pinning, and portable execution breakpoints on CPUs that expose no memory context.
The final UI proof uses Cacio virtual Swing, not a native desktop session:

| Observable behavior | Current evidence |
| --- | --- |
| Setting/removing a breakpoint leaves inspection alone | `DebuggerPanelTest.setAndUnsetRequestsOnlyEditTheBreakpoint` |
| CPU without a memory context still supports indexed execution breakpoints | `DebugSessionTest.memorylessPortableCpuUsesIndexedAddressBreakpointsAndResumeSkipping` |
| Breakpoint hits follow the next PC unless manually pinned | `DebuggerPanelTest.breakpointHitsFollowTheNextPcUnlessTheUserManuallyPinsInspection` |
| One logical PC resolves different indexed lengths after a new graph revision | `DebugTableModelImplTest.instructionViewChangesLengthsAtTheSamePcAfterPublication` |
| Mutating an operand cell discards indexed coverage and refreshes decoder boundaries | `DebugTableModelImplTest.revisionChangeInvalidatesOperandCellAndFallbackWithoutUiNotification` |
| Breakpoint-only edits retain instruction pages | `DebugTableModelImplTest.breakpointOnlyPublicationKeepsMaterializedInstructionPage` |

The historical benchmark parity check passed all 41 datasets. Current workloads use single, clustered and sparse CPU
address spaces; the string-key comparison model also uses CPU addresses. The historical timings below predate this change
and do not measure the current implementation. Dedicated `ThreadMXBean` checks measured exactly zero allocated bytes
over 10,000 warmed query batches in both dense and large sparse layouts; callbacks and query values were reused.
Concurrency tests use barriers/latches, including same-part conflicts, independent publications, memory races and
the former image-adoption races. The 50k nested-mapping test passes with a 128 MiB heap.

These are local automated and virtual Swing results. Native desktop interactions and the reported whole-computer freeze
were not profiled. Source navigation still validates saved inputs with synchronous file reads and compares editor text;
the index does not remove that separate EDT cost. External paused raw-array writes or notification-disabled writes
require explicit mutation/invalidation, as described above.

JMH 1.37 ran on Temurin 11.0.32.1, Linux aarch64, Apple Blizzard-M2-Max reported by `lscpu` (12 logical CPUs; no affinity
pinning). Runs use one benchmark thread, two forks, two 200 ms warmup iterations and three 200 ms measurement iterations,
with `-prof gc`. Short iterations make these comparative local observations rather than stable capacity guarantees.
The initial point matrix used the JVM's default heap; subsequent matrices cap forks at 512 MiB.

| --- | ---: | ---: | ---: | ---: |
| Hit, 1 entry | 10.39 ns | 9.25 ns | 24 B/op | approximately 0 B/op |
| Hit, 1k entries | 17.90 ns | 11.02 ns | 24 B/op | approximately 0 B/op |
| Hit, 50k entries | 29.60 ns | 20.25 ns | 48 B/op | approximately 0 B/op |
| Miss, 50k entries | 14.61 ns | 4.80 ns | 48 B/op | approximately 0 B/op |

These are final raw-core results with 512 MiB forks. The initial default-heap run measured the tiny indexed hit at
12.33 ns versus 10.98 ns old, illustrating sensitivity at this scale; the actual production adapter is measured separately
below. Direct tables also have a retained-memory cost:
one dense annotation has 263,872 reachable indexed bytes versus 424 in the old map; 50k annotations have 13,179,808
versus 9,716,472 bytes. Adding 50k source mappings brings the combined index to 30,795,040 bytes (there is no equivalent
old source-range index to compare). A 10k-node sparse graph with listing has 9,211,888 reachable bytes versus 2,267,712
for the old graph; compact sparse pages reduced its original 50,091,896-byte result. A 10k-node dense graph has
1,370,000 versus 2,278,696 bytes. These figures include reachable immutable payloads and exclude the shared memory image.

The historical metadata matrix completed 156 cases (13 operations, four metadata counts, three layouts), before unused
annotation-range overlap and exact-label queries were removed. At 50k dense entries,
point covering measured 248.6 ns, interval overlap 405.1 ns versus the independent scan oracle's 76,584 ns, and reverse
source overlap 4,253 ns versus its scan oracle's 101,903 ns. Snapshot acquisition measured 2.75 ns. Empty content and
breakpoint publication measured 17.2 and 13.3 ns, allocating 128 and 88 bytes per new root/edit respectively. These scan
oracles are not claimed to be previous production paths.

Rebuilding remains a real cost: a single annotation edit at 50k dense entries measured 39.2 ms and 46.2 MB allocated;
a mapping edit measured 36.1 ms and 67.2 MB; full rich-content rebuild measured 71.1 ms and 117.8 MB. Unchanged indexes
are shared, but the affected index is rebuilt rather than surgically patched. Large bulk metadata edits should remain
batched. Profiler allocation on multi-microsecond queries includes small background/profiler noise; the separate warmed
allocation test isolates zero allocation inside the core queries.

After merging graph storage, all 54 graph benchmark cases passed. At 10k dense nodes, incoming CSR queries measured
14.10 ns (99.9% CI 13.96–14.24), compared with the frozen old full-edge scan's 15,172 ns (14,620–15,724). The new primitive
query allocates approximately zero profiler bytes; the old list-producing scan measured 83.64 B/op. Full graph construction
measured 7.33 ms and 11.29 MB allocated for the indexed path versus 0.568 ms and 2.454 MB for the old representation.
This pays construction cost for repeated queries; it is not a graph-build speed improvement.

The address-only breakpoint matrix passed 72 cases across zero/one/1k/10k rules and three address-layout fixtures. At 10k rules in a dense layout, presence hit/miss measured 6.92/3.49 ns and candidate visitation
9.67 ns, with approximately zero profiler allocation. Empty/one-rule hit checks measured 4.74/6.90 ns. Rebuilding one rule
within a 10k-rule set took 1.94 ms and 3.76 MB allocated; full breakpoint construction took 2.07 ms and 4.09 MB. The
presence scan is an independent oracle, not a previous production path with an equivalent presence-only contract.

The 48-case pagination matrix uses the real generated Z80 decoder, 65,536 NOP cells, PC `0x8000`, four page sizes
(21/32/64/128), and absent/built flow. At 21 rows with absent flow, repeated row-location batches measured 0.198 µs
versus the frozen old model's 39,292 µs; repeated table-cell batches measured 10.19 µs versus 118,879 µs. The new repeated
row path reads no instruction cells after its page is populated. Initial page fill is still expensive without indexed
coverage: 12,908 µs versus 45,471 µs old; with built coverage the new fill measured 185 µs. These synthetic results isolate
the repeated predecessor-decoding problem; they do not measure a complete UI interaction or explain every desktop freeze.
Page-fill cases use single-shot timing; per-invocation model setup is outside timed work but included in GC allocation
metrics. The CPU proxy boxes its PC, and rendered table strings allocate, so this harness does not assert zero allocation
for table operations. Raw core-query allocation is verified separately.

The matched production annotation-adapter matrix passed 32 cases. It exposes a separate latency tradeoff obscured by
raw-core timings: dense hits at one entry measured 14.52 ns versus 10.42 ns old; at 50k entries, 43.74 ns versus
27.92 ns old. Those hits avoid the old path's approximately 24 B/op allocation, but are slower. Misses at 50k measured
8.45 ns versus 13.65 ns old. The adapter's immutable projection and thread-local visitor are warmed before measurement;
rebuilding that projection after a content revision is excluded. Neither these hot queries nor raw-core measurements
establish end-to-end desktop latency.

Full build, current tests, parity and footprint evidence: `/tmp/program-index-logical-view-final.log`; focused logical-view
tests: `/tmp/program-index-logical-view-focused.log` (119 tests, zero failures, one skip). Unchanged baseline test
evidence: `/tmp/program-index-baseline-tests.log`. Raw metadata and graph measurements are in
`/tmp/program-index-metadata.json`, `/tmp/program-index-graph.json`, `/tmp/program-index-breakpoints.json` and
`/tmp/program-index-pagination.json` and `/tmp/program-index-adapter.json`, with matching
`.log` files. Final raw point results: `/tmp/program-index-point-final.json` and `/tmp/program-index-point-final.log`
(32 cases). All six final matrices contain 394 measurements. Initial default-heap point results remain in
`/tmp/program-index-point.json` and `/tmp/program-index-point.log`. Reproduce the final point matrix with:

```sh
./gradlew -PemuLibPath=../emuLib :application:jmh \
  -PjmhArgs='ProgramIndexPointBenchmark.(oldPointHit|indexedPointHit|oldPointMiss|indexedPointMiss) -p entries=1,1000,10000,50000 -p layout=single,clustered -wi 2 -i 3 -w 200ms -r 200ms -f 2 -prof gc -foe true -jvmArgsAppend=-Xmx512m -rf json -rff /tmp/program-index-point-final.json'
```

### Earlier debugger validation (historical)

Application, 8080/Z80, byte-memory, Spectrum bus/ULA and emuLib suites passed with the local composite build.
Behavior tests covered the former masked/endian search and rescan implementation, stale source mappings, atomic annotation
imports, persistent rules, guest/debugger access separation, bounded history, flow metadata, register edit notifications and
Swing navigation/pinning. User/developer/root documentation builds and
emuLib Javadoc passed. A packaged headless 8080 experiment compiled MVI/STA, ran to address 5 and asserted A/memory;
its deliberately failing assertion returned exit status 1.

An ad hoc Java 21 NOP benchmark compared the original dispatch body with disabled-probe dispatch using warmed method handles
and median batches of one million instructions. 8080 measured 5.04 versus 5.75 ns/instruction; Z80 measured 12.63 versus 12.44.
These are local microbenchmark observations, not general throughput guarantees. Disabled engines retain the original memory
reference and allocate no instruction/register events. Enabled recording has intentionally greater overhead.

