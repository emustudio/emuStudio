#!/bin/bash

java -cp "compilers/raspc-rasp.jar:mem/rasp-mem.jar:lib/*" net.sf.net.emustudio.rasp.compiler.Main "$@"
