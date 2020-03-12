#!/bin/bash

java -cp "compilers/raspc-rasp.jar:mem/rasp-mem.jar:lib/*" net.emustudio.plugins.compilers.raspc.Main "$@"
