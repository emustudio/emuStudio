#!/bin/bash

java -cp "compilers/raspc-rasp.jar:mem/rasp-mem.jar:lib/*" net.sf.emustudio.rasp.compiler.Main "$@"
