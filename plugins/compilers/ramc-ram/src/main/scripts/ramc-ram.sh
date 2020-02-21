#!/bin/bash

java -cp "compilers/ramc-ram.jar:mem/ram-mem.jar:lib/*" net.sf.emustudio.ram.compiler.impl.Main "$@"
