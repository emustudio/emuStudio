# Memory and metadata export

In the Debugger tab, open **Annotations** and choose **Export memory and metadata**.
The selected annotation image bank is exported as `memory-bank-N.bin`, together
with `memory-bank-N.properties`. The binary contains the captured bytes; it does
not include later live-memory changes or refreshes of the comparison baseline.

The UTF-8 Java properties file contains the annotation format version, complete
image identity, processor identifier, logical bank, base address, byte size,
common-memory boundary and binary filename. Entries store their physical bank,
address, label, comment, bookmark flag and code/data classification. Common
addresses use physical bank zero. Metadata addresses remain offsets in the
captured image; the base field describes placement of the exported binary.

Annotation **Import** accepts this metadata only for the matching captured image
and memory layout. Keep `image.identity` and layout information unchanged.
Compiler source maps, recorded execution, live machine state and bank-switch
behavior are not part of this format.
