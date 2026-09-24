# ADR-013: Paper tape control context

## Status
Accepted

## Context
The paper tape reader and punch need direct CPU port access, GUI file control, and control from the SIMH pseudo-device.

## Decision
Keep file ownership and the port protocol inside `88-ptr-ptp`. Expose a narrow `PaperTapeContext` with attach,
detach, rewind, and observable position operations. Consumers depend on this device API instead of its implementation.

## Consequences
The paper tape device remains the only filesystem adapter for tape images. SIMH integration can control it without
duplicating file or port logic, while consumers must compile against the `88-ptr-ptp` API.
